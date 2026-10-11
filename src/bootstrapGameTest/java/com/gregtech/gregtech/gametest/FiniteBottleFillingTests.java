package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.recipe.FiniteBottleFillingRecipe;
import com.gregtech.gregtech.registry.GTFluidItems;
import com.gregtech.gregtech.registry.GTFluids;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.gametest.GameTestHolder;

/** The fluid shown in JEI must really be in a finite, drainable container. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class FiniteBottleFillingTests {
    private static TransientCraftingContainer grid() {
        var menu = new AbstractContainerMenu(null, 0) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        };
        return new TransientCraftingContainer(menu, 3, 3);
    }

    @GameTest(template = "test_empty")
    public static void allBottleFamiliesUseFiniteFluidContainers(GameTestHelper helper) {
        String[] families = {"seed_oil", "milk", "soy_milk", "honey", "green_slime_bottle",
                "pink_slime_bottle", "blue_slime_bottle", "juice", "maple_sap", "rainbow_sap",
                "lubricant_bottle"};
        for (String family : families) for (int count = 1; count <= 4; count++) {
            var id = GregTech.id("bottles/" + family + "_x" + count);
            var found = helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
            helper.assertTrue(found instanceof FiniteBottleFillingRecipe,
                    "finite 1000 mB bottle filling recipe: " + id);
            var recipe = (FiniteBottleFillingRecipe) found;
            helper.assertTrue(recipe.getIngredients().get(0).getItems().length > 0,
                    "finite fluid ingredient has a JEI example: " + id);
            var grid = grid();
            for (int i = 0; i < recipe.getIngredients().size(); i++)
                grid.setItem(i, recipe.getIngredients().get(i).getItems()[0].copy());
            helper.assertTrue(recipe.matches(grid, helper.getLevel()),
                    "all finite container examples craft: " + id);
            FluidStack before = FluidUtil.getFluidContained(grid.getItem(0)).orElse(FluidStack.EMPTY);
            FluidStack after = FluidUtil.getFluidContained(recipe.getRemainingItems(grid).get(0))
                    .orElse(FluidStack.EMPTY);
            helper.assertTrue(before.getAmount() - after.getAmount() == 1000,
                    "exact 1000 mB consumed for " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void lubricantBottlesConsumeRealFluidAndRejectDisplayItem(GameTestHelper helper) {
        var id = GregTech.id("bottles/lubricant_bottle_x4");
        var recipe = (FiniteBottleFillingRecipe) helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
        var grid = grid();
        for (int i = 0; i < recipe.getIngredients().size(); i++)
            grid.setItem(i, recipe.getIngredients().get(i).getItems()[0].copy());
        var lubricant = GTFluids.still("Lubricant").get();
        FluidStack before = FluidUtil.getFluidContained(grid.getItem(0)).orElse(FluidStack.EMPTY);
        helper.assertTrue(before.getFluid() == lubricant && before.getAmount() == 1000,
                "JEI example contains actual finite 1000 mB lubricant");
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "filled cell plus four empties matches");
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).getCount() == 4,
                "four lubricant bottles output");
        var remains = recipe.getRemainingItems(grid);
        FluidStack after = FluidUtil.getFluidContained(remains.get(0)).orElse(FluidStack.EMPTY);
        helper.assertTrue(!remains.get(0).isEmpty() && before.getAmount() - after.getAmount() == 1000,
                "one finite cell is drained completely and returned");
        grid.setItem(0, new ItemStack(GTFluidItems.forFluid(lubricant)));
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()),
                "infinite display fluid cannot craft lubricant bottles");

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FiniteBottleFillingRecipe.SERIALIZER.toNetwork(buffer, recipe);
            var copy = FiniteBottleFillingRecipe.SERIALIZER.fromNetwork(id, buffer);
            helper.assertTrue(!copy.matches(grid, helper.getLevel()) && buffer.readableBytes() == 0,
                    "network reload retains finite container requirement");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
