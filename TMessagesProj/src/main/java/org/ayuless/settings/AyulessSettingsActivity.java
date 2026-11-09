/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

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
import org.ayuless.antidelete.AntiDeleteConfig;
import org.ayuless.edits.EditsConfig;
import org.ayuless.extras.ExtrasConfig;
import org.ayuless.extras.KeepAliveService;
import org.ayuless.appearance.AppearanceConfig;
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
public class AyulessSettingsActivity extends AyulessSettingsFragment implements org.telegram.messenger.NotificationCenter.NotificationCenterDelegate {

    @Override public boolean onFragmentCreate() {
        org.telegram.messenger.NotificationCenter.getGlobalInstance().addObserver(this, org.ayuless.sync.SyncController.STATUS_CHANGED);
        return super.onFragmentCreate();
    }
    @Override public void onFragmentDestroy() {
        org.telegram.messenger.NotificationCenter.getGlobalInstance().removeObserver(this, org.ayuless.sync.SyncController.STATUS_CHANGED);
        super.onFragmentDestroy();
    }
    @Override public void didReceivedNotification(int id, int account, Object... args) { refresh(); }

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

    private static final int ID_MASTER = 100;
    private static final int ID_GHOST_BASE = 110;
    private static final int ID_READ_REPLY = 120;
    private static final int ID_SCHEDULE = 121;
    private static final int ID_DELETE = 130;
    private static final int ID_EDITS = 131;
    private static final int ID_SAVING = 132;
    private static final int ID_SERVICE = 140;
    private static final int ID_ADS = 141;
    private static final int ID_PREMIUM = 142;
    private static final int ID_MARKS = 150;
    private static final int ID_DELETED_MARK = 151;
    private static final int ID_EDITED_MARK = 152;
    private static final int ID_GHOST_MENU = 153;
    private static final int ID_KILL_MENU = 154;
    private static final int ID_CHATS = 160;
    private static final int ID_SYNC = 161;
    private static final int ID_MAINTENANCE = 170;
    private static final int ID_CACHE = 171;
    private boolean ghostExpanded;
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

