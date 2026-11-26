package gregtech.content.worldgen;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.material.generated.OreMaterials;
import gregtech.content.worldgen.GTBedrockOres.BedrockOre;
import gregtech.content.worldgen.GTBedrockOres.Depth;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.GTOreVeins.OreVein;
import gregtech.content.worldgen.GTOreVeins.SmallOre;
import gregtech.content.worldgen.OreVeinLayout.RegionalPlan;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Linked world generation sources, grouped by the material they provide. */
public final class OreSourceCatalog {
    public enum Kind {
        VEIN,
        LAYER,
        CONTACT,
        SEAM,
        SMALL_ORE,
        BOUNDARY_GEM,
        BEDROCK,
        COLTAN_FIELD,
        COLTAN_BEDROCK,
        OCEAN_PYLON,
        DUNGEON_BEDROCK;

        public boolean specialDeposit() {
            return switch (this) {
                case COLTAN_FIELD, COLTAN_BEDROCK, OCEAN_PYLON, DUNGEON_BEDROCK -> true;
                default -> false;
            };
        }
    }

    public enum Geology {
        DISABLED,
        WITHOUT_VANILLA_ORES,
        WITH_VANILLA_ORES
    }

    public record Source(String dimension, Kind kind, String name, HeightRange heights, String blockId) {
        public boolean enabled(Geology geology) {
            if (kind == Kind.BEDROCK) return true;
            return geology != Geology.DISABLED
                    && (geology == Geology.WITH_VANILLA_ORES
                            || !blockId.startsWith("minecraft:")
                            || !blockId.endsWith("_ore"));
        }

        public HeightRange availableHeights(HeightRange terrain) {
            var primary = heights.intersect(terrain);
            return primary.isEmpty() && kind == Kind.VEIN
                    ? GTOreVeins.reserveHeights(dimension, name, terrain).orElse(primary)
                    : primary;
        }
    }

    private static final String OVERWORLD = OreVeinDeclaration.OVERWORLD;
    private static final String NETHER = OreVeinDeclaration.NETHER;
    private static final String END = OreVeinDeclaration.END;

    private static volatile OreSourceCatalog linked;

    final StratumOreRules.Catalog strata;
    private final Set<GTMaterial> oreForms;
    private final Map<GTMaterial, Set<Source>> sources;
    private final Map<String, List<BedrockOre>> bedrock;
    private final Map<String, List<OreVein>> veins;
    private final Map<String, List<RegionalPlan>> regionalPlans;
    private final Map<String, List<SmallOre>> smallOres;

    private OreSourceCatalog(Builder builder, Map<String, List<OreVein>> addonVeins, StratumOreRules.Catalog strata) {
        this.strata = strata;
        veins = freeze(builder.veins);
        var plans = new LinkedHashMap<String, List<RegionalPlan>>();
        veins.forEach((dimension, entries) -> plans.put(
                dimension,
                OreVeinLayout.regionalPlans(dimension, entries, addonVeins.getOrDefault(dimension, List.of()))));
        regionalPlans = Map.copyOf(plans);
        smallOres = freeze(builder.smallOres);
        oreForms = Collections.unmodifiableSet(builder.oreForms);
        builder.sources.replaceAll((material, entries) -> Collections.unmodifiableSet(entries));
        sources = Collections.unmodifiableMap(builder.sources);
        bedrock = freeze(builder.bedrock);
    }

    /** Available after the material definitions and their links have been installed. */
    public static OreSourceCatalog current() {
        OreSourceCatalog catalog = linked;
        if (catalog == null) throw new IllegalStateException("Ore sources have not been linked yet");
        return catalog;
    }

    static OreSourceCatalog installBuiltins() {
        GTMaterialRegistry.requireCatalogInstalled();
        var builtins = build(Map.of(), Map.of(), new StratumOreRules.Catalog());
        if (linked == null) linked = builtins;
        return builtins;
    }

    static void publish(
            OreSourceCatalog builtins, Map<String, List<OreVein>> veins, Map<String, List<SmallOre>> smallOres) {
        linked = veins.isEmpty() && smallOres.isEmpty() ? builtins : build(veins, smallOres, builtins.strata);
    }

    boolean hasOreForm(GTMaterial material) {
        return oreForms.contains(material);
    }

    /** Forms must exist even when the server disables their world generation. */
    public static void flagOreMaterials() {
        for (GTMaterial material : current().oreForms) {
            material.put(MaterialProperty.GENERATE_ORE, MaterialProperty.GENERATE_ORE_PROCESSING);
        }
    }

    public Set<GTMaterial> materialsWithSources() {
        return sources.keySet();
    }

    public Set<Source> sources(GTMaterial material) {
        return sources.getOrDefault(material.resolve(), Set.of());
    }

    public Set<String> veinDimensions() {
        return veins.keySet();
    }

    public List<OreVein> veins(String dimension) {
        return veins.getOrDefault(dimension, List.of());
    }

