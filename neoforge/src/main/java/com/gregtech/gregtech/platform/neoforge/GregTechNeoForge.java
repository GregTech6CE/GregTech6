package com.gregtech.gregtech.platform.neoforge;

import com.gregtech.gregtech.api.recipe.MachineWorkCost;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialRoleFlags;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.ModReferences;
import com.gregtech.gregtech.registry.GTItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/** NeoForge lifecycle adapter. Gameplay registrations are added by subsystem ports. */
@Mod(GregTechNeoForge.MOD_ID)
public final class GregTechNeoForge {
    public static final String MOD_ID = "gregtech";
    private static final Logger LOGGER = LogUtils.getLogger();

    public GregTechNeoForge(IEventBus modEventBus) {
        // No material/metadata holder may initialize before this platform binding.
        ModData.bindPresence(ModList.get()::isLoaded);
        GTMaterialRegistry.setLogSink((warning, message) -> {
            if (warning) LOGGER.warn(message);
            else LOGGER.info(message);
        });
        // Same domain holder order as Forge Loader_Data, followed by Loader_Materials.
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        MaterialGroups.Glowstone.getClass();
        GTMaterialRegistry.init();
        MaterialRoleFlags.apply();
        int materialItems = GTItems.register(modEventBus);
        LOGGER.info("Queued {} Neo material items from the shared original definitions", materialItems);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(this::finishSharedSetup);
    }

    private void finishSharedSetup() {
        // Domain post-init only: Neo vanilla item/composition adapters are still pending.
        GTMaterialRegistry.postInit();
        MachineWorkCost.Cost cost = MachineWorkCost.calculate(
                8, 20, 1, false, 10_000, 8, 32, false);
        if (cost == null || cost.minimumPower() != 8 || cost.totalWork() != 160) {
            throw new IllegalStateException("GT6 shared machine work accounting failed to initialize");
        }
        LOGGER.info("GregTech shared core initialized: {} material objects, minimumPower={}, totalWork={}; "
                        + "NeoForge material item definitions are queued; worldgen/vanilla composition, "
                        + "item visuals and gameplay adaptations are pending",
                GTMaterialRegistry.allMaterials().size(), cost.minimumPower(), cost.totalWork());
    }
}
