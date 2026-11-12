/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.chat;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.JsonWriter;
import org.ayuless.storage.AyulessStore;
import org.ayuless.spy.SavedAttachments;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One page in memory; network, storage, SAF and formatting never block the UI thread. */
public final class ChatExporter {
    public static final class Options {
        public final boolean[] include = {true, true, true, true, true, true, false};
        public boolean json;
        public long from, until = Long.MAX_VALUE;
    }
    public interface Progress { void update(int count, boolean done, Exception error, boolean cancelled); }
    private static final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
    private final int account;
    private final long dialog, owner;
    private final Uri tree;
    private final Options options = new Options();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile int request;
    private final ContentResolver resolver = ApplicationLoader.applicationContext.getContentResolver();
    private Uri root, media;
    private final Map<Long, String> names = new HashMap<>();
    private int count;
    private long lastProgress;
    public ChatExporter(int account, long dialog, Uri tree, Options options) {
        this.account = account; this.dialog = dialog; this.tree = tree;
        owner = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        System.arraycopy(options.include, 0, this.options.include, 0, options.include.length);
        this.options.json = options.json; this.options.from = options.from; this.options.until = options.until;
    }
    public void cancel() {
        cancelled.set(true);
        int token = request;
        if (token != 0) ConnectionsManager.getInstance(account).cancelRequest(token, true);
    }
    private void check() throws IOException {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelled");
        if (org.telegram.messenger.UserConfig.getInstance(account).getClientUserId() != owner) throw new IOException("Account changed");
    }
    public void start(Progress progress) { worker.execute(() -> run(progress)); }
    private Uri create(Uri parent, String type, String name) throws IOException {
        check();
        Uri uri = DocumentsContract.createDocument(resolver, parent, type, name);
        if (uri == null) throw new IOException("Cannot create " + name);
        return uri;
    }
    private Writer writer(String name, String mime) throws IOException {
        OutputStream stream = resolver.openOutputStream(create(root, mime, name), "w");
        if (stream == null) throw new IOException("Cannot open output");
        return new BufferedWriter(new OutputStreamWriter(stream, java.nio.charset.StandardCharsets.UTF_8));
    }
    private void run(Progress progress) {
        Exception failure = null;
        try {
            check();
            Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
            String stamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
            root = create(parent, DocumentsContract.Document.MIME_TYPE_DIR, "export_" + dialog + "_" + stamp);
            media = create(root, DocumentsContract.Document.MIME_TYPE_DIR, "media");
            if (options.json) {
                try (JsonWriter out = new JsonWriter(writer("result.json", "application/json"))) {
                    out.beginObject().name("dialog_id").value(dialog).name("order").value("history_newest_first_then_saved_deleted_newest_first")
                            .name("media_policy").value("downloaded_only").name("messages").beginArray();
                    walk((m, deleted) -> writeJson(out, m, deleted), progress);
                    out.endArray().endObject();
                }
            } else {
                try (Writer css = writer("style.css", "text/css")) {
                    css.write("body{background:#e8eef2;font:16px system-ui;margin:auto;max-width:800px;padding:24px;color:#202b35}article{background:white;border-radius:12px;margin:12px 0;padding:16px;overflow-wrap:anywhere}article.out{margin-left:40px;background:#dcf8c6}.deleted{border:1px dashed #c44}time,small{color:#647383}p{white-space:pre-wrap}a{color:#17679b}");
                }
                try (Writer out = writer("messages.html", "text/html")) {
                    out.write("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"><title>Ayuless export</title><link rel=\"stylesheet\" href=\"style.css\"></head><body><h1>Ayuless · " + dialog + "</h1>");
                    walk((m, deleted) -> writeHtml(out, m, deleted), progress);
                    out.write("</body></html>");
                }
            }
            check();
        } catch (Exception e) {
            failure = e;
            if (!cancelled.get()) FileLog.e(e);
            if (root != null) try { DocumentsContract.deleteDocument(resolver, root); } catch (Exception cleanup) { FileLog.e(cleanup); }
        }
        final Exception error = failure;
        AndroidUtilities.runOnUIThread(() -> progress.update(count, true, error, cancelled.get()));
    }
    private interface Sink { void message(TLRPC.Message message, boolean deleted) throws Exception; }
    private void accept(Sink sink, TLRPC.Message m, boolean deleted, Progress progress) throws Exception {
        check();
        if (m.date < options.from || m.date >= options.until || m instanceof TLRPC.TL_messageEmpty) return;
        int kind = kind(m);
        if (!options.include[kind]) return;
        sink.message(m, deleted); count++;
        if (System.currentTimeMillis() - lastProgress > 300) {
            lastProgress = System.currentTimeMillis(); final int n = count;
            AndroidUtilities.runOnUIThread(() -> progress.update(n, false, null, false));
        }
    }
    private void walk(Sink sink, Progress progress) throws Exception {
        if (DialogObject.isEncryptedDialog(dialog)) {
            long beforeDate = Long.MAX_VALUE, beforeId = Long.MAX_VALUE;
            while (true) {
                List<TLRPC.Message> page = cachedPage(beforeDate, beforeId);
                if (page.isEmpty()) break;
                for (TLRPC.Message m : page) {
                    // Secret history is local only. Saved deleted messages are emitted separately.
                    if (!savedDeleted(m.id)) accept(sink, m, false, progress);
                }
                TLRPC.Message last = page.get(page.size() - 1); beforeDate = last.date; beforeId = last.id;
            }
        } else {
            int offset = 0;
            while (true) {
                check();
                TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
                req.peer = MessagesController.getInstance(account).getInputPeer(dialog);
                req.limit = 100; req.offset_id = offset;
                TLRPC.messages_Messages result = history(req);
                if (result.messages.isEmpty()) break;
                names.clear();
                for (TLRPC.User user : result.users) names.put(user.id, org.telegram.messenger.UserObject.getUserName(user));
                for (TLRPC.Chat chat : result.chats) names.put(-chat.id, chat.title);
                int next = Integer.MAX_VALUE;
                boolean older = false;
                for (TLRPC.Message m : result.messages) {
                    if (m.id > 0) next = Math.min(next, m.id);
                    older |= m.date < options.from;
                    accept(sink, m, false, progress);
                }
                if (older) break;
                if (next == Integer.MAX_VALUE || next == offset || offset != 0 && next > offset) throw new IOException("History did not advance");
                offset = next;
            }
        }
        if (options.include[6]) {
            long date = Long.MAX_VALUE, id = Long.MAX_VALUE;
            while (true) {
                CompletableFuture<ArrayList<TLRPC.Message>> future = new CompletableFuture<>();
                AyulessStore.getInstance(account).deletedPage(dialog, date, id, future::complete);
                ArrayList<TLRPC.Message> page = await(future);
                if (page == null) throw new IOException("Cannot read saved messages");
                if (page.isEmpty()) break;
                for (TLRPC.Message m : page) accept(sink, m, true, progress);
                TLRPC.Message last = page.get(page.size() - 1); date = last.date; id = last.id;
            }
        }
    }
    private boolean savedDeleted(int id) throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        AyulessStore.getInstance(account).isDeleted(dialog, id, future::complete);
        return await(future);
    }
    private <T> T await(CompletableFuture<T> future) throws Exception {
        long deadline = System.currentTimeMillis() + 60000;
        while (true) {
            check();
            try { return future.get(250, TimeUnit.MILLISECONDS); }
            catch (TimeoutException e) { if (System.currentTimeMillis() >= deadline) throw new IOException("Operation timed out", e); }
        }
    }
    private TLRPC.messages_Messages history(TLRPC.TL_messages_getHistory req) throws Exception {
        CompletableFuture<TLRPC.messages_Messages> future = new CompletableFuture<>();
        request = ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            if (error != null || !(response instanceof TLRPC.messages_Messages)) future.completeExceptionally(new IOException(error == null ? "Invalid history" : error.text));
            else future.complete((TLRPC.messages_Messages) response);
        });
        try { return await(future); }
        finally { int token = request; request = 0; ConnectionsManager.getInstance(account).cancelRequest(token, true); }
    }
    private List<TLRPC.Message> cachedPage(long date, long id) throws Exception {
        CompletableFuture<List<TLRPC.Message>> future = new CompletableFuture<>();
        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            ArrayList<TLRPC.Message> page = new ArrayList<>();
            try {
                org.telegram.SQLite.SQLiteCursor c = storage.getDatabase().queryFinalized(
                        "SELECT data, mid, date FROM messages_v2 WHERE uid = ? AND (date < ? OR (date = ? AND mid < ?)) ORDER BY date DESC, mid DESC LIMIT 100", dialog, date, date, id);
                try {
                    while (c.next()) {
                        NativeByteBuffer b = c.byteBufferValue(0);
                        if (b == null) throw new IOException("Missing cached data");
                        try {
                            TLRPC.Message m = TLRPC.Message.TLdeserialize(b, b.readInt32(true), true);
                            m.readAttachPath(b, owner); m.id = c.intValue(1); m.date = c.intValue(2); m.dialog_id = dialog; page.add(m);
                        } finally { b.reuse(); }
                    }
                } finally { c.dispose(); }
                future.complete(page);
            } catch (Exception e) { future.completeExceptionally(e); }
        });
        return await(future);
    }
    private int kind(TLRPC.Message m) {
        if (MessageObject.isStickerMessage(m) || MessageObject.isAnimatedStickerMessage(m) || MessageObject.isGifMessage(m)) return 5;
        if (MessageObject.isVoiceMessage(m) || MessageObject.isRoundVideoMessage(m)) return 3;
        if (MessageObject.isVideoMessage(m)) return 2;
        if (m.media != null && m.media.photo != null) return 1;
        if (MessageObject.getDocument(m) != null) return 4;
        return 0;
    }
    private String copyMedia(TLRPC.Message m, boolean deleted) throws Exception {
        if (kind(m) == 0) return null;
        String name = FileLoader.getMessageFileName(m);
        // Paths in exported HTML are deliberately restricted to safe ASCII names.
        String safe = name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.length() > 120) safe = safe.substring(safe.length() - 120);
        String target = (deleted ? "deleted_" : "") + m.id + "_" + safe;
        File source = m.attachPath == null || m.attachPath.isEmpty() ? null : new File(m.attachPath);
        if (source == null || !source.isFile()) source = FileLoader.getInstance(account).getPathToMessage(m, true);
        if (source == null || !source.isFile()) source = FileLoader.getInstance(account).getPathToMessage(m);
        if ((source == null || !source.isFile()) && name != null && !name.contains("/") && !name.contains("\\"))
            source = new File(SavedAttachments.accountDir(account), dialog + "_" + m.id + "_" + name);
        if (source == null || !source.isFile() || source.length() == 0) return null;
        Uri dest = create(media, "application/octet-stream", target);
        try (InputStream in = new BufferedInputStream(new FileInputStream(source)); OutputStream out = resolver.openOutputStream(dest, "w")) {
            if (out == null) throw new IOException("Cannot write media");
            byte[] buffer = new byte[65536]; int n;
            while ((n = in.read(buffer)) != -1) { check(); out.write(buffer, 0, n); }
        }
        // SAF providers may adjust a requested filename; links must use the actual name.
        try (android.database.Cursor cursor = resolver.query(dest,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) throw new IOException("Cannot resolve exported media name");
            target = cursor.getString(0);
        }
        return "media/" + java.net.URLEncoder.encode(target, "UTF-8").replace("+", "%20");
    }
    private String senderName(TLRPC.Message m) {
        String name = names.get(sender(m));
        return name == null ? Long.toString(sender(m)) : name;
    }
    private long sender(TLRPC.Message m) { return m.from_id == null ? 0 : org.telegram.messenger.MessageObject.getPeerId(m.from_id); }
    private void writeJson(JsonWriter out, TLRPC.Message m, boolean deleted) throws Exception {
        String file = copyMedia(m, deleted);
        out.beginObject().name("id").value(m.id).name("date_unixtime").value(m.date).name("from_id").value(sender(m)).name("from").value(senderName(m))
                .name("out").value(m.out).name("deleted").value(deleted).name("text").value(m.message == null ? "" : m.message)
                .name("media_type").value(kind(m)).name("media").value(file)
                .name("media_missing").value(kind(m) != 0 && file == null).name("entities").beginArray();
        if (m.entities != null) for (TLRPC.MessageEntity entity : m.entities) {
            out.beginObject().name("type").value(entity.getClass().getSimpleName()).name("offset").value(entity.offset)
                    .name("length").value(entity.length).name("url").value(entity.url).endObject();
        }
        out.endArray();
        if (m.reply_to != null) out.name("reply_to_message_id").value(m.reply_to.reply_to_msg_id);
        out.endObject();
    }
    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private void writeHtml(Writer out, TLRPC.Message m, boolean deleted) throws Exception {
        String file = copyMedia(m, deleted);
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date(m.date * 1000L));
        out.write("<article id=\"" + (deleted ? "deleted_" : "message_") + m.id + "\" class=\"" + (m.out ? "out " : "") + (deleted ? "deleted" : "") + "\"><b>" + escape(senderName(m)) + "</b> <time>" + date + "</time> <small>#" + m.id + (deleted ? " · ✕ Deleted" : "") + "</small><p>" + escape(m.message) + "</p>");
        if (file != null) {
            if (kind(m) == 1) out.write("<p><img loading=\"lazy\" style=\"max-width:100%\" src=\"" + escape(file) + "\" alt=\"Photo\"></p>");
            else if (kind(m) == 2 || MessageObject.isRoundVideoMessage(m)) out.write("<p><video controls preload=\"none\" style=\"max-width:100%\" src=\"" + escape(file) + "\"></video></p>");
            else if (MessageObject.isVoiceMessage(m)) out.write("<p><audio controls preload=\"none\" src=\"" + escape(file) + "\"></audio></p>");
            out.write("<a href=\"" + escape(file) + "\">" + escape(FileLoader.getMessageFileName(m)) + "</a>");
        }
        else if (kind(m) != 0) out.write("<small>Media unavailable</small>");
        out.write("</article>");
    }
}
