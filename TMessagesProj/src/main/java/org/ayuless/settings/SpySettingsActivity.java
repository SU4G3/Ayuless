/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import org.ayuless.antidelete.AntiDeleteConfig;
import org.ayuless.antidelete.DeletedCache;
import org.ayuless.edits.EditedCache;
import org.ayuless.edits.EditsConfig;
import org.ayuless.spy.SavedAttachments;
import org.ayuless.spy.SpyExtrasConfig;
import org.ayuless.spy.SpyExceptions;
import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.UsersSelectActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Ayuless > Spy: saving deleted messages and edit history, and the saved data itself. */
public class SpySettingsActivity extends AyulessSettingsFragment {

    private static final long MB = 1024L * 1024;
    private static final long[] LIMITS = {50 * MB, 100 * MB, 250 * MB, 500 * MB, 1024 * MB, 0};

    private static final int REQUEST_EXPORT = 7301;
    private static final int REQUEST_IMPORT = 7302;

    private static final int ID_ANTI_DELETE = 1;
    private static final int ID_EDIT_HISTORY = 2;
    private static final int ID_SAVE_READ_DATE = 30;
    private static final int ID_SAVE_LAST_SEEN = 31;
    private static final int ID_EXCEPTIONS = 3;
    private static final int ID_STORAGE_USED = 10;
    private static final int ID_STORAGE_LIMIT = 11;
    private static final int ID_EXPORT = 20;
    private static final int ID_IMPORT = 21;
    private static final int ID_CLEAR = 22;
    private static final int ID_SAVE_ATTACHMENTS = 40;
    private static final int ID_ATTACHMENTS_FOLDER = 41;

    private static final String[] RETENTION_KEYS = {
        "retainDeletedSecret", "retainDeletedTimed", "retainDeletedActions",
        "retainEditsSecret", "retainEditsTimed", "retainEditsActions",
        "retainAttachmentsSecret", "retainAttachmentsTimed", "retainAttachmentsActions",
        "keepExpiringMedia", "unlockPrivate", "unlockAutoDelete", "unlockTtl", "unlockSecret", "unlockStories"
    };
    private static final int[] RETENTION_LABELS = {
        R.string.AyulessRetainDeletedSecret, R.string.AyulessRetainDeletedTimed, R.string.AyulessRetainDeletedActions,
        R.string.AyulessRetainEditsSecret, R.string.AyulessRetainEditsTimed, R.string.AyulessRetainEditsActions,
        R.string.AyulessRetainAttachmentsSecret, R.string.AyulessRetainAttachmentsTimed, R.string.AyulessRetainAttachmentsActions,
        R.string.AyulessKeepExpiringMedia, R.string.AyulessUnlockPrivate, R.string.AyulessUnlockAutoDelete,
        R.string.AyulessUnlockTtl, R.string.AyulessUnlockSecret, R.string.AyulessUnlockStories
    };

    private long usedBytes = -1;
    private long attachmentsBytes = -1;

