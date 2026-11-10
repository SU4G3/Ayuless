// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.ghost;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import org.ayuless.extras.ExtrasConfig;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.LaunchActivity;
import java.util.Collections;

public final class GhostShortcut {
    public static final String ID = "ayuless_ghost";
    public static final String ACTION = "org.ayuless.ENTER_WITH_GHOST";
    private GhostShortcut() {}

    public static boolean enabled() { return ExtrasConfig.get(ExtrasConfig.KEY_GHOST_SHORTCUT); }

    public static void refresh() {
        if (Build.VERSION.SDK_INT < 23) return;
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;
        try {
            if (!enabled()) {
                ShortcutManagerCompat.removeDynamicShortcuts(context, Collections.singletonList(ID));
                ShortcutManagerCompat.disableShortcuts(context, Collections.singletonList(ID),
                        LocaleController.getString(R.string.AyulessGhostTurnedOff));
                return;
            }
            Intent intent = new Intent(context, LaunchActivity.class).setAction(ACTION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, ID)
                    .setShortLabel(LocaleController.getString(R.string.AyulessGhostShortcut))
                    .setLongLabel(LocaleController.getString(R.string.AyulessGhostShortcut))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ayuless_solar_ghost))
                    .setIntent(intent).setRank(1).build();
            ShortcutManagerCompat.enableShortcuts(context, Collections.singletonList(shortcut));
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void handleIntent(Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction()) || !enabled()) return;
        GhostConfig.setProfile(GhostConfig.activeProfile(UserConfig.selectedAccount), GhostConfig.KEY_ENABLED, true);
        intent.setAction(Intent.ACTION_MAIN);
    }
}
