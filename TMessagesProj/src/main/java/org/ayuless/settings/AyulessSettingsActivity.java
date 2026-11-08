/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ayuless.ghost.GhostConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Settings > Ayuless: header, feature categories and project links. */
public class AyulessSettingsActivity extends UniversalFragment {

    static final String SOURCE_URL = "https://github.com/SU4G3/Ayuless";
    private static final String ISSUES_URL = SOURCE_URL + "/issues";
    private static final String DOCS_URL = SOURCE_URL + "#readme";

    private static final int ID_GHOST = 1;
    private static final int ID_SPY = 2;
    private static final int ID_FILTERS = 3;
    private static final int ID_CUSTOMIZATION = 4;
    private static final int ID_SOURCE = 10;
    private static final int ID_ISSUES = 11;
    private static final int ID_DOCS = 12;

    private View headerView;

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.AyulessSettings);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Category values (e.g. ghost mode on/off) may have changed in a sub-screen.
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (headerView == null && getParentActivity() != null) {
            headerView = createHeader(getParentActivity());
        }
        if (headerView != null) {
            items.add(UItem.asCustom(headerView));
        }

        items.add(UItem.asHeader(getString(R.string.AyulessCategoriesHeader)));
        String ghostState = GhostConfig.isEnabled(currentAccount)
                ? GhostConfig.countEnabled(GhostConfig.activeProfile(currentAccount)) + "/" + GhostConfig.GHOST_KEYS.length
                : getString(R.string.AyulessOff);
        items.add(UItem.asButton(ID_GHOST, R.drawable.ayuless_solar_ghost, getString(R.string.AyulessGhostHeader), ghostState));
        items.add(UItem.asButton(ID_SPY, R.drawable.ayuless_solar_msg_stories_stealth, getString(R.string.AyulessSpy)));
        items.add(UItem.asButton(ID_FILTERS, R.drawable.ayuless_solar_msg_filter, getString(R.string.AyulessFilters), getString(R.string.AyulessSoon)));
        items.add(UItem.asButton(ID_CUSTOMIZATION, R.drawable.ayuless_solar_menu_edit_appearance, getString(R.string.AyulessCustomization)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.AyulessLinksHeader)));
        items.add(UItem.asButton(ID_SOURCE, R.drawable.ayuless_solar_msg_link2, getString(R.string.AyulessSourceCode), "GitHub"));
        items.add(UItem.asButton(ID_ISSUES, R.drawable.ayuless_solar_msg_ask_question, getString(R.string.AyulessIssues)));
        items.add(UItem.asButton(ID_DOCS, R.drawable.ayuless_solar_msg_info, getString(R.string.AyulessDocs)));
        items.add(UItem.asShadow(getString(R.string.AyulessDisclaimer)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_GHOST:
                presentFragment(new GhostSettingsActivity());
                break;
            case ID_SPY:
                presentFragment(new SpySettingsActivity());
                break;
            case ID_FILTERS:
                org.telegram.ui.Components.BulletinFactory.of(this)
                        .createSimpleBulletin(R.raw.info, getString(R.string.AyulessFiltersSoon)).show();
                break;
            case ID_CUSTOMIZATION:
                presentFragment(new AppearanceSettingsActivity());
                break;
            case ID_SOURCE:
                Browser.openUrl(getParentActivity(), SOURCE_URL);
                break;
            case ID_ISSUES:
                Browser.openUrl(getParentActivity(), ISSUES_URL);
                break;
            case ID_DOCS:
                Browser.openUrl(getParentActivity(), DOCS_URL);
                break;
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private View createHeader(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(0, dp(20), 0, dp(16));
        layout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.mipmap.ic_launcher_ayuless_round);
        layout.addView(icon, LayoutHelper.createLinear(80, 80));

        TextView name = new TextView(context);
        name.setText(getString(R.string.AyulessLauncherName));
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        layout.addView(name, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

        TextView version = new TextView(context);
        version.setText(BuildVars.BUILD_VERSION_STRING);
        version.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        version.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, getResourceProvider()));
        layout.addView(version, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        return layout;
    }
}
