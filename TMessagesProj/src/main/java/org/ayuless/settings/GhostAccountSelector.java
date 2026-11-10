/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

/*
 * Account selection layout adapted from Telegram's AlertsCreator in AyuGram4A.
 * Copyright Nikolai Kudashov, 2013-2018. GPL v. 2 or later.
 */

package org.ayuless.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ayuless.ghost.GhostConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Cells.AccountSelectCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

/** Avatar/name account dialog from the reference client, selecting a ghost profile only. */
final class GhostAccountSelector {
    interface Delegate {
        void select(int profile);
    }

    private GhostAccountSelector() {}

    static void show(BaseFragment fragment, int selectedProfile, Delegate delegate) {
        Context context = fragment.getParentActivity();
        if (context == null) return;
        Theme.ResourcesProvider resources = fragment.getResourceProvider();
        LinearLayout accounts = new LinearLayout(context);
        accounts.setOrientation(LinearLayout.VERTICAL);
        AlertDialog dialog = new AlertDialog.Builder(context, resources)
                .setTitle(getString(R.string.AyulessGhostProfile))
                .setView(accounts)
                .setNegativeButton(getString(R.string.Cancel), null).create();
        for (int profile = GhostConfig.PROFILE_ALL; profile < UserConfig.MAX_ACCOUNT_COUNT; profile++) {
            if (profile != GhostConfig.PROFILE_ALL && !UserConfig.getInstance(profile).isClientActivated()) continue;
            final int chosenProfile = profile;
            FrameLayout row = new FrameLayout(context);
            row.setPadding(dp(14), 0, dp(14), 0);
            row.setBackground(Theme.getSelectorDrawable(false));
            if (profile == GhostConfig.PROFILE_ALL) {
                BackupImageView image = new BackupImageView(context);
                image.setRoundRadius(dp(18));
                AvatarDrawable avatar = new AvatarDrawable();
                avatar.setAvatarType(AvatarDrawable.AVATAR_TYPE_FILTER_GROUPS);
                image.setImageDrawable(avatar);
                row.addView(image, LayoutHelper.createFrame(36, 36, Gravity.LEFT | Gravity.TOP, 10, 10, 0, 0));
                TextView label = new TextView(context);
                label.setText(getString(R.string.AyulessGhostProfileAll));
                label.setTextSize(15);
                label.setTypeface(AndroidUtilities.bold());
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setGravity(Gravity.CENTER_VERTICAL);
                ThemeBind.textColor(label, Theme.key_chats_menuItemText, resources);
                row.addView(label, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,
                        Gravity.LEFT | Gravity.TOP, 61, 0, 52, 0));
            } else {
                AccountSelectCell cell = new AccountSelectCell(context, false);
                // Selection belongs to the edited profile, independent of Telegram's active account.
                cell.setAccount(profile, false);
                row.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56));
            }
            ImageView check = new ImageView(context);
            check.setImageResource(R.drawable.account_check);
            check.setScaleType(ImageView.ScaleType.CENTER);
            check.setVisibility(profile == selectedProfile ? View.VISIBLE : View.INVISIBLE);
            ThemeBind.colorFilter(check, Theme.key_chats_menuItemCheck, resources, PorterDuff.Mode.MULTIPLY);
            row.addView(check, LayoutHelper.createFrame(40, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.TOP, 0, 0, 6, 0));
            row.setOnClickListener(v -> {
                dialog.dismiss();
                delegate.select(chosenProfile);
            });
            row.setContentDescription(profile == GhostConfig.PROFILE_ALL ? getString(R.string.AyulessGhostProfileAll)
                    : org.telegram.messenger.UserObject.getUserName(UserConfig.getInstance(profile).getCurrentUser()));
            row.setSelected(profile == selectedProfile);
            accounts.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56));
        }
        fragment.showDialog(dialog);
    }
}
