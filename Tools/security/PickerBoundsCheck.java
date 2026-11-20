/* SPDX-License-Identifier: GPL-3.0-or-later */
import org.ayuless.security.PickerBounds;

import java.util.Random;

public final class PickerBoundsCheck {
    private static void expect(int value, int min, int max, int expected) {
        int actual = PickerBounds.wrap(value, min, max);
        if (actual != expected) {
            throw new AssertionError(value + " in [" + min + "," + max + "]: " + actual + " != " + expected);
        }
    }

    public static void main(String[] args) {
        expect(-1, 0, 5, 5);
        expect(6, 0, 5, 0);
        expect(10, 0, 5, 4);
        expect(11, 0, 5, 5);
        expect(Integer.MIN_VALUE, 0, 5, 4);
        expect(Integer.MAX_VALUE, 0, 5, 1);
        expect(0, 1, 21, 21);
        expect(22, 1, 21, 1);
        expect(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0);
        expect(Integer.MAX_VALUE, -4, -4, -4);
        Random random = new Random(0x415955);
        for (int i = 0; i < 10000; i++) {
            int a = random.nextInt();
            int b = random.nextInt();
            int min = Math.min(a, b);
            int max = Math.max(a, b);
            int value = random.nextInt();
            int wrapped = PickerBounds.wrap(value, min, max);
            if (wrapped < min || wrapped > max) {
                throw new AssertionError("Wrapped value escaped the inclusive range");
            }
            if (value >= min && value <= max && wrapped != value) {
                throw new AssertionError("An in-range value changed");
            }
        }
        System.out.println("Picker bounds checks passed: edge cases and 10,000 ranges.");
    }
}
