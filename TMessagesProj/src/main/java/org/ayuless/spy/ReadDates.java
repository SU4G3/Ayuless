/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.spy;

import org.ayuless.storage.AyulessStore;
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
 * <p>Only private chats with users are recorded. Secret chats use a different update and are
 * never recorded.
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

    public static void get(int account, long dialogId, int msgId, AyulessStore.Callback<Integer> callback) {
        AyulessStore.getInstance(account).getReadDate(dialogId, msgId, callback);
    }

    public static String formatLocal(int date) {
        return LocaleController.formatString(R.string.AyulessSavedLocally, LocaleController.formatPmSeenDate(date));
    }
}
