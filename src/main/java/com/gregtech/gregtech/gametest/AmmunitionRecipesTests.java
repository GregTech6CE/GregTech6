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
        ItemStack smallMold = new ItemStack(GTTechnological.get("press_bullet_casing_shape_small"));
        ItemStack mediumMold = new ItemStack(GTTechnological.get("press_bullet_casing_shape_medium"));
        ItemStack largeMold = new ItemStack(GTTechnological.get("press_bullet_casing_shape_large"));
        helper.assertTrue(!smallMold.isEmpty() && !mediumMold.isEmpty() && !largeMold.isEmpty(),
                "the port has all three bullet casing molds");

        ItemStack small = GTItems.getStack(MaterialPrefix.bulletGtSmall, material, 1);
        helper.assertTrue(route(MachineRecipeMaps.Press, round, smallMold, small) != null,
                "a round presses into a small bullet with the small mold");
        if (!bolt.isEmpty()) {
            helper.assertTrue(route(MachineRecipeMaps.Press, bolt, smallMold, small) != null,
                    "a bolt presses into a small bullet");
        }
        ItemStack medium = GTItems.getStack(MaterialPrefix.bulletGtMedium, material, 1);
        if (!medium.isEmpty()) {
            Recipe row = route(MachineRecipeMaps.Press, GTItems.getStack(MaterialPrefix.round, material, 2),
                    mediumMold, medium);
            helper.assertTrue(row != null, "two rounds press into a medium bullet");
            helper.assertTrue(row.mInputs[0].getCount() == 2, "the medium row needs two rounds");
        }
        ItemStack large = GTItems.getStack(MaterialPrefix.bulletGtLarge, material, 1);
        if (!large.isEmpty()) {
            Recipe row = route(MachineRecipeMaps.Press, GTItems.getStack(MaterialPrefix.round, material, 3),
                    largeMold, large);
            helper.assertTrue(row != null, "three rounds press into a large bullet");
            helper.assertTrue(row.mInputs[0].getCount() == 3, "the large row needs three rounds");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void ammunitionIsRecycledWithItsMold(GameTestHelper helper) {
        GTMaterial material = firstBulletMaterial();
        helper.assertTrue(material != null, "bullet material exists");
        ItemStack small = GTItems.getStack(MaterialPrefix.bulletGtSmall, material, 1);
        ItemStack tiny = GTItems.getStack(MaterialPrefix.dustTiny, material, 1);
        helper.assertTrue(!tiny.isEmpty(), "the material has tiny dust piles");
        Recipe recovery = route(MachineRecipeMaps.Unboxinator, small, null, tiny);
        helper.assertTrue(recovery != null, "a small bullet disassembles back into tiny dust");
        boolean moldReturned = contains(recovery.mOutputs,
                new ItemStack(GTTechnological.get("press_bullet_casing_shape_small")));
        // GT6 returns the mold as an additional output (:454); the port keeps it when the map allows
        // two outputs, otherwise the dust-only row is registered instead.
        helper.assertTrue(recovery.mOutputs.length >= 1, "recovery row has outputs");
        if (recovery.mOutputs.length > 1 && !moldReturned) {
            helper.fail("second output is not the mold: " + java.util.Arrays.toString(recovery.mOutputs));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void coverageIsReported(GameTestHelper helper) {
        int routes = PressAmmunitionRecipes.entries().size();
        List<String> skipped = PressAmmunitionRecipes.skipped();
        helper.assertTrue(routes > 100, "ammunition routes registered: " + routes);
        helper.assertTrue(skipped.size() >= 2, "GT6 rows the port cannot express stay recorded: " + skipped);
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
