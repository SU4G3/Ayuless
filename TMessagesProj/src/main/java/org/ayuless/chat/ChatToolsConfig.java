/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.chat;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;

/** Opt-in UI and per-account, per-dialog exceptions; shared ghost profiles remain untouched. */
public final class ChatToolsConfig {
    private ChatToolsConfig() {}
    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_chat_tools", Context.MODE_PRIVATE);
    }
    public static boolean enabled() { return prefs().getBoolean("enabled", false); }
    public static void setEnabled(boolean value) { prefs().edit().putBoolean("enabled", value).apply(); }
    public static boolean excepted(int account, long dialog, boolean typing) {
        return enabled() && dialog != 0 && prefs().getBoolean(account + ":" + dialog + (typing ? ":typing" : ":read"), false);
    }
    public static void setExcepted(int account, long dialog, boolean typing, boolean value) {
        prefs().edit().putBoolean(account + ":" + dialog + (typing ? ":typing" : ":read"), value).apply();
    }
}
