/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Ayuless settings geometry, independently implemented from docs/SETTINGS-STYLE.md.
 * Headers and explanatory text live outside the rounded cards.
 */
public abstract class AyulessSettingsFragment extends UniversalFragment {

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        fragmentView = super.createView(context);
        listView.adapter.setApplyBackground(false);
        // Supply the same positive predicate for attached rows and off-screen types.
        // UniversalRecyclerView's convenience overload passes isShadow as the type
        // predicate, which incorrectly joins sections at the viewport boundaries.
        listView.setSections(
                view -> view.getParent() == listView && !UniversalAdapter.isShadow(
                        listView.getChildViewHolder(view).getItemViewType()),
                viewType -> !UniversalAdapter.isShadow(viewType),
                AndroidUtilities.dp(12), AndroidUtilities.dp(20), false);
        listView.setSectionsDrawBackground(true);
        listView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                AyulessSettingsSwitch.applyTo(view, getResourceProvider());
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
            }
        });
        return fragmentView;
    }

    @Override
    protected final void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        fillSettingsItems(items, adapter);
        for (int i = 0; i < items.size(); i++) {
            UItem item = items.get(i);
            if (item.viewType == UniversalAdapter.VIEW_TYPE_HEADER) {
                items.set(i, AyulessSectionHeader.Factory.of(item.text));
            } else if (item.viewType == UniversalAdapter.VIEW_TYPE_TEXT) {
                items.set(i, AyulessPreferenceCell.Factory.of(item));
            }
        }
    }

    protected abstract void fillSettingsItems(ArrayList<UItem> items, UniversalAdapter adapter);

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }
}
