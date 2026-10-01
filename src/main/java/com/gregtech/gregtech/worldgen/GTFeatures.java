package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.GregTech;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Feature-type registry for GT6 worldgen.
 *
 * <p>WIRING: call {@code GTFeatures.register(modEventBus)} from the {@code GregTech} mod
 * constructor (alongside the other DeferredRegister registrations). The configured/placed
 * features and the overworld biome modifier are data-defined under
 * {@code data/gregtech/worldgen/} and {@code data/gregtech/forge/biome_modifier/}.
 *
 * <p>This is also where {@link GTStructures} is registered: the dungeon's structure type, its placement
 * type and its anchor piece ({@code ServerLevel.findNearestMapStructure} and {@code /locate structure
 * gregtech:gt_dungeon} need those, while the dungeon itself stays this feature, see
 * {@link GTDungeonStructure}).</p>
 */
public final class GTFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, GregTech.NAMESPACE);

    /** Large layered ore veins (GT6 {@code WorldgenOresLarge}). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ORE_VEIN =
            FEATURES.register("gt_ore_vein", GTOreVeinFeature::new);

    /** Scattered single small-ore blocks (GT6 {@code WorldgenOresSmall}). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SMALL_ORES =
            FEATURES.register("gt_small_ores", GTSmallOreFeature::new);

    /** Stone strata layers with embedded and contact ores (GT6 {@code WorldgenStoneLayers}). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> STONE_LAYERS =
            FEATURES.register("gt_stone_layers", GTStoneLayerFeature::new);

    /** Colored clay seams on the surface near water (GT6's small clay seeds; pits are {@link #PITS}). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SURFACE_DEPOSITS =
            FEATURES.register("gt_surface_deposits", GTSurfaceDepositFeature::new);

    /** Nether red clay bands + glowstone-style crystal ore clusters (GT6 nether worldgen). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> NETHER_DEPOSITS =
            FEATURES.register("gt_nether_deposits", GTNetherDepositFeature::new);

    /** Bedrock-level ore deposits with surface indicator flowers (GT6 WorldgenOresBedrock). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BEDROCK_ORES =
            FEATURES.register("gt_bedrock_ores", GTBedrockOreFeature::new);

    /** GT6's own trees (rubber/maple/willow/blue mahoe/hazel/cinnamon/coconut/blue spruce). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TREES =
            FEATURES.register("gt_trees", GTTreesFeature::new);

    /** GT6's forest floor: fallen logs, twigs and glowtus (WorldgenLog*, WorldgenSticks, WorldgenGlowtus). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SURFACE_FLORA =
            FEATURES.register("gt_surface_flora", GTSurfaceFloraFeature::new);

    /** GT6's berry bushes (WorldgenBushes, Loader_Worldgen:633). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BUSHES =
            FEATURES.register("gt_bushes", GTBushesFeature::new);

    /** GT6's surface rock litter (WorldgenRocks, Loader_Worldgen:618). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ROCKS =
            FEATURES.register("gt_rocks", GTRocksFeature::new);

    /** GT6's deep ocean prismarine pylons (WorldgenDeepOcean, Loader_Worldgen:580). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> DEEP_OCEAN =
            FEATURES.register("gt_deep_ocean", GTDeepOceanFeature::new);

    /** GT6's ocean/river/swamp water (WorldgenOcean/River/Swamp, Loader_Worldgen:576-578). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> WATER_BODIES =
            FEATURES.register("gt_water_bodies", GTWaterBodyFeature::new);

    /** GT6's river black sands (WorldgenBlackSand, Loader_Worldgen:581). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BLACK_SAND =
            FEATURES.register("gt_black_sand", GTBlackSandFeature::new);

    /** GT6's swamp turf bogs (WorldgenTurf, Loader_Worldgen:582). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TURF =
            FEATURES.register("gt_turf", GTTurfFeature::new);

    /** GT6's large sand and clay pits (WorldgenPit, Loader_Worldgen:592-597). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PITS =
            FEATURES.register("gt_pits", GTPitFeature::new);

    /** GT6's coltan field (WorldgenColtan, Loader_Worldgen:779). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> COLTAN =
            FEATURES.register("gt_coltan", GTColtanFeature::new);

    /** GT6's fluid springs at bedrock (WorldgenFluidSpring, Loader_Worldgen:782-797). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FLUID_SPRINGS =
            FEATURES.register("gt_fluid_springs", GTFluidSpringsFeature::new);

    /** GT6's loose items lying around in the Nether (WorldgenRacks, Loader_Worldgen:619). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> NETHER_SCATTER =
            FEATURES.register("gt_nether_scatter", GTNetherScatterFeature::new);

    /** GT6's wild bumblebee hives (WorldgenHives, Loader_Worldgen:652). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BUMBLE_HIVES =
            FEATURES.register("gt_bumble_hives", GTBumbleHivesFeature::new);

    /** GT6's large underground dungeon (WorldgenDungeonGT, Loader_Worldgen:652). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> DUNGEON =
            FEATURES.register("gt_dungeon", GTDungeonFeature::new);

    private GTFeatures() {}

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        // The dungeon's structure, its placement and its anchor piece: vanilla's /locate and the structure
        // system read those, the dungeon's blocks still come from DUNGEON above.
        GTStructures.register(modEventBus);
    }
}
