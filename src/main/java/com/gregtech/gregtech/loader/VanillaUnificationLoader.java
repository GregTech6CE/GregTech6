package com.gregtech.gregtech.loader;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.VanillaUnificationDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Minecraft item binding for the single shared vanilla identity/weight table. */
public final class VanillaUnificationLoader {
    private VanillaUnificationLoader() {}
    public static void register() {
        for (var definition : VanillaUnificationDefinitions.entries()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(definition.itemPath()));
            var material = GTMaterialRegistry.get(definition.materialName());
            if (item != Items.AIR && material.isValid())
                ItemMaterialRegistry.register(item, definition.prefix(), material, definition.amount());
        }
    }
}
