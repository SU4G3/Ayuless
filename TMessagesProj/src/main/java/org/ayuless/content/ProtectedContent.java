// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.content;

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

/** Local saving restrictions and forwarding as uploaded copies. */
public final class ProtectedContent {

    /** Messages whose noforwards flag we cleared locally; forwarding them must go as a copy. */
    private static final Set<TLRPC.Message> unlocked = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private ProtectedContent() {
    }

    /** Local saving and screenshots are available in every chat. */
    public static boolean chatNoForwards(int account, MessagesController controller, TLRPC.Chat chat, boolean raw) {
        return false;
    }

    /** Clears a message-level restriction locally for saving and forwarding as a copy. */
    public static void onMessageObject(int account, TLRPC.Message message) {
        if (message == null || !message.noforwards || !canUnlock(account, message)) {
            return;
        }
        message.noforwards = false;
        unlocked.add(message);
    }

    public static boolean canUnlock(int account, TLRPC.Message message) {
        return message != null;
    }

    /** True if the server will refuse a plain forward of this message. */
    private static boolean needsCopy(MessagesController controller, MessageObject messageObject) {
        TLRPC.Message message = messageObject.messageOwner;
        if (message == null || !canUnlock(messageObject.currentAccount, message)) {
            return false;
        }
        if (unlocked.contains(message)) {
            return true;
        }
        long dialogId = MessageObject.getDialogId(message);
        if (DialogObject.isEncryptedDialog(dialogId) || RetentionConfig.timed(message)) return true;
        if (dialogId > 0) {
            TLRPC.UserFull full = controller.getUserFull(dialogId);
            return full != null && (full.noforwards_peer_enabled || full.noforwards_my_enabled);
        }
        TLRPC.Chat chat = controller.getChat(-dialogId);
        return chat != null && chat.noforwards;
    }

    /** Materialize an encrypted cache file only when the user explicitly saves or shares it. */
    public static File readableFile(int account, TLRPC.Message message) {
        File file = !android.text.TextUtils.isEmpty(message.attachPath) ? new File(message.attachPath) : null;
        if (file == null || !file.exists()) file = FileLoader.getInstance(account).getPathToMessage(message);
        if (file != null && file.exists()) return file;
        if (file == null) return null;
        File encrypted = new File(file.getPath() + ".enc");
        File key = new File(FileLoader.getInternalCacheDir(), file.getName() + ".enc.key");
        if (!encrypted.exists() || !key.exists()) return null;
        File directory = new File(ApplicationLoader.applicationContext.getCacheDir(), "ayuless_share/" + account + "/" + MessageObject.getDialogId(message) + "/" + message.id);
        if (!directory.isDirectory() && !directory.mkdirs()) return null;
        File target = new File(directory, file.getName());
        try (java.io.InputStream in = new org.telegram.messenger.secretmedia.EncryptedFileInputStream(encrypted, key);
                java.io.OutputStream out = new java.io.FileOutputStream(target)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return target;
        } catch (Exception e) {
            target.delete();
            org.telegram.messenger.FileLog.e(e);
            return null;
        }
    }

    public static void saveMedia(android.content.Context context, MessageObject message) {
        org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
            File file = readableFile(message.currentAccount, message.messageOwner);
            AndroidUtilities.runOnUIThread(() -> {
                if (file == null) {
                    Toast.makeText(context, LocaleController.getString(R.string.AyulessProtectedForwardNeedsDownload), Toast.LENGTH_LONG).show();
                    return;
                }
                boolean photo = message.isPhoto();
                org.telegram.messenger.MediaController.saveFile(file.getPath(), context,
                        photo ? 0 : message.isVideo() || message.isRoundVideo() ? 1 : 2,
                        photo ? null : message.getDocumentName(), message.getDocument() == null ? null : message.getDocument().mime_type);
            });
        });
    }

    public static boolean sendStoryCopy(int account, org.telegram.tgnet.tl.TL_stories.StoryItem story,
            long peer, MessageObject reply, boolean notify, long payStars, long monoForumPeer) {
        if (story == null) return false;
        File file = !android.text.TextUtils.isEmpty(story.attachPath) ? new File(story.attachPath) : null;
        if (file != null && !file.exists()) file = null;
        if (file == null && story.media != null && story.media.document != null) {
            file = FileLoader.getInstance(account).getPathToAttach(story.media.document);
        } else if (file == null && story.media != null && story.media.photo != null) {
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(story.media.photo.sizes, AndroidUtilities.getPhotoSize());
            if (size != null) {
                file = FileLoader.getInstance(account).getPathToAttach(size, true);
                if (!file.exists()) file = FileLoader.getInstance(account).getPathToAttach(size);
            }
        }
        if (file == null || !file.exists()) {
            Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString(R.string.AyulessProtectedForwardNeedsDownload), Toast.LENGTH_LONG).show();
            return true;
        }
        SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
        info.path = file.getAbsolutePath();
        info.caption = story.caption;
        info.entities = story.entities;
        info.isVideo = story.media.document != null && MessageObject.isVideoDocument(story.media.document);
        ArrayList<SendMessagesHelper.SendingMediaInfo> list = new ArrayList<>();
        list.add(info);
        SendMessagesHelper.prepareSendingMedia(AccountInstance.getInstance(account), list, peer, null, reply,
                null, null, false, false, null, notify, 0, 0, 0, false, null, null, 0, false, payStars, monoForumPeer, null);
        return true;
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
            File file = readableFile(account, message);
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
