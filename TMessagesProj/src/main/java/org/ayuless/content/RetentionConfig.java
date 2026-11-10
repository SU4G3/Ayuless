// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.content;

import org.ayuless.antidelete.AntiDeleteConfig;
import org.ayuless.edits.EditsConfig;
import org.ayuless.spy.SavedAttachments;
import org.ayuless.spy.SpyExceptions;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

/** Retention policy: all message categories follow the main Spy switches and exceptions. */
public final class RetentionConfig {
    public static final String DELETE = "retainDeleted";
    public static final String EDIT = "retainEdits";
    public static final String ATTACHMENT = "retainAttachments";
    private RetentionConfig() {}

    public static boolean timed(TLRPC.Message message) {
        return message.ttl != 0 || message.ttl_period != 0 || message.destroyTime != 0
                || message.destroyTimeMillis != 0 || message.media != null && message.media.ttl_seconds != 0;
    }

    // CLEANROOM: reimplement CR-04 bot capture eligibility
    public static boolean storable(int account, TLRPC.Message message, String purpose) {
        if (message == null || SpyExceptions.isExcepted(account, MessageObject.getDialogId(message))) return false;
        if (DELETE.equals(purpose)) return AntiDeleteConfig.isEnabled(account);
        if (EDIT.equals(purpose)) return EditsConfig.isEnabled(account);
        if (ATTACHMENT.equals(purpose)) return SavedAttachments.isEnabled(account);
        return false;
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
        return (AntiDeleteConfig.isEnabled(account) || SavedAttachments.isEnabled(account))
                && !SpyExceptions.isExcepted(account, dialogId);
    }
}
