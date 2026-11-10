/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.chat;

import android.view.View;
import org.ayuless.settings.AyulessSettingsFragment;
import org.ayuless.spy.SpyExceptions;
import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import java.util.ArrayList;
import static org.telegram.messenger.LocaleController.getString;

public final class ChatToolsActivity extends AyulessSettingsFragment {
    private final long dialog;
    public ChatToolsActivity(int account, long dialog) { currentAccount = account; this.dialog = dialog; }
    @Override protected CharSequence getTitle() { return getString(R.string.AyulessLauncherName); }
    @Override protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asButton(1, R.drawable.ayuless_solar_msg_stories_stealth, getString(R.string.AyulessChatDeleted)));
        items.add(UItem.asCheck(2, getString(R.string.AyulessChatDontSave)).setChecked(SpyExceptions.isExcepted(currentAccount, dialog)));
        items.add(UItem.asCheck(3, getString(R.string.AyulessChatDontRead)).setChecked(!ChatToolsConfig.excepted(currentAccount, dialog, false)));
        items.add(UItem.asCheck(4, getString(R.string.AyulessChatDontType)).setChecked(!ChatToolsConfig.excepted(currentAccount, dialog, true)));
        items.add(UItem.asShadow(getString(R.string.AyulessChatGhostInfo)));
        items.add(UItem.asButton(5, R.drawable.ayuless_solar_msg_delete, getString(R.string.AyulessChatClear)));
        items.add(UItem.asButton(6, R.drawable.ayuless_solar_msg_download, getString(R.string.AyulessChatExport)));
    }
    @Override protected void onClick(UItem item, View view, int position, float x, float y) {
        if (!ChatToolsConfig.enabled()) return;
        if (item.id == 1) presentFragment(new DeletedMessagesActivity(currentAccount, dialog));
        else if (item.id == 2) {
            ArrayList<Long> ids = SpyExceptions.get(currentAccount);
            if (!ids.remove(dialog)) ids.add(dialog);
            SpyExceptions.set(currentAccount, ids);
        } else if (item.id == 3 || item.id == 4) {
            boolean typing = item.id == 4;
            ChatToolsConfig.setExcepted(currentAccount, dialog, typing, !ChatToolsConfig.excepted(currentAccount, dialog, typing));
        } else if (item.id == 5) {
            showDialog(new AlertDialog.Builder(getParentActivity())
                    .setTitle(getString(R.string.AyulessChatClear))
                    .setMessage(getString(R.string.AyulessChatClearConfirm))
                    .setNegativeButton(getString(R.string.Cancel), null)
                    .setPositiveButton(getString(R.string.Delete), (d, w) -> AyulessStore.getInstance(currentAccount)
                            .clearSavedDialog(dialog, ok -> {
                                if (getParentActivity() != null) showDialog(new AlertDialog.Builder(getParentActivity())
                                        .setMessage(getString(ok ? R.string.AyulessChatCleared : R.string.AyulessExportError))
                                        .setPositiveButton(getString(R.string.OK), null).create());
                            })).create());
        } else if (item.id == 6) presentFragment(new ChatExportActivity(currentAccount, dialog));
        listView.adapter.update(false);
    }
    @Override protected boolean onLongClick(UItem i, View v, int p, float x, float y) { return false; }
}
