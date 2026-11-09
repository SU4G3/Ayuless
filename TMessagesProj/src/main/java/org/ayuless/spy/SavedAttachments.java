/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.spy;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.ayuless.antidelete.DeletedCache;
import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Spy > Attachments: keeps a private copy of the media of messages that anti-delete kept.
 *
 * <p>When the server deletes a message, its media usually can no longer be downloaded. If the
 * file is already in the client cache at that moment, a copy goes to
 * {@code files/ayuless/attachments/<account>/}. Later, when the cache copy is gone (cache
 * cleanup, "keep media" expiry), the saved copy is put back at the cache path so the normal
 * upstream rendering works unchanged.
 *
 * <p>Files are named {@code <dialogId>_<msgId>_<attachFileName>}, so a lookup needs no database.
 * All message categories are supported; Spy exceptions are never saved. Oldest files are
 * removed first when the total size exceeds the limit.
 */
public final class SavedAttachments {

    private static final long MB = 1024L * 1024;
    /** Slider steps; 0 means unlimited. */
    public static final long[] LIMITS = {300 * MB, 1024 * MB, 2048 * MB, 5120 * MB, 16384 * MB, 0};
    public static final long DEFAULT_LIMIT = 1024 * MB;

    private static final String PREFS = "ayuless_attachments";
    private static final String KEY_ENABLED = "enabled_";
    private static final String KEY_LIMIT = "limit_bytes";

    private static final DispatchQueue queue = new DispatchQueue("ayulessAttachmentsQueue");
    /** Messages already checked for a restore in this process, so binds stay cheap. */
    private static final Set<String> restoreChecked = Collections.synchronizedSet(new HashSet<>());

    private SavedAttachments() {
    }

    // ---- settings ----

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(int account) {
        return ApplicationLoader.applicationContext != null && prefs().getBoolean(KEY_ENABLED + account, false);
    }

    public static void setEnabled(int account, boolean enabled) {
        prefs().edit().putBoolean(KEY_ENABLED + account, enabled).apply();
    }

    public static long getLimitBytes() {
        return prefs().getLong(KEY_LIMIT, DEFAULT_LIMIT);
    }

    public static void setLimitBytes(long bytes) {
        prefs().edit().putLong(KEY_LIMIT, bytes).apply();
        queue.postRunnable(SavedAttachments::enforceLimit);
    }

    // ---- storage layout ----

    private static File rootDir() {
        return new File(ApplicationLoader.getFilesDirFixed(), "ayuless/attachments");
    }

    public static File accountDir(int account) {
        return new File(rootDir(), String.valueOf(account));
    }

    private static File savedFile(int account, long dialogId, int msgId, String attachName) {
        return new File(accountDir(account), dialogId + "_" + msgId + "_" + attachName);
    }

    // ---- saving ----

