/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.storage;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/** One previous version of an edited message. */
public final class StoredEdit {
    public final long dialogId;
    public final int msgId;
    public final int version;
    /** Edit date of this version (0 for the original, unedited text). */
    public final int editDate;
    public final String text;
    public final ArrayList<TLRPC.MessageEntity> entities;
    /** May be null when the version had no media or media did not change. */
    public final TLRPC.MessageMedia media;

    StoredEdit(long dialogId, int msgId, int version, int editDate, String text,
               ArrayList<TLRPC.MessageEntity> entities, TLRPC.MessageMedia media) {
        this.dialogId = dialogId;
        this.msgId = msgId;
        this.version = version;
        this.editDate = editDate;
        this.text = text;
        this.entities = entities;
        this.media = media;
    }
}
