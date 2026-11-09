/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package org.ayuless.sync;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import java.net.URI;

/** AyuSync endpoint configuration. No account, token or third-party endpoint is preselected. */
public final class SyncConfig {
    private SyncConfig() {}
    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_sync", Context.MODE_PRIVATE);
    }
    public static boolean enabled() { return prefs().getBoolean("enabled", false); }
    public static void setEnabled(boolean value) { prefs().edit().putBoolean("enabled", value).apply(); }
    public static boolean secure() { return !prefs().getBoolean("insecure", false); }
    public static void setSecure(boolean value) { prefs().edit().putBoolean("insecure", !value).apply(); }
    public static String server() { return prefs().getString("server", ""); }
    public static String token() { return prefs().getString("token", ""); }
    public static void setToken(String value) { prefs().edit().putString("token", value.trim()).apply(); }
    public static boolean setServer(String value) {
        value = value.trim().replaceFirst("^(https?|wss?)://", "");
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (!value.isEmpty()) {
            try {
                URI uri = new URI("https://" + value);
                if (uri.getHost() == null || uri.getPort() == 0 || uri.getPort() > 65535 || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                        || uri.getRawFragment() != null || uri.getPath() != null && !uri.getPath().isEmpty()) return false;
            } catch (Exception e) { return false; }
        }
        prefs().edit().putString("server", value).apply();
        return true;
    }
    public static String url(String path) { return (secure() ? "https://" : "http://") + server() + path; }
    public static String webSocketUrl() { return (secure() ? "wss://" : "ws://") + server() + "/sync/ws/v1"; }
}
