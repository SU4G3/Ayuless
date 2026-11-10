/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.chat;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import org.ayuless.settings.AyulessSettingsFragment;
import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import java.util.ArrayList;
import static org.telegram.messenger.LocaleController.getString;

/** Keeps one bounded page in memory, even when a dialog contains years of saved history. */
public final class DeletedMessagesActivity extends AyulessSettingsFragment {
    private final long dialog;
    private final ArrayList<TLRPC.Message> page = new ArrayList<>();
    private final ArrayList<long[]> cursors = new ArrayList<>();
    private boolean loading, end, failed;
    public DeletedMessagesActivity(int account, long dialog) {
        currentAccount = account; this.dialog = dialog;
        cursors.add(new long[]{Long.MAX_VALUE, Long.MAX_VALUE});
    }
    @Override protected CharSequence getTitle() { return getString(R.string.AyulessChatDeleted); }
    @Override public View createView(Context context) { View v = super.createView(context); load(); return v; }
    private void load() {
        loading = true;
        long[] cursor = cursors.get(cursors.size() - 1);
        AyulessStore.getInstance(currentAccount).deletedPage(dialog, cursor[0], cursor[1], result -> {
            loading = false; failed = result == null; page.clear();
            if (result != null) page.addAll(result);
            end = result == null || result.size() < 100;
            if (listView != null) listView.adapter.update(false);
        });
    }
    @Override protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (loading || page.isEmpty()) items.add(UItem.asShadow(getString(loading ? R.string.Loading : failed ? R.string.AyulessExportError : R.string.AyulessChatEmpty)));
        for (int i = 0; i < page.size(); i++) {
            TLRPC.Message m = page.get(i);
            String text = m.message == null || m.message.isEmpty() ? getString(R.string.AyulessExportFiles) : m.message;
            if (text.length() > 300) text = text.substring(0, 300) + "…";
            items.add(UItem.asButton(i + 1, R.drawable.ayuless_solar_msg_delete,
                    "✕ " + text, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date(m.date * 1000L))));
        }
        if (!loading && cursors.size() > 1) items.add(UItem.asButton(-1, 0, getString(R.string.AyulessPagePrevious)));
        if (!loading && !end) items.add(UItem.asButton(-2, 0, getString(R.string.AyulessPageNext)));
    }
    @Override protected void onClick(UItem item, View v, int p, float x, float y) {
        if (loading) return;
        if (item.id == -1) { cursors.remove(cursors.size() - 1); load(); }
        else if (item.id == -2 && !page.isEmpty()) {
            TLRPC.Message m = page.get(page.size() - 1); cursors.add(new long[]{m.date, m.id}); load();
        } else if (item.id > 0 && item.id <= page.size()) {
            int id = page.get(item.id - 1).id;
            MessagesStorage storage = MessagesStorage.getInstance(currentAccount);
            storage.getStorageQueue().postRunnable(() -> {
                boolean found = false;
                try {
                    org.telegram.SQLite.SQLiteCursor c = storage.getDatabase().queryFinalized("SELECT 1 FROM messages_v2 WHERE uid = ? AND mid = ?", dialog, id);
                    try { found = c.next(); } finally { c.dispose(); }
                } catch (Exception e) { FileLog.e(e); }
                final boolean cached = found;
                AndroidUtilities.runOnUIThread(() -> {
                    if (!cached || getParentActivity() == null) return;
                    Bundle args = new Bundle();
                    if (DialogObject.isEncryptedDialog(dialog)) args.putInt("enc_id", DialogObject.getEncryptedChatId(dialog));
                    else if (dialog > 0) args.putLong("user_id", dialog);
                    else args.putLong("chat_id", -dialog);
                    args.putInt("message_id", id);
                    ChatActivity chat = new ChatActivity(args); chat.setCurrentAccount(currentAccount);
                    presentFragment(chat);
                });
            });
        }
    }
    @Override protected boolean onLongClick(UItem i, View v, int p, float x, float y) { return false; }
}
