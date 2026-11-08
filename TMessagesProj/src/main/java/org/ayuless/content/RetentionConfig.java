// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.content;

import org.ayuless.spy.SpyExtrasConfig;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

/** Account-local opt-ins. Retention and UI restrictions are independent. */
public final class RetentionConfig {
    public static final String DELETE = "retainDeleted";
    public static final String EDIT = "retainEdits";
    public static final String ATTACHMENT = "retainAttachments";
    public static final String KEEP_MEDIA = "keepExpiringMedia";
    public static final String PRIVATE = "unlockPrivate";
    public static final String AUTO_DELETE = "unlockAutoDelete";
    public static final String TTL = "unlockTtl";
    public static final String SECRET = "unlockSecret";
    public static final String STORIES = "unlockStories";

    private RetentionConfig() {}

    public static boolean enabled(int account, String key) {
        return SpyExtrasConfig.get(account, key);
    }

    public static boolean timed(TLRPC.Message message) {
        return message.ttl != 0 || message.ttl_period != 0 || message.destroyTime != 0
                || message.destroyTimeMillis != 0 || message.media != null && message.media.ttl_seconds != 0;
    }

    public static boolean storable(int account, TLRPC.Message message, String purpose) {
        if (message == null) return false;
        if ((DialogObject.isEncryptedDialog(MessageObject.getDialogId(message)) || message instanceof TLRPC.TL_message_secret)
                && !enabled(account, purpose + "Secret")) return false;
        if (timed(message) && !enabled(account, purpose + "Timed")) return false;
        return !(message.action instanceof TLRPC.TL_messageEncryptedAction) || enabled(account, purpose + "Actions");
    }

    public static void clearOpened(int account) {
        org.telegram.messenger.ApplicationLoader.applicationContext
                .getSharedPreferences("ayuless_retained_open_" + account, 0).edit().clear().apply();
    }

    public static boolean wasOpened(int account, TLRPC.Message message) {
        return org.telegram.messenger.ApplicationLoader.applicationContext
                .getSharedPreferences("ayuless_retained_open_" + account, 0)
                .getBoolean(MessageObject.getDialogId(message) + ":" + message.id, false);
    }

    public static void rememberOpen(int account, TLRPC.Message message) {
        if (keepMedia(account, MessageObject.getDialogId(message))) {
            org.telegram.messenger.ApplicationLoader.applicationContext
                    .getSharedPreferences("ayuless_retained_open_" + account, 0).edit()
                    .putBoolean(MessageObject.getDialogId(message) + ":" + message.id, true).apply();
        }
    }

    public static boolean keepMedia(int account, long dialogId) {
        return enabled(account, KEEP_MEDIA) && !org.ayuless.spy.SpyExceptions.isExcepted(account, dialogId);
    }
}
