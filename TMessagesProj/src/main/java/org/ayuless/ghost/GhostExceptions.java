/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.ghost;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** User exceptions belong to the edited ghost profile, just like its switches. */
public final class GhostExceptions {
    private static final String KEY = "exceptionUsers";

    private GhostExceptions() { }

    public static ArrayList<Long> get(int profile) {
        ArrayList<Long> users = new ArrayList<>();
        if (ApplicationLoader.applicationContext == null) return users;
        for (String value : GhostConfig.profilePrefs(profile).getStringSet(KEY, Collections.emptySet())) {
            try {
                long id = Long.parseLong(value);
                if (DialogObject.isUserDialog(id)) users.add(id);
            } catch (NumberFormatException ignored) { }
        }
        Collections.sort(users);
        return users;
    }

    public static void set(int profile, ArrayList<Long> users) {
        Set<String> stored = new HashSet<>();
        if (users != null) {
            for (Long id : users) {
                if (id != null && DialogObject.isUserDialog(id)) stored.add(Long.toString(id));
            }
        }
        GhostConfig.profilePrefs(profile).edit().putStringSet(KEY, stored).apply();
    }

    /** Unknown encrypted peers stay hidden. Group authors never exempt the whole group. */
    public static boolean contains(int account, long dialog) {
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT
                || ApplicationLoader.applicationContext == null) return false;
        long user = dialog;
        if (DialogObject.isEncryptedDialog(dialog)) {
            TLRPC.EncryptedChat chat = MessagesController.getInstance(account)
                    .getEncryptedChat(DialogObject.getEncryptedChatId(dialog));
            if (chat == null) return false;
            user = chat.user_id;
        }
        return DialogObject.isUserDialog(user) && GhostConfig.profilePrefs(GhostConfig.activeProfile(account))
                .getStringSet(KEY, Collections.emptySet()).contains(Long.toString(user));
    }
}
