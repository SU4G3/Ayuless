/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.chat;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.view.View;
import org.ayuless.settings.AyulessSettingsFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import java.util.ArrayList;
import java.util.Calendar;
import static org.telegram.messenger.LocaleController.getString;

public final class ChatExportActivity extends AyulessSettingsFragment {
    private static final int PICK_FOLDER = 741;
    private final long dialog;
    private final ChatExporter.Options options = new ChatExporter.Options();
    private ChatExporter job;
    private AlertDialog progress;
    private Uri folder;
    public ChatExportActivity(int account, long dialog) { currentAccount = account; this.dialog = dialog; }
    @Override protected CharSequence getTitle() { return getString(R.string.AyulessChatExport); }
    private String[] formats() {
        return new String[]{getString(R.string.AyulessExportHtml), getString(R.string.AyulessExportJson), getString(R.string.AyulessExportBoth)};
    }
    @Override protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        int[] kinds = {1, 2, 3, 7, 5, 8, 4};
        int[] titles = {R.string.AyulessExportPhotos, R.string.AyulessExportVideos, R.string.AyulessExportVoice,
                R.string.AyulessExportRound, R.string.AyulessExportStickers, R.string.AyulessExportGifs, R.string.AyulessExportFiles};
        for (int i = 0; i < titles.length; i++) items.add(UItem.asCheck(kinds[i], getString(titles[i])).setChecked(options.include[kinds[i]]));
        items.add(UItem.asHeader(getString(R.string.AyulessExportSizeLimit)));
        items.add(UItem.asIntSlideView(1, 1, (int)(options.maxFileBytes / (1024 * 1024)), 4000,
                mb -> mb + " MB", mb -> options.maxFileBytes = mb * 1024 * 1024L));
        items.add(UItem.asShadow(getString(R.string.AyulessExportMediaInfo)));
        items.add(UItem.asCheck(6, getString(R.string.AyulessChatDeleted)).setChecked(options.include[6]));
        items.add(UItem.asButton(13, 0, getString(R.string.AyulessExportFormat), formats()[options.format]));
        items.add(UItem.asButton(14, 0, getString(R.string.AyulessExportFolder), folder == null ? getString(R.string.AyulessExportChooseFolder) : folderName()));
        items.add(UItem.asButton(11, 0, getString(R.string.AyulessExportFrom), options.from == 0 ? getString(R.string.AyulessExportOldest) : date(options.from)));
        items.add(UItem.asButton(12, 0, getString(R.string.AyulessExportTo), options.until == Long.MAX_VALUE ? getString(R.string.AyulessExportPresent) : date(options.until - 1)));
        items.add(UItem.asShadow(getString(R.string.AyulessExportFormatInfo)));
        items.add(UItem.asButton(15, R.drawable.ayuless_solar_msg_download, getString(R.string.AyulessChatExport)));
    }
    private String folderName() {
        String id = DocumentsContract.getTreeDocumentId(folder);
        return id.replace(':', '/');
    }
    private String date(long seconds) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date(seconds * 1000));
    }
    private void pickDate(boolean from) {
        long selected = from ? options.from : options.until;
        Calendar c = Calendar.getInstance();
        if (selected != 0 && selected != Long.MAX_VALUE) c.setTimeInMillis((from ? selected : selected - 1) * 1000);
        DatePickerDialog picker = new DatePickerDialog(getParentActivity(), (v, year, month, day) -> {
            Calendar value = Calendar.getInstance(); value.clear(); value.set(year, month, day);
            if (from) options.from = value.getTimeInMillis() / 1000;
            else { value.add(Calendar.DAY_OF_MONTH, 1); options.until = value.getTimeInMillis() / 1000; }
            listView.adapter.update(false);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        picker.setButton(android.content.DialogInterface.BUTTON_NEUTRAL, getString(R.string.Reset), (d, w) -> {
            if (from) options.from = 0; else options.until = Long.MAX_VALUE;
            listView.adapter.update(false);
        });
        showDialog(picker);
    }
    private void pickFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= 26 && folder != null) intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, folder);
        try { startActivityForResult(intent, PICK_FOLDER); }
        catch (Exception e) { showError(); }
    }
    @Override protected void onClick(UItem item, View v, int p, float x, float y) {
        if (job != null) return;
        if (item.id >= 1 && item.id <= 8) options.include[item.id] = !options.include[item.id];
        else if (item.id == 11 || item.id == 12) pickDate(item.id == 11);
        else if (item.id == 13) showDialog(new AlertDialog.Builder(getParentActivity())
                .setTitle(getString(R.string.AyulessExportFormat)).setItems(formats(), (d, which) -> {
                    options.format = which; listView.adapter.update(false);
                }).setNegativeButton(getString(R.string.Cancel), null).create());
        else if (item.id == 14) pickFolder();
        else if (item.id == 15) {
            if (options.from >= options.until) {
                showDialog(new AlertDialog.Builder(getParentActivity()).setMessage(getString(R.string.AyulessExportInvalid))
                        .setPositiveButton(getString(R.string.OK), null).create()); return;
            }
            if (folder == null) pickFolder(); else startExport();
        }
        listView.adapter.update(false);
    }
    private void showError() {
        if (getParentActivity() != null) showDialog(new AlertDialog.Builder(getParentActivity())
                .setMessage(getString(R.string.AyulessExportError)).setPositiveButton(getString(R.string.OK), null).create());
    }
    @Override public void onActivityResultFragment(int request, int result, Intent data) {
        super.onActivityResultFragment(request, result, data);
        if (request != PICK_FOLDER || result != Activity.RESULT_OK || data == null || data.getData() == null || job != null) return;
        folder = data.getData();
        listView.adapter.update(false);
    }
    private void openFolder(Uri uri) {
        if (getParentActivity() == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { getParentActivity().startActivity(intent); }
        catch (Exception e) {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= 26) picker.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
            try { getParentActivity().startActivity(picker); } catch (Exception ignored) { showError(); }
        }
    }
    private void startExport() {
        job = new ChatExporter(currentAccount, dialog, folder, options);
        ChatExporter current = job;
        progress = new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.AyulessChatExport))
                .setMessage(getString(R.string.Loading)).setNegativeButton(getString(R.string.Cancel), (d, w) -> {
                    if (job != null) job.cancel();
                }).create();
        progress.setCancelable(false); progress.setCanceledOnTouchOutside(false); showDialog(progress);
        job.start((count, complete, error, cancelled) -> {
            if (!complete) {
                if (progress != null) progress.setMessage(getString(R.string.AyulessExportProgress) + " " + count + "\n"
                        + getString(R.string.AyulessExportTotalFiles) + " " + current.getFileCount() + "\n"
                        + getString(R.string.AyulessExportTotalSize) + " " + AndroidUtilities.formatFileSize(current.getTotalBytes()));
                return;
            }
            job = null;
            if (progress != null) { progress.dismiss(); progress = null; }
            if (getParentActivity() == null || cancelled) return;
            if (error != null) { showError(); return; }
            showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.AyulessChatExportDone))
                    .setMessage(getString(R.string.AyulessExportDoneInfo) + " " + count + "\n\n"
                            + getString(R.string.AyulessExportTotalFiles) + " " + current.getFileCount() + "\n"
                            + getString(R.string.AyulessExportTotalSize) + " " + AndroidUtilities.formatFileSize(current.getTotalBytes()))
                    .setPositiveButton(getString(R.string.AyulessExportShowData), (d, w) -> openFolder(current.getExportFolder()))
                    .setNegativeButton(getString(R.string.Close), null).create());
        });
    }
    @Override public void onFragmentDestroy() {
        if (job != null) job.cancel();
        super.onFragmentDestroy();
    }
    @Override protected boolean onLongClick(UItem i, View v, int p, float x, float y) { return false; }
}
