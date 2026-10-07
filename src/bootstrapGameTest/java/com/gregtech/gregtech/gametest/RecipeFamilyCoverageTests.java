package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.content.recipe.AnvilShreddingRecipes;
import com.gregtech.gregtech.content.recipe.AutoclaveRecipes;
import com.gregtech.gregtech.content.recipe.CrusherFamilyRecipes;
import com.gregtech.gregtech.content.recipe.MachineCasingRecipes;
import com.gregtech.gregtech.content.recipe.SharpeningRecipes;
import com.gregtech.gregtech.content.recipe.ShredderRecyclingRecipes;
import com.gregtech.gregtech.content.recipe.WelderFamilyRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.TreeMap;

/**
 * Guards the GT6 handler families that were ported after the form-conversion batch: the sharpening
 * (grindstone) rows, the welder rows, the autoclave crystallisation and the anvil ore grinding.
 * Each family carries the original file:line reference in its own class.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class RecipeFamilyCoverageTests {

    private static Recipe route(RecipeMap map, ItemStack input, ItemStack secondary, ItemStack output) {
        for (Recipe recipe : map.mRecipeList) {
            if (!contains(recipe.mInputs, input)) continue;
            if (secondary != null && !contains(recipe.mInputs, secondary)) continue;
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

    private static GTMaterial firstMaterialWith(java.util.function.Predicate<GTMaterial> filter,
                                                String... prefixNames) {
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid() || !filter.test(material)) continue;
            boolean all = true;
            for (String name : prefixNames) {
                MaterialPrefix prefix = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(name);
                if (prefix == null || GTItems.getStack(prefix, material, 1).isEmpty()) { all = false; break; }
            }
            if (all) return material;
        }
        return null;
    }

    private static GTMaterial firstMaterialWith(String... prefixNames) {
        return firstMaterialWith(m -> true, prefixNames);
    }

    /** GT6 welder rows: SMITHABLE and not FLAMMABLE (Loader_Recipes_Handlers:320-332). */
    private static boolean welderEligible(GTMaterial material) {
        return material.has(com.gregtech.gregtech.api.material.MaterialProperty.SMITHABLE)
                && !material.has(com.gregtech.gregtech.api.material.MaterialProperty.FLAMMABLE);
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void sharpeningGrindsNuggetsAndToolHeads(GameTestHelper helper) {
        GTMaterial material = firstMaterialWith("nugget", "round");
        if (material != null) {
            helper.assertTrue(route(MachineRecipeMaps.Sharpening,
                    GTItems.getStack(MaterialPrefix.nugget, material, 1), null,
                    GTItems.getStack(MaterialPrefix.round, material, 1)) != null,
                    "a nugget sharpens into a round: " + material.getName());
        }
        GTMaterial head = firstMaterialWith("toolHeadRawPickaxe", "toolHeadPickaxe");
        if (head != null) {
            helper.assertTrue(route(MachineRecipeMaps.Sharpening,
                    GTItems.getStack(com.gregtech.gregtech.api.prefix.PrefixRegistry.byName("toolHeadRawPickaxe"),
                            head, 1), null,
                    GTItems.getStack(com.gregtech.gregtech.api.prefix.PrefixRegistry.byName("toolHeadPickaxe"),
                            head, 1)) != null,
                    "a raw pickaxe head sharpens into a finished one: " + head.getName());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void welderBuildsMultiIngotsAndCasings(GameTestHelper helper) {
        GTMaterial material = firstMaterialWith(RecipeFamilyCoverageTests::welderEligible, "ingot", "ingotDouble");
        if (material != null) {
            ItemStack tag = new ItemStack(GTTechnological.selectorTag(2));
            helper.assertTrue(route(MachineRecipeMaps.Welder, GTItems.getStack(MaterialPrefix.ingot, material, 2),
                    tag, GTItems.getStack(MaterialPrefix.ingotDouble, material, 1)) != null,
                    "two ingots and circuit 2 weld into a double ingot: " + material.getName());
        }
        GTMaterial bolt = firstMaterialWith(RecipeFamilyCoverageTests::welderEligible, "bolt", "stick");
        if (bolt != null) {
            helper.assertTrue(route(MachineRecipeMaps.Welder, GTItems.getStack(MaterialPrefix.bolt, bolt, 4),
                    new ItemStack(GTTechnological.selectorTag(4)), GTItems.getStack(MaterialPrefix.stick, bolt, 1)) != null,
                    "four bolts weld into a rod: " + bolt.getName());
        }
        // Casings come from the welder for every smithable, non-flammable material (GT6 :349-358).
        int casingMaterials = 0;
        for (GTMaterial m : GTMaterialRegistry.allMaterials()) {
            if (!m.isValid() || !m.has(com.gregtech.gregtech.api.material.MaterialProperty.SMITHABLE)) continue;
            ItemStack casing = GTBlocks.getStack(BlockMaterialPrefix.casingMachine, m);
            if (casing.isEmpty()) continue;
            if (route(MachineRecipeMaps.Welder, GTItems.getStack(MaterialPrefix.plate, m, 6),
                    GTItems.getStack(MaterialPrefix.stickLong, m, 2), casing) != null) casingMaterials++;
        }
        helper.assertTrue(casingMaterials > 100, "materials with a welding casing recipe: " + casingMaterials);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void autoclaveGrowsGemsFromDust(GameTestHelper helper) {
        GTMaterial material = null;
        for (GTMaterial m : GTMaterialRegistry.allMaterials()) {
            if (!m.isValid() || !MaterialWorkability.isCrystallisable(m)) continue;
            if (GTItems.getStack(MaterialPrefix.dustSmall, m, 1).isEmpty()) continue;
            if (GTItems.getStack(MaterialPrefix.gem, m, 1).isEmpty()) continue;
            material = m;
            break;
        }
        if (material == null) {
            helper.succeed();
            return;
        }
        net.minecraftforge.fluids.FluidStack steam =
                com.gregtech.gregtech.registry.GTFluids.stack("Steam", 102400);
        helper.assertTrue(steam != null, "the port has a steam fluid for the autoclave");
        final GTMaterial crystallisable = material;
        Recipe recipe = MachineRecipeMaps.Autoclave.mRecipeList.stream()
                .filter(r -> contains(r.mInputs, GTItems.getStack(MaterialPrefix.dustSmall, crystallisable, 4)))
                .findFirst().orElse(null);
        helper.assertTrue(recipe != null && recipe.mEUt == 0 && recipe.mFluidInputs.length >= 1,
                "steam powered crystallisation row exists for " + material.getName());
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void anvilGrindsOreChunksByHand(GameTestHelper helper) {
        GTMaterial material = firstMaterialWith(MaterialWorkability::isMortarGrindable,
                "crushed", "dust", "dustDiv72");
        if (material != null) {
            Recipe recipe = route(MachineRecipeMaps.Anvil,
                    GTItems.getStack(MaterialPrefix.crushed, material, 1), null,
                    GTItems.getStack(MaterialPrefix.dust, material, 1));
            helper.assertTrue(recipe != null, "crushed ore grinds into dust on the anvil: " + material.getName());
            helper.assertTrue(recipe.mInputs.length == 2 && recipe.mInputs[1].isEmpty(),
                    "the anvil row requires the second slot to stay empty (GT6 ST.emptySlot)");
            helper.assertTrue(contains(recipe.mOutputs, GTItems.getStack(MaterialPrefix.dustDiv72, material, 9)),
                    "the pulverized remainder comes out as dustDiv72");
        }
        GTMaterial rock = firstMaterialWith("rockGt", "dustSmall");
        if (rock != null) {
            helper.assertTrue(route(MachineRecipeMaps.Anvil, GTItems.getStack(MaterialPrefix.rockGt, rock, 1),
                    null, GTItems.getStack(MaterialPrefix.dustSmall, rock, 9)) != null,
                    "rock grinds into nine small piles: " + rock.getName());
        }
        helper.succeed();
    }

    /** GT6 :152-155 — recycling a part returns its pulverized remains; MORTAR materials are cheaper. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void shredderRecyclesPartsIntoDust(GameTestHelper helper) {
        // The recycling family's own rows, keyed by the material of their plate input. GT6's output is
        // OM.pulverize, whose dust can belong to the input's pulverization target instead, so rows are
        // matched by their plate input alone.
        java.util.Map<String, Recipe> plateRows = new java.util.HashMap<>();
        for (ShredderRecyclingRecipes.Entry entry : ShredderRecyclingRecipes.entries()) {
            if (!"plate".equals(entry.input())) continue;
            for (ItemStack in : entry.recipe().mInputs) {
                if (in == null || in.isEmpty()) continue;
                GTMaterial material = com.gregtech.gregtech.item.MaterialItem.getMaterial(in);
                if (material != null) plateRows.putIfAbsent(material.getName(), entry.recipe());
            }
        }
        helper.assertTrue(plateRows.size() > 100, "materials with a plate recycling row: " + plateRows.size());

        // The duration is units * multiplier * (toolQuality + 1), so the two materials being compared
        // have to share a tool quality — otherwise the 16x gap is hidden by the quality factor.
        GTMaterial soft = null;
        GTMaterial hard = null;
        for (GTMaterial m : GTMaterialRegistry.allMaterials()) {
            if (!m.isValid() || !plateRows.containsKey(m.getName())) continue;
            boolean mortar = MaterialWorkability.isMortarGrindable(m);
            if (mortar && soft == null) soft = m;
            if (!mortar && hard == null) hard = m;
            if (soft != null && hard != null && soft.getToolQuality() == hard.getToolQuality()) break;
        }
        helper.assertTrue(soft != null && hard != null,
                "the port recycles both a mortar and a non-mortar material's plates");
        long softTicks = plateRows.get(soft.getName()).mDuration / (soft.getToolQuality() + 1);
        long hardTicks = plateRows.get(hard.getName()).mDuration / (hard.getToolQuality() + 1);
        helper.assertTrue(hardTicks == softTicks * 16,
                "non-mortar materials pay the 256 multiplier instead of 16: "
                        + hardTicks + " vs " + softTicks);
        helper.succeed();
    }

    /** GT6 :150-156 — the crusher's gem ladder and the boule row. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void crusherCrumblesGemsDownTheLadder(GameTestHelper helper) {
        GTMaterial gemMaterial = firstMaterialWith("gem", "gemFlawed");
        if (gemMaterial != null) {
            helper.assertTrue(route(MachineRecipeMaps.Crusher, GTItems.getStack(MaterialPrefix.gem, gemMaterial, 1),
                    null, GTItems.getStack(MaterialPrefix.gemFlawed, gemMaterial, 2)) != null,
                    "a gem crushes into two flawed gems: " + gemMaterial.getName());
        }
        GTMaterial bouleMaterial = firstMaterialWith("bouleGt", "gem");
        if (bouleMaterial != null) {
            helper.assertTrue(route(MachineRecipeMaps.Crusher,
                    GTItems.getStack(com.gregtech.gregtech.api.prefix.PrefixRegistry.byName("bouleGt"),
                            bouleMaterial, 1), null,
                    GTItems.getStack(MaterialPrefix.gem, bouleMaterial, 4)) != null,
                    "a boule crushes into four gems: " + bouleMaterial.getName());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void familiesAreReported(GameTestHelper helper) {
        var json = new TreeMap<String, Object>();
        json.put("sharpeningRoutes", SharpeningRecipes.entries().size());
        json.put("welderRows", WelderFamilyRecipes.entries().size());
        json.put("autoclaveRoutes", AutoclaveRecipes.entries().size());
        json.put("anvilShreddingRoutes", AnvilShreddingRecipes.entries().size());
        json.put("shredderRecyclingRoutes", ShredderRecyclingRecipes.entries().size());
        json.put("crusherRows", CrusherFamilyRecipes.entries().size());
        json.put("sharpeningTable", MachineRecipeMaps.Sharpening.mRecipeList.size());
        json.put("welderTable", MachineRecipeMaps.Welder.mRecipeList.size());
        json.put("autoclaveTable", MachineRecipeMaps.Autoclave.mRecipeList.size());
        json.put("anvilTable", MachineRecipeMaps.Anvil.mRecipeList.size());
        json.put("shredderTable", MachineRecipeMaps.Shredder.mRecipeList.size());
        json.put("crusherTable", MachineRecipeMaps.Crusher.mRecipeList.size());
        json.put("sharpeningNotes", SharpeningRecipes.skipped());
        json.put("welderNotes", WelderFamilyRecipes.skipped());
        json.put("autoclaveNotes", AutoclaveRecipes.skipped());
        json.put("anvilNotes", AnvilShreddingRecipes.skipped());
        json.put("shredderNotes", ShredderRecyclingRecipes.skipped());
        json.put("crusherNotes", CrusherFamilyRecipes.skipped());
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/recipe-family-coverage.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/recipe-family-coverage.json: " + e);
            return;
        }
        helper.assertTrue(SharpeningRecipes.entries().size() > 100, "sharpening routes: "
                + SharpeningRecipes.entries().size());
        helper.assertTrue(WelderFamilyRecipes.entries().size() > 100, "welder rows: "
                + WelderFamilyRecipes.entries().size());
        helper.assertTrue(AutoclaveRecipes.entries().size() > 100, "autoclave routes: "
                + AutoclaveRecipes.entries().size());
        helper.assertTrue(AnvilShreddingRecipes.entries().size() > 100, "anvil shredding routes: "
                + AnvilShreddingRecipes.entries().size());
        helper.assertTrue(ShredderRecyclingRecipes.entries().size() > 1000, "shredder recycling routes: "
                + ShredderRecyclingRecipes.entries().size());
        helper.assertTrue(CrusherFamilyRecipes.entries().size() > 100, "crusher rows: "
                + CrusherFamilyRecipes.entries().size());
        helper.succeed();
    }
}
