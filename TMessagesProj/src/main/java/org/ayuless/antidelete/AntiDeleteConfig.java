/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.antidelete;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** Per-account anti-delete switch. On by default; stored choices are preserved. */
public final class AntiDeleteConfig {

    private static final String KEY_ENABLED = "enabled";

    private AntiDeleteConfig() {
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_antidelete_" + account, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(int account) {
        return ApplicationLoader.applicationContext != null && prefs(account).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(int account, boolean enabled) {
        prefs(account).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
