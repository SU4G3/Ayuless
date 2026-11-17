/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.ghost;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.RequestDelegateTimestamp;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Outgoing request filter for ghost mode, called from ConnectionsManager before a
 * request is serialized. Local state (unread counters, typing UI) is untouched:
 * only the network request is dropped or rewritten.
 */
public final class GhostInterceptor {

    /** Error text passed to callbacks of requests that ghost mode dropped. */
    public static final String BLOCKED_ERROR = "AYULESS_GHOST_BLOCKED";

    private static final long OFFLINE_AFTER_SEND_DELAY_MS = 1500;

    /** Requests built by ghost mode itself that must pass the filter once (read on action). */
    private static final Set<TLObject> allowedOnce = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private static final Set<TLObject> explicitOnce = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private GhostInterceptor() {
    }

    /**
     * @return true if the request must not be sent. May rewrite the request in place
     * (account.updateStatus online -> offline) and returns false in that case.
     */
    public static boolean shouldBlock(int account, TLObject req) {
        if (req == null) {
            return false;
        }
        if (explicitOnce.remove(req)) return false;
        if (allowedOnce.remove(req)) {
            // Automatic acknowledgements must still be enabled when the request leaves.
            return GhostConfig.hideRead(account) && !GhostConfig.readOnAction(account);
        }
        if (org.ayuless.extras.SponsoredBlocker.shouldBlock(req)) {
            return true;
        }
        if (isSendRequest(req) && GhostConfig.sendSilently(account)) {
            makeSilent(req);
        }
        if (isSendRequest(req)) {
            DelayedSend.fixOnSend(account, req);
        }
        if (!GhostConfig.anyActive(account)) {
            return false;
        }
        if (req instanceof TL_account.updateStatus) {
            if (GhostConfig.hideOnline(account)) {
                // Keep the request so the caller's state machine still completes,
                // but tell the server we are offline.
                ((TL_account.updateStatus) req).offline = true;
            } else if (!((TL_account.updateStatus) req).offline && GhostConfig.offlineAfterSend(account)) {
                scheduleOffline(account);
            }
            return false;
        }
        if (isReadRequest(req)) {
            return GhostConfig.hideRead(account);
        }
        if (req instanceof TLRPC.TL_messages_setTyping || req instanceof TLRPC.TL_messages_setEncryptedTyping) {
            return GhostConfig.hideTyping(account);
        }
        if (req instanceof TL_stories.TL_stories_readStories || req instanceof TL_stories.TL_stories_incrementStoryViews) {
            return GhostConfig.hideStoryViews(account);
        }
        if (isSendRequest(req) && GhostConfig.offlineAfterSend(account)) {
            scheduleOffline(account);
        }
        return false;
    }

    public static void onSuccessfulRequest(int account, TLObject request) {
        if (!GhostConfig.readOnAction(account)) return;
        if (isSendRequest(request) || request instanceof TLRPC.TL_messages_sendReaction) {
            readOnAction(account, peerOf(request));
        } else if (request instanceof TLRPC.TL_messages_sendEncrypted) {
            readOnEncryptedAction(account, ((TLRPC.TL_messages_sendEncrypted) request).peer);
        } else if (request instanceof TLRPC.TL_messages_sendEncryptedFile) {
            readOnEncryptedAction(account, ((TLRPC.TL_messages_sendEncryptedFile) request).peer);
        }
    }

    /** Explicit message action; only the bounded history request bypasses ghost mode. */
    public static void readThroughMessage(int account, org.telegram.messenger.MessageObject message,
                                          java.util.function.Consumer<Boolean> completion) {
        if (message == null || message.isSending() || message.isSendError()) {
            completion.accept(false);
            return;
        }
        final long dialogId = message.getDialogId();
        final int maxId = message.getId(), maxDate = message.messageOwner.date;
        final boolean encrypted = DialogObject.isEncryptedDialog(dialogId);
        if ((!encrypted && maxId <= 0) || (encrypted && maxDate <= 0)) {
            completion.accept(false);
            return;
        }
        readHistory(account, dialogId, maxId, maxDate, success -> {
            if (!success) { completion.accept(false); return; }
            org.telegram.messenger.MessagesStorage storage = org.telegram.messenger.MessagesStorage.getInstance(account);
            storage.getStorageQueue().postRunnable(() -> {
                int count;
                try {
                    org.telegram.SQLite.SQLiteCursor cursor = storage.getDatabase().queryFinalized(
                            "SELECT COUNT(*) FROM messages_v2 WHERE uid = ? AND out = 0 AND read_state IN(0,2) AND mid "
                                    + (encrypted ? ">= ?" : "<= ?"), dialogId, maxId);
                    try { count = cursor.next() ? cursor.intValue(0) : 0; }
                    finally { cursor.dispose(); }
                } catch (Exception e) {
                    FileLog.e(e);
                    AndroidUtilities.runOnUIThread(() -> completion.accept(false));
                    return;
                }
                // -1 preserves the unread count when no cached unread messages were covered.
                final int countDiff = count == 0 ? -1 : count;
                AndroidUtilities.runOnUIThread(() -> {
                    MessagesController controller = MessagesController.getInstance(account);
                    controller.markDialogAsRead(dialogId, maxId, maxId, maxDate, false, 0, countDiff, true, 0);
                    if (encrypted && GhostConfig.hideRead(account)) {
                        TLRPC.EncryptedChat chat = controller.getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
                        if (chat != null && chat.ttl > 0) {
                            storage.createTaskForSecretChat(chat.id, maxDate,
                                    Math.max(ConnectionsManager.getInstance(account).getCurrentTime(), maxDate), 0, null);
                        }
                    }
                    completion.accept(true);
                });
            });
        });
    }

