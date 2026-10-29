/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Short energy names such as {@code EU}. Builders canonicalize known names, so {@code "eu"} becomes
 * {@code "EU"} and {@code "ku"} matches {@link #signedDirection(String)}.
 */
public final class EnergyType {
    /** Behavior-machine main input that locks the type from the first accepted packet. */
    public static final String ANY = "*";
    /** Layout-part and builder form: a letter, then up to 15 letters, digits or underscores. */
    public static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,15}");

    public static final String EU = "EU";
    public static final String RU = "RU";
    public static final String KU = "KU";
    public static final String HU = "HU";
    public static final String CU = "CU";
    public static final String LU = "LU";
    public static final String MU = "MU";
    public static final String NU = "NU";
    public static final String QU = "QU";
    public static final String TU = "TU";
    public static final String AU = "AU";
    public static final String RF = "RF";
    public static final String MJ = "MJ";
    public static final String STEAM = "Steam";

    private static final Map<String, String> CANONICAL = canonical();

    private EnergyType() {}

    /**
     * Non-blank name. {@link #ANY} stays {@code *}. A known short name is returned in its GregTech
     * spelling; any other name that matches {@link #NAME} is kept as written.
     */
    public static String require(String type) {
        return require(type, "Missing energy type");
    }

    public static String require(String type, String missing) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException(missing);
        if (ANY.equals(type)) return ANY;
        if (!NAME.matcher(type).matches()) throw new IllegalArgumentException("Invalid energy type: " + type);
        String canonical = CANONICAL.get(type.toLowerCase(Locale.ROOT));
        return canonical != null ? canonical : type;
    }

    /** Known machine energy only; unlike {@link #require(String)}, rejects wildcards and custom names. */
    public static String requireKnown(String type) {
        String canonical = type == null ? null : CANONICAL.get(type.toLowerCase(Locale.ROOT));
        if (canonical == null) throw new IllegalArgumentException("Unknown machine energy type: " + type);
        return canonical;
    }

    public static boolean same(@Nullable String left, @Nullable String right) {
        return left != null && left.equalsIgnoreCase(right);
    }

    /** KU and RU use the sign as direction. */
    public static boolean signedDirection(String type) {
        return same(type, KU) || same(type, RU);
    }

    public static boolean time(String type) {
        return same(type, TU);
    }

    private static Map<String, String> canonical() {
        var names = List.of(EU, RU, KU, HU, CU, LU, MU, NU, QU, TU, AU, RF, MJ, STEAM,
                "Ordo", "Aer", "Aqua", "Terra", "Ignis", "Perditio");
        var map = new HashMap<String, String>();
        for (String name : names) map.put(name.toLowerCase(Locale.ROOT), name);
        return Map.copyOf(map);
    }
}
