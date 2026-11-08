/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.filters;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** One keyword / regular expression filter. Immutable. */
public final class MessageFilter {

    public final String expression;
    public final boolean enabled;
    public final boolean caseInsensitive;
    /** Inverse: hide messages that do NOT match. */
    public final boolean inverse;

    private final Pattern pattern;

    public MessageFilter(String expression, boolean enabled, boolean caseInsensitive, boolean inverse) {
        this.expression = expression == null ? "" : expression;
        this.enabled = enabled;
        this.caseInsensitive = caseInsensitive;
        this.inverse = inverse;
        this.pattern = compile(this.expression, caseInsensitive);
    }

    /** @return the compiled pattern, or null if the expression is not a valid regular expression. */
    public static Pattern compile(String expression, boolean caseInsensitive) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        try {
            return Pattern.compile(expression, caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    public boolean isValid() {
        return pattern != null;
    }

    /** Whether a message with this text should be hidden by this filter. */
    public boolean hides(CharSequence text) {
        if (!enabled || pattern == null) {
            return false;
        }
        boolean match = text != null && pattern.matcher(text).find();
        return inverse != match;
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("e", expression);
        o.put("on", enabled);
        o.put("ci", caseInsensitive);
        o.put("inv", inverse);
        return o;
    }

    static MessageFilter fromJson(JSONObject o) {
        return new MessageFilter(o.optString("e", ""), o.optBoolean("on", true), o.optBoolean("ci", true), o.optBoolean("inv", false));
    }
}
