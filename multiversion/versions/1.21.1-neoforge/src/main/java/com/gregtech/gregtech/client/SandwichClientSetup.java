package com.gregtech.gregtech.client;
import net.neoforged.bus.api.SubscribeEvent;
@net.neoforged.fml.common.EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class SandwichClientSetup {
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return SandwichItemRenderer.instance(); }
        },com.gregtech.gregtech.registry.GTSandwich.SANDWICH_BLOCK.get().asItem());
    }
}
