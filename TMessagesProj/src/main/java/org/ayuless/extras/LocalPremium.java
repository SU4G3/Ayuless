/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.extras;

import org.telegram.messenger.UserConfig;

/** Local Premium gates and refresh notifications, matching AyuGram without changing the server user object. */
public final class LocalPremium {

    public static void refresh() {
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (!UserConfig.getInstance(account).isClientActivated()) continue;
            org.telegram.messenger.AccountInstance instance = org.telegram.messenger.AccountInstance.getInstance(account);
            instance.getMessagesController().updatePremium(UserConfig.getInstance(account).isPremium());
            org.telegram.messenger.NotificationCenter.getInstance(account).postNotificationName(org.telegram.messenger.NotificationCenter.currentUserPremiumStatusChanged);
            instance.getMediaDataController().loadPremiumPromo(false);
            instance.getMediaDataController().loadReactions(false, null);
            instance.getMessagesController().getStoriesController().invalidateStoryLimit();
        }
        org.telegram.messenger.NotificationCenter.getGlobalInstance().postNotificationName(org.telegram.messenger.NotificationCenter.premiumStatusChangedGlobal);
    }

    private LocalPremium() {
    }

    /** Server-side permissions and purchases remain enforced by Telegram. */
    public static boolean isPremium(int account) {
        return UserConfig.getInstance(account).isPremium() || ExtrasConfig.localPremium();
    }
}
