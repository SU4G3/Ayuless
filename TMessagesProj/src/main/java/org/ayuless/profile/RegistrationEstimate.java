/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.profile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Original, deliberately coarse model of Telegram user ID growth, NOT verified
 * registration records. Rounded reference points written for Ayuless from the
 * publicly familiar growth eras: launch in 2013, tens of millions in 2014,
 * hundreds of millions in 2015–19, billion-range IDs in 2020 and much faster
 * allocation after 2021. IDs are not active-user counts or a strict time sequence.
 * Errors can span many months or years; recycled/exceptional IDs cannot be dated.
 * Never use this table for chat IDs (a separate allocation sequence).
 */
public final class RegistrationEstimate {
    private RegistrationEstimate() {}

    // APPROXIMATE (user ID, year, month). Independently authored heuristic anchors.
    private static final long[][] REFERENCES = {
            {1_000_000L, 2013, 11},
            {10_000_000L, 2014, 2},
            {100_000_000L, 2015, 3},
            {300_000_000L, 2016, 12},
            {500_000_000L, 2018, 1},
            {1_000_000_000L, 2020, 1},
            {2_000_000_000L, 2021, 10},
            {5_000_000_000L, 2022, 7},
            {6_000_000_000L, 2023, 5},
            {7_000_000_000L, 2024, 4},
            {8_000_000_000L, 2025, 4}
    };

    /** Null outside the model's supported range; do not invent a future date. */
    public static YearMonth estimate(long userId) {
        if (userId < REFERENCES[0][0] || userId > REFERENCES[REFERENCES.length - 1][0]) return null;
        for (int i = 1; i < REFERENCES.length; i++) {
            long[] before = REFERENCES[i - 1], after = REFERENCES[i];
            if (userId <= after[0]) {
                long start = epochDay(before), end = epochDay(after);
                double fraction = (double) (userId - before[0]) / (after[0] - before[0]);
                return YearMonth.from(LocalDate.ofEpochDay(start + Math.round(fraction * (end - start))));
            }
        }
        return null;
    }

    private static long epochDay(long[] point) {
        return LocalDate.of((int) point[1], (int) point[2], 1).toEpochDay();
    }

    public static String format(YearMonth month, Locale locale) {
        return month == null ? null : "≈ " + month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale));
    }
}
