/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.edits;

import android.text.TextUtils;

import org.ayuless.spy.SpyExceptions;
import org.ayuless.storage.AyulessStore;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * Keeps previous versions of messages edited on the server. Called from
 * MessagesController before the edited message is written to the client database.
 */
public final class EditHistory {

    private EditHistory() {
    }

    /**
     * Must be called before MessagesStorage.putMessages for the edit is queued: the lookup runs on
     * the storage queue, so it reads the row as it was before the edit.
     */
    public static void onServerEdit(int account, TLRPC.Message edited) {
        if (edited == null || !EditsConfig.isEnabled(account)) {
            return;
        }
        final long dialogId = MessageObject.getDialogId(edited);
        if (DialogObject.isEncryptedDialog(dialogId) || SpyExceptions.isExcepted(account, dialogId)) {
            return;
        }
        final int msgId = edited.id;
        final String newText = edited.message;
        final ArrayList<TLRPC.MessageEntity> newEntities = edited.entities != null ? new ArrayList<>(edited.entities) : new ArrayList<>();
        final long newMediaId = mediaId(edited.media);
        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            TLRPC.Message old = readStored(storage, dialogId, msgId);
            if (old == null || !AyulessStore.isStorable(account, old, org.ayuless.content.RetentionConfig.EDIT)) {
                return;
            }
            if (!contentChanged(old, newText, newEntities, newMediaId)) {
                return;
            }
            AyulessStore.getInstance(account).saveEdit(old);
            EditedCache.mark(account, dialogId, msgId);
        });
    }

    private static TLRPC.Message readStored(MessagesStorage storage, long dialogId, int msgId) {
        SQLiteCursor cursor = null;
        try {
            cursor = storage.getDatabase().queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + dialogId + " AND mid = " + msgId + " LIMIT 1");
            if (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data != null) {
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    data.reuse();
                    if (message != null) {
                        message.dialog_id = dialogId;
                    }
                    return message;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return null;
    }

    static boolean contentChanged(TLRPC.Message old, String newText, ArrayList<TLRPC.MessageEntity> newEntities, long newMediaId) {
        if (!TextUtils.equals(nonNull(old.message), nonNull(newText))) {
            return true;
        }
        if (!entitiesEqual(old.entities, newEntities)) {
            return true;
        }
        long oldMediaId = mediaId(old.media);
        return oldMediaId != 0 && newMediaId != 0 && oldMediaId != newMediaId;
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    private static boolean entitiesEqual(ArrayList<TLRPC.MessageEntity> a, ArrayList<TLRPC.MessageEntity> b) {
        int na = a == null ? 0 : a.size();
        int nb = b == null ? 0 : b.size();
        if (na != nb) {
            return false;
        }
        for (int i = 0; i < na; i++) {
            TLRPC.MessageEntity x = a.get(i);
            TLRPC.MessageEntity y = b.get(i);
            if (x == null || y == null) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            if (x.getClass() != y.getClass() || x.offset != y.offset || x.length != y.length || !TextUtils.equals(x.url, y.url)) {
                return false;
            }
        }
        return true;
    }

    /** Photo or document id of the message media, 0 if none. */
    private static long mediaId(TLRPC.MessageMedia media) {
        if (media == null) {
            return 0;
        }
        if (media.photo != null) {
            return media.photo.id;
        }
        if (media.document != null) {
            return media.document.id;
        }
        return 0;
    }

    public static boolean hasHistory(int account, MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        return EditedCache.hasHistory(account, messageObject.getDialogId(), messageObject.getId());
    }
}