    @Override
    public boolean onFragmentCreate() {
        refreshSize();
        return super.onFragmentCreate();
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.AyulessSpy);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        final int account = currentAccount;
        items.add(UItem.asHeader(getString(R.string.AyulessSpyHeader)));
        items.add(UItem.asCheck(ID_ANTI_DELETE, getString(R.string.AyulessAntiDelete)).setChecked(AntiDeleteConfig.isEnabled(account)));
        items.add(UItem.asShadow(getString(R.string.AyulessAntiDeleteInfo)));
        items.add(UItem.asCheck(ID_EDIT_HISTORY, getString(R.string.AyulessEditHistory)).setChecked(EditsConfig.isEnabled(account)));
        items.add(UItem.asShadow(getString(R.string.AyulessEditHistoryInfo)));
        int exceptions = SpyExceptions.get(account).size();
        items.add(UItem.asButton(ID_EXCEPTIONS, R.drawable.ayuless_solar_msg_block, getString(R.string.AyulessSpyExceptions),
                exceptions == 0 ? getString(R.string.AyulessSpyExceptionsNone) : String.valueOf(exceptions)));
        items.add(UItem.asShadow(getString(R.string.AyulessSpyExceptionsInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessAttachmentsHeader)));
        items.add(UItem.asCheck(ID_SAVE_ATTACHMENTS, getString(R.string.AyulessSaveAttachments)).setChecked(SavedAttachments.isEnabled(account)));
        items.add(UItem.asShadow(getString(R.string.AyulessSaveAttachmentsInfo)));
        items.add(UItem.asButton(ID_ATTACHMENTS_FOLDER, R.drawable.ayuless_solar_msg_download, getString(R.string.AyulessAttachmentsFolder), "ayuless/attachments"));
        items.add(UItem.asHeader(getString(R.string.AyulessAttachmentsLimit)));
        final long[] limits = SavedAttachments.LIMITS;
        final String[] labels = new String[limits.length];
        int chosen = 1;
        long current = SavedAttachments.getLimitBytes();
        for (int i = 0; i < limits.length; i++) {
            labels[i] = limits[i] <= 0 ? "∞" : AndroidUtilities.formatFileSize(limits[i], true, false);
            if (limits[i] == current) {
                chosen = i;
            }
        }
        items.add(UItem.asSlideView(labels, chosen, which -> {
            SavedAttachments.setLimitBytes(limits[which]);
            AndroidUtilities.runOnUIThread(this::refreshSize, 300);
        }));
        items.add(UItem.asShadow(formatString(R.string.AyulessAttachmentsUsage,
                attachmentsBytes < 0 ? "…" : AndroidUtilities.formatFileSize(attachmentsBytes),
                current <= 0 ? "∞" : AndroidUtilities.formatFileSize(current))));

