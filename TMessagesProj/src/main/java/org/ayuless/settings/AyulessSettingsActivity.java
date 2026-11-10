/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.pm.PackageInfoCompat;

import org.ayuless.ghost.GhostConfig;
import org.telegram.utils.glass.BaseFragmentSimpleGlass;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

/** Settings > Ayuless: header, feature categories and project links. */
public class AyulessSettingsActivity extends AyulessSettingsFragment {

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
    public void onInsets(int left, int top, int right, int bottom) {
        super.onInsets(left, top, right, bottom);
        if (listView != null) {
            BaseFragmentSimpleGlass.setListViewParams(listView, top, bottom, dp(12), 0);
        }
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
    protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (headerView == null && getParentActivity() != null) {
            headerView = createHeader(getParentActivity());
        }
        if (headerView != null) {
            items.add(UItem.asCustomShadow(headerView, 198));
        }

        items.add(UItem.asCheck(20, getString(R.string.AyulessChatTools)).setChecked(org.ayuless.chat.ChatToolsConfig.enabled()));
        items.add(UItem.asShadow(getString(R.string.AyulessChatToolsInfo)));
        items.add(UItem.asHeader(getString(R.string.AyulessCategoriesHeader)));
        String ghostState = GhostConfig.isEnabled(currentAccount)
                ? GhostConfig.countEnabled(GhostConfig.activeProfile(currentAccount)) + "/" + GhostConfig.GHOST_KEYS.length
                : getString(R.string.AyulessOff);
        items.add(UItem.asButton(ID_GHOST, R.drawable.ayuless_solar_ghost, getString(R.string.AyulessGhostHeader), ghostState));
        items.add(UItem.asButton(ID_SPY, R.drawable.ayuless_solar_msg_stories_stealth, getString(R.string.AyulessSpy)));
        String filtersState = org.ayuless.filters.MessageFilters.isEnabled()
                ? String.valueOf(org.ayuless.filters.MessageFilters.getFilters().size() + org.ayuless.filters.MessageFilters.getShadowBan().size())
                : getString(R.string.AyulessOff);
        items.add(UItem.asButton(ID_FILTERS, R.drawable.ayuless_solar_msg_filter, getString(R.string.AyulessFilters), filtersState));
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
            case 20:
                org.ayuless.chat.ChatToolsConfig.setEnabled(!org.ayuless.chat.ChatToolsConfig.enabled());
                listView.adapter.update(false);
                break;
            case ID_GHOST:
                presentFragment(new GhostSettingsActivity());
                break;
            case ID_SPY:
                presentFragment(new SpySettingsActivity());
                break;
            case ID_FILTERS:
                presentFragment(new FiltersSettingsActivity());
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
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(0, dp(28), 0, dp(28));

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ayuless_settings_logo);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        layout.addView(icon, LayoutHelper.createLinear(72, 72));

        TextView name = new TextView(context);
        name.setText(getString(R.string.AyulessLauncherName));
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        name.setTypeface(AndroidUtilities.bold());
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        ThemeBind.textColor(name, Theme.key_windowBackgroundWhiteBlackText, getResourceProvider());
        layout.addView(name, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 50, 16, 50, 0));

        TextView version = new TextView(context);
        version.setText(BuildVars.BUILD_VERSION_STRING);
        try {
            long code = PackageInfoCompat.getLongVersionCode(
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0));
            version.setText(formatString(R.string.AyulessSettingsVersion, BuildVars.BUILD_VERSION_STRING, code));
        } catch (PackageManager.NameNotFoundException e) {
            FileLog.e(e);
        }
        version.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        version.setTypeface(AndroidUtilities.bold());
        version.setGravity(Gravity.CENTER);
        version.setLineSpacing(dp(2), 1);
        ThemeBind.textColor(version, Theme.key_windowBackgroundWhiteGrayText, getResourceProvider());
        layout.addView(version, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 60, 2, 60, 0));
        return layout;
    }
}
