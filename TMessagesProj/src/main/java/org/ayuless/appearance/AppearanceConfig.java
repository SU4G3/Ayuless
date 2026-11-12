/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.appearance;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** App-wide look of Ayuless features (not per account). */
public final class AppearanceConfig {

    public static final String KEY_CUSTOM_MARKS = "customMarks";
    public static final String KEY_DELETED_TEXT = "deletedMarkText";
    public static final String KEY_EDITED_TEXT = "editedMarkText";

    public static boolean customMarks() {
        return ready() && prefs().getBoolean(KEY_CUSTOM_MARKS, false);
    }

    public static String deletedMarkText() {
        return ready() ? prefs().getString(KEY_DELETED_TEXT, "🧹") : "🧹";
    }

    public static String editedMarkText() {
        String fallback = org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.EditedMessage);
        return ready() ? prefs().getString(KEY_EDITED_TEXT, fallback) : fallback;
    }

    public static void setCustomMarks(boolean enabled) {
        prefs().edit().putBoolean(KEY_CUSTOM_MARKS, enabled).apply();
    }

    public static void setMarkText(String key, String value) {
        if (!KEY_DELETED_TEXT.equals(key) && !KEY_EDITED_TEXT.equals(key)) throw new IllegalArgumentException(key);
        SharedPreferences.Editor editor = prefs().edit();
        if (value == null) editor.remove(key); else editor.putString(key, value);
        editor.apply();
    }
    public static final String KEY_TRANSLUCENT_DELETED = "translucentDeleted";
    public static final String KEY_MARK_EMOJI = "deletedMarkEmoji";
    public static final String KEY_MARK_COLOR = "deletedMarkColor";
    public static final String KEY_GHOST_IN_DIALOGS = "ghostInDialogs";

    /** Mark color 0 means "same as the message time". */
    public static final int COLOR_DEFAULT = 0;
    /** Palette offered in settings; the first entry is the default (time color). */
    public static final int[] PALETTE = {
            COLOR_DEFAULT,
            0xFF9E9E9E, // grey
            0xFFE53935, // red
            0xFFD81B60, // crimson
            0xFFEC407A, // pink
            0xFF8E24AA, // purple
            0xFF1E88E5, // blue
            0xFF43A047, // green
            0xFFFB8C00, // orange
    };

    private AppearanceConfig() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_appearance", Context.MODE_PRIVATE);
    }

    private static boolean ready() {
        return ApplicationLoader.applicationContext != null;
    }

    public static boolean translucentDeleted() {
        return !ready() || prefs().getBoolean(KEY_TRANSLUCENT_DELETED, true);
    }

    public static void setTranslucentDeleted(boolean value) {
        prefs().edit().putBoolean(KEY_TRANSLUCENT_DELETED, value).apply();
    }

    /** One emoji, or an empty string to hide the mark. Replaces the old icon/text preference. */
    public static String markEmoji() {
        return ready() ? prefs().getString(KEY_MARK_EMOJI, "🗑️") : "🗑️";
    }

    public static void setMarkEmoji(String emoji) {
        prefs().edit().putString(KEY_MARK_EMOJI, emoji).putBoolean(KEY_CUSTOM_MARKS, false).apply();
    }

    public static int markColor() {
        return ready() ? prefs().getInt(KEY_MARK_COLOR, COLOR_DEFAULT) : COLOR_DEFAULT;
    }

    public static void setMarkColor(int color) {
        prefs().edit().putInt(KEY_MARK_COLOR, color).apply();
    }

    /** Ghost button in the chat list action bar. On by default. */
    public static boolean ghostInDialogs() {
        return !ready() || prefs().getBoolean(KEY_GHOST_IN_DIALOGS, true);
    }

    public static void setGhostInDialogs(boolean value) {
        prefs().edit().putBoolean(KEY_GHOST_IN_DIALOGS, value).apply();
    }
}
