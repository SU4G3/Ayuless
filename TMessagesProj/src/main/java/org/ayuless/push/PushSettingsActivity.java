/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.push;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.List;

/** Ayuless > Notifications: choose a UnifiedPush distributor or the background connection. */
public class PushSettingsActivity extends UniversalFragment {

    private static final String UNIFIEDPUSH_URL = "https://unifiedpush.org/users/distributors/";

    private static final int ID_NONE = 1;
    private static final int ID_LEARN_MORE = 2;
    private static final int ID_DISTRIBUTOR_BASE = 100;

    private final ArrayList<String> distributors = new ArrayList<>();

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.AyulessNotifications);
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Context context = getContext();
        distributors.clear();
        String current = null;
        if (context != null) {
            List<String> found = UnifiedPushProvider.getDistributors(context);
            distributors.addAll(found);
            current = UnifiedPushProvider.getCurrentDistributor(context);
        }

        items.add(UItem.asHeader(getString(R.string.AyulessPushDistributor)));
        items.add(UItem.asRadio(ID_NONE, getString(R.string.AyulessPushNone)).setChecked(current == null || !distributors.contains(current)));
        for (int i = 0; i < distributors.size(); i++) {
            String pkg = distributors.get(i);
            items.add(UItem.asRadio(ID_DISTRIBUTOR_BASE + i, appLabel(context, pkg), pkg).setChecked(pkg.equals(current)));
        }
        String status;
        if (current != null && UnifiedPushProvider.isRegistered()) {
            status = getString(R.string.AyulessPushStatusRegistered);
        } else if (current != null) {
            status = getString(R.string.AyulessPushStatusPending);
        } else if (distributors.isEmpty()) {
            status = getString(R.string.AyulessPushStatusNoDistributors);
        } else {
            status = getString(R.string.AyulessPushStatusFallback);
        }
        items.add(UItem.asShadow(status + "\n\n" + getString(R.string.AyulessPushInfo)));
        items.add(UItem.asButton(ID_LEARN_MORE, R.drawable.ayuless_solar_msg_info, getString(R.string.AyulessPushLearnMore)));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (item.id == ID_NONE) {
            UnifiedPushProvider.disableDistributor(context);
            update();
        } else if (item.id == ID_LEARN_MORE) {
            Browser.openUrl(getParentActivity(), UNIFIEDPUSH_URL);
        } else if (item.id >= ID_DISTRIBUTOR_BASE && item.id - ID_DISTRIBUTOR_BASE < distributors.size()) {
            UnifiedPushProvider.useDistributor(context, distributors.get(item.id - ID_DISTRIBUTOR_BASE));
            update();
            // Registration finishes asynchronously in the distributor; refresh the status afterwards.
            AndroidUtilities.runOnUIThread(this::update, 3000);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private static CharSequence appLabel(Context context, String pkg) {
        if (context == null) {
            return pkg;
        }
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(info);
        } catch (Exception e) {
            return pkg;
        }
    }
}
