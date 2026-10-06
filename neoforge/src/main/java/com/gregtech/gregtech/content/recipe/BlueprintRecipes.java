/*
 * Blueprint scanning/printing adapted from GregTech-6 Team (2024), LGPL-3.0-or-later.
 * RecipeMapScannerVisuals:89-99; RecipeMapPrinter:99-104,120-124.
 */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.data.CraftingBlueprintData;
import com.gregtech.gregtech.content.data.UsbDataCable;
import com.gregtech.gregtech.content.data.UsbDataMedia;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.List;
import net.neoforged.neoforge.fluids.FluidStack;

public final class BlueprintRecipes {
    private BlueprintRecipes() {}
    public static void register() {
        MachineRecipeMaps.ScannerVisuals.contextualRecipes((level,machine,special,items,fluids) -> {
            Recipe recipe = CanvasRecipes.scan(level,machine,special,items,fluids);
            return recipe != null ? recipe : scan(level,machine,special,items,fluids);
        });
        MachineRecipeMaps.Printer.contextualRecipes((level,machine,special,items,fluids) -> {
            Recipe recipe = CanvasRecipes.print(level,machine,special,items,fluids);
            return recipe != null ? recipe : print(level,machine,special,items,fluids);
        });
    }
    private static boolean is(ItemStack stack, String path) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("gregtech") && id.getPath().equals(path);
    }
    public static Recipe scan(Level level, BlockEntity machine, ItemStack special,
            List<ItemStack> items, List<FluidStack> fluids) {
        if (level.isClientSide) return null;
        ItemStack usb = null, blueprint = null;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            if (usb == null && UsbDataMedia.stickTier(stack) >= 1) usb = stack;
            else if (blueprint == null && is(stack, "blueprint")) blueprint = stack;
        }
        if (usb == null || blueprint == null) return null;
        ItemStack[] pattern = CraftingBlueprintData.readItem(level, blueprint);
        if (pattern.length == 0) return null;
        ItemStack written = usb.copyWithCount(1);
        var data = CraftingBlueprintData.write(level, pattern);
        com.gregtech.gregtech.platform.neoforge.StackCustomData.update(written, tag -> {
            tag.put("gt.usb.data", data);
            tag.putByte("gt.usb.tier", (byte) 1);
        });
        return exact(new Recipe(new ItemStack[]{blueprint.copyWithCount(1), usb.copyWithCount(1)},
                new ItemStack[]{written, blueprint.copyWithCount(1)}, null, null, null, null, 64, 16, 0));
    }
    public static Recipe print(Level level, BlockEntity machine, ItemStack special,
            List<ItemStack> items, List<FluidStack> fluids) {
        if (level.isClientSide) return null;
        ItemStack medium = null, paper = null;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            if (medium == null && (UsbDataMedia.stickTier(stack) >= 1 || UsbDataMedia.cableTier(stack) >= 1)) medium = stack;
            else if (paper == null && (stack.is(Items.PAPER) || is(stack, "empty_blueprint"))) paper = stack;
        }
        if (medium == null || paper == null) return null;
        var root = CraftingBlueprintData.itemData(medium);
        var data = UsbDataMedia.cableTier(medium) >= 1 ? UsbDataCable.readAdjacent(machine, medium, 1)
                : root.contains("gt.usb.data", 10) ? root.getCompound("gt.usb.data") : null;
        ItemStack[] pattern = CraftingBlueprintData.read(level, data);
        if (pattern.length == 0) return null;
        boolean blank = is(paper, "empty_blueprint");
        FluidStack dye = GTFluids.stack(blank ? "Dye_Chemical_White" : "Dye_Chemical_Blue", blank ? 16 : 144);
        if (dye == null || dye.isEmpty()) return null;
        ItemStack output = new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", "blueprint")));
        CraftingBlueprintData.writeItem(level, output, pattern);
        ItemStack crafted = preview(level, pattern);
        if (!crafted.isEmpty()) output.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, crafted.getHoverName());
        return exact(new Recipe(new ItemStack[]{paper.copyWithCount(1), medium.copyWithCount(1)},
                new ItemStack[]{output}, null, null, new FluidStack[]{dye}, null, blank ? 32 : 128, 16, 0)
                .withCatalystInputs(1));
    }
    private static Recipe exact(Recipe recipe) {
        recipe.mExactItemInputs = true;
        recipe.mExplicitCatalystsOnly = true;
        recipe.mCanBeBuffered = false;
        return recipe;
    }
    private static ItemStack preview(Level level, ItemStack[] pattern) {
        var owner = new AbstractContainerMenu(null, 0) {
            public boolean stillValid(Player player) { return false; }
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
        };
        var grid = new TransientCraftingContainer(owner, 3, 3);
        for (int i = 0; i < pattern.length; i++) grid.setItem(i, pattern[i].copy());
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), level)
                .map(holder -> holder.value().assemble(grid.asCraftInput(), level.registryAccess())).orElse(ItemStack.EMPTY);
    }
}
