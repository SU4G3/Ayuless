/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.profile;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;

/** Formatting shared by profile rows and the clipboard. Everything is local. */
public final class ProfileIdentity {
    private ProfileIdentity() {}

    public static String date(long userId, TLRPC.Chat chat) {
        // Chat creation comes from Telegram; applying user-ID growth to chats is invalid.
        YearMonth month = userId != 0 ? RegistrationEstimate.estimate(userId)
                : chat != null && chat.date > 0
                ? YearMonth.from(Instant.ofEpochSecond(chat.date).atZone(ZoneOffset.UTC)) : null;
        String value = RegistrationEstimate.format(month, LocaleController.getInstance().getCurrentLocale());
        return value == null ? LocaleController.getString(R.string.AyulessRegistrationUnknown) : value;
    }
}
