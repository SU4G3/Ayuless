/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.storage;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;

/**
 * Local store for deleted messages and edit history of one account.
 *
 * <p>All database work runs on a dedicated single-thread queue; results are delivered on the
 * UI thread. Secret-chat and self-destructing messages are never written (see {@link #isStorable}).
 */
public final class AyulessStore {

    public static final String PREFS = "ayuless_storage";
    public static final String KEY_LIMIT_BYTES = "limit_bytes";
    public static final long DEFAULT_LIMIT_BYTES = 100L * 1024 * 1024;

    /** Run the size check after this many writes, not on every insert. */
    private static final int CLEANUP_EVERY_WRITES = 50;
    /** Rows removed per cleanup round while still over the limit. */
    private static final int CLEANUP_BATCH = 200;
    /** VACUUM at most this often. */
    private static final long VACUUM_INTERVAL_MS = 24L * 60 * 60 * 1000;

    private static final AyulessStore[] instances = new AyulessStore[UserConfig.MAX_ACCOUNT_COUNT];
    private static final DispatchQueue queue = new DispatchQueue("ayulessStoreQueue");

    private final int account;
    private AyulessDbHelper helper;
    private int writesSinceCleanup;
    private long lastVacuum;

    public static AyulessStore getInstance(int account) {
        AyulessStore local = instances[account];
        if (local == null) {
            synchronized (AyulessStore.class) {
                local = instances[account];
                if (local == null) {
                    instances[account] = local = new AyulessStore(account);
                }
            }
        }
        return local;
    }

    private AyulessStore(int account) {
        this.account = account;
    }

    public interface Callback<T> {
        void run(T result);
    }

    // ---- privacy policy ----

