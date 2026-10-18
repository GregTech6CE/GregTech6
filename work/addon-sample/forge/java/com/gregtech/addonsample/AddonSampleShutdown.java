package com.gregtech.addonsample;

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
        var dust = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gregtech", "dust_ammoniumnitrate"));
        boolean dustOk = dust != null && dust != Items.AIR;
        System.out.println("[addon-sample] dust_ammoniumnitrate=" + dustOk);
        if (!dustOk) throw new IllegalStateException("sample material items were not registered");
        if (Boolean.getBoolean("addonSampleHalt")) event.getServer().halt(false);
    }
}
