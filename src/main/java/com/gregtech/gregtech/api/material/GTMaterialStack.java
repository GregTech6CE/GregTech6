package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.ItemStack;

/**
 * Stack helper binding a {@link GTMaterial} amount to an item, analogous to {@code OreDictMaterialStack}.
 */
public final class GTMaterialStack {
    public final GTMaterial material;
    public final int amount;

    public GTMaterialStack(GTMaterial material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public static GTMaterialStack of(GTMaterial material, int amount) {
        return new GTMaterialStack(material, amount);
    }

    public ItemStack toStack(MaterialPrefix prefix, int count) {
        return MaterialStackItemHelper.createStack(prefix, material, count);
    }
}
