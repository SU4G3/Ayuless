/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.ghost;

import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessageChatArguments;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

/**
 * Delayed send: a message is sent as a scheduled message a few seconds ahead instead of
 * immediately. Sending a scheduled message does not mark the account online, and the
 * server posts it on its own when the time comes.
 *
 * Only regular sends via {@link SendMessagesHelper#sendMessage(SendMessagesHelper.SendMessageParams)}
 * are delayed. Forwards, retries, quick replies, story replies, secret chats and Saved
 * Messages are sent as usual. Works without the ghost master switch.
 */
public final class DelayedSend {

    /** Delay for text messages, in seconds. */
    public static final int DELAY_TEXT = 12;
    /** Delay for messages with media, which may take longer to upload, in seconds. */
    public static final int DELAY_MEDIA = 20;
    /** Minimal distance to "now" a scheduled date still gets when the request leaves. */
    private static final int MIN_AHEAD = 5;
    /** Sends in the same chat within this window share one date, so albums stay together. */
    private static final int GROUP_WINDOW = 3;

    private static final String KEY_HINT_SHOWN = "delayedSendHintShown";

    private static int lastAccount = -1;
    private static long lastDialog;
    private static int lastComputedAt;
    private static int lastDate;

    private DelayedSend() {
    }

    /** @return the schedule date to use for this send; unchanged when delayed send does not apply. */
    public static int scheduleDate(int account, SendMessagesHelper.SendMessageParams params) {
        int scheduleDate = params.scheduleDate;
        if (scheduleDate != 0 || !GhostConfig.delayedSend(account)) {
            return scheduleDate;
        }
        SendMessageChatArguments args = params.sendMessageChatArguments != null ? params.sendMessageChatArguments : SendMessageChatArguments.EMPTY;
        long dialogId = params.peer;
        if (dialogId == 0
                || DialogObject.isEncryptedDialog(dialogId)
                || dialogId == UserConfig.getInstance(account).getClientUserId()
                || params.retryMessageObject != null
                || params.replyToStoryItem != null
                || params.sendingStory != null
                || params.quick_reply_shortcut != null || params.quick_reply_shortcut_id != 0
                || args.quickReplyShortcut != null || args.quickReplyShortcutId != 0
                || args.welcomeMessageChatId != 0) {
            return 0;
        }
        boolean media = params.photo != null || params.document != null || params.location != null
                || params.poll != null || params.path != null;
        return compute(account, dialogId, media);
    }

    private static synchronized int compute(int account, long dialogId, boolean media) {
        int now = ConnectionsManager.getInstance(account).getCurrentTime();
        if (account == lastAccount && dialogId == lastDialog && now - lastComputedAt <= GROUP_WINDOW && lastDate > now) {
            return lastDate;
        }
        lastAccount = account;
        lastDialog = dialogId;
        lastComputedAt = now;
        lastDate = now + (media ? DELAY_MEDIA : DELAY_TEXT);
        showHintOnce();
        return lastDate;
    }

    /**
     * A long upload can push a scheduled request past its date. Keep the date a few seconds
     * in the future when the request actually leaves, so the server still schedules it.
     */
    public static void fixOnSend(int account, TLObject req) {
        if (!GhostConfig.delayedSend(account)) {
            return;
        }
        int min = ConnectionsManager.getInstance(account).getCurrentTime() + MIN_AHEAD;
        if (req instanceof TLRPC.TL_messages_sendMessage) {
            TLRPC.TL_messages_sendMessage r = (TLRPC.TL_messages_sendMessage) req;
            if (r.schedule_date != 0 && r.schedule_date < min) r.schedule_date = min;
        } else if (req instanceof TLRPC.TL_messages_sendMedia) {
            TLRPC.TL_messages_sendMedia r = (TLRPC.TL_messages_sendMedia) req;
            if (r.schedule_date != 0 && r.schedule_date < min) r.schedule_date = min;
        } else if (req instanceof TLRPC.TL_messages_sendMultiMedia) {
            TLRPC.TL_messages_sendMultiMedia r = (TLRPC.TL_messages_sendMultiMedia) req;
            if (r.schedule_date != 0 && r.schedule_date < min) r.schedule_date = min;
        }
    }

    private static void showHintOnce() {
        if (GhostConfig.profilePrefs(GhostConfig.PROFILE_ALL).getBoolean(KEY_HINT_SHOWN, false)) {
            return;
        }
        GhostConfig.profilePrefs(GhostConfig.PROFILE_ALL).edit().putBoolean(KEY_HINT_SHOWN, true).apply();
        AndroidUtilities.runOnUIThread(() -> {
            try {
                Toast.makeText(ApplicationLoader.applicationContext,
                        LocaleController.getString(R.string.AyulessGhostDelayedSendHint), Toast.LENGTH_LONG).show();
            } catch (Exception ignore) {
            }
        });
    }
}
