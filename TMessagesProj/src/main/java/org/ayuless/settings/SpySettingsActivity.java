/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

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
import android.widget.LinearLayout;

import org.ayuless.antidelete.AntiDeleteConfig;
import org.ayuless.antidelete.DeletedCache;
import org.ayuless.edits.EditedCache;
import org.ayuless.edits.EditsConfig;
import org.ayuless.extras.ExtrasConfig;
import org.ayuless.spy.MessageSavingConfig;
import org.ayuless.spy.SavedAttachments;
import org.ayuless.spy.SpyExtrasConfig;
import org.ayuless.spy.SpyExceptions;
import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Cells.TextCheckBoxCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
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
    private static final int CUSTOM = 50, CATEGORIES = 51, TYPES = 52;
    private static final int FORMATTING = 53, REACTIONS = 54, BOTS = 55;
    private static final int ID_DELETED_ACTION_BASE = 60;
    private static final String[] DELETED_ACTION_KEYS = {ExtrasConfig.KEY_LOCAL_RESEND,
            ExtrasConfig.KEY_KEEP_LOCAL, ExtrasConfig.KEY_DELETE_DATE};
    private static final int[] DELETED_ACTION_TITLES = {R.string.AyulessLocalResend,
            R.string.AyulessKeepLocalSetting, R.string.AyulessDeleteDateSetting};
    private static final int ID_STORAGE_USED = 10;
    private static final int ID_STORAGE_LIMIT = 11;
    private static final int ID_EXPORT = 20;
    private static final int ID_IMPORT = 21;
    private static final int ID_CLEAR = 22;
    private static final int ID_SAVE_ATTACHMENTS = 40;
    private static final int ID_ATTACHMENTS_FOLDER = 41;

    private static final int ID_ATTACHMENTS_LIMIT = 42;
    private org.telegram.ui.Cells.TextDetailSettingsCell attachmentLimitCell;

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
    protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        final int account = currentAccount;
        items.add(UItem.asCheck(ID_ANTI_DELETE, getString(R.string.AyulessAntiDelete)).setChecked(AntiDeleteConfig.isEnabled(account)));
        int exceptions = SpyExceptions.get(account).size();
        items.add(UItem.asButton(ID_EXCEPTIONS, getString(R.string.AyulessSpyExceptions),
                exceptions == 0 ? getString(R.string.AyulessSpyExceptionsNone) : String.valueOf(exceptions)));
        items.add(UItem.asCheck(ID_EDIT_HISTORY, getString(R.string.AyulessEditHistory)).setChecked(EditsConfig.isEnabled(account)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCheck(ID_SAVE_ATTACHMENTS, getString(R.string.AyulessSaveAttachments)).setChecked(SavedAttachments.isEnabled(account)));
        items.add(UItem.asButton(ID_ATTACHMENTS_FOLDER, getString(R.string.AyulessAttachmentsFolder), "ayuless/attachments"));
        if (attachmentLimitCell == null && getParentActivity() != null) {
            attachmentLimitCell = new org.telegram.ui.Cells.TextDetailSettingsCell(getParentActivity());
            attachmentLimitCell.setMultilineDetail(true);
        }
        if (attachmentLimitCell != null) {
            attachmentLimitCell.setTextAndValue(getString(R.string.AyulessAttachmentsLimit) + " · " + limitLabel(SavedAttachments.getLimitBytes()),
                    formatString(R.string.AyulessAttachmentsUsage,
                            attachmentsBytes < 0 ? "…" : AndroidUtilities.formatFileSize(attachmentsBytes)), false);
            items.add(UItem.asCustom(ID_ATTACHMENTS_LIMIT, attachmentLimitCell));
        }

        boolean custom = MessageSavingConfig.get(account, MessageSavingConfig.CUSTOM);
        items.add(UItem.asCheck(CUSTOM, getString(R.string.AyulessCustomSaving)).setChecked(custom));
        if (custom) {
            items.add(UItem.asCheck(CATEGORIES, getString(R.string.AyulessMediaCategories))
                    .setChecked(MessageSavingConfig.get(account, MessageSavingConfig.MEDIA_CATEGORIES)));
            items.add(UItem.asButton(TYPES, getString(R.string.AyulessChooseMediaCategories))
                    .setEnabled(MessageSavingConfig.get(account, MessageSavingConfig.MEDIA_CATEGORIES)));
            items.add(UItem.asCheck(FORMATTING, getString(R.string.AyulessSaveFormatting)).setChecked(MessageSavingConfig.get(account, MessageSavingConfig.FORMATTING)));
            items.add(UItem.asCheck(REACTIONS, getString(R.string.AyulessSaveReactions)).setChecked(MessageSavingConfig.get(account, MessageSavingConfig.REACTIONS)));
            items.add(UItem.asCheck(BOTS, getString(R.string.AyulessSaveBots)).setChecked(MessageSavingConfig.get(account, MessageSavingConfig.BOTS)));
        }
        items.add(UItem.asShadow(getString(R.string.AyulessCustomSavingInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessSpyAdditional)));
        items.add(UItem.asCheck(ID_SAVE_READ_DATE, getString(R.string.AyulessSaveReadDate)).setChecked(SpyExtrasConfig.get(account, SpyExtrasConfig.KEY_SAVE_READ_DATE)));
        items.add(UItem.asCheck(ID_SAVE_LAST_SEEN, getString(R.string.AyulessSaveLastSeen)).setChecked(SpyExtrasConfig.get(account, SpyExtrasConfig.KEY_SAVE_LAST_SEEN)));
        for (int i = 0; i < DELETED_ACTION_KEYS.length; i++) {
            items.add(UItem.asCheck(ID_DELETED_ACTION_BASE + i, getString(DELETED_ACTION_TITLES[i]))
                    .setChecked(ExtrasConfig.get(DELETED_ACTION_KEYS[i])));
        }
        items.add(UItem.asShadow(getString(R.string.AyulessSpyAdditionalInfo) + "\n\n" + getString(R.string.AyulessDeletedActionsInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessStorageHeader)));
        items.add(UItem.asButton(ID_STORAGE_USED, getString(R.string.AyulessStorageUsed), usedBytes < 0 ? "…" : AndroidUtilities.formatFileSize(usedBytes)));
        items.add(UItem.asButton(ID_STORAGE_LIMIT, getString(R.string.AyulessStorageLimit), limitLabel(AyulessStore.getLimitBytes())));
        items.add(UItem.asShadow(getString(R.string.AyulessStorageLimitInfo)));

        items.add(UItem.asHeader(getString(R.string.AyulessDataHeader)));
        items.add(UItem.asButton(ID_EXPORT, R.drawable.ayuless_solar_msg_filled_shareout, getString(R.string.AyulessExport)));
        items.add(UItem.asButton(ID_IMPORT, R.drawable.ayuless_solar_msg_download, getString(R.string.AyulessImport)));
        items.add(UItem.asButton(ID_CLEAR, R.drawable.ayuless_solar_msg_delete, getString(R.string.AyulessStorageClear)).red());
        items.add(UItem.asShadow(getString(R.string.AyulessDataInfo)));
        for (UItem item : items) {
            if (item.id == ID_ANTI_DELETE || item.id == ID_EXCEPTIONS || item.id == ID_EDIT_HISTORY
                    || item.id == ID_SAVE_ATTACHMENTS || item.id == ID_ATTACHMENTS_FOLDER) item.hideDivider = true;
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (!item.enabled) return;
        final int account = currentAccount;
        if (item.id >= ID_DELETED_ACTION_BASE && item.id < ID_DELETED_ACTION_BASE + DELETED_ACTION_KEYS.length) {
            String key = DELETED_ACTION_KEYS[item.id - ID_DELETED_ACTION_BASE];
            ExtrasConfig.set(key, !ExtrasConfig.get(key));
            update();
            return;
        }
        String savingKey = item.id == CUSTOM ? MessageSavingConfig.CUSTOM : item.id == CATEGORIES ? MessageSavingConfig.MEDIA_CATEGORIES
                : item.id == FORMATTING ? MessageSavingConfig.FORMATTING : item.id == REACTIONS ? MessageSavingConfig.REACTIONS
                : item.id == BOTS ? MessageSavingConfig.BOTS : null;
        if (savingKey != null) {
            MessageSavingConfig.set(account, savingKey, !MessageSavingConfig.get(account, savingKey));
            update();
            return;
        }
        switch (item.id) {
            case TYPES:
                showMediaCategories();
                break;
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
            case ID_ATTACHMENTS_LIMIT:
                showAttachmentLimitPicker();
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
        return bytes <= 0 ? getString(R.string.AyulessStorageNoLimit)
                : bytes >= 1024 * MB && bytes % (1024 * MB) == 0
                ? (bytes / (1024 * MB)) + " GB" : (bytes / MB) + " MB";
    }

    private void showAttachmentLimitPicker() {
        if (getParentActivity() == null) return;
        CharSequence[] labels = new CharSequence[SavedAttachments.LIMITS.length];
        for (int i = 0; i < labels.length; i++) labels[i] = limitLabel(SavedAttachments.LIMITS[i]);
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(getString(R.string.AyulessAttachmentsLimit))
                .setItems(labels, (dialog, which) -> {
                    SavedAttachments.setLimitBytes(SavedAttachments.LIMITS[which]);
                    refreshSize();
                    update();
                }).create());
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

    private void showMediaCategories() {
        if (getParentActivity() == null) return;
        int[] labels = {R.string.AyulessMediaPrivateChats, R.string.AyulessMediaPublicChannels,
                R.string.AyulessMediaPrivateChannels, R.string.AyulessMediaPublicGroups, R.string.AyulessMediaPrivateGroups};
        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        TextCheckBoxCell[] cells = new TextCheckBoxCell[labels.length];
        for (int i = 0; i < labels.length; i++) {
            TextCheckBoxCell cell = cells[i] = new TextCheckBoxCell(getParentActivity(), true, false);
            cell.setTextAndCheck(getString(labels[i]), MessageSavingConfig.get(currentAccount, MessageSavingConfig.CATEGORIES[i]), i < labels.length - 1);
            cell.setOnClickListener(v -> cell.setChecked(!cell.isChecked()));
            layout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
        }
        BottomSheet sheet = new BottomSheet.Builder(getParentActivity(), false, getResourceProvider())
                .setTitle(getString(R.string.AyulessChooseMediaCategories)).setCustomView(layout).create();
        android.widget.LinearLayout buttons = new android.widget.LinearLayout(getParentActivity());
        buttons.setGravity(android.view.Gravity.END);
        for (int i = 0; i < 2; i++) {
            final boolean save = i == 1;
            android.widget.TextView button = new android.widget.TextView(getParentActivity());
            button.setText(getString(save ? R.string.Save : R.string.Cancel));
            button.setTextSize(14);
            button.setGravity(android.view.Gravity.CENTER);
            button.setPadding(org.telegram.messenger.AndroidUtilities.dp(20), 0, org.telegram.messenger.AndroidUtilities.dp(20), 0);
            button.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextBlue2, getResourceProvider()));
            button.setOnClickListener(v -> {
                if (save) {
                    for (int j = 0; j < cells.length; j++) MessageSavingConfig.set(currentAccount, MessageSavingConfig.CATEGORIES[j], cells[j].isChecked());
                    listView.adapter.update(true);
                }
                sheet.dismiss();
            });
            buttons.addView(button, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48));
        }
        layout.addView(buttons, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        showDialog(sheet);
    }
}
