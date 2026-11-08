/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.ghost;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.HashMap;

/** Client-side hold for secret chats, which cannot use server scheduling. */
public final class EncryptedDelayedSend {
    private static final String DATE_KEY = "ayulessEncryptedSendAt";
    @SuppressWarnings("unchecked")
    private static final HashMap<Integer, Runnable>[] pending = new HashMap[UserConfig.MAX_ACCOUNT_COUNT];

    private EncryptedDelayedSend() {
    }

    /** Called before the outgoing message is stored, so unsent recovery preserves its deadline. */
    public static void prepare(int account, TLRPC.Message message, boolean retry) {
        if (retry || !DialogObject.isEncryptedDialog(message.dialog_id) || !GhostConfig.delayedSend(account)) {
            return;
        }
        boolean media = message.media != null && !(message.media instanceof TLRPC.TL_messageMediaEmpty)
                && !(message.media instanceof TLRPC.TL_messageMediaWebPage);
        if (message.params == null) {
            message.params = new HashMap<>();
        }
        message.params.put(DATE_KEY, Integer.toString(DelayedSend.compute(account, message.dialog_id, media)));
    }

    /** Called on the UI thread, before assigning secret-chat sequence numbers or encrypting. */
    public static boolean hold(int account, TLRPC.Message message, Runnable send) {
        if (message == null || message.params == null || !message.params.containsKey(DATE_KEY)
                || message.action instanceof TLRPC.TL_messageEncryptedAction) {
            return false;
        }
        int date;
        try {
            date = Integer.parseInt(message.params.get(DATE_KEY));
        } catch (NumberFormatException e) {
            return false;
        }
        if (Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
            AndroidUtilities.runOnUIThread(() -> {
                if (!hold(account, message, send)) {
                    send.run();
                }
            });
            return true;
        }
        HashMap<Integer, Runnable> queue = pending[account];
        if (queue == null) {
            pending[account] = queue = new HashMap<>();
        }
        if (queue.containsKey(message.id)) {
            return true;
        }
        long delay = (date - (long) ConnectionsManager.getInstance(account).getCurrentTime()) * 1000;
        if (delay <= 0) {
            return false;
        }
        Runnable task = () -> {
            pending[account].remove(message.id);
            if (message.send_state == MessageObject.MESSAGE_SEND_STATE_SENDING) {
                send.run();
            }
        };
        queue.put(message.id, task);
        AndroidUtilities.runOnUIThread(task, delay);
        return true;
    }

    public static void cancel(int account, int messageId) {
        if (pending[account] != null) {
            Runnable task = pending[account].remove(messageId);
            if (task != null) {
                AndroidUtilities.cancelRunOnUIThread(task);
            }
        }
    }

    public static void clear(int account) {
        if (pending[account] != null) {
            for (Runnable task : pending[account].values()) {
                AndroidUtilities.cancelRunOnUIThread(task);
            }
            pending[account].clear();
        }
    }
}
