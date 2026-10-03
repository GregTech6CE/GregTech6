package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.MaterialBlockItem;
import com.gregtech.gregtech.block.MaterialBlockLike;
import com.gregtech.gregtech.registry.GTBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Original GregTechClient material block/item tint callbacks, isolated to the client. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MaterialBlockClientColors {
    private MaterialBlockClientColors() {}

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var entry : GTBlocks.BLOCKS.getEntries()) {
            if (entry.get() instanceof MaterialBlockLike materialBlock) {
                int color = materialBlock.material().getColor();
                event.register((state, level, pos, layer) -> layer == 0 ? color : 0xFFFFFF, entry.get());
            }
        }
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        for (var entry : GTBlocks.BLOCK_ITEMS.getEntries()) {
            if (entry.get() instanceof MaterialBlockItem blockItem && blockItem.material() != null) {
                event.register(ItemColorARGB.opaque((stack, layer) -> layer == 0 ? blockItem.getTintColor() : 0xFFFFFF), blockItem);
            }
        }
    }
}
