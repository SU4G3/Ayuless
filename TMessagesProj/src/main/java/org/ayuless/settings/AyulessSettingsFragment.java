/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.settings;

import android.content.Context;
import android.view.View;

import org.telegram.ui.Components.RecyclerListView;
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
        listView.setSections();
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