    private static void readHistory(int account, long dialogId, int maxId, int maxDate,
                                    java.util.function.Consumer<Boolean> completion) {
        MessagesController controller = MessagesController.getInstance(account);
        TLObject request;
        if (DialogObject.isEncryptedDialog(dialogId)) {
            TLRPC.EncryptedChat chat = controller.getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
            if (chat == null || maxDate <= 0) {
                AndroidUtilities.runOnUIThread(() -> completion.accept(false));
                return;
            }
            TLRPC.TL_messages_readEncryptedHistory read = new TLRPC.TL_messages_readEncryptedHistory();
            read.peer = new TLRPC.TL_inputEncryptedChat();
            read.peer.chat_id = chat.id;
            read.peer.access_hash = chat.access_hash;
            read.max_date = maxDate;
            request = read;
        } else {
            TLRPC.InputPeer peer = controller.getInputPeer(dialogId);
            if (peer == null || peer instanceof TLRPC.TL_inputPeerEmpty) {
                AndroidUtilities.runOnUIThread(() -> completion.accept(false));
                return;
            }
            if (peer.channel_id != 0) {
                TLRPC.TL_channels_readHistory read = new TLRPC.TL_channels_readHistory();
                read.channel = controller.getInputChannel(peer.channel_id);
                if (read.channel == null || read.channel.channel_id == 0) {
                    AndroidUtilities.runOnUIThread(() -> completion.accept(false));
                    return;
                }
                read.max_id = maxId;
                request = read;
            } else {
                TLRPC.TL_messages_readHistory read = new TLRPC.TL_messages_readHistory();
                read.peer = peer;
                read.max_id = maxId;
                request = read;
            }
        }
        sendExplicitRead(account, request, completion);
    }

    private static void sendExplicitRead(int account, TLObject request, java.util.function.Consumer<Boolean> completion) {
        explicitOnce.add(request);
        ConnectionsManager.getInstance(account).sendRequest(request, (response, error) -> {
            MessagesController controller = MessagesController.getInstance(account);
            if (error == null && response instanceof TLRPC.TL_messages_affectedMessages) {
                TLRPC.TL_messages_affectedMessages affected = (TLRPC.TL_messages_affectedMessages) response;
                controller.processNewDifferenceParams(-1, affected.pts, -1, affected.pts_count);
            } else if (error == null && response instanceof TLRPC.TL_messages_affectedHistory) {
                TLRPC.TL_messages_affectedHistory affected = (TLRPC.TL_messages_affectedHistory) response;
                controller.processNewDifferenceParams(-1, affected.pts, -1, affected.pts_count);
                if (affected.offset > 0) {
                    sendExplicitRead(account, request, completion);
                    return;
                }
            }
            boolean success = error == null && response != null && !(response instanceof TLRPC.TL_boolFalse);
            AndroidUtilities.runOnUIThread(() -> completion.accept(success));
        });
    }

    /** Same policy for content receipts, encrypted read service packets and local expiry. */
    public static boolean blocksMediaRead(int account) {
        return GhostConfig.hideRead(account);
    }

    public static boolean blocksRead(int account, long dialog) {
        return GhostConfig.hideRead(account);
    }

    public static boolean blocksMediaRead(int account, long dialog) { return blocksRead(account, dialog); }

    private static boolean isReadRequest(TLObject req) {
        return req instanceof TLRPC.TL_messages_readHistory
                || req instanceof TLRPC.TL_channels_readHistory
                || req instanceof TLRPC.TL_messages_readSavedHistory
                || req instanceof TLRPC.TL_messages_readDiscussion
                || req instanceof TLRPC.TL_messages_readMessageContents
                || req instanceof TLRPC.TL_channels_readMessageContents
                || req instanceof TLRPC.TL_messages_readEncryptedHistory
                || req instanceof TLRPC.TL_messages_readMentions
                || req instanceof TLRPC.TL_messages_readReactions
                || req instanceof TLRPC.TL_messages_readPollVotes;
    }

    private static boolean isSendRequest(TLObject req) {
        return req instanceof TLRPC.TL_messages_sendMessage
                || req instanceof TLRPC.TL_messages_sendMedia
                || req instanceof TLRPC.TL_messages_sendMultiMedia
                || req instanceof TLRPC.TL_messages_forwardMessages;
    }

