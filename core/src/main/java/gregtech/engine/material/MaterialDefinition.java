package gregtech.engine.material;

import gregtech.api.addon.material.FuelRule;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.addon.material.MaterialTarget;
import gregtech.api.addon.material.OreProcessingRule;
import gregtech.api.material.AtomicProperties;
import gregtech.api.material.MaterialProperty;
import gregtech.api.material.MaterialTextureSet;
import gregtech.api.material.MaterialToolProfile;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Canonical material identity, presentation and intrinsic physical facts. */
public record MaterialDefinition(
        Identity identity,
        Presentation presentation,
        PhysicalProperties physical,
        Set<MaterialProperty> properties,
        Source source) {
    public MaterialDefinition {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(presentation, "presentation");
        Objects.requireNonNull(physical, "physical");
        Objects.requireNonNull(source, "source");
        properties = MaterialPropertyRules.normalize(properties);
        if ((identity.id() <= 0 || identity.id() >= 10_000) && identity.id() != -1)
            throw new IllegalArgumentException("Material ID must be 1..9999: " + identity.name());
        if (!identity.name().matches("[A-Za-z][A-Za-z0-9_/]*"))
            throw new IllegalArgumentException("Use a stable, readable material name: " + identity.name());
        if (presentation.displayName().isBlank() || (presentation.color() & 0xFF000000) != 0)
            throw new IllegalArgumentException("Material requires a display name and RGB color: " + identity.name());
        if (presentation.alpha() < 0 || presentation.alpha() > 255)
            throw new IllegalArgumentException("Alpha must be 0..255");
        if (physical.meltingPointKelvin() < 0
                || physical.boilingPointKelvin() < 0
                || !Float.isFinite(physical.density())
                || physical.density() < 0)
            throw new IllegalArgumentException("Invalid physical properties for " + identity.name());
    }

    public int id() {
        return identity.id();
    }

    public String name() {
        return identity.name();
    }

    public String localName() {
        return identity.localName();
    }

    public List<String> aliases() {
        return identity.aliases();
    }

    public String displayName() {
        return presentation.displayName();
    }

    public int color() {
        return presentation.color();
    }

    public MaterialTextureSet texture() {
        return presentation.texture();
    }

    public int meltingPointKelvin() {
        return physical.meltingPointKelvin();
    }

    public int boilingPointKelvin() {
        return physical.boilingPointKelvin();
    }

    public float density() {
        return physical.density();
    }

    public String chemicalFormula() {
        return presentation.chemicalFormula();
    }

    public AtomicProperties atomicProperties() {
        return physical.atomicProperties();
    }

    public int alpha() {
        return presentation.alpha();
    }

    public String sourceId() {
        return source.id();
    }

    public String sourceName() {
        return source.name();
    }

    public record Identity(int id, String name, String localName, List<String> aliases) {
        public Identity {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(localName, "localName");
            aliases = List.copyOf(aliases);
        }
    }

    public record Presentation(
            String displayName, int color, MaterialTextureSet texture, int alpha, String chemicalFormula) {
        public Presentation {
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(chemicalFormula, "chemicalFormula");
        }
    }

    public record PhysicalProperties(
            int meltingPointKelvin, int boilingPointKelvin, float density, AtomicProperties atomicProperties) {
        public PhysicalProperties {
            Objects.requireNonNull(atomicProperties, "atomicProperties");
        }
    }

    public record Source(String id, String name) {
        public Source {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
        }
    }

    /**
     * One catalog declaration before installation. The intrinsic definition and auxiliary
     * domains travel together for authoring, but are installed into separate indexes.
     */
    public record Declaration(MaterialDefinition material, MaterialInfoDefinition info) {
        public Declaration {
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(info, "info");
            boolean group = info.relations().group();
            if (group != (material.id() == -1))
                throw new IllegalArgumentException("Group status and material ID disagree: " + material.name());
            if (!group && !material.name().matches("[A-Za-z][A-Za-z0-9_]*"))
                throw new IllegalArgumentException("Use a stable, readable material name: " + material.name());
        }

        public int id() {
            return material.id();
        }

        public String name() {
            return material.name();
        }
    }

    private static MaterialTarget target(String material, long amount) {
        return new MaterialTarget(new MaterialKey(material), amount);
    }

    public static Builder builder(int id, String name) {
        return new Builder(id, name);
    }

    public static final class Builder {
        private final int id;
        private final String name;
        private String localName = "";
        private String displayName;
        private int color = 0xFFFFFF;
        private MaterialTextureSet texture = MaterialTextureSet.DULL;
        private int meltingPointKelvin = 300;
        private int boilingPointKelvin = 1000;
        private float density = 1;
        private String formula = "";
        private AtomicProperties atoms = AtomicProperties.GT6_DEFAULT;
        private int alpha = 255;
        private final EnumSet<MaterialProperty> properties = EnumSet.noneOf(MaterialProperty.class);
        private String sourceId = "", sourceName = "";
        private boolean group, corrected;
        private MaterialToolProfile tools = new MaterialToolProfile(0, 1F, 0, 0);
        private long burnTime, divider;
        private MaterialTarget burning, crushing, smelting, pulverizing;
        private List<MaterialInfoDefinition.Part> composition = List.of();
        private List<String> byproducts = List.of(), aliases = List.of(), members = List.of();
        private int oreMultiplier = 1, processingMultiplier = 1;
        private Set<String> forms = Set.of();
        private Set<String> classifications = Set.of();
        private final Map<String, MaterialTarget> processing = new LinkedHashMap<>();

        private Builder(int id, String name) {
            this.id = id;
            this.name = name;
            this.displayName = name;
        }

        public Builder localName(String name) {
            localName = name;
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

        public Builder source(String id, String name) {
            sourceId = id;
            sourceName = name;
            return this;
        }

        public Builder group() {
            group = true;
            return this;
        }

        public Builder correctedToolStats() {
            corrected = true;
            return this;
        }

        public Builder tools(int types, float speed, long durability, int quality) {
            tools = new MaterialToolProfile(types, speed, durability, quality);
            return this;
        }

        public Builder fuel(long ticks, String ash, long amount) {
            burnTime = ticks;
            burning = ash.isEmpty() ? null : target(ash, amount);
            return this;
        }

        public Builder crushing(String name, long amount) {
            crushing = target(name, amount);
            return this;
        }

        public Builder smelting(String name, long amount) {
            smelting = target(name, amount);
            return this;
        }

        public Builder pulverizing(String name, long amount) {
            pulverizing = target(name, amount);
            return this;
        }

        public Builder composition(long value, MaterialInfoDefinition.Part... parts) {
            divider = value;
            composition = List.of(parts);
            return this;
        }

        public Builder byproducts(String... names) {
            byproducts = List.of(names);
            return this;
        }

        public Builder multipliers(int ore, int processing) {
            oreMultiplier = ore;
            processingMultiplier = processing;
            return this;
        }

        public Builder forms(String... names) {
            forms = Set.of(names);
            return this;
        }

        public Builder classifications(String... names) {
            classifications = Set.of(names);
            return this;
        }

        public Builder aliases(String... names) {
            aliases = List.of(names);
            return this;
        }

        public Builder members(String... names) {
            members = List.of(names);
            return this;
        }

        public Builder processing(String kind, String name, long amount) {
            MaterialProcessKeys.legacyTarget(kind)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown processing target: " + kind));
            MaterialTarget target = target(name, amount);
            if (processing.putIfAbsent(kind, target) != null)
                throw new IllegalArgumentException("Duplicate processing target: " + kind);
            return this;
        }

        public Declaration build() {
            var processRules = new LinkedHashMap<MaterialProcessKey<?>, List<MaterialProcessRule<?>>>();
            if (!group) {
                if (smelting == null) smelting = target(name, gregtech.api.material.GTValues.U);
                if (crushing == null) crushing = target(name, gregtech.api.material.GTValues.U);
                if (pulverizing == null) pulverizing = target(name, gregtech.api.material.GTValues.U);
            }
            addTarget(processRules, MaterialProcessKeys.SMELTING, smelting);
            addTarget(processRules, MaterialProcessKeys.CRUSHING, crushing);
            addTarget(processRules, MaterialProcessKeys.PULVERIZING, pulverizing);
            if (burnTime > 0 || burning != null)
                addProcess(
                        processRules, MaterialProcessKeys.FUEL, new FuelRule(burnTime, Optional.ofNullable(burning)));
            if (!byproducts.isEmpty() || oreMultiplier != 1 || processingMultiplier != 1)
                addProcess(
                        processRules,
                        MaterialProcessKeys.ORE_PROCESSING,
                        new OreProcessingRule(
                                byproducts.stream().map(MaterialKey::new).toList(),
                                oreMultiplier,
                                processingMultiplier));
            processing.forEach((kind, target) -> addTarget(
                    processRules, MaterialProcessKeys.legacyTarget(kind).orElseThrow(), target));
            var material = new MaterialDefinition(
                    new Identity(id, name, localName, aliases),
                    new Presentation(displayName, color, texture, alpha, formula),
                    new PhysicalProperties(meltingPointKelvin, boilingPointKelvin, density, atoms),
                    properties,
                    new Source(sourceId, sourceName));
            var info = new MaterialInfoDefinition(
                    new MaterialInfoDefinition.Tooling(tools, corrected),
                    new MaterialInfoDefinition.Composition(divider, composition, false),
                    new MaterialInfoDefinition.Relations(group, forms, classifications, members),
                    new MaterialInfoDefinition.Processes(processRules));
            return new Declaration(material, info);
        }

        private static void addTarget(
                Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> rules,
                MaterialProcessKey<MaterialTarget> key,
                MaterialTarget target) {
            if (target != null) addProcess(rules, key, target);
        }

        private static <T> void addProcess(
                Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> rules, MaterialProcessKey<T> key, T value) {
            rules.computeIfAbsent(key, ignored -> new ArrayList<>()).add(MaterialProcessRule.anyForm(value));
        }
    }
}
