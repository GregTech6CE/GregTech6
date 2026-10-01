package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.content.recipe.StoneAndToolSurvivalRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.loaders.c.GTGeneratedChem;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guards the GT6 {@code BlockStones:286-315} / {@code :331-360} stone and cobblestone extrusion rows.
 * <p>
 * Regression these tests exist for: the port had no stone extrusion at all, so a player could not turn a
 * rock into plates, rods or the raw tool heads that the sharpening chain turns into stone tools, and the
 * first implementation registered one collapsed recipe per rock type (the stone group's block-mould row
 * re-emits its own input, and an optimized self-cancelling row poisons {@link RecipeMap#findCollision}).
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class StoneExtrusionTests {

    private static final String[] MOLDS = StoneAndToolSurvivalRecipes.stoneExtrusionMolds();

    /** {@code dumps} the extruder table once: (input item id) -> recipes using it as an input. */
    private static Map<String, List<Recipe>> index() {
        Map<String, List<Recipe>> index = new LinkedHashMap<>();
        for (Recipe recipe : MachineRecipeMaps.Extruder.mRecipeList) {
            for (ItemStack input : recipe.mInputs) {
                if (input == null || input.isEmpty()) continue;
                index.computeIfAbsent(id(input), ignored -> new ArrayList<>()).add(recipe);
            }
        }
        return index;
    }

    private static String id(ItemStack stack) {
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem())
                + "@" + stack.getDamageValue();
    }

    private static boolean contains(ItemStack[] stacks, ItemStack expected) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty() || expected.isEmpty()) continue;
            if (ItemStack.isSameItemSameTags(stack, expected)) return true;
        }
        return false;
    }

    private static Recipe route(Map<String, List<Recipe>> index, ItemStack input, String moldId, ItemStack output) {
        List<Recipe> candidates = index.get(id(input));
        if (candidates == null) return null;
        for (Recipe recipe : candidates) {
            if (!contains(recipe.mInputs, input)) continue;
            if (!contains(recipe.mInputs, mold(moldId))) continue;
            if (contains(recipe.mOutputs, output)) return recipe;
        }
        return null;
    }

    private static ItemStack mold(String moldId) {
        var item = GTTechnological.get(moldId);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * Every rock that has a material form must reach it from both the stone and the cobblestone block,
     * through both mould families, at GT6's 16 EU/t for 32 ticks.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void stoneAndCobblestoneExtrudeIntoTheirForms(GameTestHelper helper) {
        var index = index();
        int checked = 0;
        int missingForms = 0;
        int groups = 0;
        int expectedRegistered = 0;
        List<String> failures = new ArrayList<>();
        for (StoneType type : StoneType.values()) {
            Block stoneBlock = GTBlocks.getStone(type, StoneVariant.STONE);
            Block cobbleBlock = GTBlocks.getStone(type, StoneVariant.COBBLE);
            if (stoneBlock == null || cobbleBlock == null) continue;
            groups++;
            // Two blocks x two mould families x (the ingot-mould and block-mould rows).
            expectedRegistered += 8;
            String material = type.material().getName();
            for (String[] shape : StoneAndToolSurvivalRecipes.stoneExtrusionShapes()) {
                ItemStack expected = GTGeneratedChem.resolveSpec(String.format(shape[1], material));
                if (expected == null || expected.isEmpty()) {
                    missingForms++;
                    continue;
                }
                for (Block block : List.of(stoneBlock, cobbleBlock)) {
                    for (String family : MOLDS) {
                        Recipe recipe = route(index, new ItemStack(block), family + shape[0], expected);
                        if (recipe == null) {
                            failures.add(id(new ItemStack(block)) + " + " + family + shape[0] + " -> " + id(expected));
                            continue;
                        }
                        checked++;
                        expectedRegistered++;
                        if (recipe.mEUt != 16 || recipe.mDuration != 32) {
                            failures.add(id(new ItemStack(block)) + " + " + family + shape[0]
                                    + " runs at " + recipe.mEUt + " EU/t for " + recipe.mDuration + " ticks");
                        }
                    }
                }
            }
        }
        helper.assertTrue(failures.isEmpty(), "missing stone extrusion routes (" + failures.size()
                + "): " + failures.subList(0, Math.min(5, failures.size())));
        helper.assertTrue(checked > 0, "stone extrusion routes verified: " + checked);
        // Every row the test resolved must be registered, and nothing else may be.
        helper.assertTrue(StoneAndToolSurvivalRecipes.stoneExtrusionCount() == expectedRegistered,
                "registered stone extrusion rows " + StoneAndToolSurvivalRecipes.stoneExtrusionCount()
                        + " != resolved routes " + expectedRegistered);
        writeReport(helper, checked, missingForms, groups, expectedRegistered);
        helper.succeed();
    }

    /** GT6 sends the ingot mould to the brick variant and the block mould to the plain stone variant. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void blockOutputsAreTheRocksOwnVariants(GameTestHelper helper) {
        var index = index();
        Block granite = GTBlocks.getStone(StoneType.GRANITE_BLACK, StoneVariant.STONE);
        Block cobble = GTBlocks.getStone(StoneType.GRANITE_BLACK, StoneVariant.COBBLE);
        Block bricks = GTBlocks.getStone(StoneType.GRANITE_BLACK, StoneVariant.BRICKS);
        helper.assertTrue(granite != null && cobble != null && bricks != null,
                "granite black has stone, cobble and brick variants");
        ItemStack graniteStack = new ItemStack(granite);
        ItemStack cobbleStack = new ItemStack(cobble);
        ItemStack bricksStack = new ItemStack(bricks);
        for (String family : MOLDS) {
            helper.assertTrue(route(index, graniteStack, family + "ingot", bricksStack) != null,
                    family + "ingot turns stone into bricks");
            helper.assertTrue(route(index, cobbleStack, family + "ingot", bricksStack) != null,
                    family + "ingot turns cobblestone into bricks");
            // GT6 registers the stone group's block-mould row even though it re-emits its own input.
            Recipe selfLoop = route(index, graniteStack, family + "block", graniteStack);
            helper.assertTrue(selfLoop != null, family + "block re-emits the stone block it was fed");
            helper.assertTrue(selfLoop.mOutputs.length == 1 && selfLoop.mOutputs[0].getCount() == 1,
                    family + "block returns exactly one block");
        }
        helper.succeed();
    }

    /**
     * The invariant the collapsed stone row broke: no registered recipe may be left without a real input
     * or without a real output, because such a recipe matches every later candidate sharing its mould.
     */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void noRegisteredRecipeIsDegenerate(GameTestHelper helper) {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for (RecipeMap map : RecipeMap.RECIPE_MAP_LIST) {
            for (Recipe recipe : map.mRecipeList) {
                if (recipe.mFakeRecipe) continue;
                inspected++;
                boolean hasInput = recipe.mFluidInputs.length > 0;
                for (ItemStack in : recipe.mInputs) if (in != null && !in.isEmpty()) hasInput = true;
                boolean hasOutput = recipe.mFluidOutputs.length > 0;
                for (ItemStack out : recipe.mOutputs) if (out != null && !out.isEmpty()) hasOutput = true;
                if (!hasInput || !hasOutput) {
                    offenders.add(map.mNameInternal + " " + java.util.Arrays.toString(recipe.mInputs)
                            + " -> " + java.util.Arrays.toString(recipe.mOutputs));
                }
            }
        }
        helper.assertTrue(offenders.isEmpty(), "degenerate recipes (" + offenders.size() + "): "
                + offenders.subList(0, Math.min(5, offenders.size())));
        helper.assertTrue(inspected > 10000, "recipes inspected: " + inspected);
        helper.succeed();
    }

    /** {@link RecipeMap#make} drops a row that optimizes down to nothing instead of registering it. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void selfCancellingRecipeIsDropped(GameTestHelper helper) {
        Block granite = GTBlocks.getStone(StoneType.GRANITE_BLACK, StoneVariant.STONE);
        helper.assertTrue(granite != null, "granite black stone exists");
        ItemStack block = new ItemStack(granite);
        ItemStack shape = mold("extruder_shape_block");
        helper.assertTrue(!shape.isEmpty(), "the block mould exists");
        int before = RecipeMap.COLLAPSED_RECIPES_DROPPED;
        helper.assertTrue(RecipeMap.make(true, new ItemStack[]{block.copy(), shape.copy()}, new ItemStack[]{block.copy()},
                null, null, null, null, 32, 16, 0) == null,
                "a row whose only input cancels against its only output is dropped");
        helper.assertTrue(RecipeMap.COLLAPSED_RECIPES_DROPPED == before + 1,
                "the dropped row is counted, got " + (RecipeMap.COLLAPSED_RECIPES_DROPPED - before));
        // Control: the same row without optimization stays registered.
        helper.assertTrue(RecipeMap.make(false, new ItemStack[]{block.copy(), shape.copy()}, new ItemStack[]{block.copy()},
                null, null, null, null, 32, 16, 0) != null,
                "without optimization the self-loop row is a literal GT6 recipe");
        // Control: an ordinary row survives optimization.
        ItemStack dust = com.gregtech.gregtech.registry.GTItems.getStack(
                com.gregtech.gregtech.data.MaterialPrefix.dust,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Rubber"), 1);
        ItemStack ingot = com.gregtech.gregtech.registry.GTItems.getStack(
                com.gregtech.gregtech.data.MaterialPrefix.ingot,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Rubber"), 1);
        helper.assertTrue(!dust.isEmpty() && !ingot.isEmpty(), "rubber dust and ingot exist");
        helper.assertTrue(RecipeMap.make(true, new ItemStack[]{dust.copy(), mold("extruder_shape_ingot")},
                new ItemStack[]{ingot.copy()}, null, null, null, null, 64, 16, 0) != null,
                "an ordinary extrusion row survives optimization");
        helper.succeed();
    }

    private static void writeReport(GameTestHelper helper, int routes, int missingForms, int groups,
                                    int registered) {
        var json = new java.util.TreeMap<String, Object>();
        json.put("stoneExtrusionRecipes", StoneAndToolSurvivalRecipes.stoneExtrusionCount());
        json.put("rockTypesWithBothGroups", groups);
        json.put("shapeRoutesVerified", routes);
        json.put("registeredRowsAccountedFor", registered);
        json.put("formsThePortLacks", missingForms);
        json.put("extruderRecipes", MachineRecipeMaps.Extruder.mRecipeList.size());
        json.put("collapsedRowsDropped", RecipeMap.COLLAPSED_RECIPES_DROPPED);
        json.put("collapsedRowsByMap", RecipeMap.collapsedByMap());
        json.put("collapsedRowSamples", RecipeMap.collapsedSamples());
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/stone-extrusion-coverage.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/stone-extrusion-coverage.json: " + e);
        }
    }
}
