/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.push;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.PushListenerController;
import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.PushService;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;

import java.nio.charset.StandardCharsets;

/** Receives UnifiedPush events from the distributor (via the connector library). */
public class AyulessPushService extends PushService {

    @Override
    public void onNewEndpoint(@NonNull PushEndpoint endpoint, @NonNull String instance) {
        String token;
        try {
            token = UnifiedPushProvider.buildToken(endpoint);
        } catch (Exception e) {
            FileLog.e(e);
            token = null;
        }
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("UnifiedPush new endpoint, temporary=" + endpoint.getTemporary() + ", keys=" + (token != null));
        }
        final String finalToken = token;
        AndroidUtilities.runOnUIThread(() -> {
            ApplicationLoader.postInitApplication();
            if (finalToken == null) {
                // Web Push needs the encryption keys; without them the server cannot deliver.
                UnifiedPushProvider.onNoDistributor("__UNIFIEDPUSH_NO_KEYS__");
            } else {
                UnifiedPushProvider.onEndpoint(finalToken);
            }
        });
    }

    @Override
    public void onMessage(@NonNull PushMessage message, @NonNull String instance) {
        long time = System.currentTimeMillis();
        String payload = extractPayload(message);
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("UnifiedPush message, decrypted=" + message.getDecrypted() + ", bytes=" + message.getContent().length);
        }
        // processRemoteMessage blocks until processing is done, so keep it off the main thread.
        // Unparseable payloads still wake the connection there and fetch updates normally.
        new Thread(() -> PushListenerController.processRemoteMessage(PushListenerController.PUSH_TYPE_WEBPUSH, payload, time), "AyulessPush").start();
    }

    @Override
    public void onRegistrationFailed(@NonNull FailedReason reason, @NonNull String instance) {
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("UnifiedPush registration failed: " + reason);
        }
        AndroidUtilities.runOnUIThread(() -> UnifiedPushProvider.onNoDistributor("__UNIFIEDPUSH_FAILED_" + reason.name() + "__"));
    }

    @Override
    public void onUnregistered(@NonNull String instance) {
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("UnifiedPush unregistered");
        }
        AndroidUtilities.runOnUIThread(() -> UnifiedPushProvider.onNoDistributor("__UNIFIEDPUSH_UNREGISTERED__"));
    }

    /**
     * The decrypted Web Push body is the notification JSON. If the server wraps an
     * MTProto-encrypted blob in a "p" field (as with FCM), pass that blob instead so the
     * upstream decryption path handles it.
     * Returns null for content that was not Web Push encrypted for us.
     */
    static String extractPayload(PushMessage message) {
        if (!message.getDecrypted()) {
            // Not encrypted with our keys, so anyone who knows the endpoint could have sent it.
            // Never trust its content; a null payload only wakes the connection.
            return null;
        }
        String text = new String(message.getContent(), StandardCharsets.UTF_8).trim();
        try {
            JSONObject json = new JSONObject(text);
            Object p = json.opt("p");
            if (p instanceof String && !json.has("loc_key")) {
                return (String) p;
            }
        } catch (Exception ignore) {
            // Not JSON: let processRemoteMessage fail and fall back to waking the connection.
        }
        return text;
    }
}
