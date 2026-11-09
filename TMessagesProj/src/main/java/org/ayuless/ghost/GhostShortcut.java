/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package org.ayuless.ghost;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import org.ayuless.extras.ExtrasConfig;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.LaunchActivity;
import java.util.Collections;
import static org.telegram.messenger.LocaleController.getString;

/** Optional Enter with Ghost shortcut; only the user's explicit launch changes ghost mode. */
public final class GhostShortcut {
    private static final String ID = "ayuless_ghost";
    public static final String ACTION = "org.ayuless.ENTER_WITH_GHOST";
    private GhostShortcut() {}
    public static void update() {
        Context context = ApplicationLoader.applicationContext;
        try {
            if (ExtrasConfig.get(ExtrasConfig.KEY_GHOST_SHORTCUT)) {
                ShortcutManagerCompat.enableShortcuts(context, Collections.singletonList(ID));
                Intent intent = new Intent(context, LaunchActivity.class).setAction(ACTION);
                ShortcutManagerCompat.addDynamicShortcuts(context, Collections.singletonList(new ShortcutInfoCompat.Builder(context, ID)
                        .setShortLabel(getString(R.string.AyulessGhostShortcut))
                        .setIcon(IconCompat.createWithResource(context, R.drawable.ayuless_solar_ghost)).setIntent(intent).build()));
            } else {
                ShortcutManagerCompat.removeDynamicShortcuts(context, Collections.singletonList(ID));
                ShortcutManagerCompat.disableShortcuts(context, Collections.singletonList(ID), getString(R.string.AyulessOff));
            }
        } catch (Exception e) { org.telegram.messenger.FileLog.e(e); }
    }
    public static void handle(Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction()) || !ExtrasConfig.get(ExtrasConfig.KEY_GHOST_SHORTCUT)) return;
        int profile = GhostConfig.activeProfile(UserConfig.selectedAccount);
        GhostConfig.setProfile(profile, GhostConfig.KEY_ENABLED, true);
        intent.setAction(Intent.ACTION_MAIN);
    }
}
