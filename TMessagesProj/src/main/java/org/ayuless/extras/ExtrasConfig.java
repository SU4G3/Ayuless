/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.extras;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** App-wide "useful features" toggles (Ayuless > Customization). Sponsored-message hiding and background work default on; other extras default off. */
public final class ExtrasConfig {

    public static final String KEY_READ_UNTIL = "readUntil";
    public static final String KEY_LOCAL_RESEND = "localResend";
    public static final String KEY_KEEP_OWN_DELETES = "keepOwnDeletes";
    public static final String KEY_DELETE_DATE = "deleteDate";
    public static final String KEY_GHOST_IN_MENU = "ghostInMenu";
    public static final String KEY_GHOST_SHORTCUT = "ghostShortcut";
    public static final String KEY_HIDE_SPONSORED = "hideSponsored";
    public static final String KEY_LOCAL_PREMIUM = "localPremium";
    public static final String KEY_WORK_IN_BACKGROUND = "workInBackground";

    private ExtrasConfig() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_extras", Context.MODE_PRIVATE);
    }

    public static boolean get(String key) {
        return ApplicationLoader.applicationContext != null && prefs().getBoolean(key, KEY_HIDE_SPONSORED.equals(key) || KEY_WORK_IN_BACKGROUND.equals(key));
    }

    public static void set(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    public static boolean hideSponsored() {
        return get(KEY_HIDE_SPONSORED);
    }

    public static boolean localPremium() {
        return get(KEY_LOCAL_PREMIUM);
    }

    public static boolean workInBackground() {
        return get(KEY_WORK_IN_BACKGROUND);
    }
}
