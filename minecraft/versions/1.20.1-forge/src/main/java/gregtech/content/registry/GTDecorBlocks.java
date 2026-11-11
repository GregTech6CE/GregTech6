package gregtech.content.registry;

import gregtech.content.block.misc.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

/** Wave 48: N11 Decorative/Infrastructure blocks + N12 Placeables. */
public final class GTDecorBlocks {
    // N11: Asphalt
    public static RegistryObject<AsphaltBlock> ASPHALT;

    // N11: Concrete
    public static RegistryObject<ConcreteBlock> CONCRETE;
    public static RegistryObject<ConcreteBlock> CONCRETE_REINFORCED;

    // N11: CFoam
    public static RegistryObject<CFoamBlock> CFOAM;
    public static RegistryObject<gregtech.content.block.misc.CFoamSlabBlock> CFOAM_SLAB;
    public static RegistryObject<CFoamBlock> CFOAM_FRESH;

    // N11: Glass
    public static RegistryObject<ColoredGlassBlock> GLASS_CLEAR;
    public static RegistryObject<ColoredGlassBlock> GLASS_GLOW;

    // N11: Spikes (5)

    // N11: GT6 four-segment bars (Brass and Steel occur in bedrock dungeons).
    public static RegistryObject<BarsBlock> BARS_IRON;
    public static RegistryObject<BarsBlock> BARS_STEEL;
    public static RegistryObject<BarsBlock> BARS_BRASS;
    public static RegistryObject<BarsBlock> BARS_BRONZE;
    public static RegistryObject<BarsBlock> BARS_WROUGHT_IRON;
    public static RegistryObject<BarsBlock> BARS_STAINLESS;
    public static RegistryObject<BarsBlock> BARS_TUNGSTEN_STEEL;

    // N11: Railroad
    public static RegistryObject<RoadStripeRailBlock> RAILROAD;

    // GT6 BlockDiggable metadata 0..6, registered once by the shared diggable catalog.
    public static RegistryObject<DiggableBlock> MUD;
    public static RegistryObject<DiggableBlock> BROWN_CLAY;
    public static RegistryObject<DiggableBlock> TURF;
    public static RegistryObject<DiggableBlock> RED_CLAY;
    public static RegistryObject<DiggableBlock> YELLOW_CLAY;
    public static RegistryObject<DiggableBlock> BLUE_CLAY;
    public static RegistryObject<DiggableBlock> WHITE_CLAY;

    // N12: Misc placeables
    public static RegistryObject<Block> LOOT_CRATE;
    public static RegistryObject<Block> CRATE;
    public static gregtech.content.registry.RegisteredBlock<gregtech.content.block.FluidSpringBlock> FLUID_SPRING;
    public static RegistryObject<GregLanternBlock> GREG_LANTERN;

    public static RegistryObject<gregtech.content.block.misc.CFoamSlabBlock> GLOW_GLASS_SLAB;

    private GTDecorBlocks() {}

    private static <T extends Block> RegistryObject<T> reference(String id) {
        return RegistryObject.create(gregtech.GregTech.id(id), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
    }

    public static void registerAll() {
        ASPHALT = reference(GTConstructionVariants.defaultId("asphalt"));
        CONCRETE = reference(GTConstructionVariants.defaultId("concrete"));
        CONCRETE_REINFORCED = reference(GTConstructionVariants.defaultId("concrete_reinforced"));
        CFOAM = reference(GTConstructionVariants.defaultId("cfoam"));
        CFOAM_FRESH = reference(GTConstructionVariants.defaultId("cfoam_fresh"));
        CFOAM_SLAB = reference(GTConstructionVariants.defaultId("cfoam_slab"));
        GLOW_GLASS_SLAB = reference(GTConstructionVariants.defaultId("glass_glow_slab"));
        GLASS_CLEAR = reference(GTConstructionVariants.defaultId("glass_clear"));
        GLASS_GLOW = reference(GTConstructionVariants.defaultId("glass_glow"));
        RAILROAD = reference("railroad");
        MUD = reference("mud");
        BROWN_CLAY = reference("clay_brown");
        TURF = reference("turf");
        RED_CLAY = reference("clay_red");
        YELLOW_CLAY = reference("clay_yellow");
        BLUE_CLAY = reference("clay_blue");
        WHITE_CLAY = reference("clay_white");
        CRATE = reference("crate");
        LOOT_CRATE = reference("loot_crate");
        GREG_LANTERN = reference("greg_lantern");
        BARS_IRON = reference("bars_iron");
        BARS_STEEL = reference("bars_steel");
        BARS_BRASS = reference("bars_brass");
        BARS_BRONZE = reference("bars_bronze");
        BARS_WROUGHT_IRON = reference("bars_wrought_iron");
        BARS_STAINLESS = reference("bars_stainless");
        BARS_TUNGSTEN_STEEL = reference("bars_tungsten_steel");
        FLUID_SPRING = GTFluidSprings.FLUID_SPRING;
    }
}
