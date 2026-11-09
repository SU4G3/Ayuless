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
import android.widget.LinearLayout;
import org.ayuless.spy.MessageSavingConfig;
import org.ayuless.spy.SavedAttachments;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Cells.TextCheckBoxCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import java.util.ArrayList;
import static org.telegram.messenger.LocaleController.getString;

/** MessageSavingPreferencesActivity layout, rebuilt with upstream Telegram cells. */
public class MessageSavingSettingsActivity extends AyulessSettingsFragment {
    private static final int CUSTOM = 1, MEDIA = 2, CATEGORIES = 3, TYPES = 4;
    private static final int FORMATTING = 5, REACTIONS = 6, BOTS = 7;
    @Override protected CharSequence getTitle() { return getString(R.string.AyulessMessageSaving); }
    @Override protected void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        boolean custom = MessageSavingConfig.get(currentAccount, MessageSavingConfig.CUSTOM);
        items.add(UItem.asCheck(MEDIA, getString(R.string.AyulessSaveMedia)).setChecked(SavedAttachments.isEnabled(currentAccount)));
        items.add(UItem.asShadow(getString(R.string.AyulessSaveMediaInfo)));
        items.add(UItem.asCheck(CUSTOM, getString(R.string.AyulessCustomSaving)).setChecked(custom));
        items.add(UItem.asShadow(getString(R.string.AyulessCustomSavingInfo)));
        if (!custom) return;
        items.add(UItem.asCheck(CATEGORIES, getString(R.string.AyulessMediaCategories)).setChecked(MessageSavingConfig.get(currentAccount, MessageSavingConfig.MEDIA_CATEGORIES)));
        items.add(UItem.asButton(TYPES, getString(R.string.AyulessChooseMediaCategories))
                .setEnabled(MessageSavingConfig.get(currentAccount, MessageSavingConfig.MEDIA_CATEGORIES)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asCheck(FORMATTING, getString(R.string.AyulessSaveFormatting)).setChecked(MessageSavingConfig.get(currentAccount, MessageSavingConfig.FORMATTING)));
        items.add(UItem.asCheck(REACTIONS, getString(R.string.AyulessSaveReactions)).setChecked(MessageSavingConfig.get(currentAccount, MessageSavingConfig.REACTIONS)));
        items.add(UItem.asCheck(BOTS, getString(R.string.AyulessSaveBots)).setChecked(MessageSavingConfig.get(currentAccount, MessageSavingConfig.BOTS)));
        items.add(UItem.asShadow(null));
    }
    @Override protected void onClick(UItem item, View view, int position, float x, float y) {
        if (!item.enabled) return;
        if (item.id == MEDIA) SavedAttachments.setEnabled(currentAccount, !SavedAttachments.isEnabled(currentAccount));
        else if (item.id == TYPES) { categories(); return; }
        else {
            String key = item.id == CUSTOM ? MessageSavingConfig.CUSTOM : item.id == CATEGORIES ? MessageSavingConfig.MEDIA_CATEGORIES
                    : item.id == FORMATTING ? MessageSavingConfig.FORMATTING : item.id == REACTIONS ? MessageSavingConfig.REACTIONS
                    : item.id == BOTS ? MessageSavingConfig.BOTS : null;
            if (key != null) MessageSavingConfig.set(currentAccount, key, !MessageSavingConfig.get(currentAccount, key));
        }
        listView.adapter.update(true);
    }
    @Override protected boolean onLongClick(UItem item, View view, int position, float x, float y) { return false; }
    private void categories() {
        if (getParentActivity() == null) return;
        int[] labels = {R.string.AyulessMediaPrivateChats, R.string.AyulessMediaPublicChannels,
                R.string.AyulessMediaPrivateChannels, R.string.AyulessMediaPublicGroups, R.string.AyulessMediaPrivateGroups};
        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        TextCheckBoxCell[] cells = new TextCheckBoxCell[labels.length];
        for (int i = 0; i < labels.length; i++) {
            TextCheckBoxCell cell = cells[i] = new TextCheckBoxCell(getParentActivity(), true, false);
            cell.setTextAndCheck(getString(labels[i]), MessageSavingConfig.get(currentAccount, MessageSavingConfig.CATEGORIES[i]), i < labels.length - 1);
            cell.setOnClickListener(v -> cell.setChecked(!cell.isChecked()));
            layout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
        }
        BottomSheet sheet = new BottomSheet.Builder(getParentActivity(), false, getResourceProvider())
                .setTitle(getString(R.string.AyulessSaveMedia)).setCustomView(layout).create();
        android.widget.LinearLayout buttons = new android.widget.LinearLayout(getParentActivity());
        buttons.setGravity(android.view.Gravity.END);
        for (int i = 0; i < 2; i++) {
            final boolean save = i == 1;
            android.widget.TextView button = new android.widget.TextView(getParentActivity());
            button.setText(getString(save ? R.string.Save : R.string.Cancel));
            button.setTextSize(14);
            button.setGravity(android.view.Gravity.CENTER);
            button.setPadding(org.telegram.messenger.AndroidUtilities.dp(20), 0, org.telegram.messenger.AndroidUtilities.dp(20), 0);
            button.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextBlue2, getResourceProvider()));
            button.setOnClickListener(v -> {
                if (save) {
                    for (int j = 0; j < cells.length; j++) MessageSavingConfig.set(currentAccount, MessageSavingConfig.CATEGORIES[j], cells[j].isChecked());
                    listView.adapter.update(true);
                }
                sheet.dismiss();
            });
            buttons.addView(button, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48));
        }
        layout.addView(buttons, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        showDialog(sheet);
    }
}
