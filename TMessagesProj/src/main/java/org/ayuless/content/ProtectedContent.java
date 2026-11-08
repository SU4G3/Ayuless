// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.content;

import org.ayuless.storage.AyulessStore;
import android.widget.Toast;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Ayuless: "restrict saving content" in groups and channels is ignored locally, so media can be
 * saved, text copied, screenshots taken and messages forwarded (as copies).
 *
 * <p>Never applies to secret chats, private chats (a person's own "disable sharing" choice),
 * chats with an auto-delete timer, or any message with a self-destruct timer / view-once media.
 * Those keep the upstream behaviour.
 */
public final class ProtectedContent {

    /** Messages whose noforwards flag we cleared locally; forwarding them must go as a copy. */
    private static final Set<TLRPC.Message> unlocked = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private ProtectedContent() {
    }

    /**
     * Chat-level flag as the UI should see it. Groups/channels are unlocked unless the chat has an
     * auto-delete timer, in which case messages are meant to disappear and stay protected.
     */
    public static boolean chatNoForwards(MessagesController controller, TLRPC.Chat chat, boolean raw) {
        if (!raw || chat == null) {
            return raw;
        }
        TLRPC.ChatFull full = controller.getChatFull(chat.id);
        return full != null && full.ttl_period != 0;
    }

    /** Clears a message-level noforwards flag for unlockable group/channel messages. */
    public static void onMessageObject(TLRPC.Message message) {
        if (message == null || !message.noforwards || !canUnlock(message)) {
            return;
        }
        message.noforwards = false;
        unlocked.add(message);
    }

    private static boolean canUnlock(TLRPC.Message message) {
        long dialogId = MessageObject.getDialogId(message);
        if (dialogId >= 0 || DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        }
        return AyulessStore.isStorable(message);
    }

    /** True if the server will refuse a plain forward of this message. */
    private static boolean needsCopy(MessagesController controller, MessageObject messageObject) {
        TLRPC.Message message = messageObject.messageOwner;
        if (message == null || !canUnlock(message)) {
            return false;
        }
        if (unlocked.contains(message)) {
            return true;
        }
        long dialogId = MessageObject.getDialogId(message);
        TLRPC.Chat chat = controller.getChat(-dialogId);
        return chat != null && chat.noforwards;
    }

    /**
     * Sends copies of messages from protected chats instead of forwarding them and removes them
     * from {@code messages}. Media is re-uploaded from the local file; media that is not
     * downloaded yet is skipped.
     *
     * @return number of protected messages that could not be copied (media not downloaded)
     */
    public static int sendCopies(int account, ArrayList<MessageObject> messages, long peer, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        AccountInstance instance = AccountInstance.getInstance(account);
        MessagesController controller = instance.getMessagesController();
        int skipped = 0;
        for (Iterator<MessageObject> it = messages.iterator(); it.hasNext(); ) {
            MessageObject messageObject = it.next();
            if (!needsCopy(controller, messageObject)) {
                continue;
            }
            it.remove();
            TLRPC.Message message = messageObject.messageOwner;
            TLRPC.MessageMedia media = message.media;
            boolean textOnly = media == null || media instanceof TLRPC.TL_messageMediaEmpty || media instanceof TLRPC.TL_messageMediaWebPage;
            if (textOnly) {
                if (message.message == null || message.message.isEmpty()) {
                    continue;
                }
                SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(message.message, peer);
                params.entities = message.entities;
                params.replyToTopMsg = replyToTopMsg;
                params.notify = notify;
                params.scheduleDate = scheduleDate;
                instance.getSendMessagesHelper().sendMessage(params);
                continue;
            }
            boolean isPhoto = media instanceof TLRPC.TL_messageMediaPhoto;
            boolean isDocument = media instanceof TLRPC.TL_messageMediaDocument;
            if (!isPhoto && !isDocument) {
                skipped++;
                continue;
            }
            File file = FileLoader.getInstance(account).getPathToMessage(message);
            if (file == null || !file.exists()) {
                skipped++;
                continue;
            }
            SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
            info.path = file.getAbsolutePath();
            info.caption = message.message;
            info.entities = message.entities;
            info.isVideo = messageObject.isVideo();
            boolean forceDocument = isDocument && !messageObject.isVideo() && !messageObject.isGif();
            ArrayList<SendMessagesHelper.SendingMediaInfo> list = new ArrayList<>();
            list.add(info);
            SendMessagesHelper.prepareSendingMedia(instance, list, peer, null, replyToTopMsg, null, null, forceDocument, false, null, notify, scheduleDate, 0, 0, false, null, null, 0, false, 0, 0, null);
        }
        if (skipped > 0) {
            AndroidUtilities.runOnUIThread(() -> Toast.makeText(ApplicationLoader.applicationContext,
                    LocaleController.getString(R.string.AyulessProtectedForwardNeedsDownload), Toast.LENGTH_LONG).show());
        }
        return skipped;
    }
}
