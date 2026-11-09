/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package org.ayuless.settings;

import android.view.View;
import org.ayuless.extras.ExtrasConfig;
import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import java.util.ArrayList;
import static org.telegram.messenger.LocaleController.getString;

/** Optional AyuGram context menu and launcher extras. */
public class ChatFeaturesSettingsActivity extends AyulessSettingsFragment {
    private static final String[] KEYS = {ExtrasConfig.KEY_READ_UNTIL, ExtrasConfig.KEY_LOCAL_RESEND,
            ExtrasConfig.KEY_KEEP_LOCAL, ExtrasConfig.KEY_DELETE_DATE, ExtrasConfig.KEY_GHOST_SHORTCUT};
    private static final int[] TITLES = {R.string.AyulessReadUntilSetting, R.string.AyulessLocalResend,
            R.string.AyulessKeepLocalSetting, R.string.AyulessDeleteDateSetting, R.string.AyulessGhostShortcut};
    @Override protected CharSequence getTitle() { return getString(R.string.AyulessChatsMessages); }
    @Override protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        for (int i = 0; i < KEYS.length; i++) items.add(UItem.asCheck(i + 1, getString(TITLES[i])).setChecked(ExtrasConfig.get(KEYS[i])));
        items.add(UItem.asShadow(getString(R.string.AyulessChatFeaturesInfo)));
    }
    @Override protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id <= 0 || item.id > KEYS.length) return;
        String key = KEYS[item.id - 1];
        ExtrasConfig.set(key, !ExtrasConfig.get(key));
        if (ExtrasConfig.KEY_GHOST_SHORTCUT.equals(key)) org.ayuless.ghost.GhostShortcut.update();
        listView.adapter.update(true);
    }
    @Override protected boolean onLongClick(UItem item, View view, int position, float x, float y) { return false; }
}
