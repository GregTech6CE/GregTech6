package com.gregtech.addonsample;

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
        var dust = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "dust_ammoniumnitrate"));
        boolean dustOk = dust != null && dust != Items.AIR;
        System.out.println("[addon-sample] dust_ammoniumnitrate=" + dustOk);
        if (!dustOk) throw new IllegalStateException("sample material items were not registered");
        if (Boolean.getBoolean("addonSampleHalt")) event.getServer().halt(false);
    }
}
