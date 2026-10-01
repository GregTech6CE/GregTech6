package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.plant.BushBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * GT6's berry bush block ({@code MultiTileEntityBush}, multi-tile 32759; the block the
 * {@code WorldgenBushes} generator plants and that players set a berry type on).
 */
public final class GTBushes {
    private GTBushes() {}

    public static final DeferredHolder<net.minecraft.world.level.block.Block,BushBlock> BUSH = GTBlocks.BLOCKS.register("bush", () -> new BushBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).randomTicks()
                    .sound(SoundType.GRASS).noOcclusion().instabreak()));

    public static void registerAll() {
        GTBlocks.BLOCK_ITEMS.register("bush", () -> new BlockItem(BUSH.get(), new Item.Properties()));
    }
}
