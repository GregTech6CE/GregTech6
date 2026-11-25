package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.content.worldgen.GTBedrockOres.BedrockOre;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.OreSourceCatalog.Kind;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

/** The active ore features and their terrain envelopes for the recipe viewers. */
public record OreGenerationSummary(
        Map<String, Dimension> dimensions,
        int boundaryGemDivisor,
        Map<String, BedrockTable> bedrock,
        int bedrockDensity,
        Set<String> disabledStratumBlocks,
        Map<Kind, HeightRange> specialDeposits) {
    private static final Map<ChunkGenerator, Features> FEATURES = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile OreGenerationSummary clientSummary;

    public OreGenerationSummary {
        dimensions = Collections.unmodifiableMap(new LinkedHashMap<>(dimensions));
        bedrock = Collections.unmodifiableMap(new LinkedHashMap<>(bedrock));
        disabledStratumBlocks = Set.copyOf(disabledStratumBlocks);
        specialDeposits = Map.copyOf(specialDeposits);
    }

    public record Dimension(HeightRange heights, boolean veins, boolean smallOres, boolean strata) {
        boolean hasSources() {
            return veins || smallOres || strata;
        }

        boolean supports(Kind kind) {
            return switch (kind) {
                case VEIN -> veins;
                case SMALL_ORE -> smallOres;
                case LAYER, CONTACT, SEAM, BOUNDARY_GEM -> strata;
                default -> false;
            };
        }
    }

    private record Features(
            boolean veins, boolean smallOres, boolean strata, boolean bedrock, boolean coltan, boolean oceanPylons) {
        private static Features read(ChunkGenerator generator) {
            boolean veins = false, smallOres = false, strata = false, bedrock = false, coltan = false, pylons = false;
            for (var biome : generator.getBiomeSource().possibleBiomes()) {
                for (var step : generator.getBiomeGenerationSettings(biome).features()) {
                    for (var holder : step) {
                        var feature = holder.value().feature().value().feature();
                        veins |= feature instanceof GTOreVeinFeature;
                        smallOres |= feature instanceof GTSmallOreFeature;
                        strata |= feature instanceof GTStoneLayerFeature;
                        bedrock |= feature instanceof GTBedrockOreFeature;
                        coltan |= feature instanceof GTColtanFeature;
                        pylons |= feature instanceof GTDeepOceanFeature;
                    }
                }
            }
            return new Features(veins, smallOres, strata, bedrock, coltan, pylons);
        }

        private Dimension ordinary(HeightRange heights, boolean geology) {
            return new Dimension(
                    heights,
                    geology && veins,
                    geology && smallOres,
                    geology && GTWorldgenDefaults.STONE_LAYERS && strata);
        }
    }

    public record BedrockTable(int floor, Map<String, Integer> divisors) {
        public BedrockTable {
            divisors = Collections.unmodifiableMap(new LinkedHashMap<>(divisors));
        }

        private static BedrockTable from(int floor, List<BedrockOre> ores) {
            var divisors = new LinkedHashMap<String, Integer>();
            for (var ore : ores) divisors.put(ore.name(), ore.chance());
            return new BedrockTable(floor, divisors);
        }
    }

    public static HeightRange terrainHeights(LevelHeightAccessor level, ChunkGenerator generator) {
        var build = new HeightRange(level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        if (!(generator instanceof NoiseBasedChunkGenerator)) return build;
        int floor = generator.getMinY();
        return build.intersect(new HeightRange(floor, floor + generator.getGenDepth() - 1));
    }

    static Dimension ordinarySources(LevelHeightAccessor level, ChunkGenerator generator) {
        if (!GregTechConfig.Worldgen.GEOLOGY.enabled()) return null;
        return FEATURES.computeIfAbsent(generator, Features::read).ordinary(terrainHeights(level, generator), true);
    }

    public static OreGenerationSummary capture(MinecraftServer server) {
        int divisor = GregTechConfig.Number.BOUNDARY_GEM_PROBABILITY.value();
        boolean geology = GregTechConfig.Worldgen.GEOLOGY.enabled();
        boolean dungeonMines = GregTechConfig.Worldgen.DUNGEONS.enabled() && GTDungeonGenerator.miningBedrockEnabled();
        var dimensions = new LinkedHashMap<String, Dimension>();
        var bedrock = new LinkedHashMap<String, BedrockTable>();
        int density = 0;
        Map<Kind, HeightRange> special = Map.of();
        for (var level : server.getAllLevels()) {
            String name = level.dimension().location().toString();
            boolean overworld = level.dimension() == Level.OVERWORLD;
            var generator = level.getChunkSource().getGenerator();
            var heights = terrainHeights(level, generator);
            if (heights.isEmpty()) continue;
            var features = FEATURES.computeIfAbsent(generator, Features::read);
            var ordinary = features.ordinary(heights, geology);
            if (ordinary.hasSources()) dimensions.put(name, ordinary);
            int floor = level.getMinBuildHeight();
            if (features.bedrock() && heights.min() == floor) {
                var generation = GTBedrockOreDimensions.generation(name, ordinary);
                if (!generation.ores().isEmpty()) {
                    bedrock.put(name, BedrockTable.from(floor, generation.ores()));
                    density = generation.density();
                }
            }
            if (overworld) {
                boolean deepOcean = geology
                        && features.oceanPylons()
                        && generator.getBiomeSource().possibleBiomes().stream()
                                .anyMatch(biome -> biome.is(Biomes.DEEP_OCEAN));
                boolean dungeon = dungeonMines
                        && level.structureManager().shouldGenerateStructures()
                        && level.getChunkSource().getGeneratorState().possibleStructureSets().stream()
                                .flatMap(set -> set.value().structures().stream())
                                .anyMatch(entry -> entry.structure().value() instanceof GTDungeonStructure);
                special = specialDeposits(
                        heights,
                        new HeightRange(floor, level.getMaxBuildHeight() - 1),
                        geology && features.coltan(),
                        deepOcean,
                        GregTechConfig.Worldgen.BEDROCK_ORES.enabled(),
                        dungeon);
            }
        }
        return new OreGenerationSummary(
                dimensions, divisor, bedrock, density, StoneLayerOrePlacement.disabledOutputs(), special);
    }

    public boolean includes(OreSourceCatalog.Source source) {
        if (source.kind().specialDeposit()) return specialDeposits.containsKey(source.kind());
        if (source.kind() == OreSourceCatalog.Kind.BEDROCK) {
            var table = bedrock.get(source.dimension());
            return table != null && table.divisors().containsKey(source.name());
        }
        var dimension = dimensions.get(source.dimension());
        return dimension != null
                && dimension.supports(source.kind())
                && !source.availableHeights(dimension.heights()).isEmpty()
                && !disabledStratumBlocks.contains(source.blockId());
    }

    private static Map<Kind, HeightRange> specialDeposits(
            HeightRange terrain,
            HeightRange build,
            boolean coltan,
            boolean deepOcean,
            boolean coltanBedrock,
            boolean dungeonMines) {
        var out = new EnumMap<Kind, HeightRange>(Kind.class);
        boolean hasBedrockFloor = terrain.min() == build.min();
        var floor = new HeightRange(build.min(), build.min());
        if (coltan) {
            var band = SpecialOreDeposits.COLTAN_HEIGHTS.intersect(terrain);
            if (!band.isEmpty()) out.put(Kind.COLTAN_FIELD, band);
            if (hasBedrockFloor && coltanBedrock) out.put(Kind.COLTAN_BEDROCK, floor);
        }
        if (deepOcean) {
            var centres = new HeightRange(SpecialOreDeposits.OCEAN_CENTER_MIN, SpecialOreDeposits.OCEAN_CENTER_MAX)
                    .intersect(terrain);
            if (!centres.isEmpty()) {
                out.put(
                        Kind.OCEAN_PYLON,
                        new HeightRange(centres.min(), centres.max() + SpecialOreDeposits.OCEAN_PYLON_REACH)
                                .intersect(build));
            }
        }
        if (hasBedrockFloor && dungeonMines) out.put(Kind.DUNGEON_BEDROCK, floor);
        return out;
    }

    public static void acceptClientSummary(OreGenerationSummary summary) {
        clientSummary = summary;
    }

    /** Uses the connected world's summary when available and a catalog preview otherwise. */
    public static OreGenerationSummary current() {
        var installed = clientSummary;
        if (installed != null) return installed;
        var overworldHeights = OreSourceCatalog.defaultTerrain(OreVeinDeclaration.OVERWORLD);
        var special = specialDeposits(overworldHeights, overworldHeights, true, true, true, true);
        var catalog = OreSourceCatalog.current();
        var names = new LinkedHashSet<>(catalog.veinDimensions());
        names.addAll(catalog.smallOreDimensions());
        var dimensions = new LinkedHashMap<String, Dimension>();
        for (String name : names) {
            dimensions.put(
                    name,
                    new Dimension(
                            OreSourceCatalog.defaultTerrain(name),
                            catalog.veinDimensions().contains(name),
                            catalog.smallOreDimensions().contains(name),
                            GTWorldgenDefaults.STONE_LAYERS && OreVeinDeclaration.OVERWORLD.equals(name)));
        }
        var bedrock = new LinkedHashMap<String, BedrockTable>();
        for (String name : catalog.bedrockDimensions()) {
            var ores = catalog.bedrockOres(name, OreSourceCatalog.Geology.WITH_VANILLA_ORES);
            bedrock.put(
                    name,
                    BedrockTable.from(OreSourceCatalog.defaultTerrain(name).min(), ores));
        }
        return new OreGenerationSummary(
                dimensions,
                BoundaryGemOres.CHANCE_DIVISOR,
                bedrock,
                GTWorldgenDefaults.BEDROCK_DENSITY_PERCENT,
                Set.of(),
                special);
    }
}
