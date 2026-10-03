package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

/** Original MTE:352,391-420; shaped outputs survive the existing native oven recipe mirror and sync. */
public final class ClayMoldRecipes {
    private ClayMoldRecipes() {}
    public static ItemStack input(ClayMoldCatalog.Raw raw) {
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",raw.id()));
        if (item==Items.AIR) throw new IllegalStateException("Missing raw clay mold "+raw.id());
        return new ItemStack(item);
    }
    public static ItemStack output(ClayMoldCatalog.Raw raw) {
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech","mold_ceramic"));
        if (item==Items.AIR) throw new IllegalStateException("Missing ceramic mold");
        return com.gregtech.gregtech.block.machine.MoldItemData.withShape(new ItemStack(item),raw.shape());
    }
    public static int register() {
        int count=0;
        for (var raw:ClayMoldCatalog.RAW)
            if (MachineRecipeMaps.add_smelting(input(raw),output(raw),false,false,true)) count++;
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original raw clay mold firing: {} rows",count);
        return count;
    }
}
