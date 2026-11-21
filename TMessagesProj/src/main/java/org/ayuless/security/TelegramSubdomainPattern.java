/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.security;

import java.util.regex.Pattern;

/** A complete t.me subdomain, retaining the username as capture group 1. */
public final class TelegramSubdomainPattern {
    public static final Pattern VALUE = Pattern.compile(
            "\\A(?:https?://)?([A-Za-z0-9_-]+)\\.t\\.me\\z");

    private TelegramSubdomainPattern() {
    }
}
