package com.gregtech.gregtech.platform.neoforge.logistics;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class StorageCapabilities {
    private StorageCapabilities() {}
    @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,StorageRegistries.MASS_STORAGE.get(),(storage,side)->storage.itemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,StorageRegistries.LOGISTICS_MASS_STORAGE.get(),(storage,side)->storage.itemHandler());
    }
}
