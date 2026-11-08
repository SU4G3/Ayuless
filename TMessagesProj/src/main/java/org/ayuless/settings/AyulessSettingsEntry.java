/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

/** The branded, single-line entry in Telegram's main settings list. */
public final class AyulessSettingsEntry extends FrameLayout {
    private final TextView title;

    private AyulessSettingsEntry(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        boolean rtl = LocaleController.isRTL;
        int start = rtl ? Gravity.RIGHT : Gravity.LEFT;
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.ayuless_settings_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(logo, LayoutHelper.createFrame(28, 28, start | Gravity.CENTER_VERTICAL,
                rtl ? 0 : 18, 0, rtl ? 18 : 0, 0));

        title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setGravity(start | Gravity.CENTER_VERTICAL);
        ThemeBind.textColor(title, Theme.key_windowBackgroundWhiteBlackText, resourcesProvider);
        addView(title, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,
                Gravity.TOP, rtl ? 20 : 66, 0, rtl ? 66 : 20, 0));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(dp(50), MeasureSpec.EXACTLY));
    }

    public static class Factory extends UItem.UItemFactory<AyulessSettingsEntry> {
        static { setup(new Factory()); }

        @Override
        public AyulessSettingsEntry createView(Context context, RecyclerListView listView,
                                              int account, int classGuid,
                                              Theme.ResourcesProvider resourcesProvider) {
            return new AyulessSettingsEntry(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter,
                             UniversalRecyclerView listView) {
            ((AyulessSettingsEntry) view).title.setText(item.text);
        }

        public static UItem of(int id, CharSequence title) {
            UItem item = UItem.ofFactory(Factory.class);
            item.id = id;
            item.text = title;
            return item;
        }
    }
}
