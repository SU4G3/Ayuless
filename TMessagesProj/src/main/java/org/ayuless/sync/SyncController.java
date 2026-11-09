/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package org.ayuless.sync;

import android.os.Build;
import android.provider.Settings;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.telegram.messenger.LocaleController.getString;

/** AyuSync v1 read/batch/force protocol, using the app's existing WebSocket library. */
public final class SyncController {
    public static final int STATUS_CHANGED = -0x415956;
    private static final DispatchQueue queue = new DispatchQueue("ayulessSync");
    private static volatile int generation;
    private static volatile int status = R.string.AyulessSyncDisabled;
    private static volatile int lastSent, lastReceived, registerCode;
    private static volatile WebSocket socket;
    private static Runnable retry;
    private static final int MAX_BODY = 1024 * 1024;
    private SyncController() {}

    public static String statusText() { return getString(status); }
    public static int lastSent() { return lastSent; }
    public static int lastReceived() { return lastReceived; }
    public static int registerCode() { return registerCode; }
    public static String deviceId() {
        return Settings.Secure.getString(ApplicationLoader.applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    private static void state(int value) {
        status = value;
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(STATUS_CHANGED));
    }
    public static void apply() {
        final int next = ++generation;
        queue.postRunnable(() -> {
            if (retry != null) { queue.cancelRunnable(retry); retry = null; }
            WebSocket previous = socket; socket = null;
            if (previous != null) previous.disconnect();
            if (next != generation) return;
            if (!SyncConfig.enabled()) { state(R.string.AyulessSyncDisabled); return; }
            connect(next);
        });
    }
    private static void connect(int version) {
        if (version != generation || !SyncConfig.enabled()) return;
        if (SyncConfig.token().isEmpty()) { state(R.string.AyulessSyncNoToken); return; }
        if (SyncConfig.server().isEmpty()) { state(R.string.AyulessSyncNoServer); return; }
        state(R.string.AyulessSyncConnecting);
        try {
            JSONObject user = http("/user/v1", null);
            if (version != generation) return;
            if (!user.optBoolean("isMVP")) { state(R.string.AyulessSyncNoMvp); return; }
            JSONObject device = new JSONObject().put("name", Build.MANUFACTURER + " " + Build.MODEL).put("identifier", deviceId());
            http("/sync/register/v1", device);
            if (version != generation) return;
            WebSocket ws = new WebSocketFactory().setConnectionTimeout(10000).createSocket(SyncConfig.webSocketUrl());
            ws.addHeader("X-APP-PACKAGE", ApplicationLoader.applicationContext.getPackageName());
            ws.addHeader("X-DEVICE-IDENTIFIER", deviceId());
            ws.addHeader("Authorization", SyncConfig.token());
            ws.setPingInterval(30000);
            ws.setMaxPayloadSize(MAX_BODY);
            ws.addListener(new WebSocketAdapter() {
                @Override public void onConnected(WebSocket current, Map<String, List<String>> headers) {
                    queue.postRunnable(() -> {
                        if (version != generation || current != socket) { current.disconnect(); return; }
                        state(R.string.AyulessSyncConnected);
                    });
                }
                @Override public void onTextMessage(WebSocket current, String text) {
                    if (text.length() > MAX_BODY) return;
                    AndroidUtilities.runOnUIThread(() -> {
                        if (version != generation || current != socket) return;
                        try { handle(new JSONObject(text), 0); lastReceived = now(); state(status); }
                        catch (Exception ignored) { /* Malformed external events must not affect local history. */ }
                    });
                }
                @Override public void onDisconnected(WebSocket current, WebSocketFrame server, WebSocketFrame client, boolean byServer) {
                    queue.postRunnable(() -> { if (version == generation && current == socket) reconnect(version); });
                }
            });
            socket = ws;
            ws.connect();
        } catch (Exception e) {
            // Never log request headers, token, server response body or private dialog states.
            if (version != generation) return;
            if (e instanceof HttpError && ((HttpError) e).code == 401) state(R.string.AyulessSyncInvalidToken);
            else if (e instanceof HttpError && ((HttpError) e).code == 403) state(R.string.AyulessSyncNoMvp);
            else reconnect(version);
        }
    }
    private static void reconnect(int version) {
        WebSocket old = socket; socket = null;
        if (old != null) old.disconnect();
        state(R.string.AyulessSyncDisconnected);
        if (retry != null) queue.cancelRunnable(retry);
        retry = () -> { retry = null; connect(version); };
        if (version == generation && SyncConfig.enabled()) queue.postRunnable(retry, 5000);
    }
    private static int now() { return (int) (System.currentTimeMillis() / 1000); }
    private static final class HttpError extends Exception {
        final int code;
        HttpError(int code) { this.code = code; }
    }
    private static JSONObject http(String path, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(SyncConfig.url(path)).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
        connection.setRequestProperty("X-APP-PACKAGE", ApplicationLoader.applicationContext.getPackageName());
        connection.setRequestProperty("Authorization", SyncConfig.token());
        try {
            if (body != null) {
                connection.setRequestMethod("POST"); connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (java.io.OutputStream stream = connection.getOutputStream()) { stream.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            }
            int code = connection.getResponseCode();
            if (path.equals("/sync/register/v1")) registerCode = code;
            if (code < 200 || code >= 300) throw new HttpError(code);
            if (!path.equals("/user/v1")) return new JSONObject();
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int count;
                while ((count = stream.read(buffer)) != -1) {
                    if (bytes.size() + count > MAX_BODY) throw new java.io.IOException("response too large");
                    bytes.write(buffer, 0, count);
                }
                return new JSONObject(bytes.toString("UTF-8"));
            }
        } finally { connection.disconnect(); }
    }
    private static int account(long userId) {
        if (userId == 0) return -1;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            UserConfig config = UserConfig.getInstance(i);
            if (config.isClientActivated() && config.getClientUserId() == userId) return i;
        }
        return -1;
    }
    private static JSONObject event(String type, long userId, JSONObject args) throws Exception {
        return new JSONObject().put("type", type).put("userId", userId).put("args", args);
    }
    private static JSONObject readEvent(int account, TLRPC.Dialog dialog, int untilId) throws Exception {
        return event("sync_read", UserConfig.getInstance(account).getClientUserId(), new JSONObject()
                .put("dialogId", dialog.id).put("untilId", untilId).put("unread", dialog.unread_count));
    }
    private static void send(JSONObject event) {
        final int version = generation;
        queue.postRunnable(() -> {
            if (version != generation || !SyncConfig.enabled() || socket == null || !socket.isOpen()) return;
            socket.sendText(event.toString()); lastSent = now(); state(status);
        });
    }
    public static void onBlockedRead(int account, TLObject request) {
        if (!SyncConfig.enabled()) return;
        long did; int max;
        if (request instanceof TLRPC.TL_messages_readHistory) {
            TLRPC.TL_messages_readHistory read = (TLRPC.TL_messages_readHistory) request;
            did = DialogObject.getPeerDialogId(read.peer); max = read.max_id;
        } else if (request instanceof TLRPC.TL_channels_readHistory) {
            TLRPC.TL_channels_readHistory read = (TLRPC.TL_channels_readHistory) request;
            did = -read.channel.channel_id; max = read.max_id;
        } else return; // encrypted dates and topic-scoped reads are not dialog-wide watermarks
        AndroidUtilities.runOnUIThread(() -> {
            TLRPC.Dialog dialog = MessagesController.getInstance(account).getDialog(did);
            if (dialog == null || max <= 0) return;
            try { send(readEvent(account, dialog, max)); } catch (Exception ignored) {}
        });
    }
    /** UI thread only; remote reads update local storage without creating any Telegram receipt. */
    private static void handle(JSONObject event, int depth) throws Exception {
        if (!SyncConfig.enabled() || depth > 4) return;
        String type = event.optString("type");
        int account = account(event.optLong("userId"));
        if ("sync_batch".equals(type)) {
            if (account < 0 && event.optLong("userId") != 0) return;
            JSONArray events = event.getJSONObject("args").getJSONArray("events");
            for (int i = 0; i < Math.min(events.length(), 5000); i++) handle(events.getJSONObject(i), depth + 1);
            return;
        }
        if (account < 0) return;
        MessagesController controller = MessagesController.getInstance(account);
        if ("sync_force".equals(type)) {
            JSONArray batch = new JSONArray();
            for (TLRPC.Dialog dialog : controller.getAllDialogs()) {
                if (DialogObject.isEncryptedDialog(dialog.id) || dialog.read_inbox_max_id <= 0) continue;
                batch.put(readEvent(account, dialog, dialog.read_inbox_max_id));
                if (batch.length() >= 100) { send(event("sync_batch", event.getLong("userId"), new JSONObject().put("events", batch))); batch = new JSONArray(); }
            }
            if (batch.length() > 0) send(event("sync_batch", event.getLong("userId"), new JSONObject().put("events", batch)));
            send(event("sync_force_finish", event.getLong("userId"), new JSONObject()));
        } else if ("sync_read".equals(type)) {
            JSONObject args = event.getJSONObject("args");
            long did = args.getLong("dialogId"); int until = args.getInt("untilId"), unread = args.getInt("unread");
            TLRPC.Dialog dialog = controller.getDialog(did);
            if (dialog == null || DialogObject.isEncryptedDialog(did) || until <= dialog.read_inbox_max_id
                    || until > dialog.top_message || unread < 0 || unread >= dialog.unread_count) return;
            LongSparseIntArray inbox = new LongSparseIntArray(), remaining = new LongSparseIntArray();
            inbox.put(did, until); remaining.put(did, unread);
            controller.dialogs_read_inbox_max.put(did, until);
            MessagesStorage storage = MessagesStorage.getInstance(account);
            storage.updateDialogsWithReadMessages(inbox, null, null, remaining, true);
            storage.markMessagesAsRead(inbox, null, null, true);
        }
    }
    public static void forceSync(int account) {
        if (!SyncConfig.enabled()) return;
        final int version = generation;
        final long userId = UserConfig.getInstance(account).getClientUserId();
        queue.postRunnable(() -> {
            if (version != generation) return;
            try { http("/sync/force/v1", new JSONObject().put("userId", userId).put("fromDate", 0)); }
            catch (Exception e) { if (version == generation) state(R.string.AyulessSyncDisconnected); }
        });
    }
}
