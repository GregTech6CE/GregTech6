/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.addon.AddonNames;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pure data for one addon material. GregTech turns it into a generated {@code gt6ce:} material at startup.
 * This is the recommended declaration entry for addons; generated materials are read through
 * {@link GregTechMaterials}.
 *
 * @param id numeric material id; declarations from {@link #builder(String)} use 0, meaning the host assigns the
 *           real id at startup. Read it back with {@code GregTechMaterials.find(name).id()}.
 * @param features generated forms and processing opt-ins; see {@link Feature}
 * @param oreByproducts ore-processing byproducts in GregTech's order; the first is the wash byproduct
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
        Set<Feature> features,
        List<String> oreByproducts
) {
    /** One generated form or processing opt-in of a material. */
    public enum Feature {
        /** Dust, the small and tiny dusts, and the unit flag. This is what generates dust items. */
        DUST,
        GAS,
        LIQUID,
        /**
         * Ingots, plates, rods, gears and the other generated metal forms, plus dusts.
         * This is the metal half of a scale check; it does not add a new prefix.
         */
        METAL,
        /**
         * Ore and small-ore blocks, the raw ore item, and the crushed, washed and centrifuged forms.
         * This is GregTech's {@code ORES} form. It does not add a worldgen vein.
         */
        ORES,
        /**
         * Plate and foil requests only. This does not add double plates, dense plates, rods or an ingot.
         * A metal that already generates those forms does not need it.
         */
        POLYMER,
        /** {@code wireFine} requests only. This does not add the electric wire blocks. */
        WIRES,
        /**
         * Opts this material's separately declared composition into Centrifuge decomposition.
         * Does not imply Electrolyzer permission or generate a missing dust/fluid form.
         */
        CENTRIFUGE,
        /**
         * Opts this material's separately declared composition into Electrolyzer decomposition.
         * Does not imply Centrifuge permission. Both opt-ins may be combined.
         */
        ELECTROLYZER
    }

    public MaterialDeclaration {
        if (id < 0 || id >= 10_000) throw new IllegalArgumentException("Material ID must be 0..9999: " + id);
        if (!AddonNames.matches(AddonNames.MATERIAL, name))
            throw new IllegalArgumentException("Use a stable, readable material name: " + name);
        if (displayName == null || displayName.isBlank() || (color & 0xFF00_0000) != 0)
            throw new IllegalArgumentException("Material requires a display name and RGB color: " + name);
        if (meltingPoint < 0 || boilingPoint < 0 || !Float.isFinite(density) || density < 0)
            throw new IllegalArgumentException("Invalid physical properties for " + name);
        if (features == null) throw new IllegalArgumentException("Missing material features for " + name);
        EnumSet<Feature> copiedFeatures = EnumSet.noneOf(Feature.class);
        for (Feature feature : features) {
            if (feature == null) throw new IllegalArgumentException("Null material feature for " + name);
            copiedFeatures.add(feature);
        }
        features = Collections.unmodifiableSet(copiedFeatures);
        if (formula == null)
            throw new IllegalArgumentException("Material formula must not be null for " + name
                    + "; pass an empty string");
        if (oreByproducts == null)
            throw new IllegalArgumentException("Ore byproducts must not be null for " + name + "; pass an empty list");
        oreByproducts = List.copyOf(oreByproducts);
        if (!features.contains(Feature.ORES) && !oreByproducts.isEmpty())
            throw new IllegalArgumentException("Ore byproducts require ores: " + name);
        var seenByproducts = new HashSet<String>();
        for (String byproduct : oreByproducts) {
            if (!AddonNames.matches(AddonNames.MATERIAL, byproduct))
                throw new IllegalArgumentException("Use a material name for an ore byproduct of " + name);
            if (!seenByproducts.add(byproduct))
                throw new IllegalArgumentException("Repeats an ore byproduct '" + byproduct + "' for " + name);
        }
    }

    /** True when this material declares {@code feature}. */
    public boolean has(Feature feature) {
        Objects.requireNonNull(feature, "feature");
        return features.contains(feature);
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
        private final EnumSet<Feature> features = EnumSet.noneOf(Feature.class);
        private List<String> oreByproducts = List.of();

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

        /** @see Feature#DUST */
        public Builder dust() { features.add(Feature.DUST); return this; }

        /** @see Feature#METAL */
        public Builder metal() { features.add(Feature.METAL); return this; }

        public Builder gas() { features.add(Feature.GAS); return this; }

        public Builder liquid() { features.add(Feature.LIQUID); return this; }

        /** @see Feature#ORES */
        public Builder ores() { features.add(Feature.ORES); return this; }

        /** @see Feature#POLYMER */
        public Builder polymer() { features.add(Feature.POLYMER); return this; }

        /** @see Feature#WIRES */
        public Builder wires() { features.add(Feature.WIRES); return this; }

        /** @see Feature#CENTRIFUGE */
        public Builder centrifuge() { features.add(Feature.CENTRIFUGE); return this; }

        /** @see Feature#ELECTROLYZER */
        public Builder electrolyzer() { features.add(Feature.ELECTROLYZER); return this; }

        /**
         * Ore-processing byproducts, in GregTech's order. The first is the wash byproduct.
         * Calling this also turns {@link #ores()} on.
         */
        public Builder oreByproducts(String... materials) {
            if (materials == null || materials.length == 0)
                throw new IllegalArgumentException("Ore byproducts need a material name");
            features.add(Feature.ORES);
            oreByproducts = List.of(materials);
            return this;
        }

        public MaterialDeclaration build() {
            return new MaterialDeclaration(id, name, displayName, color, meltingPoint, boilingPoint, density,
                    formula, features, oreByproducts);
        }
    }
}
