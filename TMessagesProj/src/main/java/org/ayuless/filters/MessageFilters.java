/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.filters;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Message filters: hide incoming messages in chats by keyword / regular expression, by
 * sender (shadow ban), or because the sender is blocked. Display only: nothing is deleted
 * or stored, and secret chats are never filtered.
 *
 * <p>Settings are app-wide. Reads come from an in-memory snapshot that is replaced on every
 * change, so the chat screen does not touch SharedPreferences per message.
 */
public final class MessageFilters {

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_PRIVATE = "privateChats";
    private static final String KEY_HIDE_BLOCKED = "hideBlocked";
    private static final String KEY_FILTERS = "filters";
    private static final String KEY_SHADOW_BAN = "shadowBan";

    /** Only the start of long texts is matched, to bound the cost of user expressions. */
    private static final int MAX_MATCH_LENGTH = 4096;

    private static final class Snapshot {
        final boolean enabled;
        final boolean privateChats;
        final boolean hideBlocked;
        final List<MessageFilter> filters;
        final Set<Long> shadowBan;

        Snapshot(boolean enabled, boolean privateChats, boolean hideBlocked, List<MessageFilter> filters, Set<Long> shadowBan) {
            this.enabled = enabled;
            this.privateChats = privateChats;
            this.hideBlocked = hideBlocked;
            this.filters = Collections.unmodifiableList(filters);
            this.shadowBan = Collections.unmodifiableSet(shadowBan);
        }
    }

    private static volatile Snapshot snapshot;

    private MessageFilters() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_filters", Context.MODE_PRIVATE);
    }

    private static Snapshot get() {
        Snapshot s = snapshot;
        if (s == null) {
            synchronized (MessageFilters.class) {
                s = snapshot;
                if (s == null) {
                    snapshot = s = load();
                }
            }
        }
        return s;
    }

    private static Snapshot load() {
        if (ApplicationLoader.applicationContext == null) {
            return new Snapshot(false, false, false, new ArrayList<>(), new LinkedHashSet<>());
        }
        SharedPreferences p = prefs();
        ArrayList<MessageFilter> filters = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(p.getString(KEY_FILTERS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                filters.add(MessageFilter.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        LinkedHashSet<Long> shadow = new LinkedHashSet<>();
        for (String id : TextUtils.split(p.getString(KEY_SHADOW_BAN, ""), ",")) {
            try {
                if (!id.isEmpty()) {
                    shadow.add(Long.parseLong(id));
                }
            } catch (NumberFormatException ignore) {
            }
        }
        return new Snapshot(p.getBoolean(KEY_ENABLED, false), p.getBoolean(KEY_PRIVATE, false), p.getBoolean(KEY_HIDE_BLOCKED, false), filters, shadow);
    }

    private static synchronized void save(boolean enabled, boolean privateChats, boolean hideBlocked, List<MessageFilter> filters, Set<Long> shadowBan) {
        JSONArray array = new JSONArray();
        try {
            for (MessageFilter f : filters) {
                array.put(f.toJson());
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        prefs().edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putBoolean(KEY_PRIVATE, privateChats)
                .putBoolean(KEY_HIDE_BLOCKED, hideBlocked)
                .putString(KEY_FILTERS, array.toString())
                .putString(KEY_SHADOW_BAN, TextUtils.join(",", shadowBan))
                .apply();
        snapshot = new Snapshot(enabled, privateChats, hideBlocked, new ArrayList<>(filters), new LinkedHashSet<>(shadowBan));
        AndroidUtilities.runOnUIThread(MessageFilters::refreshDialogLists);
    }

    /** Redraws chat list previews so they follow the new filter settings. */
    private static void refreshDialogLists() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
            }
        }
    }

    // ---- settings API ----

    public static boolean isEnabled() {
        return get().enabled;
    }

    public static boolean privateChats() {
        return get().privateChats;
    }

    public static boolean hideBlocked() {
        return get().hideBlocked;
    }

    public static ArrayList<MessageFilter> getFilters() {
        return new ArrayList<>(get().filters);
    }

    public static ArrayList<Long> getShadowBan() {
        return new ArrayList<>(get().shadowBan);
    }

    public static void setEnabled(boolean value) {
        Snapshot s = get();
        save(value, s.privateChats, s.hideBlocked, s.filters, s.shadowBan);
    }

    public static void setPrivateChats(boolean value) {
        Snapshot s = get();
        save(s.enabled, value, s.hideBlocked, s.filters, s.shadowBan);
    }

    public static void setHideBlocked(boolean value) {
        Snapshot s = get();
        save(s.enabled, s.privateChats, value, s.filters, s.shadowBan);
    }

    public static void setFilters(List<MessageFilter> filters) {
        Snapshot s = get();
        save(s.enabled, s.privateChats, s.hideBlocked, filters, s.shadowBan);
    }

    public static void setShadowBan(List<Long> ids) {
        Snapshot s = get();
        save(s.enabled, s.privateChats, s.hideBlocked, s.filters, new LinkedHashSet<>(ids));
    }

    // ---- matching ----

    /**
     * {@link #isHidden} for callers off the UI thread (notifications) or in drawing code: never
     * throws, a failed check means "not hidden".
     */
    public static boolean isHiddenSafe(int account, MessageObject obj) {
        try {
            return isHidden(account, obj);
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    /** Whether an incoming message must be hidden from the chat list of messages. */
    public static boolean isHidden(int account, MessageObject obj) {
        Snapshot s = get();
        if (!s.enabled || obj == null || obj.messageOwner == null || obj.isOut()) {
            return false;
        }
        long dialogId = obj.getDialogId();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        }
        long fromId = obj.getFromChatId();
        if (fromId != 0 && s.shadowBan.contains(fromId)) {
            return true;
        }
        if (s.hideBlocked && fromId != 0 && MessagesController.getInstance(account).blockePeers.indexOfKey(fromId) >= 0) {
            return true;
        }
        if (s.filters.isEmpty() || (dialogId > 0 && !s.privateChats) || obj.messageOwner instanceof TLRPC.TL_messageService) {
            return false;
        }
        String text = obj.messageOwner.message;
        if (text != null && text.length() > MAX_MATCH_LENGTH) {
            text = text.substring(0, MAX_MATCH_LENGTH);
        }
        for (MessageFilter f : s.filters) {
            if (f.hides(text)) {
                return true;
            }
        }
        return false;
    }
}
