/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package org.ayuless.spy;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

/** AyuConfig saving policy, adapted to Ayuless account preferences and native TL storage. */
public final class MessageSavingConfig {
    public static final String CUSTOM = "customSaving";
    public static final String FORMATTING = "saveFormatting";
    public static final String REACTIONS = "saveReactions";
    public static final String BOTS = "saveForBots";
    public static final String MEDIA_CATEGORIES = "mediaCategories";
    public static final String[] CATEGORIES = {"privateChats", "publicChannels", "privateChannels", "publicGroups", "privateGroups"};
    private MessageSavingConfig() {}

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_saving_" + account, Context.MODE_PRIVATE);
    }
    public static boolean get(int account, String key) {
        boolean retentionDefault = FORMATTING.equals(key) || REACTIONS.equals(key) || BOTS.equals(key)
                || CATEGORIES[0].equals(key) || CATEGORIES[2].equals(key) || CATEGORIES[4].equals(key);
        return ApplicationLoader.applicationContext != null && prefs(account).getBoolean(key, retentionDefault);
    }
    public static void set(int account, String key, boolean value) {
        prefs(account).edit().putBoolean(key, value).apply();
    }
    public static boolean saveFormatting(int account) { return !get(account, CUSTOM) || get(account, FORMATTING); }
    public static boolean saveReactions(int account) { return !get(account, CUSTOM) || get(account, REACTIONS); }
    public static boolean saveDialog(int account, long dialogId) {
        if (!get(account, CUSTOM) || get(account, BOTS) || dialogId <= 0 || DialogObject.isEncryptedDialog(dialogId)) return true;
        TLRPC.User user = MessagesController.getInstance(account).getUser(dialogId);
        return user == null || !user.bot;
    }
    public static boolean saveMedia(int account, TLRPC.Message message) {
        if (!get(account, CUSTOM) || !get(account, MEDIA_CATEGORIES)) return true;
        long did = MessageObject.getDialogId(message);
        if (did >= 0 || DialogObject.isEncryptedDialog(did)) return get(account, CATEGORIES[0]);
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-did);
        // Unknown chat metadata must not silently lose already opted-in retention.
        if (chat == null) return true;
        boolean publicChat = ChatObject.isPublic(chat);
        int category = ChatObject.isChannel(chat) && !chat.megagroup ? (publicChat ? 1 : 2) : (publicChat ? 3 : 4);
        return get(account, CATEGORIES[category]);
    }
    /** Copy before filtering: never strip metadata from a live Telegram message object. */
    public static TLRPC.Message snapshot(int account, TLRPC.Message message) {
        if (!get(account, CUSTOM)) return message;
        NativeByteBuffer buffer = null;
        try {
            buffer = new NativeByteBuffer(message.getObjectSize());
            message.serializeToStream(buffer);
            buffer.rewind();
            TLRPC.Message copy = TLRPC.Message.TLdeserialize(buffer, buffer.readInt32(true), true);
            copy.dialog_id = MessageObject.getDialogId(message);
            copy.attachPath = message.attachPath;
            if (!saveFormatting(account)) { copy.entities.clear(); copy.flags &= ~TLRPC.MESSAGE_FLAG_HAS_ENTITIES; }
            if (!saveReactions(account)) { copy.reactions = null; copy.flags &= ~(1 << 20); }
            return copy;
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
            return message;
        } finally {
            if (buffer != null) buffer.reuse();
        }
    }
}
