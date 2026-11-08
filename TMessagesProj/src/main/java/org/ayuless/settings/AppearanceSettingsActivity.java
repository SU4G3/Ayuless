/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import org.ayuless.appearance.AppearanceConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Ayuless > Customization: how deleted messages look in chats. */
public class AppearanceSettingsActivity extends UniversalFragment {

    private static final int ID_TRANSLUCENT = 1;
    private static final int ID_MARK_STYLE = 2;
    private static final int ID_MARK_COLOR = 3;
    private static final int ID_GHOST_IN_DIALOGS = 4;

    /** Names for AppearanceConfig.PALETTE, same order. */
    private static final int[] COLOR_NAMES = {
            R.string.AyulessColorDefault,
            R.string.AyulessColorGrey,
            R.string.AyulessColorRed,
            R.string.AyulessColorCrimson,
            R.string.AyulessColorPink,
            R.string.AyulessColorPurple,
            R.string.AyulessColorBlue,
            R.string.AyulessColorGreen,
            R.string.AyulessColorOrange,
    };

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.AyulessCustomization);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.AyulessDeletedHeader)));
        items.add(UItem.asCheck(ID_TRANSLUCENT, getString(R.string.AyulessTranslucentDeleted)).setChecked(AppearanceConfig.translucentDeleted()));
        items.add(UItem.asButton(ID_MARK_STYLE, R.drawable.ayuless_solar_msg_delete, getString(R.string.AyulessDeletedMarkStyle), styleName(AppearanceConfig.markStyle())));
        items.add(UItem.asButton(ID_MARK_COLOR, getString(R.string.AyulessDeletedMarkColor), colorLabel(indexOfColor(AppearanceConfig.markColor()))));
        items.add(UItem.asShadow(getString(R.string.AyulessDeletedAppearanceInfo)));
        items.add(UItem.asHeader(getString(R.string.AyulessUsefulHeader)));
        items.add(UItem.asCheck(ID_GHOST_IN_DIALOGS, getString(R.string.AyulessGhostInDialogs)).setChecked(AppearanceConfig.ghostInDialogs()));
        items.add(UItem.asShadow(getString(R.string.AyulessGhostInDialogsInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_TRANSLUCENT:
                AppearanceConfig.setTranslucentDeleted(!AppearanceConfig.translucentDeleted());
                update();
                break;
            case ID_GHOST_IN_DIALOGS:
                AppearanceConfig.setGhostInDialogs(!AppearanceConfig.ghostInDialogs());
                update();
                break;
            case ID_MARK_STYLE:
                showStylePicker();
                break;
            case ID_MARK_COLOR:
                showColorPicker();
                break;
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private static String styleName(int style) {
        return getString(style == AppearanceConfig.MARK_TEXT ? R.string.AyulessDeletedMarkText : R.string.AyulessDeletedMarkIcon);
    }

    private static int indexOfColor(int color) {
        for (int i = 0; i < AppearanceConfig.PALETTE.length; i++) {
            if (AppearanceConfig.PALETTE[i] == color) {
                return i;
            }
        }
        return 0;
    }

    /** Color name with a colored dot in front. */
    private CharSequence colorLabel(int index) {
        int color = AppearanceConfig.PALETTE[index];
        if (color == AppearanceConfig.COLOR_DEFAULT) {
            color = Theme.getColor(Theme.key_chat_inTimeText, getResourceProvider());
        }
        SpannableStringBuilder sb = new SpannableStringBuilder("● ");
        sb.setSpan(new ForegroundColorSpan(color), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(getString(COLOR_NAMES[index]));
        return sb;
    }

    private void showStylePicker() {
        if (getParentActivity() == null) return;
        CharSequence[] labels = {styleName(AppearanceConfig.MARK_ICON), styleName(AppearanceConfig.MARK_TEXT)};
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getString(R.string.AyulessDeletedMarkStyle));
        builder.setItems(labels, (dialog, which) -> {
            AppearanceConfig.setMarkStyle(which == 1 ? AppearanceConfig.MARK_TEXT : AppearanceConfig.MARK_ICON);
            update();
        });
        showDialog(builder.create());
    }

    private void showColorPicker() {
        if (getParentActivity() == null) return;
        CharSequence[] labels = new CharSequence[AppearanceConfig.PALETTE.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = colorLabel(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getString(R.string.AyulessDeletedMarkColor));
        builder.setItems(labels, (dialog, which) -> {
            AppearanceConfig.setMarkColor(AppearanceConfig.PALETTE[which]);
            update();
        });
        showDialog(builder.create());
    }
}
