/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors */
package org.ayuless.appearance;

import org.telegram.messenger.R;

/** Stable preference values; drawable resource IDs must not be persisted. */
public enum DeletedMarkIcon {
    NONE("none", 0, R.string.AyulessSpyExceptionsNone),
    TRASH("trash", R.drawable.ayuless_solar_msg_delete, R.string.AyulessMarkTrash),
    GHOST("ghost", R.drawable.ayuless_solar_ghost, R.string.AyulessMarkGhost),
    BLOCK("block", R.drawable.ayuless_solar_msg_block, R.string.AyulessMarkBlock),
    EYE("eye", R.drawable.ayuless_solar_eye, R.string.AyulessMarkEye),
    CLOCK("clock", R.drawable.ayuless_solar_clock, R.string.AyulessMarkClock);

    public final String key;
    public final int drawable, label;

    DeletedMarkIcon(String key, int drawable, int label) {
        this.key = key; this.drawable = drawable; this.label = label;
    }

    public static DeletedMarkIcon fromKey(String key) {
        for (DeletedMarkIcon icon : values()) if (icon.key.equals(key)) return icon;
        return TRASH;
    }

    /** Keep hidden marks hidden and map former emoji choices to the closest Solar symbol. */
    public static DeletedMarkIcon fromLegacyEmoji(String emoji) {
        if ("".equals(emoji)) return NONE;
        if ("👻".equals(emoji) || "🫥".equals(emoji)) return GHOST;
        if ("🚫".equals(emoji) || "❌".equals(emoji) || "✖️".equals(emoji) || "✖".equals(emoji)) return BLOCK;
        return TRASH;
    }
}
