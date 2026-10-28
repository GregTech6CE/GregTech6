package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** Original MTE; shaped outputs survive the existing native oven recipe mirror and sync. */
public final class ClayMoldRecipes {
    private ClayMoldRecipes() {}
    public static ItemStack input(ClayMoldCatalog.Raw raw) {
//? if >=1.21.1 {
        /*var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",raw.id()));
        if (item==Items.AIR) throw new IllegalStateException("Missing raw clay mold "+raw.id());
*///?} else {
        var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gt6ce",raw.id()));
        if (item==null || item==Items.AIR) throw new IllegalStateException("Missing raw clay mold "+raw.id());
//?}
        return new ItemStack(item);
    }
    public static ItemStack output(ClayMoldCatalog.Raw raw) {
//? if >=1.21.1 {
        /*var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce","mold_ceramic"));
        if (item==Items.AIR) throw new IllegalStateException("Missing ceramic mold");
*///?} else {
        var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gt6ce","mold_ceramic"));
        if (item==null || item==Items.AIR) throw new IllegalStateException("Missing ceramic mold");
//?}
        return gregtech.content.block.machine.MoldItemData.withShape(new ItemStack(item),raw.shape());
    }
    public static int register() {
        int count=0;
        for (var raw:ClayMoldCatalog.RAW)
            if (MachineRecipeMaps.add_smelting(input(raw),output(raw),false,false,true)) count++;
        for (var row:ClayUtilityCatalog.ROWS)
            if (MachineRecipeMaps.add_smelting(registered(row.raw()),registered(row.fired()),false,false,true)) count++;
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original raw clay mold firing: {} rows",count);
        return count;
    }

    private static ItemStack registered(String path) {
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",path));
        if(item==Items.AIR)throw new IllegalStateException("Missing clay utility "+path);
        return new ItemStack(item);
    }
}
