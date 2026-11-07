/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.ghost;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * Per-account ghost mode settings. Everything is off by default.
 * A toggle only has effect while the master switch is on.
 */
public final class GhostConfig {

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_HIDE_ONLINE = "hideOnline";
    public static final String KEY_HIDE_READ = "hideRead";
    public static final String KEY_HIDE_TYPING = "hideTyping";
    public static final String KEY_HIDE_STORY_VIEWS = "hideStoryViews";
    public static final String KEY_OFFLINE_AFTER_SEND = "offlineAfterSend";

    private GhostConfig() {
    }

    public static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_ghost_" + account, Context.MODE_PRIVATE);
    }

    private static boolean get(int account, String key) {
        if (ApplicationLoader.applicationContext == null) {
            return false;
        }
        SharedPreferences p = prefs(account);
        return p.getBoolean(KEY_ENABLED, false) && p.getBoolean(key, false);
    }

    private static void set(int account, String key, boolean value) {
        prefs(account).edit().putBoolean(key, value).apply();
    }

    public static boolean isEnabled(int account) {
        return ApplicationLoader.applicationContext != null && prefs(account).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(int account, boolean value) {
        set(account, KEY_ENABLED, value);
    }

    public static boolean hideOnline(int account) {
        return get(account, KEY_HIDE_ONLINE);
    }

    public static void setHideOnline(int account, boolean value) {
        set(account, KEY_HIDE_ONLINE, value);
    }

    public static boolean hideRead(int account) {
        return get(account, KEY_HIDE_READ);
    }

    public static void setHideRead(int account, boolean value) {
        set(account, KEY_HIDE_READ, value);
    }

    public static boolean hideTyping(int account) {
        return get(account, KEY_HIDE_TYPING);
    }

    public static void setHideTyping(int account, boolean value) {
        set(account, KEY_HIDE_TYPING, value);
    }

    public static boolean hideStoryViews(int account) {
        return get(account, KEY_HIDE_STORY_VIEWS);
    }

    public static void setHideStoryViews(int account, boolean value) {
        set(account, KEY_HIDE_STORY_VIEWS, value);
    }

    public static boolean offlineAfterSend(int account) {
        return get(account, KEY_OFFLINE_AFTER_SEND);
    }

    public static void setOfflineAfterSend(int account, boolean value) {
        set(account, KEY_OFFLINE_AFTER_SEND, value);
    }
}
