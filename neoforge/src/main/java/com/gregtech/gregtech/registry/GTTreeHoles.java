package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.wood.TreeHoleBlock;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6's three tree holes ({@code Loader_MultiTileEntities:2026-2028}, multi-tile ids 32762/32761/32760):
 * the rubber resin hole, the tapped maple and the tapped rainbowood.
 *
 * <p>Registry ids follow GT6's names. The resin hole is placed by the rubber tree shape itself (GT6
 * {@code BlockTreeSaplingAB} case 0), the two sap holes are drilled into a maple/rainbowood log with a
 * hand drill (GT6 {@code BlockTreeLogA:103} / {@code BlockTreeLogB:104}).
 */
public final class GTTreeHoles {
    private static final List<DeferredHolder<Block,? extends Block>> ALL = new ArrayList<>();
    private static final Map<WoodSpecies, DeferredHolder<Block,TreeHoleBlock>> HOLES = new LinkedHashMap<>();

    private GTTreeHoles() {}

    public static List<DeferredHolder<Block,? extends Block>> all() { return Collections.unmodifiableList(ALL); }

    public static List<Block> blocks() {
        List<Block> out = new ArrayList<>();
        for (DeferredHolder<Block,? extends Block> entry : ALL) if (entry.isBound()) out.add(entry.get());
        return out;
    }

    /** The hole block of a species, or {@code null} for species GT6 has no hole for. */
    public static TreeHoleBlock hole(WoodSpecies species) {
        DeferredHolder<Block,TreeHoleBlock> entry = HOLES.get(species);
        return entry == null || !entry.isBound() ? null : entry.get();
    }

    public static void registerAll() {
        hole("resin_hole_rubber", WoodSpecies.RUBBER, "item:gregtech:rubber_resin");
        hole("tapped_maple", WoodSpecies.MAPLE, "fluid:gregtech:maplesap");
        hole("tapped_rainbowood", WoodSpecies.RAINBOWOOD, "fluid:gregtech:rainbowsap");
    }

    private static void hole(String id, WoodSpecies species, String yield) {
        DeferredHolder<Block,TreeHoleBlock> block = GTBlocks.BLOCKS.register(id, () -> new TreeHoleBlock(species, yield,
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 3.0f)
                        .sound(SoundType.WOOD).noOcclusion()));
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        ALL.add(block);
        HOLES.put(species, block);
    }
}