        int profile = GhostConfig.activeProfile(currentAccount);
        items.add(UItem.asHeader(getString(R.string.AyulessGhostEssentials)));
        items.add(UItem.asExpandableSwitch(ID_MASTER, getString(R.string.AyulessGhostHeader),
                        GhostConfig.countEnabled(profile) + "/" + GhostConfig.GHOST_KEYS.length)
                .setChecked(GhostConfig.isEnabled(currentAccount)).setCollapsed(!ghostExpanded)
                .setClickCallback(v -> {
                    GhostConfig.setProfile(profile, GhostConfig.KEY_ENABLED, !GhostConfig.isEnabled(currentAccount));
                    refresh();
                }));
        if (ghostExpanded) {
            int[] titles = { R.string.AyulessGhostHideRead, R.string.AyulessGhostHideStoryViews,
                    R.string.AyulessGhostHideOnline, R.string.AyulessGhostHideTyping, R.string.AyulessGhostOfflineAfterSend };
            for (int i = 0; i < titles.length; i++) {
                items.add(UItem.asCheck(ID_GHOST_BASE + i, getString(titles[i]))
                        .setChecked(GhostConfig.getProfile(profile, GhostConfig.GHOST_KEYS[i])));
            }
        }
        items.add(UItem.asCheck(ID_READ_REPLY, getString(R.string.AyulessGhostReadOnAction))
                .setChecked(GhostConfig.getProfile(profile, GhostConfig.KEY_READ_ON_ACTION)));
        items.add(UItem.asCheck(ID_SCHEDULE, getString(R.string.AyulessGhostDelayedSend))
                .setChecked(GhostConfig.delayedSend(currentAccount)));
        items.add(UItem.asButton(ID_GHOST, R.drawable.ayuless_solar_ghost, getString(R.string.AyulessGhostProfiles)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.AyulessSpyEssentials)));
        items.add(UItem.asCheck(ID_DELETE, getString(R.string.AyulessAntiDelete)).setChecked(AntiDeleteConfig.isEnabled(currentAccount)));
        items.add(UItem.asCheck(ID_EDITS, getString(R.string.AyulessEditHistory)).setChecked(EditsConfig.isEnabled(currentAccount)));
        items.add(UItem.asButton(ID_SAVING, getString(R.string.AyulessMessageSaving)));
        items.add(UItem.asButton(ID_SPY, getString(R.string.AyulessSpyStorage)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.AyulessUsefulHeader)));
        items.add(UItem.asCheck(ID_SERVICE, getString(R.string.AyulessWorkInBackground)).setChecked(ExtrasConfig.workInBackground()));
        items.add(UItem.asCheck(ID_ADS, getString(R.string.AyulessHideSponsored)).setChecked(ExtrasConfig.hideSponsored()));
        items.add(UItem.asCheck(ID_PREMIUM, getString(R.string.AyulessLocalPremium)).setChecked(ExtrasConfig.localPremium()));
        items.add(UItem.asButton(ID_FILTERS, R.drawable.ayuless_solar_msg_filter, getString(R.string.AyulessFilters)));
        items.add(UItem.asButton(ID_CHATS, getString(R.string.AyulessChatsMessages)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.AyulessCustomization)));
        items.add(UItem.asCheck(ID_MARKS, getString(R.string.AyulessCustomMarks)).setChecked(AppearanceConfig.customMarks()));
        items.add(UItem.asButton(ID_DELETED_MARK, getString(R.string.AyulessDeletedText), AppearanceConfig.deletedText()).setEnabled(AppearanceConfig.customMarks()));
        items.add(UItem.asButton(ID_EDITED_MARK, getString(R.string.AyulessEditedText), AppearanceConfig.editedText()).setEnabled(AppearanceConfig.customMarks()));
        items.add(UItem.asCheck(ID_GHOST_MENU, getString(R.string.AyulessGhostMenu)).setChecked(ExtrasConfig.get(ExtrasConfig.KEY_GHOST_MENU)));
        items.add(UItem.asCheck(ID_KILL_MENU, getString(R.string.AyulessKillMenu)).setChecked(ExtrasConfig.get(ExtrasConfig.KEY_KILL_MENU)));
        items.add(UItem.asButton(ID_CUSTOMIZATION, R.drawable.ayuless_solar_menu_edit_appearance, getString(R.string.AyulessMessageAppearance)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.AyulessSyncHeader)));
        items.add(UItem.asButton(ID_SYNC, getString(R.string.AyulessSyncStatus), org.ayuless.sync.SyncController.statusText()));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.AyulessMaintenance)));
        items.add(UItem.asCheck(ID_MAINTENANCE, getString(R.string.AyulessMaintenanceTools)).setChecked(ExtrasConfig.get(ExtrasConfig.KEY_MAINTENANCE)));
        if (ExtrasConfig.get(ExtrasConfig.KEY_MAINTENANCE)) {
            items.add(UItem.asButton(0, getString(R.string.AyulessWalEnabled)).setEnabled(false));
            items.add(UItem.asButton(ID_SPY, getString(R.string.AyulessStorageClear)));
            items.add(UItem.asButton(ID_CACHE, getString(R.string.AyulessManageCache)));
        }
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.AyulessLinksHeader)));
        items.add(UItem.asButton(ID_SOURCE, R.drawable.ayuless_solar_msg_link2, getString(R.string.AyulessSourceCode), "GitHub"));
        items.add(UItem.asButton(ID_ISSUES, R.drawable.ayuless_solar_msg_ask_question, getString(R.string.AyulessIssues)));
        items.add(UItem.asButton(ID_DOCS, R.drawable.ayuless_solar_msg_info, getString(R.string.AyulessDocs)));
        items.add(UItem.asShadow(getString(R.string.AyulessDisclaimer)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (!item.enabled) return;
        int profile = GhostConfig.activeProfile(currentAccount);
        if (item.id >= ID_GHOST_BASE && item.id < ID_GHOST_BASE + GhostConfig.GHOST_KEYS.length) {
            String key = GhostConfig.GHOST_KEYS[item.id - ID_GHOST_BASE];
            GhostConfig.setProfile(profile, key, !GhostConfig.getProfile(profile, key));
            refresh();
            return;
        }
        switch (item.id) {
            case ID_MASTER: ghostExpanded = !ghostExpanded; refresh(); break;
            case ID_READ_REPLY:
                GhostConfig.setProfile(profile, GhostConfig.KEY_READ_ON_ACTION, !GhostConfig.getProfile(profile, GhostConfig.KEY_READ_ON_ACTION)); refresh(); break;
            case ID_SCHEDULE:
                GhostConfig.setProfile(profile, GhostConfig.KEY_DELAYED_SEND, !GhostConfig.delayedSend(currentAccount)); refresh(); break;
            case ID_DELETE: AntiDeleteConfig.setEnabled(currentAccount, !AntiDeleteConfig.isEnabled(currentAccount)); refresh(); break;
            case ID_EDITS: EditsConfig.setEnabled(currentAccount, !EditsConfig.isEnabled(currentAccount)); refresh(); break;
            case ID_SAVING: presentFragment(new MessageSavingSettingsActivity()); break;
            case ID_SERVICE: ExtrasConfig.set(ExtrasConfig.KEY_WORK_IN_BACKGROUND, !ExtrasConfig.workInBackground()); KeepAliveService.apply(); refresh(); break;
            case ID_ADS: ExtrasConfig.set(ExtrasConfig.KEY_HIDE_SPONSORED, !ExtrasConfig.hideSponsored()); refresh(); break;
            case ID_PREMIUM: ExtrasConfig.set(ExtrasConfig.KEY_LOCAL_PREMIUM, !ExtrasConfig.localPremium()); org.ayuless.extras.LocalPremium.refresh(); refresh(); break;
            case ID_MARKS: AppearanceConfig.setCustomMarks(!AppearanceConfig.customMarks()); refresh(); break;
            case ID_DELETED_MARK: editMark(false); break;
            case ID_EDITED_MARK: editMark(true); break;
            case ID_GHOST_MENU: ExtrasConfig.set(ExtrasConfig.KEY_GHOST_MENU, !ExtrasConfig.get(ExtrasConfig.KEY_GHOST_MENU)); refresh(); break;
            case ID_KILL_MENU: ExtrasConfig.set(ExtrasConfig.KEY_KILL_MENU, !ExtrasConfig.get(ExtrasConfig.KEY_KILL_MENU)); refresh(); break;
            case ID_CHATS: presentFragment(new ChatFeaturesSettingsActivity()); break;
            case ID_SYNC: presentFragment(new SyncSettingsActivity()); break;
            case ID_MAINTENANCE: ExtrasConfig.set(ExtrasConfig.KEY_MAINTENANCE, !ExtrasConfig.get(ExtrasConfig.KEY_MAINTENANCE)); refresh(); break;
            case ID_CACHE: presentFragment(new org.telegram.ui.CacheControlActivity()); break;
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

    private void refresh() {
        if (listView != null && listView.adapter != null) listView.adapter.update(true);
    }

    private void editMark(boolean edited) {
        String key = edited ? AppearanceConfig.KEY_EDITED_TEXT : AppearanceConfig.KEY_DELETED_TEXT;
        SettingsTextDialog.show(this, getString(edited ? R.string.AyulessEditedText : R.string.AyulessDeletedText),
                edited ? AppearanceConfig.editedText() : AppearanceConfig.deletedText(), 32,
                value -> { AppearanceConfig.setTextMark(key, value); refresh(); },
                () -> { AppearanceConfig.resetTextMark(key); refresh(); });
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
