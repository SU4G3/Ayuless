/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.antidelete;

import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.UserConfig;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory index of messages marked as deleted, so message cells never query the database on
 * the UI thread. Loaded lazily from {@link AyulessStore}; reloaded if the account's user changes.
 */
public final class DeletedCache {

    private static final ConcurrentHashMap<Long, Set<Integer>>[] maps = newMaps();
    private static final long[] loadedFor = new long[UserConfig.MAX_ACCOUNT_COUNT];

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<Long, Set<Integer>>[] newMaps() {
        ConcurrentHashMap<Long, Set<Integer>>[] m = new ConcurrentHashMap[UserConfig.MAX_ACCOUNT_COUNT];
        for (int i = 0; i < m.length; i++) {
            m[i] = new ConcurrentHashMap<>();
        }
        return m;
    }

    private DeletedCache() {
    }

    private static void ensureLoaded(int account) {
        long userId = UserConfig.getInstance(account).getClientUserId();
        synchronized (loadedFor) {
            if (loadedFor[account] == userId) {
                return;
            }
            loadedFor[account] = userId;
        }
        maps[account].clear();
        AyulessStore.getInstance(account).loadDeletedIds(result -> {
            for (java.util.Map.Entry<Long, java.util.HashSet<Integer>> e : result.entrySet()) {
                set(account, e.getKey()).addAll(e.getValue());
            }
        });
    }

    private static Set<Integer> set(int account, long dialogId) {
        Set<Integer> s = maps[account].get(dialogId);
        if (s == null) {
            Set<Integer> created = Collections.newSetFromMap(new ConcurrentHashMap<>());
            s = maps[account].putIfAbsent(dialogId, created);
            if (s == null) {
                s = created;
            }
        }
        return s;
    }

    /** Forget all cached ids for the account, e.g. after the saved data was cleared. */
    public static void clear(int account) {
        if (account >= 0 && account < maps.length) {
            maps[account].clear();
        }
    }

    public static void mark(int account, long dialogId, int msgId) {
        ensureLoaded(account);
        set(account, dialogId).add(msgId);
    }

    public static boolean isDeleted(int account, long dialogId, int msgId) {
        ensureLoaded(account);
        Set<Integer> s = maps[account].get(dialogId);
        return s != null && s.contains(msgId);
    }
}
