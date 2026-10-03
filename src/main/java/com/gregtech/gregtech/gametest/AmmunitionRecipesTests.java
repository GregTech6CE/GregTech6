package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.recipe.PressAmmunitionRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Guards GT6's ammunition rows: the Press bullet handlers ({@code Loader_Recipes_Handlers:254-258}) and
 * the Unboxinator recovery rows ({@code :454-456}) — the port had no way to produce or recycle bullets.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class AmmunitionRecipesTests {

    private static Recipe route(com.gregtech.gregtech.api.recipe.RecipeMap map, ItemStack input, ItemStack mold,
                                ItemStack output) {
        for (Recipe recipe : map.mRecipeList) {
            if (!contains(recipe.mInputs, input)) continue;
            if (mold != null && !mold.isEmpty() && !contains(recipe.mInputs, mold)) continue;
            if (contains(recipe.mOutputs, output)) return recipe;
        }
        return null;
    }

    private static boolean contains(ItemStack[] stacks, ItemStack expected) {
        if (expected.isEmpty()) return false;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameTags(stack, expected) || MaterialEquivalence.matches(expected, stack)) {
                return true;
            }
        }
        return false;
    }

    private static GTMaterial firstBulletMaterial() {
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid()) continue;
            if (GTItems.getStack(MaterialPrefix.round, material, 1).isEmpty()) continue;
            if (GTItems.getStack(MaterialPrefix.bulletGtSmall, material, 1).isEmpty()) continue;
            return material;
        }
        return null;
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void bulletsComeFromRoundsAndBolts(GameTestHelper helper) {
        GTMaterial material = firstBulletMaterial();
        helper.assertTrue(material != null, "the port has a material with rounds and bullets");
        ItemStack round = GTItems.getStack(MaterialPrefix.round, material, 1);
        ItemStack bolt = GTItems.getStack(MaterialPrefix.bolt, material, 1);
        ItemStack smallCasing = GTItems.getStack(MaterialPrefix.bulletGtSmall, com.gregtech.gregtech.content.material.Materials.Empty, 1);
        ItemStack mediumCasing = GTItems.getStack(MaterialPrefix.bulletGtMedium, com.gregtech.gregtech.content.material.Materials.Empty, 1);
        ItemStack largeCasing = GTItems.getStack(MaterialPrefix.bulletGtLarge, com.gregtech.gregtech.content.material.Materials.Empty, 1);
        helper.assertTrue(!smallCasing.isEmpty() && !mediumCasing.isEmpty() && !largeCasing.isEmpty(),
                "the port has all three charged bullet casings");

        ItemStack small = GTItems.getStack(MaterialPrefix.bulletGtSmall, material, 1);
        helper.assertTrue(route(MachineRecipeMaps.Press, round, smallCasing, small) != null,
                "a round consumes a charged small casing to make a bullet");
        if (!bolt.isEmpty()) {
            helper.assertTrue(route(MachineRecipeMaps.Press, bolt, smallCasing, small) != null,
                    "a bolt presses into a small bullet");
        }
        ItemStack medium = GTItems.getStack(MaterialPrefix.bulletGtMedium, material, 1);
        if (!medium.isEmpty()) {
            Recipe row = route(MachineRecipeMaps.Press, GTItems.getStack(MaterialPrefix.round, material, 2),
                    mediumCasing, medium);
            helper.assertTrue(row != null, "two rounds press into a medium bullet");
            helper.assertTrue(row.mInputs[0].getCount() == 2, "the medium row needs two rounds");
        }
        ItemStack large = GTItems.getStack(MaterialPrefix.bulletGtLarge, material, 1);
        if (!large.isEmpty()) {
            Recipe row = route(MachineRecipeMaps.Press, GTItems.getStack(MaterialPrefix.round, material, 3),
                    largeCasing, large);
            helper.assertTrue(row != null, "three rounds press into a large bullet");
            helper.assertTrue(row.mInputs[0].getCount() == 3, "the large row needs three rounds");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void ammunitionIsRecycledWithItsChargedCasing(GameTestHelper helper) {
        GTMaterial material = firstBulletMaterial();
        helper.assertTrue(material != null, "bullet material exists");
        ItemStack small = GTItems.getStack(MaterialPrefix.bulletGtSmall, material, 1);
        ItemStack tiny = GTItems.getStack(MaterialPrefix.dustTiny, material, 1);
        helper.assertTrue(!tiny.isEmpty(), "the material has tiny dust piles");
        Recipe recovery = route(MachineRecipeMaps.Unboxinator, small, null, tiny);
        helper.assertTrue(recovery != null, "a small bullet disassembles back into tiny dust");
        boolean casingReturned = contains(recovery.mOutputs,
                GTItems.getStack(MaterialPrefix.bulletGtSmall, com.gregtech.gregtech.content.material.Materials.Empty, 1));
        helper.assertTrue(recovery.mOutputs.length == 2 && casingReturned,
                "GT6 returns the tiny metal dust and charged casing, never a press mold");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void coverageIsReported(GameTestHelper helper) {
        int routes = PressAmmunitionRecipes.entries().size();
        List<String> skipped = PressAmmunitionRecipes.skipped();
        helper.assertTrue(routes > 100, "ammunition routes registered: " + routes);
        helper.assertTrue(skipped.isEmpty(), "all source ammunition rows are represented: " + skipped);
        var json = new java.util.TreeMap<String, Object>();
        json.put("ammunitionRoutes", routes);
        json.put("pressRecipes", MachineRecipeMaps.Press.mRecipeList.size());
        json.put("unboxinatorRecipes", MachineRecipeMaps.Unboxinator.mRecipeList.size());
        json.put("skippedGt6Rows", skipped);
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/ammunition-coverage.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/ammunition-coverage.json: " + e);
            return;
        }
        helper.succeed();
    }
}
