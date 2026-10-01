package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Guards GT6's crucible temperature requirements and the {@code NEVER_FURNACE} iron gate.
 * <p>
 * Regressions these tests exist for: the "Crucible Smelting" recipe tab was empty, so the crucible
 * showed no temperatures and no {@code Pig Iron → Wrought Iron} entry (the iron chain's only route
 * to wrought iron), and the vanilla furnace still smelted iron ore straight into iron although GT6
 * flags Iron {@code NEVER_FURNACE} and rewrites those recipes to scrap.
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class CrucibleTemperatureTests {

    /** GT6 {@code RM.CrucibleSmelting}: every melting material's forms → its smelting target. */
    @GameTest(template = "test_empty")
    public static void crucibleSmeltingRecipesCarryTemperatures(GameTestHelper helper) {
        var recipes = MachineRecipeMaps.CrucibleSmelting.mRecipeList;
        helper.assertTrue(recipes.size() > 100, "crucible smelting recipes: " + recipes.size());
        helper.assertTrue(MachineRecipeMaps.CrucibleSmelting.hasSpecialValueLabel(),
                "the crucible smelting map declares its temperature line");
        helper.assertTrue(MachineRecipeMaps.CrucibleSmelting.mSpecialValuePre.contains("Temperature"),
                "GT6 labels the special value 'Temperature: ', got "
                        + MachineRecipeMaps.CrucibleSmelting.mSpecialValuePre);
        helper.assertTrue(MachineRecipeMaps.CrucibleAlloying.hasSpecialValueLabel(),
                "the combination smelting map declares its temperature line");

        TreeSet<String> problems = new TreeSet<>();
        for (Recipe recipe : recipes) {
            if (recipe.mSpecialValue <= 0) problems.add("no temperature: " + describe(recipe));
            if (!recipe.mFakeRecipe) problems.add("display recipe is executable: " + describe(recipe));
            if (recipe.mInputs.length == 0 || recipe.mOutputs.length == 0) {
                problems.add("incomplete recipe: " + describe(recipe));
            }
        }
        helper.assertTrue(problems.isEmpty(), "crucible recipes without temperature requirements: " + problems);
        helper.succeed();
    }

    /**
     * The iron chain: GT6's {@code PigIron.mTargetSmelting = WroughtIron} means the crucible turns
     * pig iron into wrought iron at 2011 K, and {@code WroughtIron + Air → Steel} at 2046 K.
     */
    @GameTest(template = "test_empty")
    public static void pigIronMeltsIntoWroughtIron(GameTestHelper helper) {
        GTMaterial pigIron = GTMaterialRegistry.get("PigIron").resolve();
        GTMaterial wroughtIron = GTMaterialRegistry.get("WroughtIron").resolve();
        helper.assertTrue(pigIron.isValid() && wroughtIron.isValid(), "Pig Iron and Wrought Iron exist");
        helper.assertTrue(pigIron.getTargetSmeltingMaterial() == wroughtIron,
                "Pig Iron's smelting target is Wrought Iron (GT6 MT.java:1724)");

        Recipe recipe = find(MaterialPrefix.ingot, pigIron, wroughtIron);
        helper.assertTrue(recipe != null,
                "the crucible lists Pig Iron → Wrought Iron (the iron chain's wrought iron route)");
        helper.assertTrue(recipe.mSpecialValue == pigIron.getMeltingPoint(),
                "it melts at Pig Iron's melting point, got " + recipe.mSpecialValue
                        + " expected " + pigIron.getMeltingPoint());

        // Wrought iron itself is the input of the steel reaction (GT6 MT.java:3348).
        var steelReaction = com.gregtech.gregtech.api.machine.crucible.CrucibleReactions.allRecipes().stream()
                .filter(r -> r.output().resolve() == Materials.Steel.resolve())
                .findFirst().orElse(null);
        helper.assertTrue(steelReaction != null, "the crucible has a Steel reaction");
        helper.assertTrue(steelReaction.parts().stream()
                        .anyMatch(p -> p.material().resolve() == wroughtIron),
                "Steel is made from wrought iron in the crucible");
        helper.succeed();
    }

    /**
     * The full reachable chain of GT6's wrought iron: iron is melted (Melter) and the molten iron
     * is converted in the Smelter ({@code Loader_Recipes_Alloys:35}), because the crucible itself
     * only lists {@code Pig Iron → Wrought Iron} and Iron's own target is Iron.
     */
    @GameTest(template = "test_empty")
    public static void wroughtIronIsReachableThroughTheSmelter(GameTestHelper helper) {
        ItemStack ironIngot = GTItems.getStack(MaterialPrefix.ingot, Materials.Iron, 1);
        ItemStack ironDust = GTItems.getStack(MaterialPrefix.dust, Materials.Iron, 1);
        helper.assertTrue(!ironIngot.isEmpty() || !ironDust.isEmpty(), "iron has an ingot or dust form");

        boolean melts = false;
        for (Recipe recipe : MachineRecipeMaps.Melter.mRecipeList) {
            if (recipe.mFluidOutputs.length == 0) continue;
            String out = recipe.mFluidOutputs[0].getFluid().builtInRegistryHolder().key().location().getPath();
            if (!out.contains("iron")) continue;
            for (ItemStack in : recipe.mInputs) {
                if (!in.isEmpty() && (in.is(ironIngot.getItem()) || in.is(ironDust.getItem()))) melts = true;
            }
        }
        helper.assertTrue(melts, "the Melter turns iron into molten iron");

        Recipe conversion = null;
        for (Recipe recipe : MachineRecipeMaps.Smelter.mRecipeList) {
            if (recipe.mFluidInputs.length == 0 || recipe.mFluidOutputs.length == 0) continue;
            String in = recipe.mFluidInputs[0].getFluid().builtInRegistryHolder().key().location().getPath();
            String out = recipe.mFluidOutputs[0].getFluid().builtInRegistryHolder().key().location().getPath();
            if (in.contains("iron") && !in.contains("wrought") && out.contains("wrought")) conversion = recipe;
        }
        helper.assertTrue(conversion != null,
                "the Smelter converts molten iron into molten wrought iron (GT6 Loader_Recipes_Alloys:35)");
        helper.assertTrue(conversion.mEUt == 16 && conversion.mDuration == 16,
                "GT6 rates: 16 EU/t for 16 ticks, got " + conversion.mEUt + "/" + conversion.mDuration);
        helper.succeed();
    }

    /** GT6 {@code TD.Processing.NEVER_FURNACE}: those metals may not come out of a furnace. */
    @GameTest(template = "test_empty")
    public static void neverFurnaceMetalsCannotBeSmeltedInAFurnace(GameTestHelper helper) {
        TreeSet<String> problems = new TreeSet<>();
        for (String name : new String[]{"Iron", "WroughtIron", "Steel", "Titanium", "Tungsten"}) {
            GTMaterial material = GTMaterialRegistry.get(name).resolve();
            if (!material.isValid() || !material.has(MaterialProperty.NEVER_FURNACE)) {
                problems.add(name + " is not flagged NEVER_FURNACE");
            }
        }
        helper.assertTrue(problems.isEmpty(), "GT6 NEVER_FURNACE materials: " + problems);

        // The GT oven map must not produce them either — GT6 substitutes scrapGt, which is a form of
        // the same material, so the scrap prefix itself is the expected (and only) exception.
        TreeSet<String> smelted = new TreeSet<>();
        int scrapOutputs = 0;
        for (Recipe recipe : MachineRecipeMaps.Furnace.mRecipeList) {
            for (ItemStack output : recipe.mOutputs) {
                if (MachineRecipeMaps.isNeverFurnace(output)) {
                    if (output.getItem() instanceof com.gregtech.gregtech.item.MaterialItem item
                            && item.getPrefix() == MaterialPrefix.scrapGt) {
                        scrapOutputs++;
                    } else {
                        smelted.add(describe(recipe));
                    }
                }
            }
        }
        helper.assertTrue(smelted.isEmpty(), "furnace recipes still producing NEVER_FURNACE metals: " + smelted);
        helper.assertTrue(scrapOutputs > 20,
                "GT6 un-smelts those recipes into scrap, got " + scrapOutputs + " scrap outputs");

        // …and the vanilla furnace, whose recipes are rewritten on server start, must yield scrap.
        var vanilla = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream()
                .filter(r -> r.getIngredients().size() == 1
                        && java.util.Arrays.stream(r.getIngredients().get(0).getItems())
                                .anyMatch(s -> s.is(Items.IRON_ORE) || s.is(Items.RAW_IRON)
                                        || s.is(Items.DEEPSLATE_IRON_ORE)))
                .toList();
        helper.assertTrue(!vanilla.isEmpty(), "vanilla iron ore smelting recipes are loaded");
        for (var recipe : vanilla) {
            ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
            helper.assertTrue(!MachineRecipeMaps.isNeverFurnace(result),
                    "vanilla furnace no longer yields a NEVER_FURNACE metal: " + recipe.getId() + " → " + result);
        }

        // The scrap form is what GT6 substitutes; it must exist for iron.
        ItemStack scrap = GTItems.getStack(MaterialPrefix.scrapGt, Materials.Iron, 1);
        helper.assertTrue(!scrap.isEmpty(), "iron scrap exists as the un-smelted furnace result");
        helper.succeed();
    }

    /** Un-smelting must not have removed unrelated smelting recipes. */
    @GameTest(template = "test_empty")
    public static void unSmeltingKeepsOtherRecipes(GameTestHelper helper) {
        var smelting = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING);
        helper.assertTrue(smelting.size() > 100,
                "vanilla smelting recipes survived the NEVER_FURNACE rewrite: " + smelting.size());
        boolean copper = smelting.stream().anyMatch(r -> java.util.Arrays.stream(r.getIngredients().get(0).getItems())
                .anyMatch(s -> s.is(Items.COPPER_ORE) || s.is(Items.RAW_COPPER)));
        helper.assertTrue(copper, "copper ore still smeltable (GT6 only gates NEVER_FURNACE materials)");
        helper.succeed();
    }

    /** The temperature coverage stays on the record. */
    @GameTest(template = "test_empty")
    public static void coverageIsReported(GameTestHelper helper) {
        var report = new TreeMap<String, Object>();
        report.put("crucibleSmeltingRecipes", MachineRecipeMaps.CrucibleSmelting.mRecipeList.size());
        report.put("crucibleAlloyingRecipes", MachineRecipeMaps.CrucibleAlloying.mRecipeList.size());
        report.put("furnaceRecipes", MachineRecipeMaps.Furnace.mRecipeList.size());
        report.put("specialValueLabel", MachineRecipeMaps.CrucibleSmelting.mSpecialValuePre
                + "%s" + MachineRecipeMaps.CrucibleSmelting.mSpecialValuePost);
        var melting = new ArrayList<String>();
        for (String name : new String[]{"Iron", "PigIron", "WroughtIron", "Steel", "Copper", "AnnealedCopper",
                "Bauxite", "Ilmenite", "Hematite", "Magnetite"}) {
            GTMaterial material = GTMaterialRegistry.get(name).resolve();
            if (!material.isValid()) continue;
            melting.add(name + " melting " + material.getMeltingPoint() + " K → "
                    + material.getTargetSmeltingMaterial().getName());
        }
        report.put("chain", melting);
        java.nio.file.Path out = java.nio.file.Path.of("../../docs/crucible-coverage.json");
        try {
            java.nio.file.Files.writeString(out, new com.google.gson.GsonBuilder().setPrettyPrinting()
                    .create().toJson(report));
        } catch (Exception e) {
            helper.assertTrue(false, "cannot write crucible coverage report: " + e);
        }
        helper.succeed();
    }

    private static Recipe find(MaterialPrefix prefix, GTMaterial input, GTMaterial output) {
        ItemStack expectedInput = GTItems.getStack(prefix, input, 1);
        if (expectedInput.isEmpty()) return null;
        for (Recipe recipe : MachineRecipeMaps.CrucibleSmelting.mRecipeList) {
            if (recipe.mInputs.length == 0 || recipe.mOutputs.length == 0) continue;
            if (!ItemStack.isSameItemSameTags(recipe.mInputs[0], expectedInput)) continue;
            for (ItemStack out : recipe.mOutputs) {
                if (!out.isEmpty() && out.getItem() instanceof com.gregtech.gregtech.item.MaterialItem item
                        && item.getMaterial().resolve() == output) {
                    return recipe;
                }
            }
        }
        return null;
    }

    private static String describe(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack in : recipe.mInputs) sb.append(in.isEmpty() ? "-" : in.getHoverName().getString()).append(' ');
        sb.append("→ ");
        for (ItemStack out : recipe.mOutputs) sb.append(out.isEmpty() ? "-" : out.getHoverName().getString()).append(' ');
        return sb.toString().trim();
    }
}
