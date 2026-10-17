package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Decode retired IDs without registering a second object or discarding stack/block-entity data. */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LegacyContentRegistryAliases {
    @SubscribeEvent public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            var registry = (ForgeRegistry<Item>) event.<Item>getForgeRegistry();
            LegacyContentAliases.ALIASES.forEach((old, target) -> registry.addAlias(GregTech.id(old), GregTech.id(target)));
        } else if (event.getRegistryKey().equals(Registries.BLOCK)) {
            var registry = (ForgeRegistry<Block>) event.<Block>getForgeRegistry();
            LegacyContentAliases.ALIASES.forEach((old, target) -> {
                if (!old.startsWith("key_")) registry.addAlias(GregTech.id(old), GregTech.id(target));
            });
        }
    }
    @Mod.EventBusSubscriber(modid = GregTech.MODID)
    public static final class Missing {
        @SubscribeEvent public static void remap(MissingMappingsEvent event) {
            for (var mapping : event.<Item>getMappings(Registries.ITEM, GregTech.NAMESPACE)) {
                String target = LegacyContentAliases.ALIASES.get(mapping.getKey().getPath());
                if (target != null) mapping.remap(ForgeRegistries.ITEMS.getValue(GregTech.id(target)));
            }
            for (var mapping : event.<Block>getMappings(Registries.BLOCK, GregTech.NAMESPACE)) {
                String target = LegacyContentAliases.ALIASES.get(mapping.getKey().getPath());
                if (target != null) mapping.remap(ForgeRegistries.BLOCKS.getValue(GregTech.id(target)));
            }
        }
    }
}
