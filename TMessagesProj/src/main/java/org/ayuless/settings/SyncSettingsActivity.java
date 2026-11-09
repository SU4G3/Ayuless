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
import org.ayuless.sync.SyncConfig;
import org.ayuless.sync.SyncController;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import java.util.ArrayList;
import static org.telegram.messenger.LocaleController.getString;

/** AyuSyncPreferencesActivity rebuilt without the exteraGram preferences framework. */
public class SyncSettingsActivity extends AyulessSettingsFragment implements NotificationCenter.NotificationCenterDelegate {
    private static final int ENABLE = 1, SERVER = 2, TOKEN = 3, SECURE = 4, FORCE = 5, DEVICE = 6, PROFILE = 7;
    @Override protected CharSequence getTitle() { return getString(R.string.AyulessSyncHeader); }
    @Override public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, SyncController.STATUS_CHANGED);
        return super.onFragmentCreate();
    }
    @Override public void onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, SyncController.STATUS_CHANGED);
        super.onFragmentDestroy();
    }
    @Override public void didReceivedNotification(int id, int account, Object... args) { refresh(); }
    @Override protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCheck(ENABLE, getString(R.string.AyulessSyncEnable)).setChecked(SyncConfig.enabled()));
        items.add(UItem.asButton(0, getString(R.string.AyulessSyncStatus), SyncController.statusText()));
        items.add(UItem.asShadow(getString(R.string.AyulessSyncInfo)));
        items.add(UItem.asButton(SERVER, getString(R.string.AyulessSyncServer), SyncConfig.server()));
        items.add(UItem.asButton(TOKEN, getString(R.string.AyulessSyncToken), SyncConfig.token().isEmpty() ? getString(R.string.AyulessOff) : "••••••••"));
        items.add(UItem.asCheck(SECURE, getString(R.string.AyulessSyncSecure)).setChecked(SyncConfig.secure()));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(FORCE, getString(R.string.AyulessSyncForce)).setEnabled(SyncConfig.enabled()));
        items.add(UItem.asButton(PROFILE, getString(R.string.AyulessSyncProfile)).setEnabled(!SyncConfig.server().isEmpty()));
        items.add(UItem.asButton(DEVICE, getString(R.string.AyulessSyncDevice), SyncController.deviceId()));
        items.add(UItem.asButton(0, getString(R.string.AyulessSyncLastSent), time(SyncController.lastSent())));
        items.add(UItem.asButton(0, getString(R.string.AyulessSyncLastReceived), time(SyncController.lastReceived())));
        items.add(UItem.asButton(0, getString(R.string.AyulessSyncRegisterCode), String.valueOf(SyncController.registerCode())));
        items.add(UItem.asShadow(null));
    }
    @Override protected void onClick(UItem item, View view, int position, float x, float y) {
        if (!item.enabled) return;
        switch (item.id) {
            case ENABLE: SyncConfig.setEnabled(!SyncConfig.enabled()); changed(); break;
            case SECURE: SyncConfig.setSecure(!SyncConfig.secure()); changed(); break;
            case SERVER:
                SettingsTextDialog.show(this, getString(R.string.AyulessSyncServer), SyncConfig.server(), 256,
                        value -> { if (SyncConfig.setServer(value)) changed(); else BulletinFactory.of(this).createErrorBulletin(getString(R.string.AyulessSyncInvalidServer)).show(); },
                        () -> { SyncConfig.setServer(""); changed(); }); break;
            case TOKEN:
                SettingsTextDialog.show(this, getString(R.string.AyulessSyncToken), SyncConfig.token(), 2048,
                        value -> { SyncConfig.setToken(value); changed(); }, () -> { SyncConfig.setToken(""); changed(); }); break;
            case FORCE: SyncController.forceSync(currentAccount); break;
            case PROFILE: Browser.openUrl(getParentActivity(), SyncConfig.url("/ui/profile") + "?token=" + android.net.Uri.encode(SyncConfig.token())); break;
            case DEVICE: AndroidUtilities.addToClipboard(SyncController.deviceId()); BulletinFactory.of(this).createCopyBulletin(getString(R.string.AyulessSyncIdCopied)).show(); break;
        }
    }
    @Override protected boolean onLongClick(UItem item, View view, int position, float x, float y) { return false; }
    private String time(int value) { return value == 0 ? getString(R.string.AyulessSyncNever) : LocaleController.formatDateAudio(value, true); }
    private void refresh() { if (listView != null && listView.adapter != null) listView.adapter.update(false); }
    private void changed() { SyncController.apply(); refresh(); }
}
