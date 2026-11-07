/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

/**
 * Per-account SQLite database for Ayuless data (deleted messages, edit history).
 * Kept separate from the client's cache4.db so upstream schema migrations never touch it.
 */
final class AyulessDbHelper extends SQLiteOpenHelper {

    static final int DB_VERSION = 1;

    AyulessDbHelper(Context context, File dbFile) {
        super(context, dbFile.getAbsolutePath(), null, DB_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createV1(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Explicit migration ladder: step v upgrades the schema from v to v + 1.
        for (int v = oldVersion; v < newVersion; v++) {
            switch (v) {
                // case 1: migrateV1ToV2(db); break;
                default:
                    throw new IllegalStateException("No Ayuless DB migration from v" + v);
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Data is a local convenience copy: on downgrade start fresh instead of crashing.
        db.execSQL("DROP TABLE IF EXISTS deleted_messages");
        db.execSQL("DROP TABLE IF EXISTS edit_history");
        createV1(db);
    }

    private static void createV1(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS deleted_messages (" +
                "dialog_id INTEGER NOT NULL, " +
                "msg_id INTEGER NOT NULL, " +
                "topic_id INTEGER, " +
                "date INTEGER NOT NULL, " +
                "deleted_at INTEGER NOT NULL, " +
                "from_id INTEGER, " +
                "data BLOB NOT NULL, " +
                "PRIMARY KEY (dialog_id, msg_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS deleted_messages_dialog ON deleted_messages (dialog_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS deleted_messages_deleted_at ON deleted_messages (deleted_at)");

        db.execSQL("CREATE TABLE IF NOT EXISTS edit_history (" +
                "dialog_id INTEGER NOT NULL, " +
                "msg_id INTEGER NOT NULL, " +
                "edit_date INTEGER NOT NULL, " +
                "version INTEGER NOT NULL, " +
                "text TEXT, " +
                "entities BLOB, " +
                "media BLOB, " +
                "PRIMARY KEY (dialog_id, msg_id, version))");
        db.execSQL("CREATE INDEX IF NOT EXISTS edit_history_dialog ON edit_history (dialog_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS edit_history_edit_date ON edit_history (edit_date)");
    }
}
