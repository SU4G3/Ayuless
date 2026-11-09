/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package org.ayuless.edits;

import android.content.Context;
import android.view.View;
import org.ayuless.storage.AyulessStore;
import org.ayuless.storage.StoredEdit;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.ActionBar.Theme;
import java.util.ArrayList;
import static org.telegram.messenger.LocaleController.getString;

/** AyuMessageHistory/AyuMessageCell adapted to native TL snapshots and AyulessStore. */
public class EditHistoryActivity extends UniversalFragment {
    private final MessageObject original;
    private final ArrayList<MessageObject> versions = new ArrayList<>();
    private final ArrayList<CharSequence> labels = new ArrayList<>();
    private boolean loaded;
    public EditHistoryActivity(MessageObject original) {
        this.original = original;
        currentAccount = original.currentAccount;
    }
    @Override protected CharSequence getTitle() { return getString(R.string.AyulessEditsHistoryTitle); }
    @Override public boolean onFragmentCreate() {
        if (!super.onFragmentCreate()) return false;
        AyulessStore.getInstance(currentAccount).getEdits(original.getDialogId(), original.getId(), edits -> {
            if (isFinished) return;
            versions.clear();
            labels.clear();
            ArrayList<TLRPC.Message> snapshots = new ArrayList<>();
            TLRPC.Message current = copy(original.messageOwner);
            if (current != null) {
                current.attachPath = original.messageOwner.attachPath;
                snapshots.add(current);
                labels.add(getString(R.string.AyulessHistoryCurrent));
            }
            for (int i = edits.size() - 1; i >= 0; i--) {
                StoredEdit edit = edits.get(i);
                TLRPC.Message message = copy(original.messageOwner);
                if (message == null) continue;
                message.message = edit.text == null ? "" : edit.text;
                message.entities = edit.entities == null ? new ArrayList<>() : edit.entities;
                message.flags = message.entities.isEmpty() ? message.flags & ~TLRPC.MESSAGE_FLAG_HAS_ENTITIES : message.flags | TLRPC.MESSAGE_FLAG_HAS_ENTITIES;
                message.media = edit.media;
                message.flags = message.media == null ? message.flags & ~TLRPC.MESSAGE_FLAG_HAS_MEDIA : message.flags | TLRPC.MESSAGE_FLAG_HAS_MEDIA;
                message.date = edit.editDate == 0 ? original.messageOwner.date : edit.editDate;
                message.edit_date = 0;
                message.flags &= ~TLRPC.MESSAGE_FLAG_EDITED;
                message.edit_hide = true;
                message.reactions = null;
                message.flags &= ~(1 << 20);
                message.reply_to = null;
                message.flags &= ~TLRPC.MESSAGE_FLAG_REPLY;
                // A revision is a local preview, never a new read or a scheduled message.
                snapshots.add(message);
                labels.add(edit.editDate == 0 ? getString(R.string.AyulessHistoryOriginal)
                        : LocaleController.formatString(R.string.AyulessHistoryVersion, edit.version + 1));
            }
            org.ayuless.spy.SavedAttachments.prepareHistory(currentAccount, snapshots, () -> {
                if (isFinished) return;
                for (TLRPC.Message snapshot : snapshots) {
                    MessageObject object = new MessageObject(currentAccount, snapshot, true, true);
                    object.preview = true;
                    versions.add(object);
                }
                loaded = true;
                if (listView != null && listView.adapter != null) listView.adapter.update(false);
            });
        });
        return true;
    }
    @Override protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (!loaded) { items.add(UItem.asShadow(getString(R.string.Loading))); return; }
        for (int i = 0; i < versions.size(); i++) {
            MessageObject object = versions.get(i);
            items.add(UItem.asHeader(labels.get(i)));
            if (getParentActivity() != null) items.add(UItem.asCustom(new HistoryCell(getParentActivity(), object)));
        }
        items.add(UItem.asShadow(getString(R.string.AyulessHistoryHint)));
    }
    @Override protected void onClick(UItem item, View view, int position, float x, float y) {}
    @Override protected boolean onLongClick(UItem item, View view, int position, float x, float y) { return false; }

    private class HistoryCell extends ChatMessageCell {
        HistoryCell(Context context, MessageObject object) {
            super(context, EditHistoryActivity.this.currentAccount);
            setFullyDraw(true);
            isChat = false;
            setDelegate(new ChatMessageCellDelegate() {});
            setMessageObject(object, null, false, false, true);
            setOnClickListener(v -> {
                if (object.messageOwner.media != null && (object.isPhoto() || object.getDocument() != null)) {
                    org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
                        java.io.File file = org.ayuless.content.ProtectedContent.readableFile(currentAccount, object.messageOwner);
                        AndroidUtilities.runOnUIThread(() -> {
                            if (isFinished || getParentActivity() == null) return;
                            if (file == null) BulletinFactory.of(EditHistoryActivity.this).createErrorBulletin(getString(R.string.AyulessProtectedForwardNeedsDownload)).show();
                            else AndroidUtilities.openForView(file, object.getFileName(), object.getMimeType(), getParentActivity(), getResourceProvider(), false);
                        });
                    });
                } else copyText(object);
            });
            setOnLongClickListener(v -> { copyText(object); return true; });
            setBackgroundColor(Theme.getColor(Theme.key_chat_wallpaper, getResourceProvider()));
        }
    }
    private void copyText(MessageObject object) {
        if (object.messageOwner.message == null || object.messageOwner.message.isEmpty()) return;
        AndroidUtilities.addToClipboard(object.messageOwner.message);
        BulletinFactory.of(this).createCopyBulletin(getString(R.string.MessageCopied)).show();
    }
    private static TLRPC.Message copy(TLRPC.Message original) {
        NativeByteBuffer buffer = null;
        try {
            buffer = new NativeByteBuffer(original.getObjectSize());
            original.serializeToStream(buffer);
            buffer.rewind();
            TLRPC.Message message = TLRPC.Message.TLdeserialize(buffer, buffer.readInt32(true), true);
            message.dialog_id = MessageObject.getDialogId(original);
            message.attachPath = "";
            return message;
        } catch (Exception e) { org.telegram.messenger.FileLog.e(e); return null; }
        finally { if (buffer != null) buffer.reuse(); }
    }
}
