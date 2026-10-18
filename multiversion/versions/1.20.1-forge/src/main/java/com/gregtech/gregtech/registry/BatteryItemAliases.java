package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.content.energy.BatteryItemMigration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegisterEvent;

/** Aliases handle item decoding; missing mappings also handle older Forge registry snapshots. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BatteryItemAliases {
    private BatteryItemAliases() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) return;
        // Forge 47 provides addAlias on the native implementation, not IForgeRegistry.
        var registry = (ForgeRegistry<Item>) event.<Item>getForgeRegistry();
        for (var alias : BatteryItemMigration.ALIASES)
            registry.addAlias(GregTech.id(alias.oldId()), GregTech.id(alias.target().id()));
    }

    @Mod.EventBusSubscriber(modid = GregTech.MODID)
    public static final class Missing {
        private Missing() {}
        @SubscribeEvent
        public static void missing(MissingMappingsEvent event) {
            for (var mapping : event.getMappings(Registries.ITEM, GregTech.NAMESPACE)) {
                for (var alias : BatteryItemMigration.ALIASES) {
                    if (mapping.getKey().equals(GregTech.id(alias.oldId()))) {
                        mapping.remap(GTChemicalBatteries.item(alias.target().chemistry(), alias.target().tier()));
                        break;
                    }
                }
            }
        }
    }
}
