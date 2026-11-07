package gregtech.engine.material;

import gregtech.api.material.AtomicProperties;
import gregtech.api.material.MaterialProperty;
import gregtech.api.material.MaterialTextureSet;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Named material registration parameters for built-in and imported materials. */
public record MaterialDefinition(
        int id,
        String name,
        String displayName,
        int color,
        MaterialTextureSet texture,
        int meltingPointKelvin,
        int boilingPointKelvin,
        float density,
        String chemicalFormula,
        Set<MaterialProperty> properties,
        AtomicProperties atomicProperties,
        int alpha,
        MaterialFacts facts) {
    public MaterialDefinition {
        Objects.requireNonNull(name, "Material name");
        Objects.requireNonNull(displayName, "Material display name");
        Objects.requireNonNull(facts, "Material facts");
        Objects.requireNonNull(chemicalFormula, "Chemical formula");
        Objects.requireNonNull(atomicProperties, "Atomic properties");
        if (alpha < 0 || alpha > 255) throw new IllegalArgumentException("Alpha must be 0..255");
        properties = Set.copyOf(properties);
        if ((id <= 0 || id >= 10_000) && !(id == -1 && facts.group()))
            throw new IllegalArgumentException("Material ID must be 1..9999: " + id);
        if (!name.matches(facts.group() ? "[A-Za-z][A-Za-z0-9_/]*" : "[A-Za-z][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Use a stable, readable material name: " + name);
        if (displayName.isBlank() || (color & 0xFF000000) != 0)
            throw new IllegalArgumentException("Material requires a display name and RGB color: " + name);
        if (meltingPointKelvin < 0 || boilingPointKelvin < 0 || !Float.isFinite(density) || density < 0)
            throw new IllegalArgumentException("Invalid physical properties for " + name);
    }

    public MaterialDefinition(
            int id,
            String name,
            String displayName,
            int color,
            MaterialTextureSet texture,
            int melt,
            int boil,
            float density,
            String formula,
            Set<MaterialProperty> properties,
            AtomicProperties atoms,
            int alpha) {
        this(
                id,
                name,
                displayName,
                color,
                texture,
                melt,
                boil,
                density,
                formula,
                properties,
                atoms,
                alpha,
                MaterialFacts.EMPTY);
    }

    public static Builder builder(int id, String name) {
        return new Builder(id, name);
    }

    /** Compatibility with definitions written before atomic properties were exposed. */
    public MaterialDefinition(
            int id,
            String name,
            String displayName,
            int color,
            MaterialTextureSet texture,
            int meltingPointKelvin,
            int boilingPointKelvin,
            float density,
            String chemicalFormula,
            Set<MaterialProperty> properties) {
        this(
                id,
                name,
                displayName,
                color,
                texture,
                meltingPointKelvin,
                boilingPointKelvin,
                density,
                chemicalFormula,
                properties,
                AtomicProperties.GT6_DEFAULT,
                255,
                MaterialFacts.EMPTY);
    }

    public static final class Builder {
        private final int id;
        private final String name;
        private String displayName;
        private int color = 0xFFFFFF;
        private MaterialTextureSet texture = MaterialTextureSet.DULL;
        private int meltingPointKelvin = 300;
        private int boilingPointKelvin = 1000;
        private float density = 1;
        private String formula = "";
        private AtomicProperties atoms = AtomicProperties.GT6_DEFAULT;
        private int alpha = 255;
        private MaterialFacts facts = MaterialFacts.EMPTY;
        private final EnumSet<MaterialProperty> properties = EnumSet.noneOf(MaterialProperty.class);

        private Builder(int id, String name) {
            this.id = id;
            this.name = name;
            this.displayName = name;
        }

        public Builder facts(MaterialFacts value) {
            facts = value;
            return this;
        }

        public Builder displayName(String value) {
            displayName = value;
            return this;
        }

        public Builder color(int rgb) {
            color = rgb;
            return this;
        }

        public Builder texture(MaterialTextureSet value) {
            texture = value;
            return this;
        }

        public Builder meltingPointKelvin(int value) {
            meltingPointKelvin = value;
            return this;
        }

        public Builder boilingPointKelvin(int value) {
            boilingPointKelvin = value;
            return this;
        }

        public Builder density(float value) {
            density = value;
            return this;
        }

        public Builder chemicalFormula(String value) {
            formula = value;
            return this;
        }

        public Builder atomicProperties(long protons, long electrons, long neutrons, long additionalMass) {
            atoms = new AtomicProperties(protons, electrons, neutrons, additionalMass);
            return this;
        }

        public Builder alpha(int value) {
            alpha = value;
            return this;
        }

        public Builder properties(MaterialProperty... values) {
            for (var value : values) properties.add(Objects.requireNonNull(value));
            return this;
        }

        public Builder dust() {
            return properties(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST, MaterialProperty.UNIT);
        }

        public Builder gas() {
            return properties(MaterialProperty.GAS, MaterialProperty.LIQUID);
        }

        public Builder liquid() {
            return properties(MaterialProperty.LIQUID);
        }

        public MaterialDefinition build() {
            return new MaterialDefinition(
                    id,
                    name,
                    displayName,
                    color,
                    texture,
                    meltingPointKelvin,
                    boilingPointKelvin,
                    density,
                    formula,
                    properties,
                    atoms,
                    alpha,
                    facts);
        }
    }
}
