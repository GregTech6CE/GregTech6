/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.energy;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A short energy name such as {@code EU}.
 *
 * <p>Known names are canonicalized and available as constants, so {@link #of(String)} maps
 * {@code "eu"} to {@link #EU}. A name that matches {@link #NAME} but is not known stays a custom
 * type and keeps the spelling it was written with; this is how addons and behavior machines
 * declare their own energy. {@link #ANY} ({@code *}) is the behavior-machine wildcard.</p>
 *
 * <p>Equality compares the canonical name exactly. Known names therefore compare
 * case-insensitively at the factory boundary, while two custom names that differ only in case
 * are different types. This type replaces the short strings that used to travel through
 * {@code EnergyInput}, {@code EnergyOutput}, {@code FuelOutput} and the platform energy API.</p>
 */
public final class EnergyType implements Comparable<EnergyType> {
    /** Behavior-machine main input that locks the type from the first accepted packet. */
    public static final EnergyType ANY = new EnergyType("*");
    /** Layout-part and builder form: a letter, then up to 15 letters, digits or underscores. */
    public static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,15}");

    public static final EnergyType EU = new EnergyType("EU");
    public static final EnergyType RU = new EnergyType("RU");
    public static final EnergyType KU = new EnergyType("KU");
    public static final EnergyType HU = new EnergyType("HU");
    public static final EnergyType CU = new EnergyType("CU");
    public static final EnergyType LU = new EnergyType("LU");
    public static final EnergyType MU = new EnergyType("MU");
    public static final EnergyType NU = new EnergyType("NU");
    public static final EnergyType QU = new EnergyType("QU");
    public static final EnergyType TU = new EnergyType("TU");
    public static final EnergyType AU = new EnergyType("AU");
    public static final EnergyType RF = new EnergyType("RF");
    public static final EnergyType MJ = new EnergyType("MJ");
    public static final EnergyType STEAM = new EnergyType("Steam");
    public static final EnergyType ORDO = new EnergyType("Ordo");
    public static final EnergyType AER = new EnergyType("Aer");
    public static final EnergyType AQUA = new EnergyType("Aqua");
    public static final EnergyType TERRA = new EnergyType("Terra");
    public static final EnergyType IGNIS = new EnergyType("Ignis");
    public static final EnergyType PERDITIO = new EnergyType("Perditio");

    private static final Map<String, EnergyType> KNOWN = knownTypes();

    private final String name;

    private EnergyType(String name) {
        this.name = name;
    }

    /** The canonical short name, such as {@code EU} or a custom spelling. */
    public String name() {
        return name;
    }

    /** True for {@link #ANY}, the behavior-machine wildcard. */
    public boolean wildcard() {
        return this == ANY;
    }

    /** KU and RU use the sign as direction. */
    public boolean signedDirection() {
        return this == KU || this == RU;
    }

    /** True for TU, whose packet bounds differ from the other energy types. */
    public boolean time() {
        return this == TU;
    }

    /**
     * Non-blank name. {@link #ANY} stays {@code *}. A known short name is returned in its
     * GregTech spelling; any other name that matches {@link #NAME} is kept as written.
     */
    public static EnergyType of(String name) {
        return of(name, "Missing energy type");
    }

    public static EnergyType of(String name, String missing) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException(missing);
        if (ANY.name.equals(name)) return ANY;
        if (!NAME.matcher(name).matches()) throw new IllegalArgumentException("Invalid energy type: " + name);
        EnergyType known = KNOWN.get(name.toLowerCase(Locale.ROOT));
        return known != null ? known : new EnergyType(name);
    }

    /** Known machine energy only; unlike {@link #of(String)}, rejects wildcards and custom names. */
    public static EnergyType known(String name) {
        EnergyType known = name == null ? null : KNOWN.get(name.toLowerCase(Locale.ROOT));
        if (known == null) throw new IllegalArgumentException("Unknown machine energy type: " + name);
        return known;
    }

    /** Known machine energy only; rejects {@link #ANY} and custom types. */
    public static EnergyType known(EnergyType type) {
        EnergyType known = type == null ? null : KNOWN.get(type.name.toLowerCase(Locale.ROOT));
        if (known == null) throw new IllegalArgumentException("Unknown machine energy type: "
                + (type == null ? null : type.name));
        return known;
    }

    @Override public boolean equals(Object other) {
        return other instanceof EnergyType that && name.equals(that.name);
    }

    @Override public int hashCode() {
        return name.hashCode();
    }

    @Override public int compareTo(EnergyType other) {
        return name.compareTo(other.name);
    }

    @Override public String toString() {
        return name;
    }

    private static Map<String, EnergyType> knownTypes() {
        var names = List.of(EU, RU, KU, HU, CU, LU, MU, NU, QU, TU, AU, RF, MJ, STEAM,
                ORDO, AER, AQUA, TERRA, IGNIS, PERDITIO);
        var map = new HashMap<String, EnergyType>();
        for (EnergyType type : names) map.put(type.name.toLowerCase(Locale.ROOT), type);
        return Map.copyOf(map);
    }
}
