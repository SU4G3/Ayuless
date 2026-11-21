/* SPDX-License-Identifier: GPL-3.0-or-later */
import org.ayuless.security.TelegramSubdomainPattern;

import java.util.regex.Matcher;

public final class TelegramSubdomainPatternCheck {
    private static void accepted(String input, String username) {
        Matcher matcher = TelegramSubdomainPattern.VALUE.matcher(input);
        if (!matcher.find() || !username.equals(matcher.group(1))) {
            throw new AssertionError("Valid subdomain rejected: " + input);
        }
    }

    public static void main(String[] args) {
        accepted("alice.t.me", "alice");
        accepted("user_name.t.me", "user_name");
        accepted("User-123.t.me", "User-123");
        accepted("https://alice.t.me", "alice");
        accepted("http://alice.t.me", "alice");
        for (String input : new String[]{"alice.t.me.attacker.example", "alice.t.me@attacker.example",
                "alice.t.me/path", "alice.t.me\n", "alice.t.me?query", "a[b.t.me", "a\\b.t.me",
                "a]b.t.me", "a^b.t.me", "a`b.t.me", "alice.tXme", "alice.other.me", ".t.me"}) {
            if (TelegramSubdomainPattern.VALUE.matcher(input).find()) {
                throw new AssertionError("Invalid subdomain accepted: " + input);
            }
        }
        System.out.println("Telegram subdomain checks passed: usernames, ASCII punctuation, host boundaries.");
    }
}
