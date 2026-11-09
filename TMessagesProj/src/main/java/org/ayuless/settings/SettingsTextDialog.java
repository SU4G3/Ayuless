/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package org.ayuless.settings;

import android.text.InputFilter;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import java.util.function.Consumer;
import static org.telegram.messenger.LocaleController.getString;

/** Small Telegram-native replacement for AyuUi.spawnEditBox, including Reset. */
public final class SettingsTextDialog {
    private SettingsTextDialog() {}
    public static void show(BaseFragment fragment, String title, String value, int maxLength,
                            Consumer<String> save, Runnable reset) {
        if (fragment.getParentActivity() == null) return;
        EditTextBoldCursor input = new EditTextBoldCursor(fragment.getParentActivity());
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, fragment.getResourceProvider()));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        input.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        input.setText(value);
        input.setSelection(input.length());
        AlertDialog dialog = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(title).setView(input)
                .setPositiveButton(getString(R.string.Save), (d, which) -> save.accept(input.getText().toString()))
                .setNegativeButton(getString(R.string.Cancel), null)
                .setNeutralButton(getString(R.string.Reset), (d, which) -> reset.run()).create();
        fragment.showDialog(dialog);
        input.requestFocus();
        AndroidUtilities.showKeyboard(input);
    }
}
