// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.spy;

import android.content.SharedPreferences;
import org.ayuless.content.MessageSnapshots;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

/** Compatibility rules apply only when capturing new records. */
public final class MessageSavingConfig {
    public static final String KEY_CUSTOM_SAVING = "customSaving";
    public static final String KEY_SAVE_FORMATTING = "saveFormatting";
    public static final String KEY_SAVE_REACTIONS = "saveReactions";
    public static final String KEY_SAVE_BOTS = "saveForBots";
    public static final String KEY_MEDIA_CATEGORIES = "mediaCategories";
    public static final String KEY_PRIVATE_CHATS = "privateChats";
    public static final String KEY_PUBLIC_CHANNELS = "publicChannels";
    public static final String KEY_PRIVATE_CHANNELS = "privateChannels";
    public static final String KEY_PUBLIC_GROUPS = "publicGroups";
    public static final String KEY_PRIVATE_GROUPS = "privateGroups";
    private MessageSavingConfig() {}

    private static SharedPreferences prefs(int account) {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_saving_" + account, 0);
    }

    public static boolean get(int account, String key) {
        boolean fallback = !(KEY_CUSTOM_SAVING.equals(key) || KEY_MEDIA_CATEGORIES.equals(key)
                || KEY_PUBLIC_CHANNELS.equals(key) || KEY_PUBLIC_GROUPS.equals(key));
        return ApplicationLoader.applicationContext == null ? fallback : prefs(account).getBoolean(key, fallback);
    }

    public static void set(int account, String key, boolean value) {
        prefs(account).edit().putBoolean(key, value).apply();
    }

    public static boolean keepFormatting(int account) {
        return !get(account, KEY_CUSTOM_SAVING) || get(account, KEY_SAVE_FORMATTING);
    }

    public static boolean keepReactions(int account) {
        return !get(account, KEY_CUSTOM_SAVING) || get(account, KEY_SAVE_REACTIONS);
    }

    public static boolean allowBot(int account, long dialogId) {
        if (!get(account, KEY_CUSTOM_SAVING) || get(account, KEY_SAVE_BOTS)
                || dialogId <= 0 || DialogObject.isEncryptedDialog(dialogId)) return true;
        TLRPC.User user = MessagesController.getInstance(account).getUser(dialogId);
        return user == null || !user.bot;
    }

    public static boolean allowMedia(int account, long dialogId) {
        if (!get(account, KEY_CUSTOM_SAVING) || !get(account, KEY_MEDIA_CATEGORIES)) return true;
        if (dialogId >= 0 || DialogObject.isEncryptedDialog(dialogId)) return get(account, KEY_PRIVATE_CHATS);
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
        if (chat == null) return true;
        boolean broadcast = ChatObject.isChannel(chat) && !chat.megagroup;
        return get(account, broadcast ? (ChatObject.isPublic(chat) ? KEY_PUBLIC_CHANNELS : KEY_PRIVATE_CHANNELS)
                : (ChatObject.isPublic(chat) ? KEY_PUBLIC_GROUPS : KEY_PRIVATE_GROUPS));
    }

    public static TLRPC.Message snapshot(int account, TLRPC.Message source) {
        try {
            TLRPC.Message copy = MessageSnapshots.copy(source);
            if (!keepFormatting(account)) {
                copy.entities.clear();
                copy.flags &= ~128;
            }
            if (!keepReactions(account)) {
                copy.reactions = null;
                copy.flags &= ~1048576;
            }
            return copy;
        } catch (Exception e) {
            FileLog.e(e);
            return source;
        }
    }
}
