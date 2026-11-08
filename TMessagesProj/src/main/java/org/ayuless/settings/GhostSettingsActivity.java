/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.HapticFeedbackConstants;
import android.view.View;

import org.ayuless.ghost.GhostConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

/** Ayuless > Ghost mode. Edits either the shared profile or one account's own profile. */
public class GhostSettingsActivity extends AyulessSettingsFragment {

    private static final int ID_PROFILE = 1;
    private static final int ID_USE_OWN = 2;
    private static final int ID_MASTER = 3;
    private static final int ID_OPTION_BASE = 10; // + index in GhostConfig.GHOST_KEYS
    private static final int ID_READ_ON_ACTION = 20;
    private static final int ID_SEND_SILENTLY = 21;
    private static final int ID_STORY_PROMPT = 23;
    private static final int ID_DELAYED_SEND = 22;

    private static final int[] OPTION_TITLES = {
            R.string.AyulessGhostHideRead,
            R.string.AyulessGhostHideStoryViews,
            R.string.AyulessGhostHideOnline,
            R.string.AyulessGhostHideTyping,
            R.string.AyulessGhostOfflineAfterSend,
    };

    private int profile = Integer.MIN_VALUE;
    private boolean expanded = true;

    @Override
    public boolean onFragmentCreate() {
        profile = GhostConfig.activeProfile(currentAccount);
        return super.onFragmentCreate();
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.AyulessGhostHeader);
    }

    /** Whether the edited profile is the one that matters (shared, or an account using its own). */
    private boolean profileInUse() {
        return profile == GhostConfig.PROFILE_ALL || GhostConfig.hasOverride(profile);
    }

    @Override
    protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (profile == Integer.MIN_VALUE) {
            profile = GhostConfig.activeProfile(currentAccount);
        }
        final boolean inUse = profileInUse();

        items.add(UItem.asButton(ID_PROFILE, R.drawable.ayuless_solar_msg_contacts, getString(R.string.AyulessGhostProfile), profileName(profile)));
        if (profile != GhostConfig.PROFILE_ALL) {
            items.add(UItem.asCheck(ID_USE_OWN, getString(R.string.AyulessGhostUseOwn)).setChecked(inUse));
            items.add(UItem.asShadow(getString(R.string.AyulessGhostUseOwnInfo)));
        } else {
            items.add(UItem.asShadow(getString(R.string.AyulessGhostProfileAllInfo)));
        }

        final boolean master = GhostConfig.getProfile(profile, GhostConfig.KEY_ENABLED);
        final String count = GhostConfig.countEnabled(profile) + "/" + GhostConfig.GHOST_KEYS.length;
        items.add(UItem.asExpandableSwitch(ID_MASTER, getString(R.string.AyulessGhostHeader), count)
                .setChecked(master)
                .setCollapsed(!expanded)
                .setEnabled(inUse)
                .setClickCallback(v -> {
                    if (!profileInUse()) {
                        return;
                    }
                    GhostConfig.setProfile(profile, GhostConfig.KEY_ENABLED, !GhostConfig.getProfile(profile, GhostConfig.KEY_ENABLED));
                    update();
                }));
        if (expanded) {
            for (int i = 0; i < GhostConfig.GHOST_KEYS.length; i++) {
                final String key = GhostConfig.GHOST_KEYS[i];
                CharSequence title = getString(OPTION_TITLES[i]);
                if (GhostConfig.isLocked(profile, key)) {
                    title = LocaleController.formatString(R.string.AyulessGhostLocked, title);
                }
                items.add(UItem.asRoundCheckbox(ID_OPTION_BASE + i, title)
                        .setChecked(GhostConfig.getProfile(profile, key))
                        .setEnabled(inUse)
                        .setPad(1));
            }
        }
        items.add(UItem.asShadow(getString(R.string.AyulessGhostInfo) + "\n\n" + getString(R.string.AyulessGhostLockHint)));

        final boolean hideRead = GhostConfig.getProfile(profile, GhostConfig.KEY_HIDE_READ);
        items.add(UItem.asCheck(ID_READ_ON_ACTION, getString(R.string.AyulessGhostReadOnAction))
                .setChecked(GhostConfig.getProfile(profile, GhostConfig.KEY_READ_ON_ACTION))
                .setEnabled(inUse && hideRead));
        items.add(UItem.asShadow(getString(R.string.AyulessGhostReadOnActionInfo)));
        items.add(UItem.asCheck(ID_SEND_SILENTLY, getString(R.string.AyulessGhostSendSilently))
                .setChecked(GhostConfig.getProfile(profile, GhostConfig.KEY_SEND_SILENTLY))
                .setEnabled(inUse));
        items.add(UItem.asShadow(getString(R.string.AyulessGhostSendSilentlyInfo)));
        items.add(UItem.asCheck(ID_STORY_PROMPT, getString(R.string.AyulessGhostStoryPrompt))
                .setChecked(GhostConfig.getProfile(profile, GhostConfig.KEY_STORY_PROMPT, true))
                .setEnabled(inUse));
        items.add(UItem.asShadow(getString(R.string.AyulessGhostStoryPromptInfo)));
        items.add(UItem.asCheck(ID_DELAYED_SEND, getString(R.string.AyulessGhostDelayedSend))
                .setChecked(GhostConfig.getProfile(profile, GhostConfig.KEY_DELAYED_SEND))
                .setEnabled(inUse));
        items.add(UItem.asShadow(getString(R.string.AyulessGhostDelayedSendInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_PROFILE) {
            showProfilePicker();
            return;
        }
        if (item.id == ID_USE_OWN) {
            GhostConfig.setOverride(profile, !GhostConfig.hasOverride(profile));
            update();
            return;
        }
        if (item.id == ID_MASTER) {
            expanded = !expanded;
            update();
            return;
        }
        if (!item.enabled) {
            return;
        }
        String key = null;
        if (item.id >= ID_OPTION_BASE && item.id < ID_OPTION_BASE + GhostConfig.GHOST_KEYS.length) {
            key = GhostConfig.GHOST_KEYS[item.id - ID_OPTION_BASE];
        } else if (item.id == ID_READ_ON_ACTION) {
            key = GhostConfig.KEY_READ_ON_ACTION;
        } else if (item.id == ID_SEND_SILENTLY) {
            key = GhostConfig.KEY_SEND_SILENTLY;
        } else if (item.id == ID_DELAYED_SEND) {
            key = GhostConfig.KEY_DELAYED_SEND;
        }
        if (item.id == ID_STORY_PROMPT) {
            GhostConfig.setProfile(profile, GhostConfig.KEY_STORY_PROMPT, !GhostConfig.getProfile(profile, GhostConfig.KEY_STORY_PROMPT, true));
            update();
            return;
        }
        if (key != null) {
            GhostConfig.setProfile(profile, key, !GhostConfig.getProfile(profile, key));
            update();
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ID_OPTION_BASE && item.id < ID_OPTION_BASE + GhostConfig.GHOST_KEYS.length && profileInUse()) {
            final String key = GhostConfig.GHOST_KEYS[item.id - ID_OPTION_BASE];
            GhostConfig.setLocked(profile, key, !GhostConfig.isLocked(profile, key));
            try {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } catch (Exception ignore) {
            }
            update();
            return true;
        }
        return false;
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private static String profileName(int profile) {
        if (profile == GhostConfig.PROFILE_ALL) {
            return getString(R.string.AyulessGhostProfileAll);
        }
        TLRPC.User user = UserConfig.getInstance(profile).getCurrentUser();
        return user != null ? UserObject.getUserName(user) : String.valueOf(profile + 1);
    }

    private void showProfilePicker() {
        if (getParentActivity() == null) {
            return;
        }
        final ArrayList<Integer> profiles = new ArrayList<>();
        profiles.add(GhostConfig.PROFILE_ALL);
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                profiles.add(a);
            }
        }
        final CharSequence[] labels = new CharSequence[profiles.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = profileName(profiles.get(i));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getString(R.string.AyulessGhostProfile));
        builder.setItems(labels, (dialog, which) -> {
            profile = profiles.get(which);
            update();
        });
        showDialog(builder.create());
    }
}
