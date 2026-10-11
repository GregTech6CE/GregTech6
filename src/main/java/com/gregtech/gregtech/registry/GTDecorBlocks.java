package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.misc.*;
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
    public static RegistryObject<com.gregtech.gregtech.block.misc.CFoamSlabBlock> CFOAM_SLAB;
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

    // GT6 BlockDiggable metadata 0..6, with two older port IDs retained for existing test worlds.
    public static RegistryObject<DiggableBlock> MUD;
    public static RegistryObject<DiggableBlock> BROWN_CLAY;
    public static RegistryObject<DiggableBlock> TURF;
    public static RegistryObject<DiggableBlock> RED_CLAY;
    public static RegistryObject<DiggableBlock> YELLOW_CLAY;
    public static RegistryObject<DiggableBlock> BLUE_CLAY;
    public static RegistryObject<DiggableBlock> WHITE_CLAY;
    public static RegistryObject<DiggableBlock> DIGGABLE_CLAY;
    public static RegistryObject<DiggableBlock> DIGGABLE_PEAT;

    // N12: Piles (ingot/plate/gem/coin)
    public static RegistryObject<Block> INGOT_PILE;
    public static RegistryObject<Block> PLATE_PILE;
    public static RegistryObject<Block> PLATE_GEM_PILE;
    public static RegistryObject<Block> COIN_PILE;

    // N12: Misc placeables
    public static RegistryObject<Block> LOOT_CRATE;
    public static RegistryObject<Block> CRATE;
    /** GT6's book shelf (MultiTileEntityBookShelf, LoaderBookList). */
    public static RegistryObject<com.gregtech.gregtech.block.BookShelfBlock> BOOKSHELF;
    private static final java.util.Map<String, RegistryObject<com.gregtech.gregtech.block.BookShelfBlock>> BOOKSHELVES =
            new java.util.LinkedHashMap<>();
    public static RegistryObject<com.gregtech.gregtech.block.FluidSpringBlock> FLUID_SPRING;
    public static RegistryObject<GregLanternBlock> GREG_LANTERN;
    public static RegistryObject<SandwichBlock> SANDWICH_BLOCK;

    /** GT6's wild bumblebee hive ({@code MultiTileEntityBumbleHive}, placed by {@code WorldgenHives}). */
    public static RegistryObject<BumbleHiveBlock> BUMBLE_HIVE;
    private static final java.util.Map<net.minecraft.world.item.DyeColor, RegistryObject<BumbleHiveBlock>> HIVE_VARIANTS = new java.util.EnumMap<>(net.minecraft.world.item.DyeColor.class);

    public static BumbleHiveBlock hive(net.minecraft.world.item.DyeColor color) { return HIVE_VARIANTS.get(color).get(); }

    public static Block[] allHives() {
        var blocks = new java.util.ArrayList<Block>();
        blocks.add(BUMBLE_HIVE.get());
        HIVE_VARIANTS.values().forEach(h -> blocks.add(h.get()));
        return blocks.toArray(Block[]::new);
    }

    private GTDecorBlocks() {}

    public static List<RegistryObject<? extends Block>> all() { return Collections.unmodifiableList(ALL); }

    public static java.util.Collection<RegistryObject<com.gregtech.gregtech.block.BookShelfBlock>> allBookshelves() {
        return Collections.unmodifiableCollection(BOOKSHELVES.values());
    }

    public static RegistryObject<com.gregtech.gregtech.block.BookShelfBlock> bookshelf(String path) {
        return BOOKSHELVES.get(path);
    }

    public static RegistryObject<com.gregtech.gregtech.block.BookShelfBlock> bookshelf(int gt6Id) {
        var variant = com.gregtech.gregtech.content.book.BookShelfVariants.byOriginalId(gt6Id);
        return variant == null ? null : bookshelf(variant.path());
    }

    private static <T extends Block> RegistryObject<T> reg(String id, Supplier<T> blockSupplier) {
        RegistryObject<T> ro = GTBlocks.BLOCKS.register(id, blockSupplier);
        ALL.add(ro);
        GTBlocks.BLOCK_ITEMS.register(id, () -> ro.get() instanceof com.gregtech.gregtech.block.misc.ColoredConstructionBlock
                ? new com.gregtech.gregtech.block.misc.ColoredConstructionBlockItem(ro.get(), new Item.Properties())
                : new BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    private static RegistryObject<ConcreteBlock> concrete(String id, boolean reinforced,
                                                          BlockBehaviour.Properties properties) {
        RegistryObject<ConcreteBlock> ro = GTBlocks.BLOCKS.register(id, () -> new ConcreteBlock(reinforced, properties));
        ALL.add(ro);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new ConcreteBlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    private static RegistryObject<ColoredGlassBlock> coloredGlass(String id, boolean glow,
                                                                  BlockBehaviour.Properties properties) {
        RegistryObject<ColoredGlassBlock> ro = GTBlocks.BLOCKS.register(id, () -> new ColoredGlassBlock(glow, properties));
        ALL.add(ro);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new ColoredGlassBlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    private static RegistryObject<BarsBlock> bars(String id, String material, int tint,
                                                   int harvestLevel, float blastResistance) {
        var shared=com.gregtech.gregtech.block.BarsRules.spec(id);
        material=shared.material();tint=shared.tint();harvestLevel=shared.harvest();blastResistance=shared.resistance();
        final String resolvedMaterial=material;final int resolvedTint=tint,resolvedHarvest=harvestLevel;final float resolvedResistance=blastResistance;
        RegistryObject<BarsBlock> ro = GTBlocks.BLOCKS.register(id,
                () -> new BarsBlock(resolvedMaterial, resolvedTint, resolvedHarvest,
                        props(MapColor.METAL, 5f).strength(5f, resolvedResistance)
                                .sound(SoundType.METAL).noOcclusion()));
        ALL.add(ro);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BarsBlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    public static net.minecraftforge.registries.RegistryObject<com.gregtech.gregtech.block.misc.CFoamSlabBlock> GLOW_GLASS_SLAB;
    public static void registerAll() {
        GLOW_GLASS_SLAB = reg("glass_glow_slab", () -> new com.gregtech.gregtech.block.misc.ColoredGlassSlabBlock(props(MapColor.NONE, 0.5f).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> 15)));
        CFOAM_SLAB = reg("cfoam_slab", () -> new com.gregtech.gregtech.block.misc.CFoamSlabBlock(props(MapColor.WOOL, 1f).sound(SoundType.WOOL)));
        // N11: Asphalt
        ASPHALT = reg("asphalt", () -> new AsphaltBlock(props(MapColor.COLOR_BLACK, 3f).sound(SoundType.STONE)));

        // N11: Concrete
        // GT6 BlockColored: all 16 dye metadata values share one block/item id. GT6 stone
        // hardness multipliers are 1x/4x; blast resistance multipliers are 2x/8x.
        CONCRETE = concrete("concrete", false,
                BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 20f)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE));
        CONCRETE_REINFORCED = concrete("concrete_reinforced", true,
                BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(6f, 80f)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE));

        // N11: CFoam
        CFOAM = reg("cfoam", () -> new CFoamBlock(props(MapColor.WOOL, 1f).sound(SoundType.WOOL)));
        CFOAM_FRESH = reg("cfoam_fresh", () -> new com.gregtech.gregtech.block.misc.FreshCFoamBlock( props(MapColor.WOOL, 0.5f).sound(SoundType.WOOL)));

        // N11: Glass
        GLASS_CLEAR = coloredGlass("glass_clear", false,
                BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.75f, 5f)
                        .sound(SoundType.GLASS).noOcclusion().isValidSpawn((s, l, p, e) -> false));
        GLASS_GLOW = coloredGlass("glass_glow", true,
                BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.75f, 5f)
                        .sound(SoundType.GLASS).lightLevel(s -> 15).noOcclusion()
                        .isValidSpawn((s, l, p, e) -> false));

        // N11: Spikes (5)
        SPIKE_METAL = reg("spike_metal", () -> new SpikeBlock(SpikeBlock.Family.METAL, spikeProps()));
        SPIKE_STEEL = reg("spike_steel", () -> new SpikeBlock(SpikeBlock.Family.STEEL, spikeProps()));
        SPIKE_SHARP = reg("spike_sharp", () -> new SpikeBlock(SpikeBlock.Family.SHARP, spikeProps()));
        SPIKE_FANCY = reg("spike_fancy", () -> new SpikeBlock(SpikeBlock.Family.FANCY, spikeProps()));
        SPIKE_SUPER = reg("spike_super", () -> new SpikeBlock(SpikeBlock.Family.SUPER, spikeProps()));

        // N11: Bars (7). The old model had no elements, so every one of these was invisible.
        BARS_IRON = bars("bars_iron", "iron", 0xD8D8D8, 2, 5f);
        BARS_STEEL = bars("bars_steel", "steel", 0x808080, 2, 8f);
        BARS_BRASS = bars("bars_brass", "brass", 0xD2A85B, 1, 5f);
        BARS_BRONZE = bars("bars_bronze", "bronze", 0xCD7F32, 2, 5f);
        BARS_WROUGHT_IRON = bars("bars_wrought_iron", "wrought_iron", 0xC0C0C0, 2, 5f);
        BARS_STAINLESS = bars("bars_stainless", "stainless", 0xE0E0E0, 2, 5f);
        BARS_TUNGSTEN_STEEL = bars("bars_tungsten_steel", "tungsten_steel", 0x7070A0, 4, 16f);

        // N11: Railroad
        RAILROAD = reg("railroad", () -> new RoadStripeRailBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.35f, 20f)
                        .sound(SoundType.METAL)));

        // GT6 BlockDiggable: the seven existing icon IDs now use the original behavior and drops.
        MUD = reg("mud", () -> new DiggableBlock(DiggableBlock.Variant.MUD));
        BROWN_CLAY = reg("clay_brown", () -> new DiggableBlock(DiggableBlock.Variant.BROWN_CLAY));
        TURF = reg("turf", () -> new DiggableBlock(DiggableBlock.Variant.TURF));
        RED_CLAY = reg("clay_red", () -> new DiggableBlock(DiggableBlock.Variant.RED_CLAY));
        YELLOW_CLAY = reg("clay_yellow", () -> new DiggableBlock(DiggableBlock.Variant.YELLOW_CLAY));
        BLUE_CLAY = reg("clay_blue", () -> new DiggableBlock(DiggableBlock.Variant.BLUE_CLAY));
        WHITE_CLAY = reg("clay_white", () -> new DiggableBlock(DiggableBlock.Variant.WHITE_CLAY));
        DIGGABLE_CLAY = reg("diggable_clay", () -> new DiggableBlock(DiggableBlock.Variant.BROWN_CLAY));
        DIGGABLE_PEAT = reg("diggable_peat", () -> new DiggableBlock(DiggableBlock.Variant.TURF));

        // N12: Piles. GT6's placeable multi-tiles 32084/32085/32086 (MultiTileEntityIngot/Plate/
        // PlateGem, Loader_MultiTileEntities:2036-2038) and 32700 (MultiTileEntityCoin, :2240): each
        // pile stores what is piled on it through its block entity. Block ids, looks and items are
        // unchanged.
        INGOT_PILE = reg("ingot_pile", () -> new com.gregtech.gregtech.block.misc.PileBlock(
                com.gregtech.gregtech.block.misc.PileBlock.Kind.INGOT,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.25f, 0f)
                        .sound(SoundType.METAL).noOcclusion().isSuffocating((state, level, pos) -> false)));
        PLATE_PILE = reg("plate_pile", () -> new com.gregtech.gregtech.block.misc.PileBlock(
                com.gregtech.gregtech.block.misc.PileBlock.Kind.PLATE,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.25f, 0f)
                        .sound(SoundType.METAL).noOcclusion().isSuffocating((state, level, pos) -> false)));
        PLATE_GEM_PILE = reg("plate_gem_pile", () -> new com.gregtech.gregtech.block.misc.PileBlock(
                com.gregtech.gregtech.block.misc.PileBlock.Kind.GEM_PLATE,
                BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(0.25f, 0f)
                        .sound(SoundType.STONE).noOcclusion().isSuffocating((state, level, pos) -> false)));
        COIN_PILE = reg("coin_pile", () -> new com.gregtech.gregtech.block.misc.CoinPileBlock(
                props(MapColor.GOLD, 3f).sound(SoundType.METAL).noOcclusion()));

        // N12: Misc
        CRATE = reg("crate", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                .strength(1f, 3f).sound(SoundType.WOOD).ignitedByLava()));
        LOOT_CRATE = reg("loot_crate", () -> new com.gregtech.gregtech.block.LootCrateBlock(
                props(MapColor.WOOD, 4f).sound(SoundType.WOOD)));
        for (var variant : com.gregtech.gregtech.content.book.BookShelfVariants.all()) {
            var block = GTBlocks.BLOCKS.register(variant.path(), () ->
                    new com.gregtech.gregtech.block.BookShelfBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(variant.metal() ? MapColor.METAL : MapColor.WOOD)
                                    .strength(variant.hardness(), variant.resistance())
                                    .requiresCorrectToolForDrops()
                                    .sound(variant.metal() ? SoundType.METAL : SoundType.WOOD), variant));
            ALL.add(block);
            BOOKSHELVES.put(variant.path(), block);
            GTBlocks.BLOCK_ITEMS.register(variant.path(), () ->
                    new com.gregtech.gregtech.item.BookShelfBlockItem(block.get(),
                            new Item.Properties().stacksTo(16)));
            if (variant.path().equals("bookshelf")) BOOKSHELF = block;
        }
        GTFluidSprings.initialize();
        FLUID_SPRING = GTFluidSprings.FLUID_SPRING;
        GREG_LANTERN = reg("greg_lantern", () -> new GregLanternBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1f, 1f)
                        .sound(SoundType.WOOD).lightLevel(s -> 15).noOcclusion()));
        SANDWICH_BLOCK = GTBlocks.BLOCKS.register("sandwich_block", () -> new SandwichBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.25f, 0f)
                        .sound(SoundType.WOOL).noOcclusion()));
        ALL.add(SANDWICH_BLOCK);
        GTBlocks.BLOCK_ITEMS.register("sandwich_block", () -> new com.gregtech.gregtech.item.SandwichBlockItem(
                SANDWICH_BLOCK.get(), new Item.Properties()));

        // WorldgenHives: the wild bumblebee colonies.
        BUMBLE_HIVE = reg("bumble_hive", () -> new com.gregtech.gregtech.block.misc.BumbleHiveBlock(
                com.gregtech.gregtech.block.misc.BumbleHiveBlock.properties()));
        for (var color : net.minecraft.world.item.DyeColor.values()) {
            HIVE_VARIANTS.put(color, reg("bumble_hive_" + color.getName(), () ->
                    new com.gregtech.gregtech.block.misc.FixedBumbleHiveBlock(BumbleHiveBlock.properties(), color)));
        }
    }

    private static BlockBehaviour.Properties props(MapColor color, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, hardness).requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties spikeProps() {
        // GT6 BlockBaseSpike: 30 hardness, 5 blast resistance, metal tool.
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(30f, 5f)
                .requiresCorrectToolForDrops().sound(SoundType.METAL);
    }
}