    public List<RegionalPlan> regionalPlans(String dimension) {
        return regionalPlans.getOrDefault(dimension, List.of());
    }

    public Set<String> smallOreDimensions() {
        return smallOres.keySet();
    }

    public List<SmallOre> smallOres(String dimension) {
        return smallOres.getOrDefault(dimension, List.of());
    }

    public Set<String> bedrockDimensions() {
        return bedrock.keySet();
    }

    public List<BedrockOre> bedrockOres(String dimension, Geology geology) {
        return bedrockOres(dimension, geology, defaultTerrain(dimension), source -> true);
    }

    public List<BedrockOre> bedrockOres(
            String dimension, Geology geology, HeightRange terrain, Predicate<Source> active) {
        var ores = bedrock.getOrDefault(dimension, List.of());
        if (!OVERWORLD.equals(dimension) || geology == Geology.DISABLED) return ores;
        return ores.stream()
                .map(ore -> applyDepth(ore, sources(ore.material()), geology, terrain, active))
                .toList();
    }

    public static HeightRange defaultTerrain(String dimension) {
        return switch (dimension) {
            case OVERWORLD -> new HeightRange(-64, 319);
            case NETHER, END -> new HeightRange(0, 127);
            default -> new HeightRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
        };
    }

    private static <T> Map<String, List<T>> freeze(Map<String, List<T>> buckets) {
        buckets.replaceAll((dimension, entries) -> List.copyOf(entries));
        return Collections.unmodifiableMap(buckets);
    }

    private static BedrockOre applyDepth(
            BedrockOre ore,
            Set<Source> ordinarySources,
            Geology geology,
            HeightRange terrain,
            Predicate<Source> active) {
        int depthBalance = 0;
        for (Source source : ordinarySources) {
            if (!source.dimension().equals(OVERWORLD)
                    || !source.enabled(geology)
                    || source.kind() == Kind.BOUNDARY_GEM
                    || source.kind() == Kind.BEDROCK
                    || source.kind().specialDeposit()
                    || !active.test(source)) continue;
            var heights = source.availableHeights(terrain);
            if (heights.isEmpty()) continue;
            int minY = source.kind() == Kind.SEAM ? MineralWorldgenRules.HEIGHT_ANCHOR_Y : heights.min();
            if (minY < 0) depthBalance--;
            else if (minY >= MineralWorldgenRules.HEIGHT_ANCHOR_Y) depthBalance++;
        }
        if (depthBalance > 0) {
            return new BedrockOre(ore.name(), ore.material(), ore.chance() * 5 / 4, ore.flowerId(), Depth.SHALLOW);
        }
        if (depthBalance < 0) {
            return new BedrockOre(ore.name(), ore.material(), ore.chance() * 3 / 4, ore.flowerId(), Depth.DEEP);
        }
        return ore;
    }

    private static OreSourceCatalog build(
            Map<String, List<OreVein>> addonVeins,
            Map<String, List<SmallOre>> addonSmallOres,
            StratumOreRules.Catalog strata) {
        var builder = new Builder();
        for (OreVein vein : GTOreVeins.OVERWORLD_VEINS) builder.vein(OVERWORLD, vein);
        for (OreVein vein : GTOreVeins.END_VEINS) builder.vein(END, vein);
        addonVeins.forEach((dimension, rows) -> rows.forEach(vein -> builder.vein(dimension, vein)));
        GTOreVeins.OVERWORLD_SMALL_ORES.forEach(ore -> builder.smallOre(OVERWORLD, ore));
        GTOreVeins.NETHER_SMALL_ORES.forEach(ore -> builder.smallOre(NETHER, ore));
        GTOreVeins.END_SMALL_ORES.forEach(ore -> builder.smallOre(END, ore));
        addonSmallOres.forEach((dimension, rows) -> rows.forEach(ore -> builder.smallOre(dimension, ore)));

        for (var layer : StoneLayerField.layers()) {
            if (layer.isOreSeam()) {
                int minY = layer.noDeep() ? StoneLayerField.SHALLOW_LAYER_MIN_Y : -64;
                builder.source(
                        GTMaterialRegistry.get(layer.material()),
                        new Source(
                                OVERWORLD, Kind.SEAM, layer.material(), new HeightRange(minY, 319), layer.blockId()));
            }
            for (var ore : layer.ores()) builder.stratum(Kind.LAYER, layer.material(), ore, layer.noDeep());
        }
        for (var contact : GTStoneLayersGen.CONTACTS) {
            String name = contact.materialA() + "/" + contact.materialB();
            for (var ore : contact.ores()) builder.stratum(Kind.CONTACT, name, ore, false);
        }
        var gems = new Source(OVERWORLD, Kind.BOUNDARY_GEM, "boundary_gems", new HeightRange(-64, 319), "");
        BoundaryGemOres.MATERIALS.forEach(material -> builder.source(material, gems));
        builder.deriveOverworldBedrock();
        GTBedrockOres.registerNether(builder);
        builder.specialDeposits();
        return new OreSourceCatalog(builder, addonVeins, strata);
    }

