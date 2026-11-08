/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.spy;

import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Last-online times saved by this client.
 *
 * <p>Recorded from exact statuses (online / offline with a time) and from live activity the
 * client already treats as "online" for users with a hidden last seen: incoming messages,
 * typing and reading our messages. Shown instead of "last seen recently / within a week /
 * within a month" when the user's real time is hidden. Secret-chat typing counts too.
 */
public final class LastSeen {

    /** Skip disk writes when the stored value is newer than this many seconds ago. */
    private static final int WRITE_THROTTLE = 60;

    @SuppressWarnings("unchecked")
    private static final ConcurrentHashMap<Long, Integer>[] caches = new ConcurrentHashMap[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] loading = new boolean[UserConfig.MAX_ACCOUNT_COUNT];

    private LastSeen() {
    }

    public static boolean isEnabled(int account) {
        return SpyExtrasConfig.get(account, SpyExtrasConfig.KEY_SAVE_LAST_SEEN);
    }

    /** Cache for an account, or null while it is still loading from disk. */
    private static ConcurrentHashMap<Long, Integer> cache(int account) {
        ConcurrentHashMap<Long, Integer> c = caches[account];
        if (c == null) {
            AndroidUtilities.runOnUIThread(() -> startLoad(account));
        }
        return c;
    }

    private static void startLoad(int account) {
        if (caches[account] != null || loading[account]) {
            return;
        }
        loading[account] = true;
        AyulessStore.getInstance(account).loadLastSeen((HashMap<Long, Integer> stored) -> {
            ConcurrentHashMap<Long, Integer> c = new ConcurrentHashMap<>(stored);
            caches[account] = c;
            loading[account] = false;
        });
    }

    /** Records that a user was online at the given unix time. Safe to call from any thread. */
    public static void record(int account, long userId, int time) {
        if (userId <= 0 || time <= 0 || !isEnabled(account) || SpyExceptions.isExcepted(account, userId)) {
            return;
        }
        ConcurrentHashMap<Long, Integer> c = cache(account);
        if (c == null) {
            return;
        }
        Integer old = c.get(userId);
        if (old != null && old >= time) {
            return;
        }
        c.put(userId, time);
        if (old == null || time - old >= WRITE_THROTTLE) {
            AyulessStore.getInstance(account).saveLastSeen(userId, time);
        }
    }

    /** "last seen ... (saved locally)" for a user with a hidden status, or null if unknown. */
    public static String formatHidden(int account, long userId) {
        if (!isEnabled(account) || SpyExceptions.isExcepted(account, userId)) {
            return null;
        }
        ConcurrentHashMap<Long, Integer> c = cache(account);
        Integer time = c == null ? null : c.get(userId);
        if (time == null) {
            return null;
        }
        return LocaleController.formatString(R.string.AyulessSavedLocally, LocaleController.formatDateOnline(time, null));
    }

    /** Drops the in-memory cache (after the database was cleared). */
    public static void reset(int account) {
        caches[account] = null;
    }
}
