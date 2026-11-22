package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.drawable.ic_launcher_ayuless_background, R.drawable.ic_launcher_ayuless_foreground, R.string.AppIconDefault),
        // Ayuless variants share resources with their actual launcher aliases.
        VINTAGE("VintageIcon", R.drawable.ic_launcher_ayuless_classic_background, R.drawable.ic_launcher_ayuless_classic_foreground, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.ic_launcher_ayuless_ocean_background, R.drawable.ic_launcher_ayuless_ocean_foreground, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.ic_launcher_ayuless_premium_background, R.drawable.ic_launcher_ayuless_premium_foreground, R.string.AppIconPremium, true),
        TURBO("TurboIcon", R.drawable.ic_launcher_ayuless_turbo_background, R.drawable.ic_launcher_ayuless_turbo_foreground, R.string.AppIconTurbo, true),
        NOX("NoxIcon", R.drawable.ic_launcher_ayuless_night_background, R.drawable.ic_launcher_ayuless_night_foreground, R.string.AppIconNox, true);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        // Upstream preview grouping only; every launcher icon is free to select in Ayuless.
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
