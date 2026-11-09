/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ayuless.appearance.AppearanceConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.LayoutHelper;

/** Accessible inline palette; scrolls on narrow displays and at large font sizes. */
final class DeletedMarkColors extends LinearLayout {
    private static final int[] NAMES = {
            R.string.AyulessColorDefault, R.string.AyulessColorGrey, R.string.AyulessColorRed,
            R.string.AyulessColorCrimson, R.string.AyulessColorPink, R.string.AyulessColorPurple,
            R.string.AyulessColorBlue, R.string.AyulessColorGreen, R.string.AyulessColorOrange
    };
    private final LinearLayout palette;

    DeletedMarkColors(Context context, Theme.ResourcesProvider resources, Runnable onChange) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(21), dp(10), dp(21), dp(6));
        TextView title = new TextView(context);
        title.setText(getString(R.string.AyulessDeletedMarkColor));
        title.setTextSize(16);
        title.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        ThemeBind.textColor(title, Theme.key_windowBackgroundWhiteBlackText, resources);
        addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        palette = new LinearLayout(context);
        scroll.addView(palette);
        addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        for (int i = 0; i < AppearanceConfig.PALETTE.length; i++) {
            final int index = i;
            View circle = new View(context) {
                private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                @Override
                protected void onDraw(Canvas canvas) {
                    int color = AppearanceConfig.PALETTE[index];
                    if (color == AppearanceConfig.COLOR_DEFAULT) color = Theme.getColor(Theme.key_chat_inTimeText, resources);
                    float cx = getWidth() / 2f, cy = getHeight() / 2f;
                    paint.setColor(color);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cx, cy, dp(12), paint);
                    if (isSelected()) {
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(dp(2));
                        canvas.drawCircle(cx, cy, dp(16), paint);
                        paint.setColor(Color.WHITE);
                        canvas.drawLine(cx - dp(5), cy, cx - dp(1), cy + dp(4), paint);
                        canvas.drawLine(cx - dp(1), cy + dp(4), cx + dp(6), cy - dp(4), paint);
                    }
                }
                @Override
                public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(info);
                    info.setClassName("android.widget.RadioButton");
                    info.setCheckable(true);
                    info.setChecked(isSelected());
                }
            };
            circle.setContentDescription(getString(NAMES[i]));
            circle.setFocusable(true);
            circle.setOnClickListener(v -> {
                AppearanceConfig.setMarkColor(AppearanceConfig.PALETTE[index]);
                refresh();
                onChange.run();
            });
            palette.addView(circle, LayoutHelper.createLinear(44, 48));
        }
        refresh();
    }

    void refresh() {
        for (int i = 0; i < palette.getChildCount(); i++) {
            View circle = palette.getChildAt(i);
            circle.setSelected(AppearanceConfig.markColor() == AppearanceConfig.PALETTE[i]);
            circle.invalidate();
        }
    }
}
