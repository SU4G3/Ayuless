// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.content;

import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

/** Independent TL copies for storage and local previews. */
public final class MessageSnapshots {
    private MessageSnapshots() {}

    public static TLRPC.Message copy(TLRPC.Message source) {
        int flags = source.flags;
        int flags2 = source.flags2;
        SerializedData output = new SerializedData();
        try {
            source.serializeToStream(output);
            SerializedData input = new SerializedData(output.toByteArray());
            try {
                TLRPC.Message result = TLRPC.Message.TLdeserialize(input, input.readInt32(true), true);
                if (result == null) throw new IllegalStateException("Message snapshot unavailable");
                result.dialog_id = org.telegram.messenger.MessageObject.getDialogId(source);
                result.attachPath = source.attachPath;
                return result;
            } finally {
                input.cleanup();
            }
        } finally {
            source.flags = flags;
            source.flags2 = flags2;
            output.cleanup();
        }
    }
}
