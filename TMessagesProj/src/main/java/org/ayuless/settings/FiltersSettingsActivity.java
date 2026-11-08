/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import org.ayuless.filters.MessageFilter;
import org.ayuless.filters.MessageFilters;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.UsersSelectActivity;

import java.util.ArrayList;

/** Ayuless > Filters: keyword / regex filters, shadow ban, hiding blocked users. */
public class FiltersSettingsActivity extends AyulessSettingsFragment {

    private static final int ID_ENABLED = 1;
    private static final int ID_PRIVATE = 2;
    private static final int ID_HIDE_BLOCKED = 3;
    private static final int ID_ADD_FILTER = 4;
    private static final int ID_ADD_USERS = 5;
    private static final int ID_FILTER_BASE = 1000;   // + filter index
    private static final int ID_BANNED_BASE = 100000; // + shadow ban index

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.AyulessFiltersTitle);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        final boolean enabled = MessageFilters.isEnabled();
        items.add(UItem.asCheck(ID_ENABLED, getString(R.string.AyulessFiltersEnable)).setChecked(enabled));
        items.add(UItem.asShadow(getString(R.string.AyulessFiltersInfo)));
        if (!enabled) {
            return;
        }

        items.add(UItem.asHeader(getString(R.string.AyulessFiltersMain)));
        items.add(UItem.asCheck(ID_PRIVATE, getString(R.string.AyulessFiltersPrivate)).setChecked(MessageFilters.privateChats()));
        items.add(UItem.asCheck(ID_HIDE_BLOCKED, getString(R.string.AyulessFiltersHideBlocked)).setChecked(MessageFilters.hideBlocked()));
        items.add(UItem.asShadow(null));

        ArrayList<MessageFilter> filters = MessageFilters.getFilters();
        items.add(UItem.asHeader(getString(R.string.AyulessFiltersShared)));
        for (int i = 0; i < filters.size(); i++) {
            MessageFilter f = filters.get(i);
            items.add(UItem.asButton(ID_FILTER_BASE + i, f.expression, filterFlags(f)));
        }
        items.add(UItem.asButton(ID_ADD_FILTER, R.drawable.msg_add, getString(R.string.AyulessFiltersAdd)).accent());
        items.add(UItem.asShadow(getString(R.string.AyulessFiltersSharedInfo)));

        ArrayList<Long> banned = MessageFilters.getShadowBan();
        items.add(UItem.asHeader(getString(R.string.AyulessShadowBan)));
        for (int i = 0; i < banned.size(); i++) {
            items.add(UItem.asButton(ID_BANNED_BASE + i, peerName(banned.get(i))));
        }
        items.add(UItem.asButton(ID_ADD_USERS, R.drawable.msg_add, getString(R.string.AyulessShadowBanAdd)).accent());
        items.add(UItem.asShadow(getString(R.string.AyulessShadowBanInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ENABLED) {
            MessageFilters.setEnabled(!MessageFilters.isEnabled());
        } else if (item.id == ID_PRIVATE) {
            MessageFilters.setPrivateChats(!MessageFilters.privateChats());
        } else if (item.id == ID_HIDE_BLOCKED) {
            MessageFilters.setHideBlocked(!MessageFilters.hideBlocked());
        } else if (item.id == ID_ADD_FILTER) {
            showFilterEditor(-1);
            return;
        } else if (item.id == ID_ADD_USERS) {
            selectUsers();
            return;
        } else if (item.id >= ID_BANNED_BASE) {
            confirmUnban(item.id - ID_BANNED_BASE);
            return;
        } else if (item.id >= ID_FILTER_BASE) {
            showFilterEditor(item.id - ID_FILTER_BASE);
            return;
        } else {
            return;
        }
        update();
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

    private static String filterFlags(MessageFilter f) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(f.enabled ? R.string.AyulessFilterOn : R.string.AyulessOff));
        if (f.caseInsensitive) {
            sb.append(" · Aa");
        }
        if (f.inverse) {
            sb.append(" · ").append(getString(R.string.AyulessFilterInverseShort));
        }
        if (!f.isValid()) {
            sb.append(" · ").append(getString(R.string.AyulessFilterInvalidShort));
        }
        return sb.toString();
    }

    private String peerName(long id) {
        if (DialogObject.isUserDialog(id)) {
            TLRPC.User user = getMessagesController().getUser(id);
            if (user != null) {
                return UserObject.getUserName(user);
            }
        } else {
            TLRPC.Chat chat = getMessagesController().getChat(-id);
            if (chat != null) {
                return chat.title;
            }
        }
        return "ID " + id;
    }

    // ---- filter editor ----

    private void showFilterEditor(int index) {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        final ArrayList<MessageFilter> filters = MessageFilters.getFilters();
        final MessageFilter current = index >= 0 && index < filters.size() ? filters.get(index) : null;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, getResourceProvider()));
        editText.setHintText(getString(R.string.AyulessFilterExpression));
        editText.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
        editText.setLineColors(Theme.getColor(Theme.key_dialogInputField, getResourceProvider()),
                Theme.getColor(Theme.key_dialogInputFieldActivated, getResourceProvider()),
                Theme.getColor(Theme.key_text_RedBold, getResourceProvider()));
        editText.setBackground(null);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setSingleLine(true);
        editText.setPadding(0, dp(8), 0, dp(8));
        if (current != null) {
            editText.setText(current.expression);
            editText.setSelection(editText.length());
        }
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 8, 24, 4));

        final CheckBoxCell enabledCell = checkCell(context, layout, getString(R.string.AyulessFilterEnabled), current == null || current.enabled);
        final CheckBoxCell caseCell = checkCell(context, layout, getString(R.string.AyulessFilterCaseInsensitive), current == null || current.caseInsensitive);
        final CheckBoxCell inverseCell = checkCell(context, layout, getString(R.string.AyulessFilterInverse), current != null && current.inverse);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(getString(current == null ? R.string.AyulessFiltersAdd : R.string.AyulessFilterEdit));
        builder.setView(layout);
        builder.setPositiveButton(getString(R.string.Save), null);
        builder.setNegativeButton(getString(R.string.Cancel), null);
        if (current != null) {
            builder.setNeutralButton(getString(R.string.Delete), (d, w) -> {
                filters.remove(index);
                MessageFilters.setFilters(filters);
                update();
            });
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 200));
        showDialog(dialog);
        if (current != null) {
            View delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (delete instanceof android.widget.TextView) {
                ((android.widget.TextView) delete).setTextColor(Theme.getColor(Theme.key_text_RedBold, getResourceProvider()));
            }
        }
        View save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (save != null) {
            save.setOnClickListener(v -> {
                String expression = editText.getText() == null ? "" : editText.getText().toString().trim();
                if (expression.isEmpty() || MessageFilter.compile(expression, caseCell.isChecked()) == null) {
                    AndroidUtilities.shakeViewSpring(editText, -6);
                    BulletinFactory.of(this).createErrorBulletin(getString(R.string.AyulessFilterInvalid)).show();
                    return;
                }
                MessageFilter f = new MessageFilter(expression, enabledCell.isChecked(), caseCell.isChecked(), inverseCell.isChecked());
                if (current != null) {
                    filters.set(index, f);
                } else {
                    filters.add(f);
                }
                MessageFilters.setFilters(filters);
                dialog.dismiss();
                update();
            });
        }
    }

    private CheckBoxCell checkCell(Context context, LinearLayout layout, String text, boolean checked) {
        CheckBoxCell cell = new CheckBoxCell(context, CheckBoxCell.TYPE_CHECK_BOX_DEFAULT, getResourceProvider());
        cell.setBackground(Theme.getSelectorDrawable(false));
        cell.setText(text, "", checked, false);
        cell.setPadding(dp(8), 0, dp(8), 0);
        cell.setOnClickListener(v -> cell.setChecked(!cell.isChecked(), true));
        layout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        return cell;
    }

    // ---- shadow ban ----

    private void selectUsers() {
        ArrayList<Long> current = MessageFilters.getShadowBan();
        UsersSelectActivity fragment = new UsersSelectActivity(true, current, 0).asPrivateChats();
        fragment.noChatTypes = true;
        fragment.allowSelf = false;
        fragment.allowBots = true;
        fragment.setDelegate((ids, flags) -> {
            MessageFilters.setShadowBan(ids);
            update();
        });
        presentFragment(fragment);
    }

    private void confirmUnban(int index) {
        final ArrayList<Long> banned = MessageFilters.getShadowBan();
        if (index < 0 || index >= banned.size() || getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(peerName(banned.get(index)));
        builder.setMessage(getString(R.string.AyulessShadowBanRemoveConfirm));
        builder.setPositiveButton(getString(R.string.AyulessShadowBanRemove), (d, w) -> {
            banned.remove(index);
            MessageFilters.setShadowBan(banned);
            update();
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }
}
