/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.antidelete;

import android.os.SystemClock;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Remembers messages this client deleted itself, so that the matching server update is not
 * treated as a deletion by somebody else. Entries expire after {@link #TTL_MS}.
 *
 * Server delete updates use the real dialog id for channels and 0 for everything else
 * (message ids are global per account outside channels), so non-channel deletions are
 * recorded under key 0.
 */
public final class LocalDeletes {

    private static final long TTL_MS = 5 * 60 * 1000;
    private static final HashMap<String, Long>[] recent = newMaps();

    @SuppressWarnings("unchecked")
    private static HashMap<String, Long>[] newMaps() {
        HashMap<String, Long>[] maps = new HashMap[org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT];
        for (int i = 0; i < maps.length; i++) {
            maps[i] = new HashMap<>();
        }
        return maps;
    }

    private LocalDeletes() {
    }

    /** Key that the server delete update will use for this dialog. */
    public static long updateKey(int account, long dialogId) {
        if (dialogId < 0 && ChatObject.isChannel(MessagesController.getInstance(account).getChat(-dialogId))) {
            return dialogId;
        }
        return 0;
    }

    public static void record(int account, long dialogId, ArrayList<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        long key = updateKey(account, dialogId);
        long expires = SystemClock.elapsedRealtime() + TTL_MS;
        HashMap<String, Long> map = recent[account];
        synchronized (map) {
            for (int i = 0; i < ids.size(); i++) {
                map.put(key + ":" + ids.get(i), expires);
            }
        }
    }

    public static boolean contains(int account, long key, int id) {
        HashMap<String, Long> map = recent[account];
        long now = SystemClock.elapsedRealtime();
        synchronized (map) {
            Iterator<Map.Entry<String, Long>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue() < now) {
                    it.remove();
                }
            }
            return map.containsKey(key + ":" + id);
        }
    }
}