    static final class Builder {
        private final Set<GTMaterial> oreForms = new LinkedHashSet<>();
        private final Map<GTMaterial, Set<Source>> sources = new LinkedHashMap<>();
        private final Map<String, List<BedrockOre>> bedrock = new LinkedHashMap<>();
        private final Map<String, List<OreVein>> veins = new LinkedHashMap<>();
        private final Map<String, List<SmallOre>> smallOres = new LinkedHashMap<>();
        private final Map<GTMaterial, Integer> primaryWeights = new LinkedHashMap<>();

        private void deriveOverworldBedrock() {
            var candidates = new LinkedHashSet<>(primaryWeights.keySet());
            candidates.addAll(GTBedrockOres.historicalMaterials());
            for (var material : candidates.stream()
                    .sorted(Comparator.comparing(GTMaterial::getName))
                    .toList()) {
                for (var option : GTBedrockOres.overworld(material, primaryWeights.getOrDefault(material, 0))) {
                    bedrock(OVERWORLD, option.name(), material, option.divisor(), option.flower());
                }
            }
        }

        private void vein(String dimension, OreVein vein) {
            veins.computeIfAbsent(dimension, key -> new ArrayList<>()).add(vein);
            if (OVERWORLD.equals(dimension)) {
                primaryWeights.merge(vein.top().resolve(), vein.weight(), Math::max);
                primaryWeights.merge(vein.bottom().resolve(), vein.weight(), Math::max);
            }
            var heights = vein.heights(dimension);
            var source = new Source(dimension, Kind.VEIN, vein.name(), heights, "");
            for (GTMaterial material : List.of(vein.top(), vein.bottom(), vein.between(), vein.spread())) {
                source(material, source);
            }
        }

        private void smallOre(String dimension, SmallOre ore) {
            smallOres.computeIfAbsent(dimension, key -> new ArrayList<>()).add(ore);
            var heights = ore.heights(dimension, Integer.MIN_VALUE, Integer.MAX_VALUE);
            source(ore.material(), new Source(dimension, Kind.SMALL_ORE, ore.name(), heights, ""));
        }

        private void stratum(Kind kind, String host, GTStoneLayersGen.OreDef ore, boolean shallow) {
            if (!ore.providesOre()) return;
            int min = MineralWorldgenRules.stratumOreMinY(ore);
            if (shallow) min = Math.max(min, StoneLayerField.SHALLOW_LAYER_MIN_Y);
            var heights = new HeightRange(min, MineralWorldgenRules.stratumOreMaxY(ore));
            if (heights.isEmpty() || ore.chanceDiv() <= 0) {
                throw new IllegalStateException("Invalid stratum ore range or chance: " + host + "/" + ore.material());
            }
            source(GTMaterialRegistry.get(ore.material()), new Source(OVERWORLD, kind, host, heights, ore.blockId()));
        }

        private void specialDeposits() {
            var field = new Source(OVERWORLD, Kind.COLTAN_FIELD, "coltan_field", SpecialOreDeposits.COLTAN_HEIGHTS, "");
            SpecialOreDeposits.COLTAN_ROLLS.forEach(material -> source(material, field));
            var floor = new HeightRange(-64, -64);
            source(OreMaterials.Coltan, new Source(OVERWORLD, Kind.COLTAN_BEDROCK, "coltan_centre", floor, ""));
            for (var pylon : SpecialOreDeposits.OCEAN_PYLONS) {
                source(
                        pylon.material(),
                        new Source(
                                OVERWORLD,
                                Kind.OCEAN_PYLON,
                                pylon.stone().registryId(),
                                SpecialOreDeposits.OCEAN_ORE_HEIGHTS,
                                ""));
            }
            var dungeon = new Source(OVERWORLD, Kind.DUNGEON_BEDROCK, "dungeon_mine", floor, "");
            SpecialOreDeposits.DUNGEON_ROLLS.forEach(material -> source(material, dungeon));
        }

        void bedrock(String dimension, String name, GTMaterial material, int chance, String flower) {
            GTMaterial resolved = validMaterial(material, name);
            int floor = OVERWORLD.equals(dimension) ? -64 : 0;
            source(resolved, new Source(dimension, Kind.BEDROCK, name, new HeightRange(floor, floor), ""));
            bedrock.computeIfAbsent(dimension, key -> new ArrayList<>())
                    .add(new BedrockOre(name, resolved, chance, flower));
        }

        private void source(GTMaterial material, Source source) {
            GTMaterial resolved = validMaterial(material, source.name());
            if (source.kind() != Kind.SEAM) oreForms.add(resolved);
            sources.computeIfAbsent(resolved, key -> new LinkedHashSet<>()).add(source);
        }

        private static GTMaterial validMaterial(GTMaterial material, String source) {
            GTMaterial resolved = material.resolve();
            if (!resolved.isValid())
                throw new IllegalStateException("Invalid ore material for worldgen source " + source);
            return resolved;
        }
    }
}
