package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.content.recipe.FurnaceSmeltingRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.util.OM;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.TreeMap;

/**
 * Guards GT6's furnace smelting rows ({@code Loader_Recipes_Furnace:137-177}): every ore-processing
 * form of a furnace-capable material has to smelt back into its metal/gem, the amount has to follow
 * GT6's formula, and the rows have to reach the vanilla furnace as well (GT6 {@code RM.add_smelting}
 * writes into {@code FurnaceRecipes}).
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class FurnaceSmeltingTests {

    private static FurnaceSmeltingRecipes.Entry row(String prefixName, GTMaterial material) {
        for (FurnaceSmeltingRecipes.Entry entry : FurnaceSmeltingRecipes.entries()) {
            if (entry.material() == material && entry.prefix().equals(prefixName)) return entry;
        }
        return null;
    }

    /** The first material whose row for {@code prefixName} registered, with a valid output. */
    private static FurnaceSmeltingRecipes.Entry firstRow(String prefixName) {
        for (FurnaceSmeltingRecipes.Entry entry : FurnaceSmeltingRecipes.entries()) {
            if (entry.prefix().equals(prefixName) && !entry.output().isEmpty()) return entry;
        }
        return null;
    }

    /** GT6's target amount: the form's unit weight, converted through the smelting target. */
    private static long targetAmount(GTMaterial material, MaterialPrefix prefix) {
        GTMaterial smelting = material.getTargetSmeltingMaterial();
        if (smelting == null || !smelting.isValid()) smelting = material;
        long smeltingAmount = Math.max(0, material.getTargetSmeltingAmount());
        return com.gregtech.gregtech.api.machine.crucible.CrucibleMath.units(
                com.gregtech.gregtech.api.machine.crucible.CrucibleMath.units(
                        smeltingAmount, GTValues.U, GTValues.U, false),
                GTValues.U, prefix.getMaterialWeight(), false);
    }

    /** A dust of a furnace-capable material smelts into one unit of its metal. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dustSmeltsBackIntoItsMetal(GameTestHelper helper) {
        FurnaceSmeltingRecipes.Entry entry = firstRow("dust");
        helper.assertTrue(entry != null, "the port registers dust furnace rows");
        GTMaterial material = entry.material();
        helper.assertTrue(MaterialWorkability.isFurnace(material),
                "only GT6 FURNACE materials get a row: " + material.getName());
        ItemStack expected = OM.ingot(entry.material().getTargetSmeltingMaterial().isValid()
                ? entry.material().getTargetSmeltingMaterial() : material,
                targetAmount(material, MaterialPrefix.dust));
        helper.assertTrue(ItemStack.isSameItemSameTags(expected, entry.output()),
                "dust smelts into " + expected + " but the row yields " + entry.output());
        int rows = 0;
        for (FurnaceSmeltingRecipes.Entry candidate : FurnaceSmeltingRecipes.entries()) {
            if (candidate.prefix().equals("dust")) rows++;
        }
        helper.assertTrue(rows > 20, "materials with a dust furnace row: " + rows);
        helper.succeed();
    }

    /** A small pile is a quarter unit and a nugget is a ninth — GT6's OM.ingot cascade decides the form. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void pilesAndNuggetsSmeltIntoTheirOwnForm(GameTestHelper helper) {
        for (String prefixName : new String[]{"dustSmall", "dustTiny", "nugget", "crushed", "oreRaw"}) {
            FurnaceSmeltingRecipes.Entry entry = firstRow(prefixName);
            if (entry == null) continue;
            MaterialPrefix prefix = PrefixRegistry.byName(prefixName);
            GTMaterial material = entry.material();
            ItemStack expected = OM.ingot(material.getTargetSmeltingMaterial().isValid()
                    ? material.getTargetSmeltingMaterial() : material, targetAmount(material, prefix));
            helper.assertTrue(ItemStack.isSameItemSameTags(expected, entry.output()),
                    prefixName + " of " + material.getName() + " smelts into " + expected
                            + " but the row yields " + entry.output());
        }
        helper.succeed();
    }

    /** Crushed ore is 9/8 units, so the cascade returns nuggets rather than an ingot (GT6 :194). */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void crushedOreKeepsItsExtraFraction(GameTestHelper helper) {
        FurnaceSmeltingRecipes.Entry entry = firstRow("crushed");
        if (entry == null) {
            helper.succeed();
            return;
        }
        long target = targetAmount(entry.material(), MaterialPrefix.crushed);
        helper.assertTrue(target == MaterialPrefix.crushed.getMaterialWeight(),
                "crushed ore keeps its 9/8 weight: " + target);
        helper.assertTrue(entry.output().getCount() >= 9,
                "a unit and an eighth comes back as at least nine ninth-units: " + entry.output().getCount());
        helper.succeed();
    }

    /** GT6 writes these rows into the vanilla furnace list; the port has to mirror them there. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void rowsReachTheVanillaFurnace(GameTestHelper helper) {
        var added = com.gregtech.gregtech.loaders.Loader_OvenRecipes.addedToVanillaFurnace();
        helper.assertTrue(!added.isEmpty(), "GT furnace rows were mirrored into the vanilla furnace");
        ItemStack sample = added.get(0);
        int vanilla = 0;
        for (var recipe : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
            for (var ingredient : recipe.getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) {
                    if (ItemStack.isSameItemSameTags(stack, sample)) vanilla++;
                }
            }
        }
        helper.assertTrue(vanilla >= 1, "the vanilla furnace knows the GT row for " + sample);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void furnaceSmeltingIsReported(GameTestHelper helper) {
        var json = new TreeMap<String, Object>();
        json.put("rows", FurnaceSmeltingRecipes.entries().size());
        json.put("furnaceTable", MachineRecipeMaps.Furnace.mRecipeList.size());
        json.put("vanillaRows", com.gregtech.gregtech.loaders.Loader_OvenRecipes.addedToVanillaFurnace().size());
        json.put("skipped", FurnaceSmeltingRecipes.skipped());
        json.put("vanillaSmeltingRecipes",
                helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).size());
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/furnace-smelting-coverage.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/furnace-smelting-coverage.json: " + e);
            return;
        }
        helper.assertTrue(FurnaceSmeltingRecipes.entries().size() > 100,
                "furnace smelting rows: " + FurnaceSmeltingRecipes.entries().size());
        helper.succeed();
    }
}
