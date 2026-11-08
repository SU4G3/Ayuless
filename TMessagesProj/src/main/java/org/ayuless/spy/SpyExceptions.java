/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.spy;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-account list of dialogs (users, groups, channels) excluded from Spy: deleted messages and
 * edit history are not saved for them. Values are dialog ids (negative for chats).
 */
public final class SpyExceptions {

    private static final String KEY_DIALOGS = "exception_dialogs";

    @SuppressWarnings("unchecked")
    private static final HashSet<Long>[] cache = new HashSet[UserConfig.MAX_ACCOUNT_COUNT];

    private SpyExceptions() {
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_spy_" + account, Context.MODE_PRIVATE);
    }

    private static synchronized HashSet<Long> load(int account) {
        HashSet<Long> set = cache[account];
        if (set == null) {
            set = new HashSet<>();
            if (ApplicationLoader.applicationContext != null) {
                Set<String> stored = prefs(account).getStringSet(KEY_DIALOGS, null);
                if (stored != null) {
                    for (String s : stored) {
                        try {
                            set.add(Long.parseLong(s));
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
            }
            cache[account] = set;
        }
        return set;
    }

    /** True if Spy must not save anything for this dialog. Safe to call from any thread. */
    public static synchronized boolean isExcepted(int account, long dialogId) {
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT || dialogId == 0) {
            return false;
        }
        return load(account).contains(dialogId);
    }

    public static synchronized ArrayList<Long> get(int account) {
        return new ArrayList<>(load(account));
    }

    public static synchronized void set(int account, ArrayList<Long> dialogIds) {
        HashSet<Long> set = new HashSet<>();
        HashSet<String> stored = new HashSet<>();
        if (dialogIds != null) {
            for (Long id : dialogIds) {
                if (id != null && id != 0) {
                    set.add(id);
                    stored.add(String.valueOf(id));
                }
            }
        }
        cache[account] = set;
        prefs(account).edit().putStringSet(KEY_DIALOGS, stored).apply();
    }
}
