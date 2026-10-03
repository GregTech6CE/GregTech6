package com.gregtech.gregtech.client;

import com.gregtech.gregtech.block.tool.FluidAttachmentBlock;
import com.gregtech.gregtech.block.tool.PortableContainerBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** World shell colors for portable containers and fluid attachments. */
@EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class MaterialShellColors {
    private MaterialShellColors() {}
    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var block : BuiltInRegistries.BLOCK) {
            if (block instanceof PortableContainerBlock container)
                event.register((state, level, pos, tint) -> tint == 0 ? container.spec().material().getColor() : 0xFFFFFF, block);
            else if (block instanceof FluidAttachmentBlock attachment)
                event.register((state, level, pos, tint) -> tint == 0 ? attachment.spec().material().getColor() : 0xFFFFFF, block);
        }
    }
}
