package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.misc.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.ArrayList;
import java.util.List;

/** Retain palette registrations for saved worlds, then publish default flat color handles. */
public final class GTConstructionBlocks {
    private GTConstructionBlocks() {}
    private static final List<RegisteredBlock<? extends Block>> LEGACY = new ArrayList<>();
    public static RegisteredBlock<AsphaltBlock> ASPHALT;
    public static RegisteredBlock<CFoamSlabBlock> CFOAM_SLAB;
    public static RegisteredBlock<ConcreteBlock> CONCRETE, CONCRETE_REINFORCED;
    public static RegisteredBlock<CFoamBlock> CFOAM, CFOAM_FRESH;
    public static RegisteredBlock<ColoredGlassBlock> GLASS_CLEAR, GLASS_GLOW;

    public static List<RegisteredBlock<? extends Block>> all() {
        var result = new ArrayList<RegisteredBlock<? extends Block>>(LEGACY);
        result.addAll(GTConstructionVariants.all());
        return List.copyOf(result);
    }

    public static void initialize() {
        if (!LEGACY.isEmpty()) throw new IllegalStateException("Construction blocks registered twice");
        var clearSlab = RegisteredBlock.registerBlock("glass_clear_slab", () -> new ColoredGlassSlabBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.5f).sound(SoundType.GLASS).noOcclusion()));
        item("glass_clear_slab", clearSlab);
        var glowSlab = RegisteredBlock.registerBlock("glass_glow_slab", () -> new ColoredGlassSlabBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.5f).sound(SoundType.GLASS)
                        .noOcclusion().lightLevel(state -> 15)));
        item("glass_glow_slab", glowSlab);
        CFOAM_SLAB = RegisteredBlock.registerBlock("cfoam_slab", () -> new CFoamSlabBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1, 1)
                        .requiresCorrectToolForDrops().sound(SoundType.WOOL)));
        item("cfoam_slab", CFOAM_SLAB);
        ASPHALT = RegisteredBlock.registerBlock("asphalt", () -> new AsphaltBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3, 3)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE)));
        item("asphalt", ASPHALT);
        CONCRETE = concrete("concrete", false, 1.5f, 20);
        CONCRETE_REINFORCED = concrete("concrete_reinforced", true, 6, 80);
        CFOAM = RegisteredBlock.registerBlock("cfoam", () -> new CFoamBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1, 1)
                        .requiresCorrectToolForDrops().sound(SoundType.WOOL)));
        item("cfoam", CFOAM);
        CFOAM_FRESH = RegisteredBlock.registerBlock("cfoam_fresh", () -> new FreshCFoamBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(.5f, .5f)
                        .requiresCorrectToolForDrops().sound(SoundType.WOOL)));
        item("cfoam_fresh", CFOAM_FRESH);
        GLASS_CLEAR = glass("glass_clear", false);
        GLASS_GLOW = glass("glass_glow", true);

        GTConstructionVariants.initialize(List.copyOf(LEGACY));
        ASPHALT = GTConstructionVariants.defaultBlock("asphalt");
        CONCRETE = GTConstructionVariants.defaultBlock("concrete");
        CONCRETE_REINFORCED = GTConstructionVariants.defaultBlock("concrete_reinforced");
        CFOAM = GTConstructionVariants.defaultBlock("cfoam");
        CFOAM_FRESH = GTConstructionVariants.defaultBlock("cfoam_fresh");
        CFOAM_SLAB = GTConstructionVariants.defaultBlock("cfoam_slab");
        GLASS_CLEAR = GTConstructionVariants.defaultBlock("glass_clear");
        GLASS_GLOW = GTConstructionVariants.defaultBlock("glass_glow");
    }

    private static RegisteredBlock<ConcreteBlock> concrete(String id, boolean reinforced, float hardness, float resistance) {
        var block = RegisteredBlock.registerBlock(id, () -> new ConcreteBlock(reinforced,
                BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(hardness, resistance)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE)));
        LEGACY.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new ConcreteBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegisteredBlock<ColoredGlassBlock> glass(String id, boolean glow) {
        var block = RegisteredBlock.registerBlock(id, () -> new ColoredGlassBlock(glow,
                BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(.75f, 5).sound(SoundType.GLASS)
                        .lightLevel(state -> glow ? 15 : 0).noOcclusion().isValidSpawn((s, l, p, e) -> false)));
        LEGACY.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new ColoredGlassBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static void item(String id, RegisteredBlock<? extends Block> block) {
        LEGACY.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new ColoredConstructionBlockItem(block.get(), new Item.Properties()));
    }
}
