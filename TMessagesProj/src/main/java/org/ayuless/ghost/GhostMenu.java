/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package org.ayuless.ghost;

import org.ayuless.extras.ExtrasConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.ItemOptions;
import static org.telegram.messenger.LocaleController.getString;

/** AyuGram's drawer actions, using the current upstream overflow menu. */
public final class GhostMenu {
    private GhostMenu() {}
    public static void add(BaseFragment fragment, int account, ItemOptions menu) {
        if (ExtrasConfig.get(ExtrasConfig.KEY_GHOST_MENU)) {
            menu.add(R.drawable.ayuless_solar_ghost,
                    getString(GhostConfig.isEnabled(account) ? R.string.AyulessDisableGhost : R.string.AyulessEnableGhost),
                    () -> GhostBadge.toggle(fragment, account, null));
        }
        if (ExtrasConfig.get(ExtrasConfig.KEY_KILL_MENU)) {
            menu.add(R.drawable.ayuless_solar_msg_delete, getString(R.string.AyulessKillApp), () -> {
                if (fragment.getParentActivity() == null) return;
                fragment.getParentActivity().finishAndRemoveTask();
                android.os.Process.killProcess(android.os.Process.myPid());
            });
        }
    }
}
