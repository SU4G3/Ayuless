/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.security;

/** Inclusive NumberPicker wrapping without overflow or negative indices. */
public final class PickerBounds {
    private PickerBounds() {
    }

    public static int wrap(int value, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Invalid picker bounds");
        }
        long count = (long) max - min + 1;
        long offset = ((long) value - min) % count;
        if (offset < 0) {
            offset += count;
        }
        return (int) (min + offset);
    }
}