    /**
     * Hard privacy rule: secret chats and anything with a self-destruct / auto-delete timer is
     * never stored.
     */
    public static boolean isStorable(TLRPC.Message message) {
        if (message == null) {
            return false;
        }
        long dialogId = MessageObject.getDialogId(message);
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        }
        if (message.ttl != 0 || message.ttl_period != 0 || message.destroyTime != 0 || message.destroyTimeMillis != 0) {
            return false;
        }
        if (message.media != null && message.media.ttl_seconds != 0) {
            return false;
        }
        return !(message.action instanceof TLRPC.TL_messageEncryptedAction);
    }

    // ---- writes ----

    /** Save a message that was deleted on the server side. No-op for non-storable messages. */
    public void saveDeleted(TLRPC.Message message) {
        if (!isStorable(message)) {
            return;
        }
        final long dialogId = MessageObject.getDialogId(message);
        final int msgId = message.id;
        final Long topicId = topicIdOf(message);
        final int date = message.date;
        final Long fromId = message.from_id != null ? MessageObject.getPeerId(message.from_id) : null;
        final byte[] data = serialize(message);
        if (data == null) {
            return;
        }
        final long deletedAt = System.currentTimeMillis() / 1000;
        queue.postRunnable(() -> {
            try {
                ContentValues values = new ContentValues();
                values.put("dialog_id", dialogId);
                values.put("msg_id", msgId);
                if (topicId != null) {
                    values.put("topic_id", topicId);
                }
                values.put("date", date);
                values.put("deleted_at", deletedAt);
                if (fromId != null) {
                    values.put("from_id", fromId);
                }
                values.put("data", data);
                db().insertWithOnConflict("deleted_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                afterWrite();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    /**
     * Save the version of a message as it was before an edit. Call with the old message object
     * before applying the server edit.
     */
    public void saveEdit(TLRPC.Message before) {
        if (!isStorable(before)) {
            return;
        }
        final long dialogId = MessageObject.getDialogId(before);
        final int msgId = before.id;
        final int editDate = before.edit_date;
        final String text = before.message;
        final byte[] entities = serializeEntities(before.entities);
        final byte[] media = before.media != null && !(before.media instanceof TLRPC.TL_messageMediaEmpty) ? serialize(before.media) : null;
        queue.postRunnable(() -> {
            try {
                SQLiteDatabase db = db();
                int version = 0;
                try (Cursor c = db.rawQuery("SELECT MAX(version) FROM edit_history WHERE dialog_id = ? AND msg_id = ?",
                        new String[]{String.valueOf(dialogId), String.valueOf(msgId)})) {
                    if (c.moveToFirst() && !c.isNull(0)) {
                        version = c.getInt(0) + 1;
                    }
                }
                ContentValues values = new ContentValues();
                values.put("dialog_id", dialogId);
                values.put("msg_id", msgId);
                values.put("edit_date", editDate);
                values.put("version", version);
                values.put("text", text);
                values.put("entities", entities);
                if (media != null) {
                    values.put("media", media);
                }
                db.insertWithOnConflict("edit_history", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                afterWrite();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void deleteDialog(long dialogId) {
        queue.postRunnable(() -> {
            try {
                String[] args = {String.valueOf(dialogId)};
                SQLiteDatabase db = db();
                db.delete("deleted_messages", "dialog_id = ?", args);
                db.delete("edit_history", "dialog_id = ?", args);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    /** Remove all stored data and delete the database file (used on logout). */
    public void clearAll() {
        queue.postRunnable(() -> {
            try {
                if (helper != null) {
                    helper.close();
                    helper = null;
                }
                SQLiteDatabase.deleteDatabase(dbFile());
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    // ---- reads ----

    public void isDeleted(long dialogId, int msgId, Callback<Boolean> callback) {
        queue.postRunnable(() -> {
            boolean result = false;
            try (Cursor c = db().rawQuery("SELECT 1 FROM deleted_messages WHERE dialog_id = ? AND msg_id = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(msgId)})) {
                result = c.moveToFirst();
            } catch (Exception e) {
                FileLog.e(e);
            }
            final boolean r = result;
            AndroidUtilities.runOnUIThread(() -> callback.run(r));
        });
    }

    /** Deleted messages of a dialog, newest first. */
    public void getDeleted(long dialogId, int limit, int offset, Callback<ArrayList<TLRPC.Message>> callback) {
        queue.postRunnable(() -> {
            ArrayList<TLRPC.Message> result = new ArrayList<>();
            try (Cursor c = db().rawQuery("SELECT data FROM deleted_messages WHERE dialog_id = ? ORDER BY msg_id DESC LIMIT ? OFFSET ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(limit), String.valueOf(offset)})) {
                while (c.moveToNext()) {
                    TLRPC.Message message = deserializeMessage(c.getBlob(0));
                    if (message != null) {
                        message.dialog_id = dialogId;
                        result.add(message);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> callback.run(result));
        });
    }

    /** All (dialog_id, msg_id) pairs of stored deleted messages. Callback runs on the UI thread. */
    public void loadDeletedIds(Callback<java.util.HashMap<Long, java.util.HashSet<Integer>>> callback) {
        queue.postRunnable(() -> {
            java.util.HashMap<Long, java.util.HashSet<Integer>> result = new java.util.HashMap<>();
            try (Cursor c = db().rawQuery("SELECT dialog_id, msg_id FROM deleted_messages", null)) {
                while (c.moveToNext()) {
                    long dialogId = c.getLong(0);
                    java.util.HashSet<Integer> ids = result.get(dialogId);
                    if (ids == null) {
                        result.put(dialogId, ids = new java.util.HashSet<>());
                    }
                    ids.add(c.getInt(1));
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> callback.run(result));
        });
    }

    /** Previous versions of a message, oldest first. */
    public void getEdits(long dialogId, int msgId, Callback<ArrayList<StoredEdit>> callback) {
        queue.postRunnable(() -> {
            ArrayList<StoredEdit> result = new ArrayList<>();
            try (Cursor c = db().rawQuery("SELECT version, edit_date, text, entities, media FROM edit_history WHERE dialog_id = ? AND msg_id = ? ORDER BY version ASC",
                    new String[]{String.valueOf(dialogId), String.valueOf(msgId)})) {
                while (c.moveToNext()) {
                    result.add(new StoredEdit(dialogId, msgId, c.getInt(0), c.getInt(1), c.getString(2),
                            deserializeEntities(c.getBlob(3)), c.isNull(4) ? null : deserializeMedia(c.getBlob(4))));
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> callback.run(result));
        });
    }

    public void sizeBytes(Callback<Long> callback) {
        queue.postRunnable(() -> {
            final long size = sizeBytesInternal();
            AndroidUtilities.runOnUIThread(() -> callback.run(size));
        });
    }

    // ---- size limit ----

    public static long getLimitBytes() {
        return prefs().getLong(KEY_LIMIT_BYTES, DEFAULT_LIMIT_BYTES);
    }

    public static void setLimitBytes(long bytes) {
        prefs().edit().putLong(KEY_LIMIT_BYTES, bytes).apply();
    }

    private void afterWrite() {
        if (++writesSinceCleanup < CLEANUP_EVERY_WRITES) {
            return;
        }
        writesSinceCleanup = 0;
        enforceLimit();
    }

    private void enforceLimit() {
        long limit = getLimitBytes();
        if (limit <= 0) {
            return;
        }
        SQLiteDatabase db = db();
        boolean removed = false;
        int guard = 0;
        while (sizeBytesInternal() > limit && guard++ < 1000) {
            int deleted = db.delete("deleted_messages",
                    "rowid IN (SELECT rowid FROM deleted_messages ORDER BY deleted_at ASC LIMIT " + CLEANUP_BATCH + ")", null);
            int edits = db.delete("edit_history",
                    "rowid IN (SELECT rowid FROM edit_history ORDER BY edit_date ASC LIMIT " + CLEANUP_BATCH + ")", null);
            if (deleted == 0 && edits == 0) {
                break;
            }
            removed = true;
            // Free pages are reused by SQLite but do not shrink the file; checkpoint the WAL so
            // page_count reflects live data more closely.
            db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).close();
        }
        long now = System.currentTimeMillis();
        if (removed && now - lastVacuum > VACUUM_INTERVAL_MS) {
            lastVacuum = now;
            try {
                db.execSQL("VACUUM");
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    /** Live data size: used pages only, so deletions count immediately even before VACUUM. */
    private long sizeBytesInternal() {
        try {
            SQLiteDatabase db = db();
            long pageSize = longPragma(db, "page_size");
            long pageCount = longPragma(db, "page_count");
            long freePages = longPragma(db, "freelist_count");
            return Math.max(0, pageCount - freePages) * pageSize;
        } catch (Exception e) {
            FileLog.e(e);
            return 0;
        }
    }

    private static long longPragma(SQLiteDatabase db, String name) {
        try (Cursor c = db.rawQuery("PRAGMA " + name, null)) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }

    // ---- helpers ----

    private SQLiteDatabase db() {
        if (helper == null) {
            helper = new AyulessDbHelper(ApplicationLoader.applicationContext, dbFile());
        }
        return helper.getWritableDatabase();
    }

    /** Same per-account directory layout as MessagesStorage (account 0 in the files root). */
    private File dbFile() {
        File dir = ApplicationLoader.getFilesDirFixed();
        if (account != 0) {
            dir = new File(dir, "account" + account + "/");
            dir.mkdirs();
        }
        return new File(dir, "ayuless_" + account + ".db");
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Long topicIdOf(TLRPC.Message message) {
        if (message.reply_to != null && message.reply_to.forum_topic) {
            int top = message.reply_to.reply_to_top_id != 0 ? message.reply_to.reply_to_top_id : message.reply_to.reply_to_msg_id;
            return (long) top;
        }
        return null;
    }

    private static byte[] serialize(org.telegram.tgnet.TLObject object) {
        try {
            SerializedData data = new SerializedData(object.getObjectSize());
            object.serializeToStream(data);
            byte[] bytes = data.toByteArray();
            data.cleanup();
            return bytes;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private static byte[] serializeEntities(ArrayList<TLRPC.MessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        try {
            SerializedData sizer = new SerializedData(true);
            sizer.writeInt32(entities.size());
            for (TLRPC.MessageEntity entity : entities) {
                entity.serializeToStream(sizer);
            }
            SerializedData data = new SerializedData(sizer.length());
            sizer.cleanup();
            data.writeInt32(entities.size());
            for (TLRPC.MessageEntity entity : entities) {
                entity.serializeToStream(data);
            }
            byte[] bytes = data.toByteArray();
            data.cleanup();
            return bytes;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private static TLRPC.Message deserializeMessage(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        SerializedData data = new SerializedData(bytes);
        try {
            return TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        } finally {
            data.cleanup();
        }
    }

    private static TLRPC.MessageMedia deserializeMedia(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        SerializedData data = new SerializedData(bytes);
        try {
            return TLRPC.MessageMedia.TLdeserialize(data, data.readInt32(false), false);
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        } finally {
            data.cleanup();
        }
    }

    private static ArrayList<TLRPC.MessageEntity> deserializeEntities(byte[] bytes) {
        ArrayList<TLRPC.MessageEntity> result = new ArrayList<>();
        if (bytes == null) {
            return result;
        }
        SerializedData data = new SerializedData(bytes);
        try {
            int count = data.readInt32(false);
            for (int i = 0; i < count; i++) {
                TLRPC.MessageEntity entity = TLRPC.MessageEntity.TLdeserialize(data, data.readInt32(false), false);
                if (entity != null) {
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            data.cleanup();
        }
        return result;
    }
}
