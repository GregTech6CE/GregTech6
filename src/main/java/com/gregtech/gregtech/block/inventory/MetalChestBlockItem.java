package com.gregtech.gregtech.block.inventory;

import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

public final class MetalChestBlockItem extends BlockItem {
    public MetalChestBlockItem(MetalChestBlock block, Properties properties) { super(block, properties); }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.gregtech.gregtech.client.MetalChestItemRenderer.instance();
            }
        });
    }
}
