package com.gregtech.addonsample;

import gregtech.api.addon.platform.AddonFluids;
import gregtech.api.addon.platform.AddonItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = "addon_sample")
public final class AddonSampleShutdown {
    private AddonSampleShutdown() {}

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        var dustId = ResourceLocation.fromNamespaceAndPath("addon_sample", "dust_ammoniumnitrate");
        var dust = BuiltInRegistries.ITEM.get(dustId);
        var apiDust = AddonItems.stack("dust", "AmmoniumNitrate");
        boolean dustOk = dust != null && dust != Items.AIR && !apiDust.isEmpty()
                && apiDust.getItem() == dust && dustId.equals(BuiltInRegistries.ITEM.getKey(dust));
        System.out.println("[addon-sample] dust_ammoniumnitrate=" + dustOk);
        if (!dustOk) throw new IllegalStateException("sample material items were not registered");
        var moltenId = ResourceLocation.fromNamespaceAndPath("addon_sample", "molten_samplealloy");
        var flowingId = ResourceLocation.fromNamespaceAndPath("addon_sample", "molten_samplealloy_flowing");
        var molten = AddonFluids.still("GenMolten_SampleAlloy", 144);
        var flowing = AddonFluids.fluid("GenMolten_SampleAlloy", AddonFluids.Kind.FLOWING, 144);
        boolean fluidsOk = molten != null && !molten.isEmpty() && flowing != null && !flowing.isEmpty()
                && moltenId.equals(BuiltInRegistries.FLUID.getKey(molten.getFluid()))
                && flowingId.equals(BuiltInRegistries.FLUID.getKey(flowing.getFluid()));
        var display = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("addon_sample", "fluid_item_molten_samplealloy"));
        System.out.println("[addon-sample] molten_samplealloy=" + fluidsOk);
        if (!fluidsOk || display == null || display == Items.AIR)
            throw new IllegalStateException("sample material fluids or display item were not registered in addon_sample");
        if (Boolean.getBoolean("addonSampleHalt")) event.getServer().halt(false);
    }
}
