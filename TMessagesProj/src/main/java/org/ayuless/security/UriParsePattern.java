/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.security;

import java.util.regex.Pattern;

/** RFC 3986 appendix B capture groups, with no backtracking within components. */
public final class UriParsePattern {
    // Every component stops at its delimiter. Backtracking inside one cannot make
    // a valid match possible, but can make malformed untrusted links expensive.
    public static final Pattern VALUE = Pattern.compile(
            "^(([^:/?#]++):)?(//([^/?#]*+))?([^?#]*+)(\\?([^#]*+))?(#(.*))?");

    private UriParsePattern() {
    }
}
