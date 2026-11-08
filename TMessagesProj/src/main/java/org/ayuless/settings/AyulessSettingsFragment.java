/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

/**
 * Base for all Ayuless settings screens: rounded, inset card sections with the simple
 * glass action bar, the same style upstream uses for its own settings screens
 * (e.g. HeaderSettingsActivity, DataSettingsActivity).
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
        // Supply the same positive predicate for attached rows and off-screen types.
        // UniversalRecyclerView's convenience overload passes isShadow as the type
        // predicate, which incorrectly joins sections at the viewport boundaries.
        listView.setSections(
                view -> view.getParent() == listView && !UniversalAdapter.isShadow(
                        listView.getChildViewHolder(view).getItemViewType()),
                viewType -> !UniversalAdapter.isShadow(viewType),
                AndroidUtilities.dp(12), AndroidUtilities.dp(16), true);
        listView.setSectionsDrawBackground(true);
        return fragmentView;
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }
}
