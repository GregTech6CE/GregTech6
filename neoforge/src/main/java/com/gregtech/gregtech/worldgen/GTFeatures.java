package com.gregtech.gregtech.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Original ore-vein/small-ore/stone-layer features; other feature modules await source migration. */
public final class GTFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, "gregtech");
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ORE_VEIN =
            FEATURES.register("gt_ore_vein", GTOreVeinFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SMALL_ORES =
            FEATURES.register("gt_small_ores", GTSmallOreFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> STONE_LAYERS =
            FEATURES.register("gt_stone_layers", GTStoneLayerFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> TREES=FEATURES.register("gt_trees",GTTreesFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> BUMBLE_HIVES=FEATURES.register("gt_bumble_hives",GTBumbleHivesFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> BUSHES=FEATURES.register("gt_bushes",GTBushesFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> SURFACE_FLORA=FEATURES.register("gt_surface_flora",GTSurfaceFloraFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> PITS=FEATURES.register("gt_pits",GTPitFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> TURF=FEATURES.register("gt_turf",GTTurfFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> SURFACE_DEPOSITS=FEATURES.register("gt_surface_deposits",GTSurfaceDepositFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> WATER_BODIES=FEATURES.register("gt_water_bodies",GTWaterBodyFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> ROCKS=FEATURES.register("gt_rocks",GTRocksFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> BEDROCK_ORES=FEATURES.register("gt_bedrock_ores",GTBedrockOreFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> COLTAN=FEATURES.register("gt_coltan",GTColtanFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> DEEP_OCEAN=FEATURES.register("gt_deep_ocean",GTDeepOceanFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> NETHER_SCATTER=FEATURES.register("gt_nether_scatter",GTNetherScatterFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> BLACK_SAND=FEATURES.register("gt_black_sand",GTBlackSandFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> NETHER_DEPOSITS=FEATURES.register("gt_nether_deposits",GTNetherDepositFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> FLUID_SPRINGS=FEATURES.register("gt_fluid_springs",GTFluidSpringsFeature::new);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> DUNGEON=FEATURES.register("gt_dungeon",GTDungeonFeature::new);
    private GTFeatures() {}
    public static void register(IEventBus bus) { GTStructures.register(bus); FEATURES.register(bus); }
}
