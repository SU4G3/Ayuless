#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Ayuless contributors
"""Run the local user-ID date model checks with a JDK, without Android/Gradle."""
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "TMessagesProj/src/main/java/org/ayuless/profile/RegistrationEstimate.java"
CHECK = r"""
import java.time.YearMonth;
import java.util.Locale;
import java.util.TimeZone;
import org.ayuless.profile.RegistrationEstimate;

public final class RegistrationEstimateCheck {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }
    public static void main(String[] args) {
        // Invalid IDs and unsupported recent/early IDs must not be extrapolated.
        for (long id : new long[] {Long.MIN_VALUE, -1, 0, 999_999, 8_000_000_001L, Long.MAX_VALUE}) {
            check(RegistrationEstimate.estimate(id) == null);
        }
        check(RegistrationEstimate.estimate(1_000_000).equals(YearMonth.of(2013, 11)));
        check(RegistrationEstimate.estimate(8_000_000_000L).equals(YearMonth.of(2025, 4)));
        // A midpoint spans 21 calendar months, rather than integer-truncating the ID ratio.
        YearMonth middle = RegistrationEstimate.estimate(200_000_000);
        check(middle.equals(YearMonth.of(2016, 1)));
        check(RegistrationEstimate.format(middle, Locale.ENGLISH).equals("≈ January 2016"));
        check(RegistrationEstimate.format(middle, Locale.forLanguageTag("ru")).equals("≈ январь 2016"));
        check(RegistrationEstimate.format(null, Locale.ENGLISH) == null);
        YearMonth previous = YearMonth.of(2013, 11);
        for (long id = 1_000_000; id <= 8_000_000_000L; id += 123_457) {
            YearMonth month = RegistrationEstimate.estimate(id);
            check(month != null && !month.isBefore(previous));
            previous = month;
        }
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
        check(RegistrationEstimate.estimate(200_000_000).equals(middle));
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
        check(RegistrationEstimate.estimate(200_000_000).equals(middle));
        System.out.println("Registration estimate: bounds, interpolation, monotonicity, EN/RU and timezones passed");
    }
}
"""


def main():
    java_home = os.environ.get("JAVA_HOME")
    bin_dir = Path(java_home) / "bin" if java_home else None
    with tempfile.TemporaryDirectory(prefix="ayuless-registration-") as directory:
        target = Path(directory)
        test = target / "RegistrationEstimateCheck.java"
        test.write_text(CHECK, encoding="utf-8")
        subprocess.run([str(bin_dir / "javac") if bin_dir else "javac", "-encoding", "UTF-8",
                        "-d", directory, str(SOURCE), str(test)], check=True)
        subprocess.run([str(bin_dir / "java") if bin_dir else "java", "-cp", directory,
                        "RegistrationEstimateCheck"], check=True)


if __name__ == "__main__":
    main()
