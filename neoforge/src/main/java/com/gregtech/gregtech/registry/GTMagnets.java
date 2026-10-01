package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.MagnetBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 magnet blocks (N4) — used in motor/generator recipes and magnetic effects. */
public final class GTMagnets {
    private static final List<DeferredHolder<Block,MagnetBlock>> ALL = new ArrayList<>();

    private GTMagnets() {}

    public static List<DeferredHolder<Block,MagnetBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(String id, GTMaterial mat, String materialName) {
        DeferredHolder<Block,MagnetBlock> block = GTBlocks.BLOCKS.register(id, () ->
                new MagnetBlock(materialName, mat, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3f + mat.getDensity() / 2000f, 3f + mat.getDensity() / 2000f)
                        .requiresCorrectToolForDrops()));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void registerAll(){for(var v:com.gregtech.gregtech.content.energy.MagnetCatalog.ALL)add(v.id(),v.material(),v.name());}
}
