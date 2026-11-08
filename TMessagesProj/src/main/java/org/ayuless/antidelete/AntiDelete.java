/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.antidelete;

import android.text.TextUtils;

import org.ayuless.spy.SpyExceptions;
import org.ayuless.storage.AyulessStore;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.collection.LongSparseArray;

/**
 * Anti-delete: messages removed by the server (deleted by the other side, an admin, or
 * "clear history for both") stay in the local database and in the chat, marked as deleted.
 * Deletions made by this client are left alone (see {@link LocalDeletes}).
 */
public final class AntiDelete {

    private AntiDelete() {
    }

    /**
     * Removes kept messages from a pending server-deletion batch and stores them.
     *
     * @param deleted map of update key (channel dialog id, or 0 for non-channel) to message ids,
     *                as built by MessagesController.processUpdateArray. Not modified.
     * @return the batch that should actually be deleted locally (may be the same object, or null
     *         if nothing is left).
     */
    public static LongSparseArray<ArrayList<Integer>> filter(int account, LongSparseArray<ArrayList<Integer>> deleted) {
        if (deleted == null || deleted.size() == 0 || !AntiDeleteConfig.isEnabled(account)) {
            return deleted;
        }
        final ArrayList<TLRPC.Message> kept = new ArrayList<>();
        final MessagesStorage storage = MessagesStorage.getInstance(account);
        final CountDownLatch latch = new CountDownLatch(1);
        storage.getStorageQueue().postRunnable(() -> {
            try {
                long selfId = UserConfig.getInstance(account).getClientUserId();
                for (int a = 0; a < deleted.size(); a++) {
                    long key = deleted.keyAt(a);
                    ArrayList<Integer> candidates = new ArrayList<>();
                    for (Integer id : deleted.valueAt(a)) {
                        if (!LocalDeletes.contains(account, key, id)) {
                            candidates.add(id);
                        }
                    }
                    if (candidates.isEmpty()) {
                        continue;
                    }
                    String ids = TextUtils.join(",", candidates);
                    String where = key != 0 ? "uid = " + key : "is_channel = 0";
                    SQLiteCursor cursor = storage.getDatabase().queryFinalized(String.format(Locale.US,
                            "SELECT uid, data FROM messages_v2 WHERE mid IN(%s) AND %s", ids, where));
                    try {
                        while (cursor.next()) {
                            long uid = cursor.longValue(0);
                            NativeByteBuffer data = cursor.byteBufferValue(1);
                            if (data == null) {
                                continue;
                            }
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            if (message != null) {
                                message.readAttachPath(data, selfId);
                                message.dialog_id = uid;
                                if (AyulessStore.isStorable(message) && !SpyExceptions.isExcepted(account, uid)) {
                                    kept.add(message);
                                }
                            }
                            data.reuse();
                        }
                    } finally {
                        cursor.dispose();
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
                kept.clear();
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                return deleted;
            }
        } catch (InterruptedException e) {
            return deleted;
        }
        if (kept.isEmpty()) {
            return deleted;
        }

        HashSet<String> keptKeys = new HashSet<>();
        for (TLRPC.Message m : kept) {
            keptKeys.add(LocalDeletes.updateKey(account, m.dialog_id) + ":" + m.id);
        }
        LongSparseArray<ArrayList<Integer>> result = new LongSparseArray<>();
        for (int a = 0; a < deleted.size(); a++) {
            long key = deleted.keyAt(a);
            ArrayList<Integer> left = new ArrayList<>();
            for (Integer id : deleted.valueAt(a)) {
                if (!keptKeys.contains(key + ":" + id)) {
                    left.add(id);
                }
            }
            if (!left.isEmpty()) {
                result.put(key, left);
            }
        }

        AyulessStore store = AyulessStore.getInstance(account);
        for (TLRPC.Message m : kept) {
            store.saveDeleted(m);
            org.ayuless.spy.SavedAttachments.onKept(account, m);
            DeletedCache.mark(account, m.dialog_id, m.id);
        }
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(account)
                .postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_AVATAR));
        return result.size() == 0 ? null : result;
    }

    /** Prefixes the time label of a kept message with the deleted mark (see DeletedStyle). */
    public static CharSequence decorateTime(int account, MessageObject messageObject, CharSequence time) {
        return org.ayuless.appearance.DeletedStyle.decorateTime(account, messageObject, time);
    }
}
