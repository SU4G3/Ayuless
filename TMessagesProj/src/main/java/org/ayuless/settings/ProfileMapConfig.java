/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.settings;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.IMapsProvider;

/** App-wide customization preferences; no account identifiers leave the device. */
public final class ProfileMapConfig {
    private ProfileMapConfig() {}

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ayuless_profile_maps", Context.MODE_PRIVATE);
    }

    public static boolean showProfileIdentity() { return prefs().getBoolean("profileIdentity", true); }
    public static void setShowProfileIdentity(boolean value) { prefs().edit().putBoolean("profileIdentity", value).apply(); }
    public static int mapType() {
        return validType(prefs().getInt("mapType", IMapsProvider.MAP_TYPE_NORMAL));
    }
    public static void setMapType(int value) { prefs().edit().putInt("mapType", validType(value)).apply(); }
    public static int validType(int value) {
        return value == IMapsProvider.MAP_TYPE_SATELLITE || value == IMapsProvider.MAP_TYPE_HYBRID ? value : IMapsProvider.MAP_TYPE_NORMAL;
    }
}
