/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;




import gregtech.api.addon.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only material handles. The material table itself stays inside GregTech.
 * This is the recommended read entry for addons; materials are declared through
 * {@link MaterialDeclaration}.
 */
public final class GregTechMaterials {
    private GregTechMaterials() {}

    /** The named material, or {@link #invalid()} when the name is unknown. Never null. */
    public static Material find(String name) { return AddonRuntime.services().materials().find(name); }

    public static Material find(int id) { return AddonRuntime.services().materials().find(id); }

    /** The sentinel returned for an unknown name or id. {@link Material#valid()} is false. */
    public static Material invalid() { return AddonRuntime.services().materials().invalid(); }

    public static Iterable<Material> all() { return AddonRuntime.services().materials().all(); }

    public interface Catalog {
        Material find(String name);

        Material find(int id);

        Material invalid();

        Iterable<Material> all();
    }

    /** One material after aliases and links have resolved. */
    public interface Material {
        String name();

        int id();

        boolean valid();

        int color();

        int meltingPoint();

        int boilingPoint();

        float density();

        /** Tooltip formula, from the composition when the material has one. */
        String formula();

        String translationKey();

        /** Material-table mass, in nucleons. Not converted. */
        long mass();

        /** Material-table tool quality. Not converted. */
        int toolQuality();

        /** Material-table tool speed. Not converted. */
        float toolSpeed();

        /** Material-table tool durability. Not converted. */
        long toolDurability();

        List<Component> composition();

        /**
         * Ore-processing byproduct names after {@link #resolve()}, in wash order.
         * Empty when this material has none. Read this from {@code onRecipesReady}.
         * A call before materials link returns the names stored then and does not throw.
         */
        List<String> byproducts();

        Material resolve();
    }

    /**
     * One material unit. {@code amount} is in GregTech units ({@link gregtech.api.material.GTValues#U}).
     * One ingot is {@code 1 U}.
     */
    public record Component(String material, long amount) {}

    /**
     * Prefix name and material units of one item. An unrecognized item is {@link #EMPTY}.
     * {@code prefix} is empty when the item has units and no prefix.
     */
    public record StackComposition(String prefix, List<Component> units) {
        public static final StackComposition EMPTY = new StackComposition("", List.of());

        public StackComposition {
            prefix = prefix == null ? "" : prefix;
            units = units == null || units.isEmpty() ? List.of() : List.copyOf(units);
        }

        /**
         * Null units, empty material names and non-positive amounts are dropped.
         * A unit list with nothing left is empty. A null prefix becomes an empty name.
         */
        public static StackComposition of(String prefix, List<Component> units) {
            if (units == null || units.isEmpty()) return EMPTY;
            var copy = new ArrayList<Component>();
            for (Component unit : units) {
                if (unit == null || unit.material() == null || unit.material().isEmpty() || unit.amount() <= 0) continue;
                copy.add(new Component(unit.material(), unit.amount()));
            }
            if (copy.isEmpty()) return EMPTY;
            return new StackComposition(prefix, copy);
        }

        public boolean present() { return !units.isEmpty(); }
    }
}
