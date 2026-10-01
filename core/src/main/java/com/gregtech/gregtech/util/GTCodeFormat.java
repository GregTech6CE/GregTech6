package com.gregtech.gregtech.util;

/** GT6-style number formatting ({@code gregapi.util.UT.Code#makeString}). */
public final class GTCodeFormat {
    private GTCodeFormat() {}

    /** Converts a number to a string with underscores as decimal separators (GT6 style). */
    public static String makeString(long number) {
        if (number > -10_000 && number < 10_000) {
            return Long.toString(number);
        }
        StringBuilder out = new StringBuilder();
        long value = number;
        if (value < 0) {
            value = -value;
            out.append('-');
        }
        boolean leading = true;
        for (long place = 1_000_000_000_000_000_000L; place > 0; place /= 10) {
            long digit = (value / place) % 10;
            if (leading && digit != 0) {
                leading = false;
            }
            if (!leading) {
                out.append(digit);
                if (place != 1) {
                    for (long j = place; j > 0; j /= 1000) {
                        if (j == 1) {
                            out.append('_');
                            break;
                        }
                    }
                }
            }
        }
        return out.toString();
    }

    public static long divUp(long a, long b) {
        if (b <= 0) {
            return a;
        }
        return (a + b - 1) / b;
    }
}
