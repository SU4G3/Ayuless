/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.appearance;

import android.graphics.Canvas;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.style.ReplacementSpan;

import org.ayuless.antidelete.DeletedCache;
import org.telegram.messenger.MessageObject;

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
        String emoji = AppearanceConfig.markEmoji();
        if (TextUtils.isEmpty(emoji)) return time;
        SpannableString mark = new SpannableString(emoji + " ");
        mark.setSpan(new EmojiMarkSpan(), 0, emoji.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return TextUtils.concat(mark, time);
    }

    /** Tint the emoji layer, including color fonts, without changing the timestamp color. */
    private static final class EmojiMarkSpan extends ReplacementSpan {
        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            if (fm != null) paint.getFontMetricsInt(fm);
            return (int) Math.ceil(paint.measureText(text, start, end));
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x,
                         int top, int y, int bottom, Paint paint) {
            Paint tint = new Paint();
            int color = AppearanceConfig.markColor();
            tint.setColorFilter(new PorterDuffColorFilter(
                    color == AppearanceConfig.COLOR_DEFAULT ? paint.getColor() : color, PorterDuff.Mode.SRC_IN));
            float width = paint.measureText(text, start, end);
            int save = canvas.saveLayer(x, top, x + width, bottom, tint);
            canvas.drawText(text, start, end, x, y, paint);
            canvas.restoreToCount(save);
        }
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
