/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import android.content.Context;
import android.view.View;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

/** A noninteractive section label on the screen background, outside the card. */
final class AyulessSectionHeader {
    public static class Factory extends UItem.UItemFactory<HeaderCell> {
        static { setup(new Factory()); }

        @Override
        public HeaderCell createView(Context context, RecyclerListView listView, int account,
                                     int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new HeaderCell(context, Theme.key_windowBackgroundWhiteBlueHeader,
                    24, 6, 3, false, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter,
                             UniversalRecyclerView listView) {
            view.setBackground(null);
            ((HeaderCell) view).setText(item.text);
        }

        @Override
        public boolean isShadow() {
            return true;
        }

        @Override
        public boolean isClickable() {
            return false;
        }

        static UItem of(CharSequence text) {
            UItem item = UItem.ofFactory(Factory.class);
            item.text = text;
            return item;
        }
    }
}
