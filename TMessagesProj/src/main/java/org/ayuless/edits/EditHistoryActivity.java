// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.edits;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import org.ayuless.content.MessageSnapshots;
import org.ayuless.content.ProtectedContent;
import org.ayuless.spy.SavedAttachments;
import org.ayuless.storage.AyulessStore;
import org.ayuless.storage.StoredEdit;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import java.io.File;
import java.util.ArrayList;

/** A read-only collection of native message bubbles; it has no chat receipt delegate. */
public final class EditHistoryActivity extends UniversalFragment {
    private final TLRPC.Message original;
    private final ArrayList<TLRPC.Message> messages = new ArrayList<>();
    private final ArrayList<String> labels = new ArrayList<>();
    private boolean loading = true;

    public EditHistoryActivity(int account, MessageObject source) {
        currentAccount = account;
        original = source.messageOwner;
    }

    @Override
    public boolean onFragmentCreate() {
        if (!super.onFragmentCreate()) return false;
        AyulessStore.getInstance(currentAccount).getEdits(MessageObject.getDialogId(original), original.id, edits -> {
            if (isFinished) return;
            addPreview(null);
            for (int i = edits.size() - 1; i >= 0; i--) addPreview(edits.get(i));
            SavedAttachments.prepareHistory(currentAccount, messages, () -> {
                if (isFinished) return;
                loading = false;
                if (listView != null) listView.adapter.update(true);
            });
        });
        return true;
    }

    private void addPreview(StoredEdit edit) {
        try {
            TLRPC.Message preview = MessageSnapshots.copy(original);
            String label;
            if (edit == null) {
                label = LocaleController.getString(R.string.AyulessHistoryCurrent);
            } else {
                preview.message = edit.text == null ? "" : edit.text;
                preview.entities = edit.entities == null ? new ArrayList<>() : new ArrayList<>(edit.entities);
                preview.flags = preview.entities.isEmpty() ? preview.flags & ~128 : preview.flags | 128;
                preview.media = edit.media;
                preview.flags = preview.media == null ? preview.flags & ~512 : preview.flags | 512;
                preview.date = edit.editDate == 0 ? original.date : edit.editDate;
                preview.edit_date = 0;
                preview.edit_hide = false;
                preview.flags &= ~32768;
                preview.reply_to = null;
                preview.flags &= ~8;
                preview.reactions = null;
                preview.flags &= ~1048576;
                preview.rich_message = null;
                preview.flags2 &= ~(8192 | 1024);
                preview.schedule_repeat_period = 0;
                preview.attachPath = "";
                label = edit.editDate == 0 ? LocaleController.getString(R.string.AyulessHistoryOriginal)
                        : LocaleController.formatString(R.string.AyulessHistoryVersion, edit.version + 1);
            }
            preview.from_scheduled = false;
            preview.flags &= ~(1 | 32 | 262144);
            preview.unread = false;
            preview.media_unread = false;
            preview.send_state = 0;
            messages.add(preview);
            labels.add(label);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        listView.adapter.setApplyBackground(false);
        listView.setBackground(wallpaper());
        return view;
    }

    @Override
    protected CharSequence getTitle() { return LocaleController.getString(R.string.AyulessEditsHistoryTitle); }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Context context = getContext();
        if (context == null) return;
        if (loading) {
            FrameLayout spinner = new FrameLayout(context);
            org.telegram.ui.Components.RadialProgressView progress = new org.telegram.ui.Components.RadialProgressView(context);
            spinner.addView(progress, LayoutHelper.createFrame(32, 32, android.view.Gravity.CENTER));
            items.add(UItem.asCustom(spinner, 80));
            return;
        }
        for (int index = 0; index < messages.size(); index++) {
            final TLRPC.Message message = messages.get(index);
            items.add(UItem.asHeader(labels.get(index)));
            MessageObject object = new MessageObject(currentAccount, message, true, true);
            ChatMessageCell bubble = new ChatMessageCell(context, currentAccount, false, null, getResourceProvider());
            bubble.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {});
            bubble.isChat = MessageObject.getDialogId(message) < 0;
            bubble.setBackground(wallpaper());
            bubble.setMessageObject(object, null, false, false, false);
            // This parent owns gestures, so previews cannot run ordinary chat/media actions.
            FrameLayout row = new FrameLayout(context) {
                @Override
                public boolean onInterceptTouchEvent(android.view.MotionEvent event) { return true; }
            };
            row.setBackground(wallpaper());
            row.addView(bubble, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            row.setOnClickListener(v -> openOrCopy(object));
            row.setOnLongClickListener(v -> copy(message.message));
            items.add(UItem.asCustom(row, LayoutHelper.WRAP_CONTENT));
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.AyulessHistoryHint)));
    }

    private android.graphics.drawable.Drawable wallpaper() {
        android.graphics.drawable.Drawable background = Theme.getCachedWallpaper();
        if (background != null) {
            android.graphics.drawable.Drawable.ConstantState state = background.getConstantState();
            if (state != null) return state.newDrawable().mutate();
        }
        return new android.graphics.drawable.ColorDrawable(Theme.getColor(Theme.key_chat_wallpaper, getResourceProvider()));
    }

    private boolean copy(String text) {
        if (isFinished || text == null || text.isEmpty()) return false;
        AndroidUtilities.addToClipboard(text);
        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.MessageCopied)).show();
        return true;
    }

    private void openOrCopy(MessageObject object) {
        TLRPC.Message message = object.messageOwner;
        if (message.media == null || message.media.photo == null && message.media.document == null) {
            copy(message.message);
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            File file = ProtectedContent.readableFile(currentAccount, message);
            AndroidUtilities.runOnUIThread(() -> {
                if (isFinished || getParentActivity() == null) return;
                if (file == null || !file.isFile()) {
                    BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.AyulessProtectedForwardNeedsDownload)).show();
                } else {
                    String mime = object.getDocument() == null ? "image/jpeg" : object.getDocument().mime_type;
                    AndroidUtilities.openForView(file, object.getFileName(), mime, getParentActivity(), getResourceProvider(), false);
                }
            });
        });
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {}
    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) { return false; }
}
