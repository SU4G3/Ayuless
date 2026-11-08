/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.spy;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** Per-account switches of Spy > Additional. Everything is off by default. */
public final class SpyExtrasConfig {

    public static final String KEY_SAVE_READ_DATE = "saveReadDate";
    public static final String KEY_SAVE_LAST_SEEN = "saveLastSeen";

    private SpyExtrasConfig() {
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_spy_extras_" + account, Context.MODE_PRIVATE);
    }

    public static boolean get(int account, String key) {
        return ApplicationLoader.applicationContext != null && prefs(account).getBoolean(key, false);
    }

    public static void set(int account, String key, boolean value) {
        prefs(account).edit().putBoolean(key, value).apply();
    }
}
