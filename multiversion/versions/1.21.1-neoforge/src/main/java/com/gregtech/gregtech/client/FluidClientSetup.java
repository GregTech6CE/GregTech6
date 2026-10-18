package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.fluid.GTWaterParity;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class FluidClientSetup {
    private FluidClientSetup() {}
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        for (var binding : GTFluids.entries().entrySet()) {
            var entry = binding.getValue();
            var type = net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES.get(binding.getKey());
            if (type == null) continue;
            IClientFluidTypeExtensions extension = GTWaterParity.isWorldWater(entry.registryName()) ? WaterFluidClientExtensions.INSTANCE : new IClientFluidTypeExtensions() {
                public ResourceLocation getStillTexture() { return FluidAppearance.appearance(entry).texture(); }
                public ResourceLocation getFlowingTexture() { return getStillTexture(); }
                public int getTintColor() { return FluidAppearance.appearance(entry).tint(); }
            };
            event.registerFluidType(extension, type);
        }
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var binding : GTFluids.entries().entrySet()) {
                ItemBlockRenderTypes.setRenderLayer(BuiltInRegistries.FLUID.get(binding.getKey()), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("gregtech", binding.getKey().getPath()+"_flowing")), RenderType.translucent());
            }
        });
    }
}
