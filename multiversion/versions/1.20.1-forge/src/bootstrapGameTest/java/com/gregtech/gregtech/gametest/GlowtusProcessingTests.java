package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.worldgen.GTSurfaceFlora;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** GT6 BlockGlowtus material and RM.mortarize / RM.biomass(4) contracts. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class GlowtusProcessingTests {
    private static Recipe find(com.gregtech.gregtech.api.recipe.RecipeMap map,
                               ItemStack item, FluidStack fluid) {
        return map.findRecipe(List.of(item), fluid == null ? List.of() : List.of(fluid),
                false, 1, map.mOutputItemsCount);
    }

    private static boolean output(Recipe recipe, ItemStack expected) {
        return recipe != null && recipe.mOutputs.length == 1
                && recipe.mOutputs[0].getCount() == expected.getCount()
                && ItemStack.isSameItemSameTags(recipe.mOutputs[0], expected);
    }

    @GameTest(template = "test_empty")
    public static void allColoursContainQuarterGlowstoneAndGrind(GameTestHelper helper) {
        ItemStack smallDust = GTItems.getStack(MaterialPrefix.dustSmall, Materials.Glowstone);
        helper.assertTrue(!smallDust.isEmpty(), "Glowstone small dust exists");
        for (String colour : GTSurfaceFlora.GLOWTUS_COLOURS) {
            ItemStack flower = new ItemStack(GTSurfaceFlora.glowtus(colour));
            var data = ItemMaterialRegistry.get(flower).orElseThrow();
            helper.assertTrue(data.material() == Materials.Glowstone && data.amount() == GTValues.U4,
                    colour + " Glowtus has one quarter-unit of Glowstone");
            for (var map : List.of(MachineRecipeMaps.Mortar, MachineRecipeMaps.Shredder)) {
                Recipe recipe = find(map, flower, null);
                helper.assertTrue(output(recipe, smallDust) && recipe.mDuration == 16 && recipe.mEUt == 16,
                        colour + " Glowtus grinds to one small Glowstone dust");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void allColoursFermentFromOneFlower(GameTestHelper helper) {
        for (String colour : GTSurfaceFlora.GLOWTUS_COLOURS) {
            ItemStack flower = new ItemStack(GTSurfaceFlora.glowtus(colour));
            check(helper, flower, "Water", 270, 270, 64);
            check(helper, flower, "Rotten_Drink", 270, 810, 32);
            check(helper, flower, "Honeydew", 270, 810, 32);
        }
        helper.succeed();
    }

    private static void check(GameTestHelper helper, ItemStack flower, String inputField,
                              int inputAmount, int outputAmount, int ticks) {
        Recipe recipe = find(MachineRecipeMaps.Fermenter, flower, GTFluids.stack(inputField, inputAmount));
        FluidStack expected = GTFluids.stack("BiomassIC2", outputAmount);
        helper.assertTrue(recipe != null && recipe.mInputs.length == 1
                        && recipe.mInputs[0].getCount() == 1
                        && recipe.mDuration == ticks && recipe.mEUt == 16
                        && recipe.mFluidOutputs.length == 1
                        && recipe.mFluidOutputs[0].isFluidEqual(expected)
                        && recipe.mFluidOutputs[0].getAmount() == outputAmount,
                flower.getHoverName().getString() + " uses GT6's biomass divisor with " + inputField);
    }
}
