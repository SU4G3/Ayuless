/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.ghost;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;

import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;

import java.util.ArrayList;

/**
 * "Offer ghost mode for stories": before the story viewer opens, asks whether to view the stories
 * without marking them as seen. Choosing ghost mode hides story views only for that viewing; the
 * stored ghost settings are not changed.
 */
public final class StoryGhostPrompt {

    /** Ghost mode chosen in the prompt for the story viewer that is open now (per account). */
    private static final boolean[] session = new boolean[UserConfig.MAX_ACCOUNT_COUNT];

    private StoryGhostPrompt() {
    }

    static boolean sessionActive(int account) {
        return account >= 0 && account < session.length && session[account];
    }

    /**
     * Called at the start of every story viewer open. Returns true when a prompt was shown: the
     * caller must stop, and {@code open} runs later if the user picks an option.
     */
    public static boolean intercept(int account, Context context, ArrayList<Long> peerIds, Runnable open) {
        if (account < 0 || account >= session.length) {
            return false;
        }
        session[account] = false;
        if (context == null || open == null || !GhostConfig.storyPrompt(account) || GhostConfig.hideStoryViewsStored(account)) {
            return false;
        }
        if (!hasOtherPeer(account, peerIds)) {
            return false;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.AyulessStoryPromptTitle));
        builder.setMessage(getString(R.string.AyulessStoryPromptText));
        builder.setPositiveButton(getString(R.string.AyulessStoryPromptGhost), (d, w) -> {
            session[account] = true;
            open.run();
        });
        builder.setNegativeButton(getString(R.string.AyulessStoryPromptNormal), (d, w) -> {
            session[account] = false;
            open.run();
        });
        try {
            builder.show();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /** Own stories are never marked as viewed, so there is nothing to ask about. */
    private static boolean hasOtherPeer(int account, ArrayList<Long> peerIds) {
        if (peerIds == null || peerIds.isEmpty()) {
            return false;
        }
        long self = UserConfig.getInstance(account).getClientUserId();
        for (Long id : peerIds) {
            if (id != null && id != self) {
                return true;
            }
        }
        return false;
    }
}
