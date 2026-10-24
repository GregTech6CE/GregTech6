package gregtech.registry;

import gregtech.block.wood.TreeHoleBlock;
import gregtech.block.wood.WoodSpecies;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}

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
//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Block, ? extends Block>> ALL = new ArrayList<>();
    private static final Map<WoodSpecies, DeferredHolder<Block, TreeHoleBlock>> HOLES = new LinkedHashMap<>();
*///?} else {
    private static final List<RegistryObject<? extends Block>> ALL = new ArrayList<>();
    private static final Map<WoodSpecies, RegistryObject<TreeHoleBlock>> HOLES = new LinkedHashMap<>();
//?}

    private GTTreeHoles() {
    }

//? if >=1.21.1 {
    /*public static List<DeferredHolder<Block, ? extends Block>> all() {
        return Collections.unmodifiableList(ALL);
    }
*///?} else {
    public static List<RegistryObject<? extends Block>> all() {
        return Collections.unmodifiableList(ALL);
    }
//?}

    public static List<Block> blocks() {
        List<Block> out = new ArrayList<>();
//? if >=1.21.1 {
        /*for (DeferredHolder<Block, ? extends Block> entry : ALL) if (entry.isBound()) out.add(entry.get());
*///?} else {
        for (RegistryObject<? extends Block> entry : ALL) if (entry.isPresent()) out.add(entry.get());
//?}
        return out;
    }

    /** The hole block of a species, or {@code null} for species GT6 has no hole for. */
    public static TreeHoleBlock hole(WoodSpecies species) {
//? if >=1.21.1 {
        /*DeferredHolder<Block, TreeHoleBlock> entry = HOLES.get(species);
        return entry == null || !entry.isBound() ? null : entry.get();
*///?} else {
        RegistryObject<TreeHoleBlock> entry = HOLES.get(species);
        return entry == null || !entry.isPresent() ? null : entry.get();
//?}
    }

    public static void registerAll() {
        hole("resin_hole_rubber", WoodSpecies.RUBBER, "item:gt6ce:rubber_resin");
        hole("tapped_maple", WoodSpecies.MAPLE, "fluid:gt6ce:maplesap");
        hole("tapped_rainbowood", WoodSpecies.RAINBOWOOD, "fluid:gt6ce:rainbowsap");
    }

    private static void hole(String id, WoodSpecies species, String yield) {
//? if >=1.21.1 {
        /*DeferredHolder<Block, TreeHoleBlock> block = GTBlocks.BLOCKS.register(id, () -> new TreeHoleBlock(species, yield,
*///?} else {
        RegistryObject<TreeHoleBlock> block = GTBlocks.BLOCKS.register(id, () -> new TreeHoleBlock(species, yield,
//?}
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 3.0f)
        .sound(SoundType.WOOD).noOcclusion()));
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        ALL.add(block);
        HOLES.put(species, block);
    }
}