    private static void makeSilent(TLObject req) {
        if (req instanceof TLRPC.TL_messages_sendMessage) {
            ((TLRPC.TL_messages_sendMessage) req).silent = true;
        } else if (req instanceof TLRPC.TL_messages_sendMedia) {
            ((TLRPC.TL_messages_sendMedia) req).silent = true;
        } else if (req instanceof TLRPC.TL_messages_sendMultiMedia) {
            ((TLRPC.TL_messages_sendMultiMedia) req).silent = true;
        } else if (req instanceof TLRPC.TL_messages_forwardMessages) {
            ((TLRPC.TL_messages_forwardMessages) req).silent = true;
        }
    }

    private static TLRPC.InputPeer peerOf(TLObject req) {
        if (req instanceof TLRPC.TL_messages_sendMessage) {
            return ((TLRPC.TL_messages_sendMessage) req).peer;
        } else if (req instanceof TLRPC.TL_messages_sendMedia) {
            return ((TLRPC.TL_messages_sendMedia) req).peer;
        } else if (req instanceof TLRPC.TL_messages_sendMultiMedia) {
            return ((TLRPC.TL_messages_sendMultiMedia) req).peer;
        } else if (req instanceof TLRPC.TL_messages_forwardMessages) {
            return ((TLRPC.TL_messages_forwardMessages) req).to_peer;
        } else if (req instanceof TLRPC.TL_messages_sendReaction) {
            return ((TLRPC.TL_messages_sendReaction) req).peer;
        }
        return null;
    }

    /**
     * Acting in a chat (sending a message or a reaction) reveals activity anyway, so the
     * chat is marked as read on the server up to its last known message. The read request
     * is allowed through the filter exactly once.
     */
    private static void readOnAction(int account, TLRPC.InputPeer peer) {
        if (peer == null || peer instanceof TLRPC.TL_inputPeerEmpty) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (!GhostConfig.readOnAction(account)) {
                return;
            }
            MessagesController controller = MessagesController.getInstance(account);
            long dialogId = DialogObject.getPeerDialogId(peer);
            if (dialogId == 0 || DialogObject.isEncryptedDialog(dialogId)) {
                return;
            }
            TLRPC.Dialog dialog = controller.dialogs_dict.get(dialogId);
            if (dialog == null || dialog.top_message <= 0) {
                return;
            }
            TLObject read;
            if (peer.channel_id != 0) {
                TLRPC.TL_channels_readHistory req = new TLRPC.TL_channels_readHistory();
                req.channel = controller.getInputChannel(-dialogId);
                if (req.channel == null || req.channel.channel_id == 0) return;
                req.max_id = dialog.top_message;
                read = req;
            } else {
                TLRPC.TL_messages_readHistory req = new TLRPC.TL_messages_readHistory();
                req.peer = peer;
                req.max_id = dialog.top_message;
                read = req;
            }
            allowedOnce.add(read);
            ConnectionsManager.getInstance(account).sendRequest(read, null);
        });
    }

    /** Secret-chat history receipts use dates; encrypted service packets must never trigger this. */
    private static void readOnEncryptedAction(int account, TLRPC.TL_inputEncryptedChat peer) {
        if (peer == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (!GhostConfig.readOnAction(account)) {
                return;
            }
            long dialogId = DialogObject.makeEncryptedDialogId(peer.chat_id);
            TLRPC.Dialog dialog = MessagesController.getInstance(account).dialogs_dict.get(dialogId);
            if (dialog == null || dialog.last_message_date <= 0) {
                return;
            }
            TLRPC.TL_messages_readEncryptedHistory read = new TLRPC.TL_messages_readEncryptedHistory();
            read.peer = peer;
            read.max_date = dialog.last_message_date;
            allowedOnce.add(read);
            ConnectionsManager.getInstance(account).sendRequest(read, null);
        });
    }

    /**
     * Sending a message marks the account online on the server. Push an offline status
     * shortly after; updateStatus(offline=true) passes this filter, so there is no loop.
     */
    private static void scheduleOffline(int account) {
        Utilities.stageQueue.postRunnable(() -> {
            TL_account.updateStatus req = new TL_account.updateStatus();
            req.offline = true;
            ConnectionsManager.getInstance(account).sendRequest(req, null);
        }, OFFLINE_AFTER_SEND_DELAY_MS);
    }

    /**
     * Completes a dropped request with an error, on the same queue as real responses.
     * Callers must still be notified: e.g. typing state is only reset in the callback.
     */
    public static void completeBlocked(TLObject req, RequestDelegate onComplete, RequestDelegateTimestamp onCompleteTimestamp) {
        FileLog.d("ayuless ghost: dropped " + req);
        if (onComplete == null && onCompleteTimestamp == null) {
            return;
        }
        Utilities.stageQueue.postRunnable(() -> {
            TLRPC.TL_error error = new TLRPC.TL_error();
            error.code = 400;
            error.text = BLOCKED_ERROR;
            if (onComplete != null) {
                onComplete.run(null, error);
            } else {
                onCompleteTimestamp.run(null, error, 0);
            }
        });
    }
}
