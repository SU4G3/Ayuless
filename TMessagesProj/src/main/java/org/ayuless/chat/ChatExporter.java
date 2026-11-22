/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.chat;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.telegram.tgnet.SerializedData;
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
        // Text, photos, videos, voice, files, stickers, deleted, round videos, GIFs.
        public final boolean[] include = {true, false, false, false, false, false, false, false, false};
        public static final int HTML = 0, JSON = 1, BOTH = 2;
        public int format = HTML;
        public long maxFileBytes = 8 * 1024 * 1024L;
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
    private Uri root;
    private final Map<String, Uri> directories = new HashMap<>();
    private SQLiteDatabase staging;
    private File stagingFile;
    private String title;
    private volatile int fileCount;
    private volatile long totalBytes;
    private Progress progressListener;
    public Uri getExportFolder() { return root; }
    public int getFileCount() { return fileCount; }
    public long getTotalBytes() { return totalBytes; }
    private final Map<Long, String> names = new HashMap<>();
    private int count;
    private long lastProgress;
    public ChatExporter(int account, long dialog, Uri tree, Options options) {
        this.account = account; this.dialog = dialog; this.tree = tree;
        owner = org.telegram.messenger.UserConfig.getInstance(account).getClientUserId();
        System.arraycopy(options.include, 0, this.options.include, 0, options.include.length);
        this.options.format = options.format; this.options.maxFileBytes = options.maxFileBytes; this.options.from = options.from; this.options.until = options.until;
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
    private OutputStream output(Uri parent, String mime, String name) throws IOException {
        OutputStream stream = resolver.openOutputStream(create(parent, mime, name), "w");
        if (stream == null) throw new IOException("Cannot open output");
        fileCount++;
        return new FilterOutputStream(stream) {
            @Override public void write(int value) throws IOException { out.write(value); totalBytes++; }
            @Override public void write(byte[] bytes, int off, int length) throws IOException {
                out.write(bytes, off, length); totalBytes += length;
            }
        };
    }
    private Writer writer(String name, String mime) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(output(root, mime, name), java.nio.charset.StandardCharsets.UTF_8));
    }
    private Uri directory(String name) throws IOException {
        Uri uri = directories.get(name);
        if (uri == null) {
            uri = create(root, DocumentsContract.Document.MIME_TYPE_DIR, name);
            directories.put(name, uri);
        }
        return uri;
    }
    private void asset(String folder, String name, String mime) throws IOException {
        try (InputStream in = ApplicationLoader.applicationContext.getAssets().open("chat-export/" + name);
             OutputStream out = output(directory(folder), mime, name)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) { check(); out.write(buffer, 0, n); }
        }
    }
    private String chatTitle() {
        MessagesController controller = MessagesController.getInstance(account);
        if (DialogObject.isEncryptedDialog(dialog)) {
            TLRPC.EncryptedChat chat = controller.getEncryptedChat(DialogObject.getEncryptedChatId(dialog));
            TLRPC.User user = chat == null ? null : controller.getUser(chat.user_id);
            return user == null ? Long.toString(dialog) : org.telegram.messenger.UserObject.getUserName(user);
        }
        if (dialog > 0) {
            TLRPC.User user = controller.getUser(dialog);
            if (user != null) return org.telegram.messenger.UserObject.getUserName(user);
        } else {
            TLRPC.Chat chat = controller.getChat(-dialog);
            if (chat != null) return chat.title;
        }
        return Long.toString(dialog);
    }
    private String chatType() {
        if (DialogObject.isEncryptedDialog(dialog)) return "secret_chat";
        if (dialog > 0) {
            TLRPC.User user = MessagesController.getInstance(account).getUser(dialog);
            return user != null && user.bot ? "bot_chat" : "personal_chat";
        }
        TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialog);
        return chat != null && org.telegram.messenger.ChatObject.isChannel(chat) && !chat.megagroup
                ? "private_channel" : "private_group";
    }
    private void stage(TLRPC.Message m, boolean deleted) throws Exception {
        SerializedData data = new SerializedData();
        try {
            m.serializeToStream(data);
            ContentValues row = new ContentValues();
            row.put("id", m.id); row.put("date", m.date); row.put("deleted", deleted ? 1 : 0);
            row.put("data", data.toByteArray()); row.put("path", m.attachPath);
            row.put("sender", senderName(m));
            if (!staging.inTransaction()) staging.beginTransaction();
            staging.insertOrThrow("messages", null, row);
            if ((count + 1) % 100 == 0) { staging.setTransactionSuccessful(); staging.endTransaction(); }
        } finally { data.cleanup(); }
    }
    private void run(Progress progress) {
        progressListener = progress;
        Exception failure = null;
        try {
            check(); title = chatTitle();
            // Disk staging keeps memory bounded and merges saved deleted messages in chronological order.
            stagingFile = File.createTempFile("chat-export-", ".sqlite", ApplicationLoader.applicationContext.getCacheDir());
            staging = SQLiteDatabase.openOrCreateDatabase(stagingFile, null);
            staging.execSQL("CREATE TABLE messages (id INTEGER, date INTEGER, deleted INTEGER, data BLOB, path TEXT, sender TEXT)");
            staging.execSQL("CREATE INDEX message_order ON messages(date, id, deleted)");
            walk(this::stage);
            if (staging.inTransaction()) { staging.setTransactionSuccessful(); staging.endTransaction(); }
            check();
            Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
            String stamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss_SSS", Locale.US).format(new Date());
            root = create(parent, DocumentsContract.Document.MIME_TYPE_DIR, "ChatExport_" + stamp);
            boolean html = options.format != Options.JSON, json = options.format != Options.HTML;
            if (html) { asset("css", "style.css", "text/css"); asset("js", "script.js", "text/javascript"); }
            try (Writer page = html ? writer("messages.html", "text/html") : null;
                 JsonWriter out = json ? new JsonWriter(writer("result.json", "application/json")) : null;
                 Cursor cursor = staging.rawQuery("SELECT data,path,sender,deleted FROM messages ORDER BY date,id,deleted", null)) {
                ExportHtml.Page htmlPage = page == null ? null : new ExportHtml.Page(page, title);
                if (out != null) {
                    out.setIndent(" ");
                    out.beginObject().name("name").value(title).name("type").value(chatType())
                            .name("id").value(DialogObject.isEncryptedDialog(dialog) ? dialog : Math.abs(dialog))
                            .name("media_policy").value("downloaded_only").name("messages").beginArray();
                }
                while (cursor.moveToNext()) {
                    check();
                    SerializedData data = new SerializedData(cursor.getBlob(0));
                    try {
                        TLRPC.Message m = TLRPC.Message.TLdeserialize(data, data.readInt32(true), true);
                        m.attachPath = cursor.getString(1); m.dialog_id = dialog;
                        names.clear(); names.put(sender(m), cursor.getString(2));
                        boolean deleted = cursor.getInt(3) != 0;
                        String file = copyMedia(m, deleted); // One copy, even when both formats are selected.
                        if (htmlPage != null) htmlPage.message(htmlMessage(m, deleted, file));
                        if (out != null) writeJson(out, m, deleted, file);
                        reportProgress();
                    } finally { data.cleanup(); }
                }
                if (out != null) out.endArray().endObject();
                if (htmlPage != null) htmlPage.close();
            }
            check();
        } catch (Exception e) {
            failure = e;
            if (!cancelled.get()) FileLog.e(e);
            if (root != null) try { DocumentsContract.deleteDocument(resolver, root); } catch (Exception cleanup) { FileLog.e(cleanup); }
        } finally {
            if (staging != null) {
                if (staging.inTransaction()) staging.endTransaction();
                staging.close();
            }
            if (stagingFile != null) SQLiteDatabase.deleteDatabase(stagingFile);
        }
        final Exception error = failure;
        AndroidUtilities.runOnUIThread(() -> progress.update(count, true, error, cancelled.get()));
    }
    private interface Sink { void message(TLRPC.Message message, boolean deleted) throws Exception; }
    private void accept(Sink sink, TLRPC.Message m, boolean deleted) throws Exception {
        check();
        if (m.date < options.from || m.date >= options.until || m instanceof TLRPC.TL_messageEmpty) return;
        if (deleted && !options.include[6]) return;
        sink.message(m, deleted); count++;
        reportProgress();
    }
    private void reportProgress() {
        if (System.currentTimeMillis() - lastProgress > 300) {
            lastProgress = System.currentTimeMillis(); final int n = count;
            AndroidUtilities.runOnUIThread(() -> progressListener.update(n, false, null, false));
        }
    }
    private void walk(Sink sink) throws Exception {
        if (DialogObject.isEncryptedDialog(dialog)) {
            long beforeDate = Long.MAX_VALUE, beforeId = Long.MAX_VALUE;
            while (true) {
                List<TLRPC.Message> page = cachedPage(beforeDate, beforeId);
                if (page.isEmpty()) break;
                for (TLRPC.Message m : page) {
                    // Secret history is local only. Saved deleted messages are emitted separately.
                    if (!savedDeleted(m.id)) accept(sink, m, false);
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
                    accept(sink, m, false);
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
                for (TLRPC.Message m : page) accept(sink, m, true);
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
        if (MessageObject.isStickerMessage(m) || MessageObject.isAnimatedStickerMessage(m)) return 5;
        if (MessageObject.isGifMessage(m)) return 8;
        if (MessageObject.isRoundVideoMessage(m)) return 7;
        if (MessageObject.isVoiceMessage(m)) return 3;
        if (MessageObject.isVideoMessage(m)) return 2;
        if (m.media != null && m.media.photo != null) return 1;
        if (MessageObject.getDocument(m) != null) return 4;
        return 0;
    }
    private String copyMedia(TLRPC.Message m, boolean deleted) throws Exception {
        if (kind(m) == 0 || !options.include[kind(m)]) return null;
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
        if (source.length() > options.maxFileBytes) return null;
        String folder = mediaFolder(m);
        Uri dest = create(directory(folder), "application/octet-stream", target);
        OutputStream stream = resolver.openOutputStream(dest, "w");
        if (stream == null) throw new IOException("Cannot write media");
        fileCount++;
        long copied = 0;
        try (OutputStream out = stream; InputStream in = new BufferedInputStream(new FileInputStream(source))) {
            byte[] buffer = new byte[65536]; int n;
            while ((n = in.read(buffer)) != -1) {
                check(); copied += n;
                if (copied > options.maxFileBytes) throw new IOException("Media grew beyond size limit");
                out.write(buffer, 0, n); totalBytes += n;
                reportProgress();
            }
        }
        // SAF providers may adjust a requested filename; links must use the actual name.
        try (android.database.Cursor cursor = resolver.query(dest,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) throw new IOException("Cannot resolve exported media name");
            target = cursor.getString(0);
        }
        return folder + "/" + java.net.URLEncoder.encode(target, "UTF-8").replace("+", "%20");
    }
    private String senderName(TLRPC.Message m) {
        String name = names.get(sender(m));
        if (name != null) return name;
        long id = sender(m);
        MessagesController controller = MessagesController.getInstance(account);
        if (id > 0) {
            TLRPC.User user = controller.getUser(id);
            if (user != null) return org.telegram.messenger.UserObject.getUserName(user);
        } else if (id < 0) {
            TLRPC.Chat chat = controller.getChat(-id);
            if (chat != null) return chat.title;
        }
        return id == 0 ? title : Long.toString(id);
    }
    private long sender(TLRPC.Message m) { return m.from_id == null ? (m.peer_id == null ? 0 : MessageObject.getPeerId(m.peer_id)) : MessageObject.getPeerId(m.from_id); }
    private String mediaFolder(TLRPC.Message m) {
        switch (kind(m)) {
            case 1: return "photos";
            case 2: return "video_files";
            case 3: return "voice_messages";
            case 5: return "stickers";
            case 7: return "round_video_messages";
            case 8: return "animations";
            default: return "files";
        }
    }
    private String mediaType(TLRPC.Message m) {
        switch (kind(m)) {
            case 1: return "photo";
            case 2: return "video_file";
            case 3: return "voice_message";
            case 5: return "sticker";
            case 7: return "video_message";
            case 8: return "animation";
            case 4: return MessageObject.isMusicMessage(m) ? "audio_file" : "file";
            default: return "text";
        }
    }
    private static String timestamp(long seconds) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date(seconds * 1000));
    }
    private String text(TLRPC.Message m) {
        if (m.action != null && !(m.action instanceof TLRPC.TL_messageActionEmpty)) {
            CharSequence description = new MessageObject(account, m, false, false).messageText;
            if (description != null) return description.toString();
        }
        return m.message == null ? "" : m.message;
    }
    private List<ExportHtml.Entity> entities(TLRPC.Message m) {
        List<ExportHtml.Entity> result = new ArrayList<>();
        String text = text(m);
        if (m.entities != null) for (TLRPC.MessageEntity entity : m.entities) {
            int offset = entity.offset, length = entity.length;
            if (offset < 0 || length <= 0 || offset > text.length() - length) continue;
            String type = entity.getClass().getSimpleName().replace("TL_messageEntity", "").toLowerCase(Locale.ROOT);
            String url = entity.url;
            String value = text.substring(offset, offset + length);
            switch (type) {
                case "texturl": type = "text_link"; break;
                case "url": type = "link"; url = value.contains("://") ? value : "https://" + value; break;
                case "email": url = "mailto:" + value; break;
                case "phone": url = "tel:" + value; break;
                case "mention": url = "https://t.me/" + value.substring(1); break;
                case "strike": type = "strikethrough"; break;
                case "blockquote_layer180": type = "blockquote"; break;
            }
            result.add(new ExportHtml.Entity(offset, length, type, url));
        }
        return result;
    }
    private void jsonEntity(JsonWriter out, ExportHtml.Part part) throws IOException {
        ExportHtml.Entity entity = part.entities.isEmpty() ? null : part.entities.get(0);
        out.beginObject().name("type").value(entity == null ? "plain" : entity.type).name("text").value(part.text);
        if (entity != null && entity.url != null) out.name("href").value(entity.url);
        out.endObject();
    }
    private void writeJson(JsonWriter out, TLRPC.Message m, boolean deleted, String file) throws Exception {
        out.beginObject().name("id").value(m.id).name("type").value(m.action != null && !(m.action instanceof TLRPC.TL_messageActionEmpty) ? "service" : "message")
                .name("date").value(timestamp(m.date)).name("date_unixtime").value(Integer.toString(m.date))
                .name("from").value(senderName(m)).name("from_id").value((sender(m) < 0 ? "channel" : "user") + Math.abs(sender(m)))
                .name("out").value(m.out).name("deleted").value(deleted);
        if (m.edit_date != 0) out.name("edited").value(timestamp(m.edit_date)).name("edited_unixtime").value(Integer.toString(m.edit_date));
        if (m.reply_to != null) out.name("reply_to_message_id").value(m.reply_to.reply_to_msg_id);
        if (m.fwd_from != null && m.fwd_from.from_name != null) out.name("forwarded_from").value(m.fwd_from.from_name);
        if (kind(m) != 0) {
            out.name(kind(m) == 1 ? "photo" : "file").value(file == null ? null : java.net.URLDecoder.decode(file, "UTF-8"))
                    .name("media_type").value(mediaType(m)).name("file_name").value(FileLoader.getMessageFileName(m))
                    .name("media_missing").value(file == null);
            TLRPC.Document document = MessageObject.getDocument(m);
            if (document != null) {
                out.name("mime_type").value(document.mime_type).name("file_size").value(document.size);
                for (TLRPC.DocumentAttribute attribute : document.attributes) {
                    if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                        out.name("width").value(attribute.w).name("height").value(attribute.h)
                                .name("duration_seconds").value((int) attribute.duration);
                    } else if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                        out.name("duration_seconds").value((int) attribute.duration);
                        if (attribute.title != null) out.name("title").value(attribute.title);
                        if (attribute.performer != null) out.name("performer").value(attribute.performer);
                    } else if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                        out.name("width").value(attribute.w).name("height").value(attribute.h);
                    } else if (attribute instanceof TLRPC.TL_documentAttributeSticker && attribute.alt != null) {
                        out.name("sticker_emoji").value(attribute.alt);
                    }
                }
            }
        }
        List<ExportHtml.Part> parts = ExportHtml.parts(text(m), entities(m));
        out.name("text");
        if (parts.stream().allMatch(part -> part.entities.isEmpty())) out.value(text(m));
        else {
            out.beginArray();
            for (ExportHtml.Part part : parts) {
                if (part.entities.isEmpty()) out.value(part.text); else jsonEntity(out, part);
            }
            out.endArray();
        }
        out.name("text_entities").beginArray();
        for (ExportHtml.Part part : parts) jsonEntity(out, part);
        out.endArray();
        out.name("entities").beginArray();
        if (m.entities != null) for (TLRPC.MessageEntity entity : m.entities) {
            out.beginObject().name("type").value(entity.getClass().getSimpleName()).name("offset").value(entity.offset)
                    .name("length").value(entity.length);
            if (entity.url != null) out.name("url").value(entity.url);
            out.endObject();
        }
        out.endArray();
        if (m.reply_markup instanceof TLRPC.TL_replyInlineMarkup) {
            out.name("inline_bot_buttons").beginArray();
            for (org.telegram.tgnet.tl.TL_keyboard.KeyboardInlineButtonRow row : ((TLRPC.TL_replyInlineMarkup) m.reply_markup).rows) {
                out.beginArray();
                for (org.telegram.tgnet.tl.TL_keyboard.KeyboardInlineButton button : row.buttons) {
                    out.beginObject().name("type").value(button.getType().getClass().getSimpleName().replace("TL_inlineButtonType", "").toLowerCase(Locale.ROOT).replace("copy", "copy_text").replace("switchinline", "switch_inline"))
                            .name("text").value(button.text);
                    if (button.getUrl() != null) out.name("data").value(button.getUrl());
                    if (button.getData() != null) out.name("dataBase64").value(android.util.Base64.encodeToString(button.getData(), android.util.Base64.NO_WRAP))
                            .name("data").value(new String(button.getData(), java.nio.charset.StandardCharsets.UTF_8));
                    if (button.getType() instanceof org.telegram.tgnet.tl.TL_keyboard.TL_inlineButtonTypeCopy)
                        out.name("data").value(((org.telegram.tgnet.tl.TL_keyboard.TL_inlineButtonTypeCopy) button.getType()).copy_text);
                    out.endObject();
                }
                out.endArray();
            }
            out.endArray();
        }
        out.endObject();
    }
    private static String escape(String value) { return ExportHtml.escape(value); }
    private ExportHtml.Message htmlMessage(TLRPC.Message m, boolean deleted, String file) {
        ExportHtml.Message result = new ExportHtml.Message();
        result.id = m.id; result.date = m.date; result.edited = m.edit_date;
        result.sender = sender(m); result.name = senderName(m); result.text = text(m);
        result.deleted = deleted; result.file = file; result.fileName = FileLoader.getMessageFileName(m);
        result.kind = kind(m); result.mediaType = mediaType(m); result.music = MessageObject.isMusicMessage(m);
        result.entities = entities(m);
        result.service = m.action != null && !(m.action instanceof TLRPC.TL_messageActionEmpty);
        result.replyId = m.reply_to == null ? 0 : m.reply_to.reply_to_msg_id;
        if (m.fwd_from != null) {
            result.forwarded = m.fwd_from.from_name;
            if (result.forwarded == null && m.fwd_from.from_id != null) {
                long id = MessageObject.getPeerId(m.fwd_from.from_id);
                TLRPC.User user = id > 0 ? MessagesController.getInstance(account).getUser(id) : null;
                TLRPC.Chat chat = id < 0 ? MessagesController.getInstance(account).getChat(-id) : null;
                result.forwarded = user != null ? org.telegram.messenger.UserObject.getUserName(user) : chat != null ? chat.title : Long.toString(id);
            }
            if (result.forwarded == null) result.forwarded = "";
        }
        if (m.reply_markup instanceof TLRPC.TL_replyInlineMarkup) {
            for (org.telegram.tgnet.tl.TL_keyboard.KeyboardInlineButtonRow row : ((TLRPC.TL_replyInlineMarkup) m.reply_markup).rows) {
                List<ExportHtml.Button> buttons = new ArrayList<>();
                for (org.telegram.tgnet.tl.TL_keyboard.KeyboardInlineButton button : row.buttons) {
                    ExportHtml.Button b = new ExportHtml.Button(); b.text = button.text; b.url = button.getUrl();
                    if (button.getData() != null) b.data = new String(button.getData(), java.nio.charset.StandardCharsets.UTF_8);
                    if (button.getType() instanceof org.telegram.tgnet.tl.TL_keyboard.TL_inlineButtonTypeCopy)
                        b.data = ((org.telegram.tgnet.tl.TL_keyboard.TL_inlineButtonTypeCopy) button.getType()).copy_text;
                    buttons.add(b);
                }
                result.buttons.add(buttons);
            }
        }
        return result;
    }
}
