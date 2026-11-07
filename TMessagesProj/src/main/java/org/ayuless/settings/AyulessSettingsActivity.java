/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.ayuless.antidelete.AntiDeleteConfig;
import org.ayuless.antidelete.DeletedCache;
import org.ayuless.edits.EditedCache;
import org.ayuless.edits.EditsConfig;
import org.ayuless.ghost.GhostConfig;
import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Settings > Ayuless: all Ayuless toggles for the current account in one place. */
public class AyulessSettingsActivity extends UniversalFragment {

    private static final String SOURCE_URL = "https://github.com/SU4G3/Ayuless";
    private static final long MB = 1024L * 1024;
    private static final long[] LIMITS = {50 * MB, 100 * MB, 250 * MB, 500 * MB, 0};

    private static final int ID_GHOST = 1;
    private static final int ID_HIDE_ONLINE = 2;
    private static final int ID_HIDE_READ = 3;
    private static final int ID_HIDE_TYPING = 4;
    private static final int ID_HIDE_STORIES = 5;
    private static final int ID_OFFLINE_AFTER_SEND = 6;
    private static final int ID_ANTI_DELETE = 10;
    private static final int ID_EDIT_HISTORY = 11;
    private static final int ID_STORAGE_USED = 20;
    private static final int ID_STORAGE_LIMIT = 21;
    private static final int ID_STORAGE_CLEAR = 22;
    private static final int ID_VERSION = 30;
    private static final int ID_SOURCE = 31;

    private long usedBytes = -1;

    @Override
    public boolean onFragmentCreate() {
        refreshSize();
        return super.onFragmentCreate();
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.AyulessSettings);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        final int account = currentAccount;
        final boolean ghost = GhostConfig.isEnabled(account);

        items.add(UItem.asHeader(getString(R.string.AyulessGhostHeader)));
        items.add(UItem.asCheck(ID_GHOST, getString(R.string.AyulessGhostEnabled)).setChecked(ghost));
        items.add(UItem.asCheck(ID_HIDE_ONLINE, getString(R.string.AyulessGhostHideOnline)).setChecked(GhostConfig.hideOnline(account)).setEnabled(ghost));
        items.add(UItem.asCheck(ID_HIDE_READ, getString(R.string.AyulessGhostHideRead)).setChecked(GhostConfig.hideRead(account)).setEnabled(ghost));
        items.add(UItem.asCheck(ID_HIDE_TYPING, getString(R.string.AyulessGhostHideTyping)).setChecked(GhostConfig.hideTyping(account)).setEnabled(ghost));
        items.add(UItem.asCheck(ID_HIDE_STORIES, getString(R.string.AyulessGhostHideStoryViews)).setChecked(GhostConfig.hideStoryViews(account)).setEnabled(ghost));
        items.add(UItem.asCheck(ID_OFFLINE_AFTER_SEND, getString(R.string.AyulessGhostOfflineAfterSend)).setChecked(GhostConfig.offlineAfterSend(account)).setEnabled(ghost));
        items.add(UItem.asShadow(getString(R.string.AyulessGhostInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessMessagesHeader)));
        items.add(UItem.asCheck(ID_ANTI_DELETE, getString(R.string.AyulessAntiDelete)).setChecked(AntiDeleteConfig.isEnabled(account)));
        items.add(UItem.asShadow(getString(R.string.AyulessAntiDeleteInfo)));
        items.add(UItem.asCheck(ID_EDIT_HISTORY, getString(R.string.AyulessEditHistory)).setChecked(EditsConfig.isEnabled(account)));
        items.add(UItem.asShadow(getString(R.string.AyulessEditHistoryInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessStorageHeader)));
        items.add(UItem.asButton(ID_STORAGE_USED, getString(R.string.AyulessStorageUsed), usedBytes < 0 ? "…" : AndroidUtilities.formatFileSize(usedBytes)));
        items.add(UItem.asButton(ID_STORAGE_LIMIT, getString(R.string.AyulessStorageLimit), limitLabel(AyulessStore.getLimitBytes())));
        items.add(UItem.asButton(ID_STORAGE_CLEAR, getString(R.string.AyulessStorageClear)).red());
        items.add(UItem.asShadow(getString(R.string.AyulessAccountInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessAboutHeader)));
        items.add(UItem.asButton(ID_VERSION, getString(R.string.AyulessVersion), BuildVars.BUILD_VERSION_STRING));
        items.add(UItem.asButton(ID_SOURCE, getString(R.string.AyulessSourceCode), "GitHub"));
        items.add(UItem.asShadow(getString(R.string.AyulessDisclaimer)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        final int account = currentAccount;
        switch (item.id) {
            case ID_GHOST:
                GhostConfig.setEnabled(account, !GhostConfig.isEnabled(account));
                break;
            case ID_HIDE_ONLINE:
                if (!item.enabled) return;
                GhostConfig.setHideOnline(account, !GhostConfig.hideOnline(account));
                break;
            case ID_HIDE_READ:
                if (!item.enabled) return;
                GhostConfig.setHideRead(account, !GhostConfig.hideRead(account));
                break;
            case ID_HIDE_TYPING:
                if (!item.enabled) return;
                GhostConfig.setHideTyping(account, !GhostConfig.hideTyping(account));
                break;
            case ID_HIDE_STORIES:
                if (!item.enabled) return;
                GhostConfig.setHideStoryViews(account, !GhostConfig.hideStoryViews(account));
                break;
            case ID_OFFLINE_AFTER_SEND:
                if (!item.enabled) return;
                GhostConfig.setOfflineAfterSend(account, !GhostConfig.offlineAfterSend(account));
                break;
            case ID_ANTI_DELETE:
                AntiDeleteConfig.setEnabled(account, !AntiDeleteConfig.isEnabled(account));
                break;
            case ID_EDIT_HISTORY:
                EditsConfig.setEnabled(account, !EditsConfig.isEnabled(account));
                break;
            case ID_STORAGE_LIMIT:
                showLimitPicker();
                return;
            case ID_STORAGE_CLEAR:
                confirmClear();
                return;
            case ID_SOURCE:
                Browser.openUrl(getParentActivity(), SOURCE_URL);
                return;
            default:
                return;
        }
        update();
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

    private void refreshSize() {
        AyulessStore.getInstance(currentAccount).sizeBytes(size -> {
            usedBytes = size == null ? 0 : size;
            update();
        });
    }

    private String limitLabel(long bytes) {
        return bytes <= 0 ? getString(R.string.AyulessStorageNoLimit) : AndroidUtilities.formatFileSize(bytes);
    }

    private void showLimitPicker() {
        if (getParentActivity() == null) return;
        final CharSequence[] labels = new CharSequence[LIMITS.length];
        for (int i = 0; i < LIMITS.length; i++) {
            labels[i] = limitLabel(LIMITS[i]);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getString(R.string.AyulessStorageLimit));
        builder.setItems(labels, (dialog, which) -> {
            AyulessStore.setLimitBytes(LIMITS[which]);
            update();
        });
        showDialog(builder.create());
    }

    private void confirmClear() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getString(R.string.AyulessStorageClear));
        builder.setMessage(getString(R.string.AyulessStorageClearConfirm));
        builder.setPositiveButton(getString(R.string.Delete), (dialog, which) -> {
            AyulessStore.getInstance(currentAccount).clearAll();
            DeletedCache.clear(currentAccount);
            EditedCache.clear(currentAccount);
            usedBytes = 0;
            update();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_delete, getString(R.string.AyulessStorageCleared)).show();
            AndroidUtilities.runOnUIThread(this::refreshSize, 500);
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        dialog.redPositive();
    }
}