        items.add(UItem.asHeader(getString(R.string.AyulessSpyAdditional)));
        items.add(UItem.asCheck(ID_SAVE_READ_DATE, getString(R.string.AyulessSaveReadDate)).setChecked(SpyExtrasConfig.get(account, SpyExtrasConfig.KEY_SAVE_READ_DATE)));
        items.add(UItem.asShadow(getString(R.string.AyulessSaveReadDateInfo)));
        items.add(UItem.asCheck(ID_SAVE_LAST_SEEN, getString(R.string.AyulessSaveLastSeen)).setChecked(SpyExtrasConfig.get(account, SpyExtrasConfig.KEY_SAVE_LAST_SEEN)));
        items.add(UItem.asShadow(getString(R.string.AyulessSaveLastSeenInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessRetentionHeader)));
        for (int i = 0; i < RETENTION_KEYS.length; i++) {
            if (i == 10) items.add(UItem.asHeader(getString(R.string.AyulessRestrictedHeader)));
            items.add(UItem.asCheck(100 + i, getString(RETENTION_LABELS[i]))
                    .setChecked(SpyExtrasConfig.get(account, RETENTION_KEYS[i])));
        }
        items.add(UItem.asShadow(getString(R.string.AyulessRetentionInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessStorageHeader)));
        items.add(UItem.asButton(ID_STORAGE_USED, getString(R.string.AyulessStorageUsed), usedBytes < 0 ? "…" : AndroidUtilities.formatFileSize(usedBytes)));
        items.add(UItem.asButton(ID_STORAGE_LIMIT, getString(R.string.AyulessStorageLimit), limitLabel(AyulessStore.getLimitBytes())));
        items.add(UItem.asShadow(getString(R.string.AyulessStorageLimitInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessDataHeader)));
        items.add(UItem.asButton(ID_EXPORT, R.drawable.ayuless_solar_msg_filled_shareout, getString(R.string.AyulessExport)));
        items.add(UItem.asButton(ID_IMPORT, R.drawable.ayuless_solar_msg_download, getString(R.string.AyulessImport)));
        items.add(UItem.asButton(ID_CLEAR, R.drawable.ayuless_solar_msg_delete, getString(R.string.AyulessStorageClear)).red());
        items.add(UItem.asShadow(getString(R.string.AyulessDataInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        final int account = currentAccount;
        if (item.id >= 100 && item.id < 100 + RETENTION_KEYS.length) {
            String key = RETENTION_KEYS[item.id - 100];
            SpyExtrasConfig.set(account, key, !SpyExtrasConfig.get(account, key));
            update();
            return;
        }
        switch (item.id) {
            case ID_ANTI_DELETE:
                AntiDeleteConfig.setEnabled(account, !AntiDeleteConfig.isEnabled(account));
                update();
                break;
            case ID_EDIT_HISTORY:
                EditsConfig.setEnabled(account, !EditsConfig.isEnabled(account));
                update();
                break;
            case ID_SAVE_READ_DATE:
                SpyExtrasConfig.set(account, SpyExtrasConfig.KEY_SAVE_READ_DATE, !SpyExtrasConfig.get(account, SpyExtrasConfig.KEY_SAVE_READ_DATE));
                update();
                break;
            case ID_SAVE_LAST_SEEN:
                SpyExtrasConfig.set(account, SpyExtrasConfig.KEY_SAVE_LAST_SEEN, !SpyExtrasConfig.get(account, SpyExtrasConfig.KEY_SAVE_LAST_SEEN));
                update();
                break;
            case ID_EXCEPTIONS:
                selectExceptions();
                break;
            case ID_SAVE_ATTACHMENTS:
                SavedAttachments.setEnabled(account, !SavedAttachments.isEnabled(account));
                update();
                break;
            case ID_ATTACHMENTS_FOLDER:
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip,
                        SavedAttachments.accountDir(account).getAbsolutePath()).show();
                break;
            case ID_STORAGE_LIMIT:
                showLimitPicker();
                break;
            case ID_EXPORT:
                startExport();
                break;
            case ID_IMPORT:
                startImport();
                break;
            case ID_CLEAR:
                confirmClear();
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

    private void selectExceptions() {
        final int account = currentAccount;
        UsersSelectActivity fragment = new UsersSelectActivity(true, SpyExceptions.get(account), 0);
        fragment.noChatTypes = true;
        fragment.allowSelf = true;
        fragment.setDelegate((ids, flags) -> {
            SpyExceptions.set(account, ids);
            update();
        });
        presentFragment(fragment);
    }

    private void refreshSize() {
        AyulessStore.getInstance(currentAccount).sizeBytes(size -> {
            usedBytes = size == null ? 0 : size;
            update();
        });
        SavedAttachments.usedBytes(bytes -> {
            attachmentsBytes = bytes;
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

    // ---- export / import ----

    private void startExport() {
        String date = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "ayuless-" + date + ".db");
        launch(intent, REQUEST_EXPORT);
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        launch(intent, REQUEST_IMPORT);
    }

    private void launch(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            FileLog.e(e);
            BulletinFactory.of(this).createErrorBulletin(getString(R.string.AyulessNoFilePicker)).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        final Uri uri = data.getData();
        final int account = currentAccount;
        if (requestCode == REQUEST_EXPORT) {
            AyulessStore.getInstance(account).exportTo(uri, ok -> {
                if (ok) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, getString(R.string.AyulessExportDone)).show();
                } else {
                    BulletinFactory.of(this).createErrorBulletin(getString(R.string.AyulessExportFailed)).show();
                }
            });
        } else if (requestCode == REQUEST_IMPORT) {
            AyulessStore.getInstance(account).importFrom(uri, count -> {
                if (count < 0) {
                    BulletinFactory.of(this).createErrorBulletin(getString(R.string.AyulessImportFailed)).show();
                    return;
                }
                DeletedCache.reload(account);
                EditedCache.reload(account);
                refreshSize();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, formatString(R.string.AyulessImportDone, count)).show();
            });
        }
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
