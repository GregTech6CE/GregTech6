/* GregTech-6 Team / Gregorius Techneticies, LGPL-3.0-or-later.
 * Original Loader_Tools.java:68-117; ItemArmorBase:81-99. */
package gregtech.content.hazard;

import java.util.List;

/** Original six four-piece suits. The existing radiation IDs stay stable. */
public final class HazmatSuitCatalog {

    private HazmatSuitCatalog() {}

    public static final List<String> FAMILIES =
            List.of("insect", "frost", "heat", "radiation", "biochemgas", "universal");

    public static String texture(String family) {
        if (!FAMILIES.contains(family)) throw new IllegalArgumentException(family);
        return "hazard_" + (family.equals("insect") ? "insects" : family);
    }

    public static String tooltip(String family) {
        return "gt.armor.hazmat." + family + ".head.tooltip_main";
    }
}
