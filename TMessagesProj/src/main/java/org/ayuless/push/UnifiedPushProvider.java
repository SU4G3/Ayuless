/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.PushListenerController;
import org.telegram.messenger.SharedConfig;
import org.unifiedpush.android.connector.UnifiedPush;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PublicKeySet;

import java.util.Collections;
import java.util.List;

import kotlin.Unit;

/**
 * Push delivery through a UnifiedPush distributor instead of Firebase.
 *
 * The distributor gives us a Web Push endpoint plus a P-256 public key and auth secret
 * (generated and kept by the connector library). We register that with the server as
 * token type 10 (Web Push). Incoming messages are RFC 8291 encrypted; the connector
 * decrypts them and {@link AyulessPushService} hands the payload to the upstream
 * push processing.
 *
 * Without a distributor we fall back to the upstream keep-alive background connection.
 */
public final class UnifiedPushProvider implements PushListenerController.IPushListenerServiceProvider {

    public static final UnifiedPushProvider INSTANCE = new UnifiedPushProvider();

    /** Single registration shared by all accounts, same model as upstream FCM. */
    static final String INSTANCE_NAME = "default";

    /** Set when Ayuless (not the user) turned the keep-alive service on. */
    private static final String KEY_AUTO_KEEPALIVE = "ayuless_push_auto_keepalive";
    private static final String KEY_PUSH_SERVICE = "pushService";

    private UnifiedPushProvider() {
    }

    @Override
    public boolean hasServices() {
        // Always "available": onRequestPushToken decides between a distributor and the fallback.
        return true;
    }

    @Override
    public String getLogTitle() {
        return "UnifiedPush";
    }

    @Override
    public int getPushType() {
        return PushListenerController.PUSH_TYPE_WEBPUSH;
    }

    @Override
    public void onRequestPushToken() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return;
        }
        try {
            UnifiedPush.tryUseCurrentOrDefaultDistributor(context, success -> {
                if (success) {
                    register(context);
                } else {
                    onNoDistributor("__NO_UNIFIEDPUSH_DISTRIBUTOR__");
                }
                return Unit.INSTANCE;
            });
        } catch (Throwable e) {
            FileLog.e(e);
            onNoDistributor("__UNIFIEDPUSH_ERROR__");
        }
    }

    /** Package names of installed distributors. */
    public static List<String> getDistributors(Context context) {
        try {
            return UnifiedPush.getDistributors(context);
        } catch (Throwable e) {
            FileLog.e(e);
            return Collections.emptyList();
        }
    }

    /** Package name of the distributor in use, or null. */
    public static String getCurrentDistributor(Context context) {
        try {
            String ack = UnifiedPush.getAckDistributor(context);
            return ack != null ? ack : UnifiedPush.getSavedDistributor(context);
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    /** User picked a distributor in settings. */
    public static void useDistributor(Context context, String packageName) {
        try {
            UnifiedPush.saveDistributor(context, packageName);
            register(context);
        } catch (Throwable e) {
            FileLog.e(e);
            onNoDistributor("__UNIFIEDPUSH_ERROR__");
        }
    }

    /** User chose "no distributor": unregister and use the background connection. */
    public static void disableDistributor(Context context) {
        try {
            UnifiedPush.removeDistributor(context);
        } catch (Throwable e) {
            FileLog.e(e);
        }
        onNoDistributor("__NO_UNIFIEDPUSH_DISTRIBUTOR__");
    }

    public static boolean isRegistered() {
        return SharedConfig.pushType == PushListenerController.PUSH_TYPE_WEBPUSH && !TextUtils.isEmpty(SharedConfig.pushString);
    }

    static void register(Context context) {
        try {
            UnifiedPush.register(context, INSTANCE_NAME, null, null);
        } catch (Throwable e) {
            FileLog.e(e);
            onNoDistributor("__UNIFIEDPUSH_ERROR__");
        }
    }

    /** Builds the Web Push token: {"endpoint": ..., "keys": {"p256dh": ..., "auth": ...}}. */
    static String buildToken(PushEndpoint endpoint) throws JSONException {
        PublicKeySet keys = endpoint.getPubKeySet();
        if (keys == null || TextUtils.isEmpty(endpoint.getUrl())) {
            return null;
        }
        JSONObject keysJson = new JSONObject();
        keysJson.put("p256dh", keys.getPubKey());
        keysJson.put("auth", keys.getAuth());
        JSONObject token = new JSONObject();
        token.put("endpoint", endpoint.getUrl());
        token.put("keys", keysJson);
        return token.toString();
    }

    static void onEndpoint(String token) {
        disableKeepAliveFallback();
        SharedConfig.pushStringStatus = "";
        PushListenerController.sendRegistrationToServer(PushListenerController.PUSH_TYPE_WEBPUSH, token);
    }

    static void onNoDistributor(String status) {
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("UnifiedPush unavailable: " + status + ", using background connection");
        }
        SharedConfig.pushStringStatus = status;
        enableKeepAliveFallback();
        PushListenerController.sendRegistrationToServer(PushListenerController.PUSH_TYPE_WEBPUSH, null);
    }

    /** Turn the keep-alive service on unless the user already chose a value. */
    private static void enableKeepAliveFallback() {
        SharedPreferences prefs = MessagesController.getGlobalNotificationsSettings();
        if (!prefs.contains(KEY_PUSH_SERVICE)) {
            prefs.edit().putBoolean(KEY_PUSH_SERVICE, true).putBoolean(KEY_AUTO_KEEPALIVE, true).apply();
            ApplicationLoader.startPushService();
        }
    }

    /** Undo enableKeepAliveFallback once a distributor works. A user choice is kept. */
    private static void disableKeepAliveFallback() {
        SharedPreferences prefs = MessagesController.getGlobalNotificationsSettings();
        if (prefs.getBoolean(KEY_AUTO_KEEPALIVE, false)) {
            prefs.edit().remove(KEY_PUSH_SERVICE).remove(KEY_AUTO_KEEPALIVE).apply();
            ApplicationLoader.startPushService();
        }
    }
}
