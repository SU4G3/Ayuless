/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.appearance;

import android.graphics.Canvas;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

import org.ayuless.antidelete.DeletedCache;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColoredImageSpan;

/** How messages kept by anti-delete look in a chat. */
public final class DeletedStyle {

    /** Opacity of a deleted message when "translucent deleted messages" is on. */
    private static final float DELETED_ALPHA = 0.55f;
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
        return decorateTimeLabel(time);
    }

    /** Shared by real messages and the live settings preview. */
    public static CharSequence decorateTimeLabel(CharSequence time) {
        if (AppearanceConfig.customMarks()) {
            String prefix = AppearanceConfig.deletedMarkText();
            return TextUtils.isEmpty(prefix) ? time : TextUtils.concat(prefix, " ", time);
        }
        DeletedMarkIcon icon = AppearanceConfig.markIcon();
        if (icon == DeletedMarkIcon.NONE) return time;
        SpannableString mark = new SpannableString("\uFFFC ");
        ColoredImageSpan span = new ColoredImageSpan(icon.drawable);
        if (Theme.chat_timePaint != null) {
            span.setRelativeSize(Theme.chat_timePaint.getFontMetricsInt());
        } else {
            span.setSize(org.telegram.messenger.AndroidUtilities.dp(12));
        }
        int color = AppearanceConfig.markColor();
        if (color != AppearanceConfig.COLOR_DEFAULT) span.setOverrideColor(color);
        mark.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return TextUtils.concat(mark, time);
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
