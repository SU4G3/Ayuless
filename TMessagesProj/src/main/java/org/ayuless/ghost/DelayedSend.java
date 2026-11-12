/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.ghost;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessageChatArguments;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Delayed send: a message is sent as a scheduled message a few seconds ahead instead of
 * immediately. Sending a scheduled message does not mark the account online, and the
 * server posts it on its own when the time comes.
 *
 * Delayed: regular sends, forwards, story replies, Saved Messages, and retries of messages
 * that were already sent as scheduled (they keep their date, moved a few seconds ahead if
 * it passed). Not delayed:
 * <ul>
 * <li>retries of messages that failed as normal sends: the failed message already lives in
 * the chat history, turning it into a scheduled one would leave a stuck local copy;</li>
 * <li>quick reply shortcuts sent into a chat ({@code messages.sendQuickReplyMessages} has
 * no {@code schedule_date});</li>
 * <li>editing quick reply and welcome message templates: these are not sends to a chat;</li>
 * <li>stories.</li>
 * </ul>
 * Secret chats use a client-side hold instead (see EncryptedDelayedSend).
 * Works without the ghost master switch.
 */
public final class DelayedSend {

    /** Delay for text messages, in seconds. */
    public static final int DELAY_TEXT = 12;
    /** Delay for messages with media, which may take longer to upload, in seconds. */
    public static final int DELAY_MEDIA = 20;
    /** Minimal distance to "now" a scheduled date still gets when the request leaves. */
    private static final int MIN_AHEAD = 11;
    /** Sends in the same chat within this window share one date, so albums stay together. */
    private static final int GROUP_WINDOW = 3;

    private static final String AUTOMATIC = "ayuless_ghost_delay";
    private static final Set<SendMessageChatArguments> automaticArguments = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

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
                || params.retryMessageObject != null
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

    /** Schedule date for forwarding messages to {@code dialogId}; unchanged when delayed send does not apply. */
    public static int forwardScheduleDate(int account, long dialogId, int scheduleDate, ArrayList<MessageObject> messages) {
        if (scheduleDate != 0 || dialogId == 0 || !GhostConfig.delayedSend(account) || DialogObject.isEncryptedDialog(dialogId)) {
            return scheduleDate;
        }
        boolean media = false;
        if (messages != null) {
            for (int i = 0; i < messages.size(); i++) {
                MessageObject m = messages.get(i);
                if (m != null && (m.getQuickReplyName() != null || m.getQuickReplyId() != 0)) {
                    return scheduleDate; // quick reply templates are not sends to a chat
                }
                if (m != null && m.messageOwner != null && m.messageOwner.media != null
                        && !(m.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty)
                        && !(m.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage)) {
                    media = true;
                }
            }
        }
        return compute(account, dialogId, media);
    }

    static synchronized int compute(int account, long dialogId, boolean media) {
        int now = ConnectionsManager.getInstance(account).getCurrentTime();
        if (account == lastAccount && dialogId == lastDialog && now - lastComputedAt <= GROUP_WINDOW && lastDate > now) {
            return lastDate;
        }
        lastAccount = account;
        lastDialog = dialogId;
        lastComputedAt = now;
        lastDate = now + (media ? DELAY_MEDIA : DELAY_TEXT);
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
        } else if (req instanceof TLRPC.TL_messages_forwardMessages) {
            TLRPC.TL_messages_forwardMessages r = (TLRPC.TL_messages_forwardMessages) req;
            if (r.schedule_date != 0 && r.schedule_date < min) r.schedule_date = min;
        }
    }

    /** Persist the origin so explicit scheduling still opens the upstream scheduled view. */
    public static void markAutomatic(TLRPC.Message message, boolean automatic) {
        if (!automatic) return;
        if (message.params == null) message.params = new HashMap<>();
        message.params.put(AUTOMATIC, "1");
    }

    /** Carry the origin through asynchronous preparation of protected-content copies. */
    public static SendMessageChatArguments arguments(boolean automatic) {
        if (!automatic) return null;
        SendMessageChatArguments args = new SendMessageChatArguments.Builder().build();
        automaticArguments.add(args);
        return args;
    }

    public static boolean isAutomatic(SendMessageChatArguments args) {
        return args != null && automaticArguments.contains(args);
    }

    public static boolean isAutomatic(ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) return false;
        for (MessageObject message : messages) {
            if (message == null || message.messageOwner == null || message.messageOwner.params == null
                    || !"1".equals(message.messageOwner.params.get(AUTOMATIC))) return false;
        }
        return true;
    }
}
