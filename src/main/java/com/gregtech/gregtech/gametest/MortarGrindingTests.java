package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.recipe.MortarGrindingRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Guards GT6's Mortar handler block ({@code Loader_Recipes_Handlers:79-112}) — hand-grinding is the
 * early-game route to dust, so an empty mortar table means a player cannot process anything before the
 * first machine.
 * <p>
 * The rows are gated on the original material flags, which the port imports through
 * {@code tools/extract_gt6_workability.py} ({@code MORTAR} 195 materials, {@code BRITTLE} 78,
 * {@code FOOD} 26). Outputs are GT6's "pulverized remains", i.e. the exact material amount of the input
 * converted with {@code OM.pulverize} (a 1.25U purified ore therefore yields small piles, not one dust).
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MortarGrindingTests {

    private static final Set<String> DUST_FORMS = Set.of("dust", "dustSmall", "dustTiny", "dustDiv72");

    private static Recipe routeFor(ItemStack input, GTMaterial material) {
        GTMaterial resolved = material.resolve();
        // GT6's pulverized remains go to the material's pulverization target, which is not always the
        // material itself (IronCompressed's dust is iron), so both count as a valid mortar output.
        GTMaterial pulverTarget = resolved.getTargetPulverMaterial();
        for (Recipe recipe : MachineRecipeMaps.Mortar.mRecipeList) {
            if (!contains(recipe.mInputs, input)) continue;
            for (ItemStack output : recipe.mOutputs) {
                if (output == null || output.isEmpty()) continue;
                var form = MaterialEquivalence.form(output);
                if (form == null || !DUST_FORMS.contains(form.prefix().getName())) continue;
                if (form.material() == resolved || form.material() == pulverTarget) return recipe;
            }
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

    private static GTMaterial firstMortarMaterial(String prefixName, boolean requireBrittle) {
        MaterialPrefix prefix = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(prefixName);
        if (prefix == null) return null;
        MaterialPrefix dust = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName("dust");
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid() || !MaterialWorkability.isMortarGrindable(material)) continue;
            if (requireBrittle && !MaterialWorkability.isBrittle(material) && !MaterialWorkability.isFood(material)) {
                continue;
            }
            // GT6's no-dust family (WroughtIron, AnnealedCopper, ...): OM.pulverize returns nothing there,
            // so the original mortar row is a no-op as well and the port stays consistent by skipping it.
            if (GTItems.getStack(dust, material, 1).isEmpty()) continue;
            if (!GTItems.getStack(prefix, material, 1).isEmpty()) return material;
        }
        return null;
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void nuggetsAndOreChunksGrindIntoDust(GameTestHelper helper) {
        int checked = 0;
        List<String> problems = new ArrayList<>();
        for (String row : List.of("nugget", "crushedPurified", "crushedPurifiedTiny", "rockGt",
                "crushedCentrifuged", "billet", "chunkGt", "round", "bolt", "screw", "wireFine")) {
            GTMaterial material = firstMortarMaterial(row, false);
            if (material == null) continue;
            MaterialPrefix prefix = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(row);
            ItemStack input = GTItems.getStack(prefix, material, 1);
            if (input.isEmpty()) continue;
            checked++;
            if (routeFor(input, material) == null) problems.add(row + " / " + material.getName());
        }
        helper.assertTrue(checked >= 8, "mortar rows checked: " + checked);
        helper.assertTrue(problems.isEmpty(), "GT6 mortar rows without a grinding recipe: " + problems);
        helper.succeed();
    }

    /** GT6's gem ladder: a row only exists when the material has no finer gem form, or is brittle/food. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void gemLadderFollowsTheOriginalConditions(GameTestHelper helper) {
        MaterialPrefix gemFlawed = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName("gemFlawed");
        MaterialPrefix gem = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName("gem");
        helper.assertTrue(gemFlawed != null && gem != null, "gem prefixes exist");

        GTMaterial guarded = null;
        GTMaterial brittle = null;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid() || !MaterialWorkability.isMortarGrindable(material)) continue;
            boolean hasGem = !GTItems.getStack(gem, material, 1).isEmpty();
            boolean hasFiner = !GTItems.getStack(gemFlawed, material, 1).isEmpty();
            boolean brittleOrFood = MaterialWorkability.isBrittle(material) || MaterialWorkability.isFood(material);
            if (hasGem && hasFiner && !brittleOrFood && guarded == null) guarded = material;
            if (hasGem && brittleOrFood && brittle == null) brittle = material;
        }
        if (guarded != null) {
            ItemStack input = GTItems.getStack(gem, guarded, 1);
            helper.assertTrue(routeFor(input, guarded) == null,
                    "a real gem with a finer form is not ground by the mortar: " + guarded.getName());
        }
        if (brittle != null) {
            ItemStack input = GTItems.getStack(gem, brittle, 1);
            helper.assertTrue(routeFor(input, brittle) != null,
                    "brittle/food gems are ground by the mortar: " + brittle.getName());
        }
        helper.assertTrue(guarded != null || brittle != null, "the port has at least one gem ladder case");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void coverageIsReported(GameTestHelper helper) {
        int recipes = MachineRecipeMaps.Mortar.mRecipeList.size();
        int routes = MortarGrindingRecipes.entries().size();
        List<String> skipped = MortarGrindingRecipes.skipped();
        helper.assertTrue(routes > 500, "mortar grinding routes registered: " + routes);
        helper.assertTrue(recipes >= routes, "mortar table holds them: " + recipes);
        helper.assertTrue(!skipped.isEmpty(), "GT6 rows the port cannot express stay recorded: " + skipped);
        var json = new java.util.TreeMap<String, Object>();
        json.put("mortarRecipes", recipes);
        json.put("gt6GrindingRoutes", routes);
        json.put("skippedGt6Rows", skipped);
        List<String> byPrefix = new ArrayList<>();
        MortarGrindingRecipes.entries().stream().map(MortarGrindingRecipes.Entry::input).distinct().sorted()
                .forEach(byPrefix::add);
        json.put("inputPrefixes", byPrefix);
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/mortar-coverage.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/mortar-coverage.json: " + e);
            return;
        }
        helper.succeed();
    }
}
