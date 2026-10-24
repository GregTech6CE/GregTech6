package gregtech.registry;

import gregtech.block.misc.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Wave 48: N11 Decorative/Infrastructure blocks + N12 Placeables. */
public final class GTDecorBlocks {
    private static final List<RegistryObject<? extends Block>> ALL = new ArrayList<>();

    // N11: Asphalt
    public static RegistryObject<AsphaltBlock> ASPHALT;

    // N11: Concrete
    public static RegistryObject<ConcreteBlock> CONCRETE;
    public static RegistryObject<ConcreteBlock> CONCRETE_REINFORCED;

    // N11: CFoam
    public static RegistryObject<CFoamBlock> CFOAM;
    public static RegistryObject<gregtech.block.misc.CFoamSlabBlock> CFOAM_SLAB;
    public static RegistryObject<CFoamBlock> CFOAM_FRESH;

    // N11: Glass
    public static RegistryObject<ColoredGlassBlock> GLASS_CLEAR;
    public static RegistryObject<ColoredGlassBlock> GLASS_GLOW;

    // N11: Spikes (5)
    public static RegistryObject<Block> SPIKE_METAL;
    public static RegistryObject<Block> SPIKE_STEEL;
    public static RegistryObject<Block> SPIKE_SHARP;
    public static RegistryObject<Block> SPIKE_FANCY;
    public static RegistryObject<Block> SPIKE_SUPER;

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

    // N12: Piles (ingot/plate/gem/coin)
    public static RegistryObject<Block> INGOT_PILE;
    public static RegistryObject<Block> PLATE_PILE;
    public static RegistryObject<Block> PLATE_GEM_PILE;
    public static RegistryObject<Block> COIN_PILE;

    // N12: Misc placeables
    public static RegistryObject<Block> LOOT_CRATE;
    public static RegistryObject<Block> CRATE;
    /** GT6's book shelf (MultiTileEntityBookShelf, LoaderBookList). */
    public static RegistryObject<gregtech.block.BookShelfBlock> BOOKSHELF;
    private static final java.util.Map<String, RegistryObject<gregtech.block.BookShelfBlock>> BOOKSHELVES =
            new java.util.LinkedHashMap<>();
    public static RegistryObject<gregtech.block.FluidSpringBlock> FLUID_SPRING;
    public static RegistryObject<GregLanternBlock> GREG_LANTERN;
    public static RegistryObject<SandwichBlock> SANDWICH_BLOCK;

    /** GT6's wild bumblebee hive ({@code MultiTileEntityBumbleHive}, placed by {@code WorldgenHives}). */
    public static RegistryObject<BumbleHiveBlock> BUMBLE_HIVE;
    private static final java.util.Map<net.minecraft.world.item.DyeColor, RegistryObject<BumbleHiveBlock>> HIVE_VARIANTS = new java.util.EnumMap<>(net.minecraft.world.item.DyeColor.class);

    public static BumbleHiveBlock hive(net.minecraft.world.item.DyeColor color) { return HIVE_VARIANTS.get(color).get(); }

    public static Block[] allHives() {
        var blocks = new java.util.ArrayList<Block>();
        HIVE_VARIANTS.values().forEach(h -> blocks.add(h.get()));
        return blocks.toArray(Block[]::new);
    }

    private GTDecorBlocks() {}

    public static List<RegistryObject<? extends Block>> all() { return Collections.unmodifiableList(ALL); }

    public static java.util.Collection<RegistryObject<gregtech.block.BookShelfBlock>> allBookshelves() {
        return Collections.unmodifiableCollection(BOOKSHELVES.values());
    }

    public static RegistryObject<gregtech.block.BookShelfBlock> bookshelf(String path) {
        return BOOKSHELVES.get(path);
    }

    public static RegistryObject<gregtech.block.BookShelfBlock> bookshelf(int gt6Id) {
        var variant = gregtech.content.book.BookShelfVariants.byOriginalId(gt6Id);
        return variant == null ? null : bookshelf(variant.path());
    }

