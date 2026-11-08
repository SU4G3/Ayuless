/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.ghost;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import androidx.core.content.ContextCompat;

import org.ayuless.appearance.AppearanceConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

/**
 * Ghost button in the chat list action bar.
 *
 * <p>Shown when the "ghost status in chat list" option is on and the account's active ghost
 * profile has at least one ghost option selected. The icon is fully opaque while ghost mode
 * is on and dimmed while it is off. Tapping it flips the master switch of the active profile.
 */
public final class GhostBadge {

    /** Action bar item id; chosen far away from the small ids used by DialogsActivity. */
    public static final int ITEM_ID = 0x41790001;

    private static final float ALPHA_OFF = 0.4f;

    private GhostBadge() {
    }

    public static ActionBarMenuItem create(ActionBarMenu menu) {
        ActionBarMenuItem item = menu.addItem(ITEM_ID, R.drawable.ayuless_solar_ghost);
        item.setContentDescription(getString(R.string.AyulessGhostHeader));
        item.setVisibility(View.GONE);
        return item;
    }

    /** Whether the button should be visible for an account (before search/animation factors). */
    public static boolean shouldShow(int account) {
        return AppearanceConfig.ghostInDialogs()
                && GhostConfig.countEnabled(GhostConfig.activeProfile(account)) > 0;
    }

    /** Updates the icon to show whether ghost mode is on. */
    public static void updateIcon(ActionBarMenuItem item, int account) {
        if (item == null || item.getIconView() == null) {
            return;
        }
        item.getIconView().setAlpha(GhostConfig.isEnabled(account) ? 1f : ALPHA_OFF);
    }

    /** Flips the master switch of the account's active profile and shows a short notice. */
    public static void toggle(BaseFragment fragment, int account, ActionBarMenuItem item) {
        int profile = GhostConfig.activeProfile(account);
        boolean on = !GhostConfig.getProfile(profile, GhostConfig.KEY_ENABLED);
        GhostConfig.setProfile(profile, GhostConfig.KEY_ENABLED, on);
        updateIcon(item, account);
        if (fragment != null && fragment.getParentActivity() != null) {
            BulletinFactory.of(fragment).createSimpleBulletin(
                    ContextCompat.getDrawable(fragment.getParentActivity(), R.drawable.ayuless_solar_ghost),
                    getString(on ? R.string.AyulessGhostTurnedOn : R.string.AyulessGhostTurnedOff),
                    getString(profile == GhostConfig.PROFILE_ALL ? R.string.AyulessGhostProfileAll : R.string.AyulessGhostThisAccount)
            ).show();
        }
    }
}
