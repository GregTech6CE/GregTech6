/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.addon.AddonRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Material catalog entry. Materials are declared through {@link MaterialDeclaration} and read
 * through immutable definition snapshots from {@link #catalog()}.
 */
public final class GregTechMaterials {
    private GregTechMaterials() {}

    /** Immutable definition view. Requires completed material bootstrap. */
    public static MaterialCatalogView catalog() {
        return AddonRuntime.services().materialCatalog();
    }

    /**
     * Resolves a registered name or alias to its canonical material key. Unknown names,
     * substitution groups and sentinel materials return empty.
     * Available from {@link AddonMaterialContributor#link()} and after material bootstrap.
     *
     * @throws IllegalStateException if the material registry is unavailable
     */
    public static Optional<MaterialKey> resolve(String name) {
        return AddonRuntime.services().resolveMaterial(name);
    }

    /** Immutable process rules from the current definition view. */
    public static MaterialProcessesView processes() {
        return catalog().processes();
    }

    /** Immutable definition-time form rules from the current definition view. */
    public static MaterialFormRulesView forms() {
        return catalog().forms();
    }

    /** Native item, block and fluid bindings from the most recently published content generation. */
    public static MaterialFormsView formBindings() {
        return catalog().formBindings();
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

        /**
         * Stores {@code units} as given: an empty list stays empty, but individual entries are
         * not filtered. Use {@link #of(String, List)} to drop null units, empty material names
         * and non-positive amounts.
         */
        public StackComposition {
            if (prefix == null)
                throw new IllegalArgumentException("Stack composition prefix must not be null; pass an empty string");
            if (units == null)
                throw new IllegalArgumentException("Stack composition units must not be null; pass an empty list");
            units = units.isEmpty() ? List.of() : List.copyOf(units);
        }

        /**
         * Null units, empty material names and non-positive amounts are dropped.
         * A unit list with nothing left is empty. A null prefix is rejected; pass an empty name.
         */
        public static StackComposition of(String prefix, List<Component> units) {
            if (prefix == null)
                throw new IllegalArgumentException("Stack composition prefix must not be null; pass an empty string");
            if (units == null || units.isEmpty()) return EMPTY;
            var copy = new ArrayList<Component>();
            for (Component unit : units) {
                if (unit == null || unit.material() == null || unit.material().isEmpty() || unit.amount() <= 0)
                    continue;
                copy.add(new Component(unit.material(), unit.amount()));
            }
            if (copy.isEmpty()) return EMPTY;
            return new StackComposition(prefix, copy);
        }

        public boolean present() {
            return !units.isEmpty();
        }
    }
}
