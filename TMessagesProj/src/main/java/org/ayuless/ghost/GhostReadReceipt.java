/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.ghost;

import org.ayuless.extras.ExtrasConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.function.Consumer;

/** Queries the real inbox watermark, never the locally cleared ghost-mode unread flag. */
public final class GhostReadReceipt {
    public static final int UNKNOWN = -1, NOT_SENT = 0, SENT = 1;

    private GhostReadReceipt() { }

    public static boolean shouldShow(int account, MessageObject message) {
        return account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT && message != null
                && ExtrasConfig.get(ExtrasConfig.KEY_GHOST_READ_RECEIPT) && GhostConfig.anyActive(account)
                && !message.isOutOwner() && !message.scheduled && message.getId() > 0
                && DialogObject.isUserDialog(message.getDialogId())
                && message.getDialogId() != UserConfig.getInstance(account).getClientUserId();
    }

    /** This read-only RPC does not acknowledge the message or update local unread counters. */
    public static int query(int account, MessageObject message, Consumer<Integer> callback) {
        if (!shouldShow(account, message)) {
            callback.accept(UNKNOWN);
            return 0;
        }
        final long dialog = message.getDialogId();
        final int messageId = message.getId();
        TLRPC.InputPeer peer = MessagesController.getInstance(account).getInputPeer(dialog);
        if (peer == null || peer instanceof TLRPC.TL_inputPeerEmpty) {
            callback.accept(UNKNOWN);
            return 0;
        }
        TLRPC.TL_inputDialogPeer input = new TLRPC.TL_inputDialogPeer();
        input.peer = peer;
        TLRPC.TL_messages_getPeerDialogs request = new TLRPC.TL_messages_getPeerDialogs();
        request.peers.add(input);
        return ConnectionsManager.getInstance(account).sendRequest(request, (response, error) -> {
            int status = UNKNOWN;
            if (error == null && response instanceof TLRPC.TL_messages_peerDialogs) {
                for (TLRPC.Dialog result : ((TLRPC.TL_messages_peerDialogs) response).dialogs) {
                    if (result.peer != null && DialogObject.getPeerDialogId(result.peer) == dialog) {
                        status = result.read_inbox_max_id >= messageId ? SENT : NOT_SENT;
                        break;
                    }
                }
            }
            final int resolved = status;
            AndroidUtilities.runOnUIThread(() -> callback.accept(resolved));
        });
    }
}
