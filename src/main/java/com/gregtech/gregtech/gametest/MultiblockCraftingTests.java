package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.data.BasicMachineRecipePack;
import com.gregtech.gregtech.data.MachineRecipeIngredients;
import com.gregtech.gregtech.data.MultiblockCraftingRecipes;
import com.gregtech.gregtech.data.MultiblockRecipePack;
import com.gregtech.gregtech.registry.GTBasicMachines;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * Guards the multiblock crafting recipes against the original GT6 "Multiblock Machines"
 * registrations (see {@link MultiblockCraftingRecipes}).
 * <p>
 * Regression these tests exist for: the multiblock machine family — the legacy {@code large*}
 * blocks (which GT6 registers as multiblocks) and the {@code *_main} controller blocks — had no
 * crafting recipe anywhere, so none of them could be obtained in survival.
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MultiblockCraftingTests {

    private record MachineRecipe(String id, ShapedRecipe recipe) {}

    private static List<MachineRecipe> multiblockRecipes(GameTestHelper helper) {
        List<MachineRecipe> result = new ArrayList<>();
        for (var recipe : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(recipe instanceof ShapedRecipe shaped)) continue;
            if (!recipe.getId().getPath().startsWith("machines/multiblock/")) continue;
            result.add(new MachineRecipe(recipe.getId().toString(), shaped));
        }
        return result;
    }

    /** Every table target must exist and carry the original GT6 pattern. */
    @GameTest(template = "test_empty")
    public static void everyTargetBlockGetsItsOriginalRecipe(GameTestHelper helper) {
        var byResult = new IdentityHashMap<Item, MachineRecipe>();
        for (MachineRecipe machine : multiblockRecipes(helper)) {
            byResult.put(machine.recipe().getResultItem(helper.getLevel().registryAccess()).getItem(), machine);
        }

        TreeSet<String> problems = new TreeSet<>();
        int checked = 0;
        for (MultiblockCraftingRecipes.Entry entry : MultiblockCraftingRecipes.ENTRIES) {
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath("gregtech", entry.blockId());
            Block block = ForgeRegistries.BLOCKS.getValue(blockId);
            if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) {
                problems.add("table references a block that does not exist: " + entry.blockId());
                continue;
            }
            checked++;
            MachineRecipe machine = byResult.get(block.asItem());
            if (machine == null) {
                problems.add("no crafting recipe: " + entry.blockId() + " (" + entry.originalName() + ")");
                continue;
            }
            String[] rows = entry.rows();
            if (machine.recipe().getHeight() != rows.length || machine.recipe().getWidth() != rows[0].length()) {
                problems.add(entry.blockId() + ": pattern " + machine.recipe().getWidth() + "x"
                        + machine.recipe().getHeight() + " != original " + rows[0].length() + "x" + rows.length);
                continue;
            }
            for (int y = 0; y < rows.length; y++) {
                for (int x = 0; x < rows[y].length(); x++) {
                    Ingredient ingredient = machine.recipe().getIngredients().get(x + y * rows[y].length());
                    boolean expectedFilled = rows[y].charAt(x) != ' ';
                    if (ingredient.isEmpty() == expectedFilled) {
                        problems.add(entry.blockId() + ": cell " + x + "," + y + " filled=" + !ingredient.isEmpty()
                                + " expected=" + expectedFilled);
                    }
                }
            }
        }
        helper.assertTrue(checked >= 50, "multiblock targets checked: " + checked);
        helper.assertTrue(problems.isEmpty(), "multiblock crafting recipes incomplete: " + problems);
        helper.succeed();
    }

    /** The machine family that previously had no recipe at all must now be covered. */
    @GameTest(template = "test_empty")
    public static void multiblockMachinesAreCovered(GameTestHelper helper) {
        TreeSet<String> missing = new TreeSet<>();
        int checked = 0;
        for (var entry : GTBasicMachines.all()) {
            if (!entry.isPresent()) continue;
            var spec = entry.get().basicSpec();
            if (!BasicMachineRecipePack.MULTIBLOCK_CONTROLLERS.contains(spec.machineName())) continue;
            checked++;
            if (MultiblockCraftingRecipes.find(spec.id()) == null) {
                missing.add(spec.id() + " (" + spec.machineName() + ")");
            }
        }
        // 13 = the "large*" batch machines, which this port still implements as single-block machines
        // (GT6 makes them multiblocks). The distillation tower / cryo tower / coke oven / fusion reactor /
        // implosion compressor / lightning rod are multiblock-only now, so they are covered by the
        // standalone controller loop below instead of this one.
        helper.assertTrue(checked >= 13, "legacy multiblock machine variants: " + checked);
        helper.assertTrue(missing.isEmpty(), "legacy multiblock machines still without a recipe: " + missing);

        // Standalone controller blocks that GT6 registers in its multiblock tab.
        for (String controller : List.of(
                "coke_oven_main", "implosion_compressor_main", "distillation_tower_main",
                "cryo_distillation_main", "fusion_reactor_main", "heat_exchanger_main",
                "bedrock_drill_main", "lightning_rod_main", "large_boiler_main",
                "large_crucible_main", "large_dynamo_main", "large_gas_turbine_main",
                "large_turbine_main")) {
            Block block = ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", controller));
            helper.assertTrue(block != null, "controller block exists: " + controller);
            helper.assertTrue(MultiblockCraftingRecipes.find(controller) != null,
                    "controller has an original GT6 crafting recipe: " + controller);
        }
        helper.succeed();
    }

    /** No table ingredient may resolve to something that matches nothing. */
    @GameTest(template = "test_empty")
    public static void everyMultiblockIngredientResolves(GameTestHelper helper) {
        List<String> unresolved = new ArrayList<>();
        var steel = com.gregtech.gregtech.api.material.GTMaterialRegistry.get("StainlessSteel");
        for (MultiblockCraftingRecipes.Entry entry : MultiblockCraftingRecipes.ENTRIES) {
            for (var pair : entry.keys().entrySet()) {
                Object ingredient = MachineRecipeIngredients.resolve(pair.getValue(), steel, 1);
                for (Object option : ingredient instanceof List<?> list ? list : List.of(ingredient)) {
                    if (!(option instanceof java.util.Map<?, ?> map)) {
                        unresolved.add(entry.blockId() + " " + pair.getKey() + " -> " + option);
                        continue;
                    }
                    Object item = map.get("item");
                    if (item != null && ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(item.toString())) == null) {
                        unresolved.add(entry.blockId() + " " + pair.getKey() + " -> missing item " + item);
                    }
                    // Tag ingredients are covered by the pattern comparison above, which fails when a
                    // filled cell resolves to an empty ingredient (empty or unknown tag).
                }
            }
        }
        helper.assertTrue(unresolved.isEmpty(), "multiblock ingredients resolving to nothing: " + unresolved);
        helper.succeed();
    }

    /** Coverage must stay reported so the remaining gaps stay visible. */
    @GameTest(template = "test_empty")
    public static void coverageIsReported(GameTestHelper helper) {
        var report = new java.util.TreeMap<String, Object>();
        report.put("tableEntries", MultiblockCraftingRecipes.ENTRIES.size());
        report.put("emittedRecipes", MultiblockRecipePack.emittedBlockIds().size());
        report.put("coveredMachines", new TreeSet<>(MultiblockCraftingRecipes.coveredMachines()));
        java.nio.file.Path out = java.nio.file.Path.of("../../docs/multiblock-recipes-coverage.json");
        try {
            java.nio.file.Files.writeString(out, new com.google.gson.GsonBuilder().setPrettyPrinting()
                    .create().toJson(report));
        } catch (Exception e) {
            helper.assertTrue(false, "cannot write multiblock coverage report: " + e);
        }
        helper.assertTrue(MultiblockCraftingRecipes.ENTRIES.size() >= 50,
                "multiblock recipe entries: " + MultiblockCraftingRecipes.ENTRIES.size());
        helper.succeed();
    }
}
