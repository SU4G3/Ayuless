/* SPDX-License-Identifier: GPL-3.0-or-later */
import org.ayuless.security.UriParsePattern;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UriParsePatternCheck {
    private static final Pattern PREVIOUS = Pattern.compile(
            "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    private static void compare(String input) {
        Matcher previous = PREVIOUS.matcher(input);
        Matcher current = UriParsePattern.VALUE.matcher(input);
        boolean matched = previous.matches();
        if (matched != current.matches()) {
            throw new AssertionError("Match changed: " + input);
        }
        if (matched) {
            for (int i = 0; i <= previous.groupCount(); i++) {
                if (!java.util.Objects.equals(previous.group(i), current.group(i))) {
                    throw new AssertionError("Capture " + i + " changed: " + input);
                }
            }
        }
    }

    public static void main(String[] args) {
        for (String input : new String[]{"", "https://example.org/a?b=c#d", "//user@host:443/a",
                "tg://resolve?domain=test", "mailto:a@example.org", "a:b:c", "?q#f", "#fragment",
                "https://[::1]:80/", "https://пример.рф/a", "https://host/p?q=x#f", "https://host/#bad\nfragment"}) {
            compare(input);
        }
        Random random = new Random(0x555249);
        String alphabet = "abc:/?#\n\r\u2028\"@[]012";
        for (int i = 0; i < 20000; i++) {
            StringBuilder input = new StringBuilder();
            int length = random.nextInt(60);
            for (int j = 0; j < length; j++) {
                input.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            compare(input.toString());
        }
        StringBuilder attack = new StringBuilder("https://");
        for (int i = 0; i < 200000; i++) {
            attack.append('"');
        }
        attack.append("#\n");
        if (UriParsePattern.VALUE.matcher(attack).matches()) {
            throw new AssertionError("Malformed fragment accepted");
        }
        System.out.println("URI checks passed: equivalent captures on 20,000 inputs and long malformed input.");
    }
}
