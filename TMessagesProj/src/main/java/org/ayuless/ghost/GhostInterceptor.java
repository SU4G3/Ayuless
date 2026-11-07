/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.ghost;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.RequestDelegateTimestamp;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

/**
 * Outgoing request filter for ghost mode, called from ConnectionsManager before a
 * request is serialized. Local state (unread counters, typing UI) is untouched:
 * only the network request is dropped or rewritten.
 */
public final class GhostInterceptor {

    /** Error text passed to callbacks of requests that ghost mode dropped. */
    public static final String BLOCKED_ERROR = "AYULESS_GHOST_BLOCKED";

    private static final long OFFLINE_AFTER_SEND_DELAY_MS = 1500;

    private GhostInterceptor() {
    }

    /**
     * @return true if the request must not be sent. May rewrite the request in place
     * (account.updateStatus online -> offline) and returns false in that case.
     */
    public static boolean shouldBlock(int account, TLObject req) {
        if (req == null || !GhostConfig.isEnabled(account)) {
            return false;
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
