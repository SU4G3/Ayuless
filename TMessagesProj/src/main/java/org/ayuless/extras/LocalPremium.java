/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.extras;

import org.telegram.messenger.UserConfig;

/**
 * Local Premium: unlocks a small set of Premium-gated screens that work with requests a
 * regular account may already send. Nothing is sent to the server that claims Premium,
 * and server-side limits (uploads, folders, pins, reactions, stories) are not touched.
 *
 * Covered: whole-chat translation (translate bar and auto-translate in any chat), which
 * uses the same messages.translateText request as single-message translation.
 */
public final class LocalPremium {

    // CLEANROOM: reimplement CR-08 active-account gate refresh
    private LocalPremium() {
    }

    /** Premium check for gates listed above only. Never use it for limits or payments. */
    public static boolean isPremium(int account) {
        return UserConfig.getInstance(account).isPremium() || ExtrasConfig.localPremium();
    }
}
