/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.ghost;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * Ghost mode settings. Everything is off by default.
 *
 * <p>Settings live in profiles: one shared profile ({@link #PROFILE_ALL}) used by every
 * account, and one profile per account. An account uses its own profile only after
 * "use own settings" is turned on for it; otherwise the shared profile applies.
 *
 * <p>The ghost toggles only have effect while the master switch of the active profile is on.
 * "Send silently" is independent of the master switch.
 */
public final class GhostConfig {

    /** Profile id of the settings shared by all accounts. */
    public static final int PROFILE_ALL = -1;

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_HIDE_ONLINE = "hideOnline";
    public static final String KEY_HIDE_READ = "hideRead";
    public static final String KEY_HIDE_TYPING = "hideTyping";
    public static final String KEY_HIDE_STORY_VIEWS = "hideStoryViews";
    public static final String KEY_OFFLINE_AFTER_SEND = "offlineAfterSend";
    public static final String KEY_READ_ON_ACTION = "readOnAction";
    public static final String KEY_SEND_SILENTLY = "sendSilently";
    /** Ask before opening stories whether to view them in ghost mode. On by default. */
    public static final String KEY_STORY_PROMPT = "storyPrompt";
    /** Per-account flag: use the account's own profile instead of the shared one. */
    public static final String KEY_OVERRIDE = "override";

    /** The five toggles behind the master switch, in display order. */
    public static final String[] GHOST_KEYS = {
            KEY_HIDE_READ, KEY_HIDE_STORY_VIEWS, KEY_HIDE_ONLINE, KEY_HIDE_TYPING, KEY_OFFLINE_AFTER_SEND
    };

    private GhostConfig() {
    }

    private static boolean ready() {
        return ApplicationLoader.applicationContext != null;
    }

    /** Raw preferences of a profile (shared profile or one account). */
    public static SharedPreferences profilePrefs(int profile) {
        String name = profile == PROFILE_ALL ? "ayuless_ghost_all" : "ayuless_ghost_" + profile;
        return ApplicationLoader.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * Whether an account uses its own profile. Accounts configured before profiles existed
     * (they already have a stored master switch) keep their own settings.
     */
    public static boolean hasOverride(int account) {
        if (!ready()) {
            return false;
        }
        SharedPreferences p = profilePrefs(account);
        return p.getBoolean(KEY_OVERRIDE, p.contains(KEY_ENABLED));
    }

    public static void setOverride(int account, boolean value) {
        profilePrefs(account).edit().putBoolean(KEY_OVERRIDE, value).apply();
    }

    /** The profile that is active for an account. */
    public static int activeProfile(int account) {
        return hasOverride(account) ? account : PROFILE_ALL;
    }

    // ---- profile level (used by the settings screen) ----

    public static boolean getProfile(int profile, String key) {
        return ready() && profilePrefs(profile).getBoolean(key, false);
    }

    public static boolean getProfile(int profile, String key, boolean def) {
        return ready() ? profilePrefs(profile).getBoolean(key, def) : def;
    }

    public static void setProfile(int profile, String key, boolean value) {
        profilePrefs(profile).edit().putBoolean(key, value).apply();
    }

    /** Number of the five ghost toggles that are on in a profile. */
    public static int countEnabled(int profile) {
        int n = 0;
        for (String key : GHOST_KEYS) {
            if (getProfile(profile, key)) {
                n++;
            }
        }
        return n;
    }

    // ---- account level (used by the request filter) ----

    private static boolean getGhost(int account, String key) {
        if (!ready()) {
            return false;
        }
        SharedPreferences p = profilePrefs(activeProfile(account));
        return p.getBoolean(KEY_ENABLED, false) && p.getBoolean(key, false);
    }

    public static boolean isEnabled(int account) {
        return getProfile(activeProfile(account), KEY_ENABLED);
    }

    public static boolean hideOnline(int account) {
        return getGhost(account, KEY_HIDE_ONLINE);
    }

    public static boolean hideRead(int account) {
        return getGhost(account, KEY_HIDE_READ);
    }

    public static boolean hideTyping(int account) {
        return getGhost(account, KEY_HIDE_TYPING);
    }

    /** Story views are hidden by the settings or by ghost mode chosen in the story prompt. */
    public static boolean hideStoryViews(int account) {
        return hideStoryViewsStored(account) || StoryGhostPrompt.sessionActive(account);
    }

    /** Story views are hidden by the stored settings alone. */
    public static boolean hideStoryViewsStored(int account) {
        return getGhost(account, KEY_HIDE_STORY_VIEWS);
    }

    /** Whether to offer ghost mode before opening stories. Independent of the master switch. */
    public static boolean storyPrompt(int account) {
        return getProfile(activeProfile(account), KEY_STORY_PROMPT, true);
    }

    public static boolean offlineAfterSend(int account) {
        return getGhost(account, KEY_OFFLINE_AFTER_SEND);
    }

    /** Mark a chat as read when sending a message or a reaction there, despite hidden reads. */
    public static boolean readOnAction(int account) {
        return getGhost(account, KEY_HIDE_READ) && getGhost(account, KEY_READ_ON_ACTION);
    }

    /** Send every message without notification sound. Works without the master switch. */
    public static boolean sendSilently(int account) {
        return getProfile(activeProfile(account), KEY_SEND_SILENTLY);
    }
}
