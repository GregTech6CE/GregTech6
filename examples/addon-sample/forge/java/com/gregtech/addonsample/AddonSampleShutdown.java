package com.gregtech.addonsample;

import gregtech.api.addon.platform.AddonFluids;
import gregtech.api.addon.platform.AddonItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "addon_sample")
public final class AddonSampleShutdown {
    private AddonSampleShutdown() {}

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        var dustId = new ResourceLocation("addon_sample", "dust_ammoniumnitrate");
        var dust = ForgeRegistries.ITEMS.getValue(dustId);
        var apiDust = AddonItems.stack("dust", "AmmoniumNitrate");
        boolean dustOk = dust != null && dust != Items.AIR && !apiDust.isEmpty()
                && apiDust.getItem() == dust && dustId.equals(ForgeRegistries.ITEMS.getKey(dust));
        System.out.println("[addon-sample] dust_ammoniumnitrate=" + dustOk);
        if (!dustOk) throw new IllegalStateException("sample material items were not registered");
        var moltenId = new ResourceLocation("addon_sample", "molten_samplealloy");
        var flowingId = new ResourceLocation("addon_sample", "molten_samplealloy_flowing");
        var molten = AddonFluids.still("GenMolten_SampleAlloy", 144);
        var flowing = AddonFluids.fluid("GenMolten_SampleAlloy", AddonFluids.Kind.FLOWING, 144);
        boolean fluidsOk = molten != null && !molten.isEmpty() && flowing != null && !flowing.isEmpty()
                && moltenId.equals(ForgeRegistries.FLUIDS.getKey(molten.getFluid()))
                && flowingId.equals(ForgeRegistries.FLUIDS.getKey(flowing.getFluid()));
        var display = ForgeRegistries.ITEMS.getValue(new ResourceLocation("addon_sample", "fluid_item_molten_samplealloy"));
        System.out.println("[addon-sample] molten_samplealloy=" + fluidsOk);
        if (!fluidsOk || display == null || display == Items.AIR)
            throw new IllegalStateException("sample material fluids or display item were not registered in addon_sample");
        if (Boolean.getBoolean("addonSampleHalt")) event.getServer().halt(false);
    }
}
