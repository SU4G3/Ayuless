/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

/** Telegram row behavior with the measured Ayuless settings divider geometry. */
final class AyulessPreferenceCell extends TextCell {
    private final Paint dividerPaint = new Paint();
    private final Theme.ResourcesProvider resourcesProvider;
    private boolean divider;

    private AyulessPreferenceCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, 21, false, false, resourcesProvider);
        this.resourcesProvider = resourcesProvider;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // The base cell shifts values 6 dp toward the edge; settings use a full 21 dp inset.
        valueTextView.offsetLeftAndRight(dp(LocaleController.isRTL ? 6 : -6));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (divider) {
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + 1);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (divider) {
            dividerPaint.setColor(Theme.getColor(Theme.key_divider, resourcesProvider));
            int inset = dp(imageView.getVisibility() == VISIBLE ? 68 : 20);
            canvas.drawLine(LocaleController.isRTL ? 0 : inset, getHeight() - 1,
                    getWidth() - (LocaleController.isRTL ? inset : 0), getHeight() - 1, dividerPaint);
        }
    }

    public static class Factory extends UItem.UItemFactory<AyulessPreferenceCell> {
        static { setup(new Factory()); }

        @Override
        public AyulessPreferenceCell createView(Context context, RecyclerListView listView,
                                                int account, int classGuid,
                                                Theme.ResourcesProvider resourcesProvider) {
            return new AyulessPreferenceCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter,
                             UniversalRecyclerView listView) {
            AyulessPreferenceCell cell = (AyulessPreferenceCell) view;
            if (item.iconResId == 0) {
                if (TextUtils.isEmpty(item.textValue)) {
                    cell.setText(item.text, false);
                } else {
                    cell.setTextAndValue(item.text, item.textValue, false);
                }
            } else if (TextUtils.isEmpty(item.textValue)) {
                cell.setTextAndIcon(item.text, item.iconResId, false);
            } else {
                cell.setTextAndValueAndIcon(item.text, item.textValue, item.iconResId, false);
            }
            cell.setColors(item.red ? Theme.key_text_RedBold : item.accent
                            ? Theme.key_windowBackgroundWhiteBlueText4 : Theme.key_windowBackgroundWhiteGrayIcon,
                    item.red ? Theme.key_text_RedRegular : item.accent
                            ? Theme.key_windowBackgroundWhiteBlueText4 : Theme.key_windowBackgroundWhiteBlackText);
            cell.setEnabled(item.enabled, false);
            cell.divider = divider;
            cell.setWillNotDraw(!divider);
            cell.requestLayout();
        }

        static UItem of(UItem source) {
            UItem item = UItem.ofFactory(Factory.class);
            item.id = source.id;
            item.text = source.text;
            item.textValue = source.textValue;
            item.iconResId = source.iconResId;
            item.enabled = source.enabled;
            item.accent = source.accent;
            item.red = source.red;
            item.hideDivider = source.hideDivider;
            return item;
        }
    }
}