    /**
     * Called for every message anti-delete kept. Copies its media if it is already downloaded.
     * Runs the file work on a background queue.
     */
    public static void onKept(int account, TLRPC.Message message) {
        if (message == null || message.media == null || !isEnabled(account)
                || !MessageSavingConfig.saveMedia(account, message)) {
            return;
        }
        if (!AyulessStore.isStorable(account, message, org.ayuless.content.RetentionConfig.ATTACHMENT)) {
            return;
        }
        final long dialogId = MessageObject.getDialogId(message);
        if (SpyExceptions.isExcepted(account, dialogId)) {
            return;
        }
        final String attachName = FileLoader.getMessageFileName(message);
        if (TextUtils.isEmpty(attachName) || attachName.contains("/")) {
            return;
        }
        queue.postRunnable(() -> {
            try {
                File src = null;
                if (!TextUtils.isEmpty(message.attachPath)) {
                    File f = new File(message.attachPath);
                    if (f.exists()) {
                        src = f;
                    }
                }
                if (src == null) {
                    File f = FileLoader.getInstance(account).getPathToMessage(message, true);
                    if (f != null && f.exists()) {
                        src = f;
                    }
                }
                if (src == null) {
                    File path = FileLoader.getInstance(account).getPathToMessage(message);
                    File encrypted = new File(path.getPath() + ".enc");
                    File key = new File(FileLoader.getInternalCacheDir(), path.getName() + ".enc.key");
                    if (encrypted.exists() && key.exists()) {
                        File dst = savedFile(account, dialogId, message.id, attachName + ".enc");
                        File dir = dst.getParentFile();
                        if (dir != null && (dir.isDirectory() || dir.mkdirs())) {
                            if (AndroidUtilities.copyFile(encrypted, dst)) {
                                AndroidUtilities.copyFile(key, savedFile(account, dialogId, message.id, attachName + ".enc.key"));
                                enforceLimit();
                            }
                        }
                        return;
                    }
                }
                if (src == null || src.length() == 0) {
                    return;
                }
                File dst = savedFile(account, dialogId, message.id, attachName);
                if (dst.exists() && dst.length() == src.length()) {
                    return;
                }
                File dir = dst.getParentFile();
                if (dir != null && !dir.exists() && !dir.mkdirs()) {
                    return;
                }
                if (AndroidUtilities.copyFile(src, dst)) {
                    enforceLimit();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    /** Prepare local media paths for the history screen without decrypting files on the UI thread. */
    public static void prepareHistory(int account, java.util.ArrayList<TLRPC.Message> messages, Runnable ready) {
        queue.postRunnable(() -> {
            for (TLRPC.Message message : messages) {
                String name = FileLoader.getMessageFileName(message);
                if (TextUtils.isEmpty(name) || name.contains("/")) continue;
                File saved = savedFile(account, MessageObject.getDialogId(message), message.id, name);
                if (saved.isFile()) message.attachPath = saved.getAbsolutePath();
                else {
                    File encrypted = new File(saved.getPath() + ".enc");
                    File key = new File(saved.getPath() + ".enc.key");
                    if (encrypted.isFile() && key.isFile()) {
                        try {
                            File path = FileLoader.getInstance(account).getPathToMessage(message);
                            File parent = path.getParentFile();
                            if (parent != null) parent.mkdirs();
                            AndroidUtilities.copyFile(key, new File(FileLoader.getInternalCacheDir(), path.getName() + ".enc.key"));
                            AndroidUtilities.copyFile(encrypted, new File(path.getPath() + ".enc"));
                        } catch (Exception e) { FileLog.e(e); }
                    }
                }
            }
            AndroidUtilities.runOnUIThread(ready);
        });
    }

    // ---- restoring ----

    /**
     * Called from MessageObject.checkMediaExistance when the media file is missing. If this is a
     * kept deleted message with a saved copy, copies it back to the cache path and notifies the
     * UI as if the file had just been downloaded.
     */
    public static void onMediaMissing(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null || messageObject.messageOwner.media == null) {
            return;
        }
        final int account = messageObject.currentAccount;
        final long dialogId = messageObject.getDialogId();
        final int msgId = messageObject.getId();
        if (!DeletedCache.isDeleted(account, dialogId, msgId)) {
            return;
        }
        final String key = account + ":" + dialogId + ":" + msgId;
        if (!restoreChecked.add(key)) {
            return;
        }
        final TLRPC.Message message = messageObject.messageOwner;
        final String attachName = FileLoader.getMessageFileName(message);
        if (TextUtils.isEmpty(attachName) || attachName.contains("/")) {
            return;
        }
        queue.postRunnable(() -> {
            try {
                File saved = savedFile(account, dialogId, msgId, attachName);
                if (!saved.exists()) {
                    File encrypted = savedFile(account, dialogId, msgId, attachName + ".enc");
                    File keyFile = savedFile(account, dialogId, msgId, attachName + ".enc.key");
                    File path = FileLoader.getInstance(account).getPathToMessage(message);
                    if (encrypted.exists() && keyFile.exists()) {
                        File target = new File(path.getPath() + ".enc");
                        File targetKey = new File(FileLoader.getInternalCacheDir(), path.getName() + ".enc.key");
                        if (AndroidUtilities.copyFile(keyFile, targetKey) && AndroidUtilities.copyFile(encrypted, target)) {
                            AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(account)
                                    .postNotificationName(NotificationCenter.fileLoaded, attachName, target));
                        }
                    }
                    return;
                }
                File target = FileLoader.getInstance(account).getPathToMessage(message, true);
                if (target == null || TextUtils.isEmpty(target.getPath()) || target.exists()) {
                    return;
                }
                File dir = target.getParentFile();
                if (dir != null && !dir.exists() && !dir.mkdirs()) {
                    return;
                }
                if (!AndroidUtilities.copyFile(saved, target)) {
                    return;
                }
                // Touch the saved copy so it counts as recently used for the oldest-first cleanup.
                saved.setLastModified(System.currentTimeMillis());
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(account)
                        .postNotificationName(NotificationCenter.fileLoaded, attachName, target));
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    // ---- size limit and usage ----

    public interface SizeCallback {
        void run(long bytes);
    }

    /** Total size of saved attachments of all accounts. Callback runs on the UI thread. */
    public static void usedBytes(SizeCallback callback) {
        queue.postRunnable(() -> {
            long size = 0;
            for (File f : allFiles()) {
                size += f.length();
            }
            final long result = size;
            AndroidUtilities.runOnUIThread(() -> callback.run(result));
        });
    }

    /** Must run on the queue. Removes the oldest files until the total fits the limit. */
    private static void enforceLimit() {
        long limit = getLimitBytes();
        if (limit <= 0) {
            return;
        }
        File[] files = allFiles();
        long total = 0;
        for (File f : files) {
            total += f.length();
        }
        if (total <= limit) {
            return;
        }
        Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File f : files) {
            if (total <= limit) {
                break;
            }
            long len = f.length();
            if (f.delete()) {
                total -= len;
            }
        }
    }

    private static File[] allFiles() {
        java.util.ArrayList<File> out = new java.util.ArrayList<>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            File[] list = accountDir(a).listFiles();
            if (list == null) {
                continue;
            }
            for (File f : list) {
                if (f.isFile()) {
                    out.add(f);
                }
            }
        }
        return out.toArray(new File[0]);
    }

    /** Delete all saved attachments of the account (data clear, logout). */
    public static void clear(int account) {
        restoreChecked.clear();
        queue.postRunnable(() -> {
            File[] list = accountDir(account).listFiles();
            if (list != null) {
                for (File f : list) {
                    f.delete();
                }
            }
        });
    }

    /** Delete saved attachments of one dialog, e.g. when its stored data is removed. */
    public static void clearDialog(int account, long dialogId) {
        queue.postRunnable(() -> {
            File[] list = accountDir(account).listFiles();
            if (list == null) {
                return;
            }
            String prefix = dialogId + "_";
            for (File f : list) {
                if (f.getName().startsWith(prefix)) {
                    f.delete();
                }
            }
        });
    }
}
