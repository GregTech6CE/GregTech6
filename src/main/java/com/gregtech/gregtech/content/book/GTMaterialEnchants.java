package com.gregtech.gregtech.content.book;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.loaders.c.GTEnchantmentTable;
import com.gregtech.gregtech.loaders.c.GTMaterialFields;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The port's material enchantment lookup, for the material dictionary's enchantment page
 * (GT6's {@code UT.Books.addMaterialDictionary} prints one page per non-empty enchantment list:
 * tools, weapons, ammo, ranged, fishing, armors).
 *
 * <p>The table ({@link GTEnchantmentTable}) is transcribed from GT6's {@code MT.java}, which keys its
 * rows by the receiver of the {@code addEnchantmentFor…} call — a GT6 <em>field</em> name. Those are
 * often short ({@code Ma}, {@code Fe}, {@code PO4}) or spelled differently from the material
 * ({@code Polycarbonate} = "Hard Plastic", {@code HSLA} = "HSLA-Steel"), so every row is resolved
 * through {@link GTMaterialFields} (generated from {@code MT.java}) and then through the port's own
 * material registry. Rows that name a material this port does not have — mostly other mods' metals
 * GT6 enchanted for compatibility, such as {@code Vinteum} or {@code Pyrotheum} — resolve to nothing
 * and simply produce no page.</p>
 */
public final class GTMaterialEnchants {
    private GTMaterialEnchants() {}

    /** GT6 field name -> port material name, for the rows neither a field nor a registry name. */
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("Ad", "Adamantium"),
            Map.entry("Atl", "Atlarus"),
            Map.entry("Au", "Gold"),
            Map.entry("Bi", "Bismuth"),
            Map.entry("Fe", "Iron"),
            Map.entry("Ke", "Trinium"),
            Map.entry("Ni", "Nickel"),
            Map.entry("Pb", "Lead"),
            Map.entry("PO4", "Phosphate"),
            Map.entry("Pt", "Platinum"));

    /** The GT6 kind names, in GT6's own order (tools, weapons, ammo, ranged, fishing, armors). */
    public static final List<String> KINDS =
            List.of("Tools", "Weapons", "Ammo", "Ranged", "Fishing", "Armors");

    /**
     * The port material a table row names, or null when this port has no such material.
     * <p>
     * GT6's field table wins over the plain name: {@code Ma} is GT6's field for {@code Magic} (the
     * pseudo-element), not for magnesium — the port spelled that one {@code Mg}.
     * </p>
     */
    @Nullable
    public static GTMaterial resolve(String row) {
        String mapped = GTMaterialFields.materialOf(row);
        GTMaterial material = GTMaterialRegistry.get(mapped != null ? mapped : row);
        if (material.isValid()) return material;
        String alias = ALIASES.get(row);
        if (alias == null) return null;
        material = GTMaterialRegistry.get(alias);
        return material.isValid() ? material : null;
    }

    /** How many of the table's rows name a material this port registers. */
    public static int resolvedRows() {
        int count = 0;
        for (GTEnchantmentTable.Material row : GTEnchantmentTable.MATERIALS) {
            if (resolve(row.material()) != null) count++;
        }
        return count;
    }

    /** The enchantments a material gets, grouped by the use GT6 lists them for. */
    public static Map<String, List<String>> enchantmentsOf(GTMaterial material) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        String name = material.getName();
        for (GTEnchantmentTable.Material row : GTEnchantmentTable.MATERIALS) {
            GTMaterial resolved = resolve(row.material());
            if (resolved == null || !resolved.getName().equals(name)) continue;
            for (GTEnchantmentTable.Entry entry : row.entries()) {
                out.computeIfAbsent(entry.kind(), key -> new ArrayList<>())
                        .add(entry.enchantment() + " " + roman(entry.level()));
            }
        }
        // Damage is GT6's shorthand that forwards to weapons *and* ammo (OreDictMaterial:1091).
        if (out.containsKey("Damage")) {
            for (String kind : new String[] {"Weapons", "Ammo"}) {
                List<String> target = out.computeIfAbsent(kind, key -> new ArrayList<>());
                for (String value : out.get("Damage")) {
                    if (!target.contains(value)) target.add(value);
                }
            }
            out.remove("Damage");
        }
        return out;
    }

    /** GT6 writes levels as Roman numerals in the book (the port keeps it readable). */
    public static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(level);
        };
    }
}
