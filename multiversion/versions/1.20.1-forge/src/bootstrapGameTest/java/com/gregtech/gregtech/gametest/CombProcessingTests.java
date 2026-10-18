package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's comb processing ({@code MultiItemFood.java:251-270}) — the twenty
 * {@code RM.Centrifuge.addRecipe1} rows that turn a comb into its fluid, wax and side products.
 *
 * <p>The port already registered the twenty comb items
 * ({@code tools/transpile_gt6_multiitems.py} reads {@code MultiItemFood.addItem}), but GT6 keeps the
 * processing rows in that same item class, which {@code tools/transpile_gt6_chem.py} never scans, so
 * nothing could be done with a comb. §73 generated those rows into
 * {@code loaders/c/GTCombGen.java}.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class CombProcessingTests {

    /** GT6's twenty combs, in its registration order. */
    private static final String[] COMBS = {
            "honey_comb", "water_comb", "magic_comb", "nether_comb", "end_comb", "rock_comb",
            "jungle_comb", "frozen_comb", "shroomy_comb", "sandy_comb", "clay_comb", "sticky_comb",
            "royal_comb", "soul_comb", "amnesic_comb", "military_comb", "pyro_comb", "cryo_comb",
            "aero_comb", "tera_comb"};

    private static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", id));
    }

    /** The centrifuge row that consumes a given item, or null. */
    private static Recipe rowFor(Item comb) {
        for (Recipe recipe : MachineRecipeMaps.Centrifuge.mRecipeList) {
            if (recipe.mFakeRecipe) continue;
            for (ItemStack in : recipe.mInputs) {
                if (in != null && !in.isEmpty() && in.is(comb) && in.getCount() == 1) return recipe;
            }
        }
        return null;
    }

    /** Every comb item GT6 registers can actually be centrifuged. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void everyCombIsProcessable(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String id : COMBS) {
            Item comb = item(id);
            if (comb == null) {
                problems.add(id + ": not registered");
                continue;
            }
            Recipe recipe = rowFor(comb);
            if (recipe == null) {
                problems.add(id + ": no centrifuge row");
                continue;
            }
            if (recipe.mEUt != 16 || recipe.mDuration != 64) {
                problems.add(id + ": GT6 uses 16 EU/t for 64 ticks, got " + recipe.mEUt + "/" + recipe.mDuration);
            }
            boolean hasOutput = false;
            for (FluidStack fluid : recipe.mFluidOutputs) {
                if (fluid != null && !fluid.isEmpty()) hasOutput = true;
            }
            for (ItemStack out : recipe.mOutputs) {
                if (out != null && !out.isEmpty()) hasOutput = true;
            }
            if (!hasOutput) problems.add(id + ": the row yields nothing");
        }
        h.assertTrue(problems.isEmpty(), "comb processing, problems (" + problems.size() + "): " + problems);
        h.succeed();
    }

    /** GT6's Honey Comb row: 100 mB of honey plus a bee-wax dust ({@code MultiItemFood:251}). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void honeyCombYieldsHoneyAndWax(GameTestHelper h) {
        Recipe recipe = rowFor(item("honey_comb"));
        h.assertTrue(recipe != null, "the honey comb has a centrifuge row");
        boolean honey = false;
        for (FluidStack fluid : recipe.mFluidOutputs) {
            if (fluid != null && fluid.getAmount() == 100
                    && fluid.getFluid() == com.gregtech.gregtech.registry.GTFluids.still("Honey").get()) {
                honey = true;
            }
        }
        h.assertTrue(honey, "it yields 100 mB of honey: " + java.util.Arrays.toString(recipe.mFluidOutputs));
        ItemStack wax = com.gregtech.gregtech.registry.GTItems.getStack(
                com.gregtech.gregtech.data.MaterialPrefix.dust,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("WaxBee"), 1);
        h.assertTrue(!wax.isEmpty(), "the port registers bee wax (" + wax + ")");
        boolean sawWax = false;
        for (ItemStack out : recipe.mOutputs) {
            if (out != null && !out.isEmpty() && out.is(wax.getItem())) sawWax = true;
        }
        h.assertTrue(sawWax, "GT6's honey comb row also yields bee wax: "
                + java.util.Arrays.toString(recipe.mOutputs));
        h.succeed();
    }

    /** GT6's Royal Comb row is the only one with two fluid outputs and a single 100 % chance. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void royalCombYieldsRoyalJelly(GameTestHelper h) {
        Recipe recipe = rowFor(item("royal_comb"));
        h.assertTrue(recipe != null, "the royal comb has a centrifuge row");
        h.assertTrue(recipe.mFluidOutputs.length == 2,
                "GT6's royal comb yields two fluids, got " + recipe.mFluidOutputs.length);
        int honey = 0, jelly = 0;
        for (FluidStack fluid : recipe.mFluidOutputs) {
            if (fluid == null) continue;
            if (fluid.getFluid() == com.gregtech.gregtech.registry.GTFluids.still("Honey").get()) {
                honey = fluid.getAmount();
            }
            if (fluid.getFluid() == com.gregtech.gregtech.registry.GTFluids.still("RoyalJelly").get()) {
                jelly = fluid.getAmount();
            }
        }
        h.assertTrue(honey == 50 && jelly == 10,
                "GT6 yields 50 mB honey and 10 mB royal jelly, got " + honey + "/" + jelly);
        h.assertTrue(recipe.mChances.length >= 1 && recipe.mChances[0] == 10000,
                "its only output chance is 100 %: " + java.util.Arrays.toString(recipe.mChances));
        h.succeed();
    }
}
