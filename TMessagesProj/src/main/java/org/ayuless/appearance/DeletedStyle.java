/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.appearance;

import android.graphics.Canvas;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import org.ayuless.antidelete.DeletedCache;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColoredImageSpan;

/** How messages kept by anti-delete look in a chat. */
public final class DeletedStyle {

    /** Opacity of a deleted message when "translucent deleted messages" is on. */
    private static final float DELETED_ALPHA = 0.55f;
    private static final int TEXT_FALLBACK_COLOR = 0xFF9E9E9E;
    /** Placeholder the icon span is drawn over; the time width is measured from text. */
    private static final String ICON_PLACEHOLDER = "🗑";

    private DeletedStyle() {
    }

    public static boolean isDeleted(int account, MessageObject messageObject) {
        return messageObject != null && messageObject.messageOwner != null && !messageObject.scheduled
                && DeletedCache.isDeleted(account, messageObject.getDialogId(), messageObject.getId());
    }

    /** Prefixes the time label of a deleted message with the configured mark. */
    public static CharSequence decorateTime(int account, MessageObject messageObject, CharSequence time) {
        if (!isDeleted(account, messageObject)) {
            return time;
        }
        final int color = AppearanceConfig.markColor();
        if (AppearanceConfig.markStyle() == AppearanceConfig.MARK_TEXT) {
            SpannableString label = new SpannableString(LocaleController.getString(R.string.AyulessDeletedMark) + " ");
            label.setSpan(new ForegroundColorSpan(color != AppearanceConfig.COLOR_DEFAULT ? color : TEXT_FALLBACK_COLOR),
                    0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return TextUtils.concat(label, time);
        }
        SpannableString icon = new SpannableString(ICON_PLACEHOLDER + " ");
        ColoredImageSpan span = new ColoredImageSpan(R.drawable.ayuless_solar_msg_delete);
        span.setRelativeSize(Theme.chat_timePaint.getFontMetricsInt());
        if (color != AppearanceConfig.COLOR_DEFAULT) {
            span.setOverrideColor(color);
        }
        icon.setSpan(span, 0, ICON_PLACEHOLDER.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return TextUtils.concat(icon, time);
    }

    /**
     * Starts a translucent layer for a deleted message cell.
     *
     * @return the save count to restore after drawing, or -1 if no layer was started.
     */
    public static int beginDraw(int account, MessageObject messageObject, Canvas canvas, int width, int height) {
        if (width <= 0 || height <= 0 || !AppearanceConfig.translucentDeleted() || !isDeleted(account, messageObject)) {
            return -1;
        }
        return canvas.saveLayerAlpha(0, 0, width, height, (int) (255 * DELETED_ALPHA));
    }
}
