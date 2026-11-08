/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.edits;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** Per-account edit history switch. Off by default; the settings UI comes in issue #11. */
public final class EditsConfig {

    private static final String KEY_ENABLED = "enabled";

    private EditsConfig() {
    }

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_edits_" + account, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(int account) {
        return ApplicationLoader.applicationContext != null && prefs(account).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(int account, boolean enabled) {
        prefs(account).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
