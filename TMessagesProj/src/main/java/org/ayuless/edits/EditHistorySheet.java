/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.edits;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.ayuless.storage.AyulessStore;
import org.ayuless.storage.StoredEdit;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

/** Simple sheet listing the current text of a message and all stored previous versions. */
public final class EditHistorySheet {

    private EditHistorySheet() {
    }

    public static void show(Context context, int account, MessageObject messageObject) {
        if (context == null || messageObject == null) {
            return;
        }
        AyulessStore.getInstance(account).getEdits(messageObject.getDialogId(), messageObject.getId(), edits -> {
            LinearLayout list = new LinearLayout(context);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(4), AndroidUtilities.dp(21), AndroidUtilities.dp(12));

            TLRPC.Message current = messageObject.messageOwner;
            addVersion(context, list, "Current", current.edit_date != 0 ? current.edit_date : current.date, current.message, current.entities, messageObject.isOutOwner());
            for (int i = edits.size() - 1; i >= 0; i--) {
                StoredEdit e = edits.get(i);
                String label = e.editDate == 0 ? "Original" : "Version " + (e.version + 1);
                int date = e.editDate != 0 ? e.editDate : current.date;
                addVersion(context, list, label, date, e.text, e.entities, messageObject.isOutOwner());
            }

            ScrollView scroll = new ScrollView(context);
            scroll.addView(list);
            BottomSheet.Builder builder = new BottomSheet.Builder(context);
            builder.setTitle("Edit history", true);
            builder.setCustomView(scroll);
            builder.show();
        });
    }

    private static void addVersion(Context context, LinearLayout list, String label, int date, String text,
                                   ArrayList<TLRPC.MessageEntity> entities, boolean out) {
        TextView header = new TextView(context);
        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        header.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        String when = date != 0 ? LocaleController.formatDateAudio(date, true) : "";
        header.setText(TextUtils.isEmpty(when) ? label : label + " · " + when);
        header.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(4));
        list.addView(header);

        TextView body = new TextView(context);
        body.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        body.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        body.setTextIsSelectable(true);
        SpannableStringBuilder sb = new SpannableStringBuilder(text == null ? "" : text);
        if (entities != null && !entities.isEmpty()) {
            try {
                MessageObject.addEntitiesToText(sb, entities, out, false, false, false);
            } catch (Exception ignore) {
                // Fall back to plain text.
            }
        }
        body.setText(sb.length() == 0 ? "(empty)" : sb);
        list.addView(body);
    }
}