    private static <T extends Block> RegistryObject<T> reference(String id) {
        return RegistryObject.create(gregtech.GregTech.id(id),net.minecraftforge.registries.ForgeRegistries.BLOCKS);
    }
    private static void add(java.util.Collection<? extends RegisteredBlock<? extends Block>> blocks){
        blocks.forEach(block->ALL.add(reference(block.getId().getPath())));
    }
    public static RegistryObject<gregtech.block.misc.CFoamSlabBlock> GLOW_GLASS_SLAB;
    public static void registerAll(){
        GTConstructionBlocks.initialize();GTSpikes.initialize();GTBars.initialize();
        GTSurfaceBlocks.initializeDiggables();GTMaterialPiles.initialize();GTCoinPiles.initialize();
        GTBookShelves.initialize();SharedDecorBlocks.initialize();GTSandwich.initialize();GTBumbleBlocks.initializeHives();
        ASPHALT=reference(GTConstructionVariants.defaultId("asphalt"));
        CONCRETE=reference(GTConstructionVariants.defaultId("concrete"));
        CONCRETE_REINFORCED=reference(GTConstructionVariants.defaultId("concrete_reinforced"));
        CFOAM=reference(GTConstructionVariants.defaultId("cfoam"));
        CFOAM_FRESH=reference(GTConstructionVariants.defaultId("cfoam_fresh"));
        CFOAM_SLAB=reference(GTConstructionVariants.defaultId("cfoam_slab"));
        GLOW_GLASS_SLAB=reference(GTConstructionVariants.defaultId("glass_glow_slab"));
        GLASS_CLEAR=reference(GTConstructionVariants.defaultId("glass_clear"));
        GLASS_GLOW=reference(GTConstructionVariants.defaultId("glass_glow"));
        RAILROAD=reference("railroad");
        MUD=reference("mud");
        BROWN_CLAY=reference("clay_brown");
        TURF=reference("turf");
        RED_CLAY=reference("clay_red");
        YELLOW_CLAY=reference("clay_yellow");
        BLUE_CLAY=reference("clay_blue");
        WHITE_CLAY=reference("clay_white");
        INGOT_PILE=reference("ingot_pile");
        PLATE_PILE=reference("plate_pile");
        PLATE_GEM_PILE=reference("plate_gem_pile");
        COIN_PILE=reference("coin_pile");
        CRATE=reference("crate");
        LOOT_CRATE=reference("loot_crate");
        BOOKSHELF=reference("bookshelf");
        GREG_LANTERN=reference("greg_lantern");
        SANDWICH_BLOCK=reference("sandwich_block");
        BUMBLE_HIVE=reference("bumble_hive_light_gray");
        SPIKE_METAL=reference("spike_metal");
        SPIKE_STEEL=reference("spike_steel");
        SPIKE_SHARP=reference("spike_sharp");
        SPIKE_FANCY=reference("spike_fancy");
        SPIKE_SUPER=reference("spike_super");
        BARS_IRON=reference("bars_iron");
        BARS_STEEL=reference("bars_steel");
        BARS_BRASS=reference("bars_brass");
        BARS_BRONZE=reference("bars_bronze");
        BARS_WROUGHT_IRON=reference("bars_wrought_iron");
        BARS_STAINLESS=reference("bars_stainless");
        BARS_TUNGSTEN_STEEL=reference("bars_tungsten_steel");
        add(GTConstructionBlocks.all());add(GTSpikes.all());add(GTBars.all());add(GTMaterialPiles.all());add(GTBookShelves.all());add(SharedDecorBlocks.all());
        ALL.addAll(List.of(MUD,BROWN_CLAY,TURF,RED_CLAY,YELLOW_CLAY,BLUE_CLAY,WHITE_CLAY,COIN_PILE,SANDWICH_BLOCK));
        for(var entry:GTBookShelves.all())BOOKSHELVES.put(entry.getId().getPath(),reference(entry.getId().getPath()));
        for(var color:net.minecraft.world.item.DyeColor.values()){var hive=GTDecorBlocks.<BumbleHiveBlock>reference("bumble_hive_"+color.getName());HIVE_VARIANTS.put(color,hive);ALL.add(hive);}
        GTFluidSprings.initialize();FLUID_SPRING=GTFluidSprings.FLUID_SPRING;
    }
}
