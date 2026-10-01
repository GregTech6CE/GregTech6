package com.gregtech.gregtech.item;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.client.MaterialTooltips;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One registered item per (prefix, material) pair.
 * <p>
 * Material data lives on the {@link Item} instance (registry identity), not on stack NBT.
 * This matches the GregTech Modern approach on 1.20.1: no MetaItem / stack-tag material payload.
 */
public class MaterialItem extends Item {
    private final MaterialPrefix prefix;
    private final GTMaterial material;

    public MaterialItem(Properties properties, MaterialPrefix prefix, GTMaterial material) {
        super(properties);
        this.prefix = prefix;
        this.material = material;
    }

    public MaterialPrefix getPrefix() {
        return prefix;
    }

    public GTMaterial getMaterial() {
        return material;
    }

    /** Resolves material from the registered item type, never from stack NBT. */
    public static GTMaterial getMaterial(ItemStack stack) {
        return stack.getItem() instanceof MaterialItem materialItem
                ? materialItem.material
                : GTMaterialRegistry.get("NULL");
    }

    /** Resolves prefix from the registered item type, never from stack NBT. */
    public static MaterialPrefix getPrefix(ItemStack stack) {
        return stack.getItem() instanceof MaterialItem materialItem ? materialItem.prefix : null;
    }

    public static boolean isMaterialItem(ItemStack stack, MaterialPrefix prefix, GTMaterial material) {
        if (!(stack.getItem() instanceof MaterialItem materialItem)) return false;
        return materialItem.prefix == prefix && materialItem.material.resolve() == material.resolve();
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(), MaterialPresentation.name(material));
    }

    @Override
    public String getDescriptionId() {
        return "item." + GregTech.MODID + "." + prefix.getRegistryName();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        MaterialTooltips.append(stack, material, prefix, tooltip, flag);
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public int getTintColor() {
        return 0xFF000000 | (material.getColor() & 0xFFFFFF);
    }
}
