package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Neo platform item: material and prefix belong to its actual registry identity, never stack NBT. */
public class MaterialItem extends Item {
    private final MaterialPrefix prefix;
    private final GTMaterial material;

    public MaterialItem(Properties properties, MaterialPrefix prefix, GTMaterial material) {
        super(properties);
        this.prefix = prefix;
        this.material = material;
    }

    public MaterialPrefix getPrefix() { return prefix; }
    public GTMaterial getMaterial() { return material; }

    public static GTMaterial getMaterial(ItemStack stack) {
        return stack.getItem() instanceof MaterialItem item ? item.material : GTMaterialRegistry.get("NULL");
    }

    public static MaterialPrefix getPrefix(ItemStack stack) {
        return stack.getItem() instanceof MaterialItem item ? item.prefix : null;
    }

    public static boolean isMaterialItem(ItemStack stack, MaterialPrefix prefix, GTMaterial material) {
        return stack.getItem() instanceof MaterialItem item
                && item.prefix == prefix && item.material.resolve() == material.resolve();
    }

    @Override
    public Component getName(ItemStack stack) {
        // Same domain translation data/argument contract as Forge MaterialPresentation.
        return Component.translatable(getDescriptionId(),
                Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback()));
    }

    @Override
    public String getDescriptionId() {
        return "item.gregtech." + prefix.getRegistryName();
    }

    public int getTintColor() {
        return 0xFF000000 | (material.getColor() & 0xFFFFFF);
    }
}
