package gregtech.engine.material;

import gregtech.api.material.GTMaterial;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable associated declarations. Material references remain keys until the link pass. */
public record MaterialFacts(
        String localName,
        String sourceId,
        String sourceName,
        boolean group,
        boolean correctedToolStats,
        ToolStats tools,
        long burnTime,
        Target burning,
        Target crushing,
        Target smelting,
        Target pulverizing,
        long divider,
        List<Part> composition,
        List<String> byproducts,
        int oreMultiplier,
        int processingMultiplier,
        Set<String> forms,
        Set<String> classifications,
        List<String> aliases,
        List<String> members,
        Map<String, Target> processing) {
    public static final MaterialFacts EMPTY = builder().build();

    public MaterialFacts {
        Objects.requireNonNull(localName);
        Objects.requireNonNull(sourceId);
        Objects.requireNonNull(sourceName);
        Objects.requireNonNull(tools);
        composition = List.copyOf(composition);
        byproducts = List.copyOf(byproducts);
        forms = Set.copyOf(forms);
        classifications = Set.copyOf(classifications);
        aliases = List.copyOf(aliases);
        members = List.copyOf(members);
        processing = Map.copyOf(processing);
        if (burnTime < 0 || divider < 0 || (!composition.isEmpty() && divider == 0))
            throw new IllegalArgumentException("Invalid fuel time or composition divider");
        if (oreMultiplier < 1 || oreMultiplier > 127 || processingMultiplier < 1 || processingMultiplier > 127)
            throw new IllegalArgumentException("Ore multipliers must be 1..127");
        long total = 0;
        for (Part part : composition) total = Math.addExact(total, part.amount());
    }

    public record Part(String material, long amount) {
        public Part {
            requireKey(material);
            if (amount <= 0) throw new IllegalArgumentException("Component amount must be positive: " + material);
        }
    }

    /** Zero disables a process; quantities use material units, never item counts. */
    public record Target(String material, long amount) {
        public Target {
            requireKey(material);
            if (amount < 0) throw new IllegalArgumentException("Negative target amount: " + material);
        }
    }

    public record ToolStats(int types, float speed, long durability, int quality) {
        public ToolStats {
            if (types < 0 || !Float.isFinite(speed) || speed < 0 || durability < 0 || quality < 0)
                throw new IllegalArgumentException("Invalid tool stats");
        }

        public boolean matches(GTMaterial material) {
            return material.getToolTypes() == types
                    && material.getToolSpeed() == speed
                    && material.getToolDurability() == durability
                    && material.getToolQuality() == quality;
        }
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank() || !GTMaterial.sanitize(key).equals(key))
            throw new IllegalArgumentException("Expected canonical material key: " + key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String localName = "", sourceId = "", sourceName = "";
        private boolean group, corrected;
        private ToolStats tools = new ToolStats(0, 1F, 0, 0);
        private long burnTime, divider;
        private Target burning, crushing, smelting, pulverizing;
        private List<Part> composition = List.of();
        private List<String> byproducts = List.of(), aliases = List.of(), members = List.of();
        private int oreMultiplier = 1, processingMultiplier = 1;
        private Set<String> forms = Set.of();
        private Set<String> classifications = Set.of();
        private final Map<String, Target> processing = new java.util.LinkedHashMap<>();

        public Builder localName(String name) {
            localName = name;
            return this;
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
            tools = new ToolStats(types, speed, durability, quality);
            return this;
        }

        public Builder fuel(long ticks, String ash, long amount) {
            burnTime = ticks;
            burning = ash.isEmpty() ? null : new Target(ash, amount);
            return this;
        }

        public Builder crushing(String name, long amount) {
            crushing = new Target(name, amount);
            return this;
        }

        public Builder smelting(String name, long amount) {
            smelting = new Target(name, amount);
            return this;
        }

        public Builder pulverizing(String name, long amount) {
            pulverizing = new Target(name, amount);
            return this;
        }

        public Builder composition(long value, Part... parts) {
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
            if (!Set.of("Solidifying", "Bending", "Compressing", "Cutting", "Forging", "Smashing", "Working")
                    .contains(kind)) throw new IllegalArgumentException("Unknown processing target: " + kind);
            if (processing.putIfAbsent(kind, new Target(name, amount)) != null)
                throw new IllegalArgumentException("Duplicate processing target: " + kind);
            return this;
        }

        public MaterialFacts build() {
            return new MaterialFacts(
                    localName,
                    sourceId,
                    sourceName,
                    group,
                    corrected,
                    tools,
                    burnTime,
                    burning,
                    crushing,
                    smelting,
                    pulverizing,
                    divider,
                    composition,
                    byproducts,
                    oreMultiplier,
                    processingMultiplier,
                    forms,
                    classifications,
                    aliases,
                    members,
                    processing);
        }
    }
}
