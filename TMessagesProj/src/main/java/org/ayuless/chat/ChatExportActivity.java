/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.chat;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import org.ayuless.settings.AyulessSettingsFragment;
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
    private boolean custom;
    private ChatExporter job;
    private AlertDialog progress;
    public ChatExportActivity(int account, long dialog) { currentAccount = account; this.dialog = dialog; }
    @Override protected CharSequence getTitle() { return getString(R.string.AyulessChatExport); }
    @Override protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        int[] titles = {R.string.AyulessExportText, R.string.AyulessExportPhotos, R.string.AyulessExportVideos,
                R.string.AyulessExportVoice, R.string.AyulessExportFiles, R.string.AyulessExportStickers, R.string.AyulessChatDeleted};
        for (int i = 0; i < titles.length; i++) items.add(UItem.asCheck(i + 1, getString(titles[i])).setChecked(options.include[i]));
        items.add(UItem.asShadow(getString(R.string.AyulessExportMediaInfo)));
        items.add(UItem.asCheck(10, getString(R.string.AyulessExportCustomDates)).setChecked(custom));
        if (custom) {
            items.add(UItem.asButton(11, 0, getString(R.string.AyulessExportFrom), date(options.from)));
            items.add(UItem.asButton(12, 0, getString(R.string.AyulessExportTo), date(options.until - 1)));
        }
        items.add(UItem.asCheck(13, "JSON").setChecked(options.json));
        items.add(UItem.asShadow(getString(R.string.AyulessExportFormatInfo)));
        items.add(UItem.asButton(14, R.drawable.ayuless_solar_msg_download, getString(R.string.AyulessExportChooseFolder)));
    }
    private String date(long seconds) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date(seconds * 1000));
    }
    private void pickDate(boolean from) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((from ? options.from : options.until - 1) * 1000);
        new DatePickerDialog(getParentActivity(), (v, year, month, day) -> {
            Calendar value = Calendar.getInstance(); value.clear(); value.set(year, month, day);
            if (from) options.from = value.getTimeInMillis() / 1000;
            else { value.add(Calendar.DAY_OF_MONTH, 1); options.until = value.getTimeInMillis() / 1000; }
            listView.adapter.update(false);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
    @Override protected void onClick(UItem item, View v, int p, float x, float y) {
        if (job != null) return;
        if (item.id >= 1 && item.id <= 7) options.include[item.id - 1] = !options.include[item.id - 1];
        else if (item.id == 10) {
            custom = !custom;
            options.from = custom ? System.currentTimeMillis() / 1000 - 30 * 86400L : 0;
            options.until = custom ? System.currentTimeMillis() / 1000 + 86400 : Long.MAX_VALUE;
        } else if (item.id == 11 || item.id == 12) pickDate(item.id == 11);
        else if (item.id == 13) options.json = !options.json;
        else if (item.id == 14) {
            boolean any = false; for (boolean include : options.include) any |= include;
            if (!any || options.from >= options.until) {
                showDialog(new AlertDialog.Builder(getParentActivity()).setMessage(getString(R.string.AyulessExportInvalid))
                        .setPositiveButton(getString(R.string.OK), null).create()); return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try { startActivityForResult(intent, PICK_FOLDER); }
            catch (Exception e) { showError(); }
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
        Uri tree = data.getData();
        job = new ChatExporter(currentAccount, dialog, tree, options);
        progress = new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.AyulessChatExport))
                .setMessage(getString(R.string.Loading)).setNegativeButton(getString(R.string.Cancel), (d, w) -> {
                    if (job != null) job.cancel();
                }).create();
        progress.setCancelable(false); progress.setCanceledOnTouchOutside(false); showDialog(progress);
        job.start((count, complete, error, cancelled) -> {
            if (!complete) { if (progress != null) progress.setMessage(getString(R.string.AyulessExportProgress) + " " + count); return; }
            job = null;
            if (progress != null) { progress.dismiss(); progress = null; }
            if (getParentActivity() == null || cancelled) return;
            if (error != null) { showError(); return; }
            showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.AyulessChatExportDone))
                    .setMessage(getString(R.string.AyulessExportDoneInfo) + "\n" + count)
                    .setPositiveButton(getString(R.string.OK), null).create());
        });
    }
    @Override public void onFragmentDestroy() {
        if (job != null) job.cancel();
        super.onFragmentDestroy();
    }
    @Override protected boolean onLongClick(UItem i, View v, int p, float x, float y) { return false; }
}
