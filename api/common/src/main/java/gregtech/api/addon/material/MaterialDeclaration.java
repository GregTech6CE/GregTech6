/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;




import gregtech.api.addon.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Pure data for one addon material. GregTech turns it into a generated {@code gt6ce:} material at startup.
 * This is the recommended declaration entry for addons; generated materials are read through
 * {@link GregTechMaterials}.
 *
 * @param id numeric material id; declarations from {@link #builder(String)} use 0, meaning the host assigns the
 *           real id at startup. Read it back with {@code GregTechMaterials.find(name).id()}.
 */
public record MaterialDeclaration(
        int id,
        String name,
        String displayName,
        int color,
        int meltingPoint,
        int boilingPoint,
        float density,
        String formula,
        boolean dust,
        boolean gas,
        boolean liquid,
        boolean metal,
        boolean ores,
        List<String> oreByproducts,
        boolean polymer,
        boolean wires,
        boolean centrifuge,
        boolean electrolyzer
) {
    public MaterialDeclaration {
        if (id < 0 || id >= 10_000) throw new IllegalArgumentException("Material ID must be 0..9999: " + id);
        Objects.requireNonNull(name, "Material name");
        if (!AddonNames.matches(AddonNames.MATERIAL, name))
            throw new IllegalArgumentException("Use a stable, readable material name: " + name);
        Objects.requireNonNull(displayName, "Material display name");
        if (displayName.isBlank() || (color & 0xFF00_0000) != 0)
            throw new IllegalArgumentException("Material requires a display name and RGB color: " + name);
        if (meltingPoint < 0 || boilingPoint < 0 || !Float.isFinite(density) || density < 0)
            throw new IllegalArgumentException("Invalid physical properties for " + name);
        formula = formula == null ? "" : formula;
        oreByproducts = oreByproducts == null ? List.of() : List.copyOf(oreByproducts);
        if (!ores && !oreByproducts.isEmpty())
            throw new IllegalArgumentException("Ore byproducts require ores: " + name);
        var seenByproducts = new HashSet<String>();
        for (String byproduct : oreByproducts) {
            if (!AddonNames.matches(AddonNames.MATERIAL, byproduct))
                throw new IllegalArgumentException("Use a material name for an ore byproduct of " + name);
            if (!seenByproducts.add(byproduct))
                throw new IllegalArgumentException("Repeats an ore byproduct '" + byproduct + "' for " + name);
        }
    }

    /** Compatibility constructor: existing declarations do not implicitly permit decomposition. */
    public MaterialDeclaration(int id, String name, String displayName, int color,
            int meltingPoint, int boilingPoint, float density, String formula,
            boolean dust, boolean gas, boolean liquid, boolean metal, boolean ores,
            List<String> oreByproducts, boolean polymer, boolean wires) {
        this(id, name, displayName, color, meltingPoint, boilingPoint, density, formula,
                dust, gas, liquid, metal, ores, oreByproducts, polymer, wires, false, false);
    }

    /** Declares a material by name. GregTech assigns the numeric id at startup. */
    public static Builder builder(String name) { return new Builder(0, name); }

    public static final class Builder {
        private final int id;
        private final String name;
        private String displayName;
        private int color = 0xFFFFFF;
        private int meltingPoint = 300;
        private int boilingPoint = 1000;
        private float density = 1;
        private String formula = "";
        private boolean dust;
        private boolean gas;
        private boolean liquid;
        private boolean metal;
        private boolean ores;
        private List<String> oreByproducts = List.of();
        private boolean polymer;
        private boolean wires;
        private boolean centrifuge;
        private boolean electrolyzer;

        private Builder(int id, String name) {
            this.id = id;
            this.name = name;
            this.displayName = name;
        }

        public Builder displayName(String value) { displayName = value; return this; }
        public Builder color(int rgb) { color = rgb; return this; }
        public Builder meltingPointKelvin(int value) { meltingPoint = value; return this; }
        public Builder boilingPointKelvin(int value) { boilingPoint = value; return this; }
        public Builder density(float value) { density = value; return this; }
        public Builder chemicalFormula(String value) { formula = value; return this; }

        /** Dust, the small and tiny dusts, and the unit flag. This is what generates dust items. */
        public Builder dust() { dust = true; return this; }

        /**
         * Ingots, plates, rods, gears and the other generated metal forms, plus dusts.
         * This is the metal half of a scale check; it does not add a new prefix.
         */
        public Builder metal() { metal = true; return this; }

        public Builder gas() { gas = true; return this; }
        public Builder liquid() { liquid = true; return this; }

        /**
         * Ore and small-ore blocks, the raw ore item, and the crushed, washed and centrifuged forms.
         * This is GregTech's {@code ORES} form. It does not add a worldgen vein.
         */
        public Builder ores() { ores = true; return this; }

        /**
         * Plate and foil requests only. This does not add double plates, dense plates, rods or an ingot.
         * A metal that already generates those forms does not need it.
         */
        public Builder polymer() { polymer = true; return this; }

        /** {@code wireFine} requests only. This does not add the electric wire blocks. */
        public Builder wires() { wires = true; return this; }

        /**
         * Opts this material's separately declared composition into Centrifuge decomposition.
         * Does not imply Electrolyzer permission or generate a missing dust/fluid form.
         */
        public Builder centrifuge() { centrifuge = true; return this; }

        /**
         * Opts this material's separately declared composition into Electrolyzer decomposition.
         * Does not imply Centrifuge permission. Both opt-ins may be combined.
         */
        public Builder electrolyzer() { electrolyzer = true; return this; }

        /**
         * Ore-processing byproducts, in GregTech's order. The first is the wash byproduct.
         * Calling this also turns {@link #ores()} on.
         */
        public Builder oreByproducts(String... materials) {
            ores = true;
            if (materials == null || materials.length == 0)
                throw new IllegalArgumentException("Ore byproducts need a material name");
            oreByproducts = List.of(materials);
            return this;
        }

        public MaterialDeclaration build() {
            return new MaterialDeclaration(id, name, displayName, color, meltingPoint, boilingPoint, density,
                    formula, dust, gas, liquid, metal, ores, oreByproducts, polymer, wires, centrifuge, electrolyzer);
        }
    }
}
