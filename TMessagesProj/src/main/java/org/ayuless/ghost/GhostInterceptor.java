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
        if (allowedOnce.remove(req)) {
            return false;
        }
        if (org.ayuless.extras.SponsoredBlocker.shouldBlock(req)) {
            return true;
        }
        if (isSendRequest(req) && GhostConfig.sendSilently(account)) {
            makeSilent(req);
        }
        if (!GhostConfig.isEnabled(account)) {
            return false;
        }
        if ((isSendRequest(req) || req instanceof TLRPC.TL_messages_sendReaction) && GhostConfig.readOnAction(account)) {
            readOnAction(account, peerOf(req));
        }
        if (req instanceof TL_account.updateStatus) {
            if (GhostConfig.hideOnline(account)) {
                // Keep the request so the caller's state machine still completes,
                // but tell the server we are offline.
                ((TL_account.updateStatus) req).offline = true;
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

    private static boolean isReadRequest(TLObject req) {
        return req instanceof TLRPC.TL_messages_readHistory
                || req instanceof TLRPC.TL_channels_readHistory
                || req instanceof TLRPC.TL_messages_readSavedHistory
                || req instanceof TLRPC.TL_messages_readDiscussion
                || req instanceof TLRPC.TL_messages_readMessageContents
                || req instanceof TLRPC.TL_channels_readMessageContents
                || req instanceof TLRPC.TL_messages_readEncryptedHistory;
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
            if (peer instanceof TLRPC.TL_inputPeerChannel) {
                TLRPC.TL_channels_readHistory req = new TLRPC.TL_channels_readHistory();
                req.channel = controller.getInputChannel(-dialogId);
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
