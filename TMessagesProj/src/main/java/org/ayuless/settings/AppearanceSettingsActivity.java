/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.text.TextUtils;
import android.view.View;

import org.ayuless.appearance.AppearanceConfig;
import org.ayuless.extras.ExtrasConfig;
import org.ayuless.extras.KeepAliveService;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

/** Ayuless > Customization: how deleted messages look in chats. */
public class AppearanceSettingsActivity extends AyulessSettingsFragment {

    private static final int ID_TRANSLUCENT = 1;
    private static final int ID_MARK_STYLE = 2;
    private static final int ID_GHOST_IN_DIALOGS = 4;
    private static final int ID_HIDE_SPONSORED = 10;
    private static final int ID_LOCAL_PREMIUM = 11;
    private static final int ID_WORK_IN_BACKGROUND = 12;

    private DeletedMessagePreview preview;
    private DeletedMarkColors colors;

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.AyulessCustomization);
    }

    @Override
    protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (preview == null && getParentActivity() != null) {
            preview = new DeletedMessagePreview(getParentActivity(), getResourceProvider());
            colors = new DeletedMarkColors(getParentActivity(), getResourceProvider(), this::update);
        }
        if (preview != null) {
            preview.refresh();
            items.add(UItem.asCustomShadow(preview, 150));
        }
        items.add(UItem.asHeader(getString(R.string.AyulessDeletedHeader)));
        items.add(UItem.asCheck(ID_TRANSLUCENT, getString(R.string.AyulessTranslucentDeleted)).setChecked(AppearanceConfig.translucentDeleted()));
        items.add(UItem.asButton(ID_MARK_STYLE, R.drawable.ayuless_solar_msg_delete, getString(R.string.AyulessDeletedMarkStyle), TextUtils.isEmpty(AppearanceConfig.markEmoji()) ? getString(R.string.AyulessSpyExceptionsNone) : AppearanceConfig.markEmoji()));
        if (colors != null) items.add(UItem.asCustom(colors));
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.AyulessUsefulHeader)));
        items.add(UItem.asCheck(ID_GHOST_IN_DIALOGS, getString(R.string.AyulessGhostInDialogs)).setChecked(AppearanceConfig.ghostInDialogs()));
        items.add(UItem.asCheck(ID_HIDE_SPONSORED, getString(R.string.AyulessHideSponsored)).setChecked(ExtrasConfig.hideSponsored()));
        items.add(UItem.asCheck(ID_LOCAL_PREMIUM, getString(R.string.AyulessLocalPremium)).setChecked(ExtrasConfig.localPremium()));
        items.add(UItem.asCheck(ID_WORK_IN_BACKGROUND, getString(R.string.AyulessWorkInBackground)).setChecked(ExtrasConfig.workInBackground()));
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
            case ID_HIDE_SPONSORED:
                ExtrasConfig.set(ExtrasConfig.KEY_HIDE_SPONSORED, !ExtrasConfig.hideSponsored());
                update();
                break;
            case ID_LOCAL_PREMIUM:
                ExtrasConfig.set(ExtrasConfig.KEY_LOCAL_PREMIUM, !ExtrasConfig.localPremium());
                update();
                break;
            case ID_WORK_IN_BACKGROUND:
                ExtrasConfig.set(ExtrasConfig.KEY_WORK_IN_BACKGROUND, !ExtrasConfig.workInBackground());
                KeepAliveService.apply();
                update();
                break;
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void update() {
        if (preview != null) preview.refresh();
        if (colors != null) colors.refresh();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void showStylePicker() {
        if (getParentActivity() == null) return;
        String[] emojis = {"", "🗑️", "❌", "👻", "🚫", "💀", "✖️", "🫥"};
        CharSequence[] labels = new CharSequence[emojis.length + 1];
        for (int i = 0; i < emojis.length; i++) {
            labels[i] = i == 0 ? getString(R.string.AyulessSpyExceptionsNone) : emojis[i];
        }
        labels[emojis.length] = getString(R.string.AyulessChooseEmoji);
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(getString(R.string.AyulessDeletedMarkStyle))
                .setItems(labels, (dialog, which) -> {
                    if (which == emojis.length) {
                        showCustomEmoji();
                    } else {
                        AppearanceConfig.setMarkEmoji(emojis[which]);
                        update();
                    }
                }).create());
    }

    private void showCustomEmoji() {
        EditTextBoldCursor input = new EditTextBoldCursor(getParentActivity());
        input.setTextSize(24);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
        input.setSingleLine(true);
        input.setText(AppearanceConfig.markEmoji());
        input.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        AlertDialog dialog = new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(getString(R.string.AyulessChooseEmoji))
                .setView(input)
                .setPositiveButton(getString(R.string.Save), null)
                .setNegativeButton(getString(R.string.Cancel), null).create();
        showDialog(dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty() && (!Emoji.fullyConsistsOfEmojis(value) || Emoji.parseEmojis(value).size() != 1)) {
                input.setError(getString(R.string.AyulessChooseOneEmoji));
                return;
            }
            AppearanceConfig.setMarkEmoji(value);
            update();
            dialog.dismiss();
        });
        input.requestFocus();
        AndroidUtilities.showKeyboard(input);
    }
}
