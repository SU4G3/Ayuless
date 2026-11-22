/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.ayuless.appearance.AppearanceConfig;
import org.ayuless.appearance.DeletedMarkIcon;
import org.ayuless.extras.ExtrasConfig;
import org.ayuless.extras.KeepAliveService;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
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
    private static final int ID_PROFILE_IDENTITY = 13;

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
        items.add(UItem.asButton(ID_MARK_STYLE, R.drawable.ayuless_solar_msg_delete, getString(R.string.AyulessDeletedMarkStyle), getString(AppearanceConfig.markIcon().label)));
        if (colors != null) items.add(UItem.asCustom(colors));
        items.add(UItem.asShadow(getString(R.string.AyulessDeletedAppearanceInfo)));
        items.add(UItem.asHeader(getString(R.string.AyulessUsefulHeader)));
        items.add(UItem.asCheck(ID_PROFILE_IDENTITY, getString(R.string.AyulessShowProfileIdentity)).setChecked(ProfileMapConfig.showProfileIdentity()));
        items.add(UItem.asShadow(getString(R.string.AyulessRegistrationInfo)));
        items.add(UItem.asCheck(ID_GHOST_IN_DIALOGS, getString(R.string.AyulessGhostInDialogs)).setChecked(AppearanceConfig.ghostInDialogs()));
        items.add(UItem.asShadow(getString(R.string.AyulessGhostInDialogsInfo)));
        items.add(UItem.asCheck(ID_HIDE_SPONSORED, getString(R.string.AyulessHideSponsored)).setChecked(ExtrasConfig.hideSponsored()));
        items.add(UItem.asCheck(ID_LOCAL_PREMIUM, getString(R.string.AyulessLocalPremium)).setChecked(ExtrasConfig.localPremium()));
        items.add(UItem.asCheck(ID_WORK_IN_BACKGROUND, getString(R.string.AyulessWorkInBackground)).setChecked(ExtrasConfig.workInBackground()));
        items.add(UItem.asShadow(getString(R.string.AyulessUsefulFeaturesInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_PROFILE_IDENTITY:
                ProfileMapConfig.setShowProfileIdentity(!ProfileMapConfig.showProfileIdentity());
                update();
                break;
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
                org.ayuless.extras.LocalPremium.refresh();
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
        DeletedMarkIcon[] choices = DeletedMarkIcon.values();
        CharSequence[] labels = new CharSequence[choices.length];
        int[] icons = new int[choices.length];
        for (int i = 0; i < choices.length; i++) {
            labels[i] = getString(choices[i].label);
            icons[i] = choices[i].drawable;
        }
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(getString(R.string.AyulessDeletedMarkStyle))
                .setItems(labels, icons, (dialog, which) -> {
                    AppearanceConfig.setMarkIcon(choices[which]);
                    update();
                }).create());
    }
}
