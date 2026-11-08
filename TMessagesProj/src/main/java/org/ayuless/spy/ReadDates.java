/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.spy;

import org.ayuless.storage.AyulessStore;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

/**
 * Locally saved read dates of outgoing messages in private chats.
 *
 * <p>When the other side reads our messages, the server sends the new read pointer
 * (updateReadHistoryOutbox). If that update arrives live, its time is close to the real read
 * time, so it is stored for the range of messages it newly covers. The date is shown in the
 * message menu when the server does not provide one (hidden read dates or an old message).
 *
 * <p>Private chats with users and secret chats are recorded. Secret chats report reads by
 * message date (updateEncryptedMessagesRead), so their ranges hold dates instead of ids.
 */
public final class ReadDates {

    /** Updates older than this (seconds) are catch-up, not live, and are not recorded. */
    private static final int LIVE_WINDOW = 30;

    private ReadDates() {
    }

    public static boolean isEnabled(int account) {
        return SpyExtrasConfig.get(account, SpyExtrasConfig.KEY_SAVE_READ_DATE);
    }

    /**
     * Called for updateReadHistoryOutbox of a private dialog.
     *
     * @param previousMax read pointer before this update
     * @param maxId       new read pointer
     * @param updateDate  date of the update batch
     * @param now         current server time
     */
    public static void onReadOutbox(int account, long userId, int previousMax, int maxId, int updateDate, int now) {
        if (userId <= 0 || maxId <= previousMax || !isEnabled(account) || SpyExceptions.isExcepted(account, userId)) {
            return;
        }
        if (Math.abs(now - updateDate) >= LIVE_WINDOW) {
            return;
        }
        AyulessStore.getInstance(account).saveReadOutbox(userId, previousMax, maxId, updateDate);
    }

    /**
     * Called for updateEncryptedMessagesRead: our outgoing messages with date &lt;= maxDate in
     * the secret chat were read at readDate. The lower bound is the newest outgoing message
     * that was already marked read before this update.
     */
    public static void onEncryptedReadOutbox(int account, int chatId, int maxDate, int readDate, int now) {
        if (maxDate <= 0 || readDate <= 0 || !isEnabled(account) || Math.abs(now - readDate) >= LIVE_WINDOW) {
            return;
        }
        long dialogId = DialogObject.makeEncryptedDialogId(chatId);
        if (SpyExceptions.isExcepted(account, dialogId)) {
            return;
        }
        TLRPC.EncryptedChat chat = MessagesController.getInstance(account).getEncryptedChatDB(chatId, true);
        if (chat != null && SpyExceptions.isExcepted(account, chat.user_id)) {
            return;
        }
        // Runs on the storage queue before upstream marks these messages as read.
        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            int lowerDate = 0;
            SQLiteCursor cursor = null;
            try {
                cursor = storage.getDatabase().queryFinalized("SELECT MAX(date) FROM messages_v2 WHERE uid = ? AND out = 1 AND (read_state & 1) != 0", dialogId);
                if (cursor.next() && !cursor.isNull(0)) {
                    lowerDate = cursor.intValue(0);
                }
            } catch (Exception e) {
                FileLog.e(e);
                return;
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            if (lowerDate < maxDate) {
                AyulessStore.getInstance(account).saveReadOutbox(dialogId, lowerDate, maxDate, readDate);
            }
        });
    }

    /** @param msgId message id, or message date in secret chats */
    public static void get(int account, long dialogId, int msgId, AyulessStore.Callback<Integer> callback) {
        AyulessStore.getInstance(account).getReadDate(dialogId, msgId, callback);
    }

    public static String formatLocal(int date) {
        return LocaleController.formatString(R.string.AyulessSavedLocally, LocaleController.formatPmSeenDate(date));
    }
}
