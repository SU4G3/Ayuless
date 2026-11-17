/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */

package org.ayuless.extras;

import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

/**
 * Hide sponsored messages: requests for sponsored messages (channels, bots, video ads)
 * and sponsored search results are not sent. The callers already treat an error reply
 * as "nothing to show".
 */
public final class SponsoredBlocker {

    private SponsoredBlocker() {
    }

    public static boolean shouldBlock(TLObject req) {
        return (req instanceof TLRPC.TL_messages_getSponsoredMessages
                || req instanceof TLRPC.TL_contacts_getSponsoredPeers)
                && ExtrasConfig.hideSponsored();
    }
}
