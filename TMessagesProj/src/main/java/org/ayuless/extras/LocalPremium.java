/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.extras;

import org.telegram.messenger.UserConfig;

/** Refreshes client feature availability without altering server subscription flags. */
public final class LocalPremium {

    public static void refresh() {
        org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> {
            for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
                UserConfig config = UserConfig.getInstance(account);
                if (!config.isClientActivated()) continue;
                org.telegram.messenger.MessagesController controller = org.telegram.messenger.MessagesController.getInstance(account);
                controller.updatePremium(config.isPremium());
                org.telegram.messenger.NotificationCenter.getInstance(account).postNotificationName(
                        org.telegram.messenger.NotificationCenter.currentUserPremiumStatusChanged);
                org.telegram.messenger.MediaDataController media = org.telegram.messenger.MediaDataController.getInstance(account);
                media.loadPremiumPromo(false);
                media.loadReactions(false, null);
                controller.getStoriesController().invalidateStoryLimit();
            }
            org.telegram.messenger.NotificationCenter.getGlobalInstance().postNotificationName(
                    org.telegram.messenger.NotificationCenter.premiumStatusChangedGlobal);
        });
    }
    private LocalPremium() {
    }

    /** Translation uses the same self-account gate as the other client features. */
    public static boolean isPremium(int account) {
        return UserConfig.getInstance(account).isPremium();
    }
}
