/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ayuless.appearance.AppearanceConfig;
import org.ayuless.appearance.DeletedStyle;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.LayoutHelper;

/** A local sample bubble. Uses the same mark renderer and opacity as actual deleted messages. */
final class DeletedMessagePreview extends FrameLayout {
    private final LinearLayout bubble;
    private final TextView time;

    DeletedMessagePreview(Context context, Theme.ResourcesProvider resources) {
        super(context);
        bubble = new LinearLayout(context) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Path tail = new Path();

            @Override
            protected void onDraw(Canvas canvas) {
                paint.setColor(Theme.getColor(Theme.key_chat_inBubble, resources));
                canvas.drawRoundRect(dp(6), 0, getWidth(), getHeight(), dp(16), dp(16), paint);
                tail.reset();
                tail.moveTo(dp(12), getHeight() - dp(20));
                tail.lineTo(0, getHeight());
                tail.lineTo(dp(20), getHeight() - dp(2));
                tail.close();
                canvas.drawPath(tail, paint);
                super.onDraw(canvas);
            }
        };
        bubble.setWillNotDraw(false);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(22), dp(12), dp(16), dp(10));
        TextView message = new TextView(context);
        message.setTextSize(16);
        message.setText(getString(R.string.AyulessDeletedPreview));
        ThemeBind.textColor(message, Theme.key_chat_messageTextIn, resources);
        bubble.addView(message, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        time = new TextView(context);
        time.setTextSize(12);
        time.setGravity(Gravity.RIGHT);
        ThemeBind.textColor(time, Theme.key_chat_inTimeText, resources);
        bubble.addView(time, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));
        addView(bubble, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL, 20, 0, 36, 0));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription(getString(R.string.AyulessDeletedPreview));
    }

    void refresh() {
        bubble.setAlpha(AppearanceConfig.translucentDeleted() ? 0.55f : 1f);
        time.setText(DeletedStyle.decorateTimeLabel("12:34"));
        bubble.invalidate();
    }
}
