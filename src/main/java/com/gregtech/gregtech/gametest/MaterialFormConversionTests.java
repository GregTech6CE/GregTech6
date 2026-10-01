package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.material.MaterialUnification;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.content.recipe.MaterialFormConversionRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.loaders.Loader_FormConversionCraftingRecipes;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Guards GT6's bulk material form conversions: dust &harr; dust block, small/tiny piles &rarr; dust,
 * ingot &harr; ingot block, plate &rarr; dense plate, and their hand-crafting counterparts.
 * <p>
 * Regression these tests exist for: the port had the block prefixes ({@code blockDust},
 * {@code blockIngot}, …) but <em>no</em> conversion between them and the item forms — the only
 * compression recipe in the whole port was a single hand-written Adamantium plate &rarr; dense plate.
 * GT6 registers all of them generically per material instead of listing them individually
 * ({@code Loader_Recipes_Handlers:217-233} for the Compressor, {@code :451-550} for the
 * Boxinator/Unboxinator and {@code :552-625} for the crafting grid).
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MaterialFormConversionTests {

    private static TransientCraftingContainer grid() {
        AbstractContainerMenu menu = new AbstractContainerMenu(null, 0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) {
                return ItemStack.EMPTY;
            }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        };
        return new TransientCraftingContainer(menu, 3, 3);
    }

    /**
     * {@link RecipeMap#addRecipe} unifies outputs onto the vanilla item where GT6 has one for that form
     * ({@code MaterialUnification.canonical}: copper ingot -> {@code minecraft:copper_ingot}), and
     * {@link MaterialEquivalence} only knows item forms, not blocks — so both comparisons are needed.
     */
    private static boolean contains(Recipe recipe, ItemStack[] stacks, ItemStack expected) {
        if (expected.isEmpty()) return false;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameTags(stack, expected) || MaterialEquivalence.matches(expected, stack)) {
                return true;
            }
        }
        return false;
    }

    /** A machine recipe of {@code map} whose inputs include {@code input} and output is {@code output}. */
    private static Recipe route(RecipeMap map, ItemStack input, ItemStack output) {
        for (Recipe recipe : map.mRecipeList) {
            if (contains(recipe, recipe.mInputs, input) && contains(recipe, recipe.mOutputs, output)) return recipe;
        }
        return null;
    }

    private static ItemStack item(MaterialPrefix prefix, GTMaterial material, int count) {
        return GTItems.getStack(prefix, material, count);
    }

    private static ItemStack block(BlockMaterialPrefix prefix, GTMaterial material) {
        return GTBlocks.getStack(prefix, material);
    }

    @GameTest(template = "test_empty")
    public static void machineConversionCoverage(GameTestHelper helper) {
        var report = new TreeMap<String, Object>();
        var byKind = new TreeMap<String, Integer>();
        var byMap = new TreeMap<String, Integer>();
        for (var entry : MaterialFormConversionRecipes.entries()) {
            byKind.merge(entry.kind(), 1, Integer::sum);
            byMap.merge(entry.map().mNameInternal, 1, Integer::sum);
        }
        report.put("byKind", byKind);
        report.put("byMap", byMap);
        report.put("skipped", MaterialFormConversionRecipes.skipped());
        report.put("craftingGridRecipes", Loader_FormConversionCraftingRecipes.registeredIds().size());
        report.put("craftingGridSkipped", Loader_FormConversionCraftingRecipes.skipped());
        // Totals of every recipe map GT6 drives with the generic prefix handlers, so the next porting
        // round can see which handler family is still empty instead of guessing.
        var allMaps = new TreeMap<String, Integer>();
        for (RecipeMap map : List.of(MachineRecipeMaps.Compressor, MachineRecipeMaps.Boxinator,
                MachineRecipeMaps.Unboxinator, MachineRecipeMaps.Welder, MachineRecipeMaps.Anvil,
                MachineRecipeMaps.AnvilBendBig, MachineRecipeMaps.AnvilBendSmall, MachineRecipeMaps.Shredder,
                MachineRecipeMaps.Mortar, MachineRecipeMaps.RollingMill, MachineRecipeMaps.Lathe,
                MachineRecipeMaps.Cutter, MachineRecipeMaps.Sharpening, MachineRecipeMaps.Autoclave,
                MachineRecipeMaps.Press, MachineRecipeMaps.Crusher, MachineRecipeMaps.Wiremill,
                MachineRecipeMaps.RollBender, MachineRecipeMaps.RollFormer, MachineRecipeMaps.ClusterMill,
                MachineRecipeMaps.Extruder, MachineRecipeMaps.Loom, MachineRecipeMaps.Sifting,
                MachineRecipeMaps.Sluice)) {
            allMaps.put(map.mNameInternal, map.mRecipeList.size());
        }
        report.put("recipeMapTotals", allMaps);
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/material-form-conversions.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (java.io.IOException e) {
            helper.fail("could not write docs/material-form-conversions.json: " + e);
            return;
        }
        helper.assertTrue(byKind.getOrDefault("compressor", 0) > 100,
                "compressor form conversions registered (plate -> dense plate, dust -> plate gem): " + byKind);
        helper.assertTrue(byKind.getOrDefault("unboxinator", 0) > 1000,
                "unboxinator form conversions registered (block -> 9, dust -> 9 tiny piles): " + byKind);
        helper.assertTrue(byKind.getOrDefault("boxinator", 0) > 1000,
                "boxinator form conversions registered (9 -> block, 9 tiny piles -> dust): " + byKind);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void copperStorageRoutes(GameTestHelper helper) {
        GTMaterial copper = GTMaterialRegistry.get("Copper");
        helper.assertTrue(copper != null && copper.isValid(), "copper material exists");

        // 9 ingots <-> 1 block of ingots (GT6 Loader_Recipes_Handlers:533 / :478)
        helper.assertTrue(route(MachineRecipeMaps.Boxinator, item(MaterialPrefix.ingot, copper, 9),
                block(BlockMaterialPrefix.blockIngot, copper)) != null, "boxinator packs 9 copper ingots into a block");
        helper.assertTrue(route(MachineRecipeMaps.Unboxinator, block(BlockMaterialPrefix.blockIngot, copper),
                item(MaterialPrefix.ingot, copper, 9)) != null, "unboxinator unpacks a copper ingot block into 9 ingots");

        // 9 dust <-> 1 dust block (GT6 :529 / :476)
        helper.assertTrue(route(MachineRecipeMaps.Boxinator, item(MaterialPrefix.dust, copper, 9),
                block(BlockMaterialPrefix.blockDust, copper)) != null, "boxinator packs 9 copper dusts into a dust block");
        helper.assertTrue(route(MachineRecipeMaps.Unboxinator, block(BlockMaterialPrefix.blockDust, copper),
                item(MaterialPrefix.dust, copper, 9)) != null, "unboxinator unpacks a copper dust block into 9 dusts");

        // 9 tiny / 4 small piles -> 1 dust (GT6 :542 / :543)
        helper.assertTrue(route(MachineRecipeMaps.Boxinator, item(MaterialPrefix.dustTiny, copper, 9),
                item(MaterialPrefix.dust, copper, 1)) != null, "9 copper tiny piles compress into one dust");
        helper.assertTrue(route(MachineRecipeMaps.Boxinator, item(MaterialPrefix.dustSmall, copper, 4),
                item(MaterialPrefix.dust, copper, 1)) != null, "4 copper small piles compress into one dust");

        // 9 plates -> 1 dense plate (GT6 :219 / :228)
        helper.assertTrue(route(MachineRecipeMaps.Compressor, item(MaterialPrefix.plate, copper, 9),
                item(MaterialPrefix.plateDense, copper, 1)) != null, "9 copper plates compress into a dense plate");

        // 1 dust -> 9 tiny piles (GT6 :487)
        helper.assertTrue(route(MachineRecipeMaps.Unboxinator, item(MaterialPrefix.dust, copper, 1),
                item(MaterialPrefix.dustTiny, copper, 9)) != null, "one copper dust grinds into 9 tiny piles");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void compressionDurationFollowsWorkability(GameTestHelper helper) {
        GTMaterial copper = GTMaterialRegistry.get("Copper");
        helper.assertTrue(MaterialWorkability.isEasyWorkable(copper),
                "GT6 flags copper SOFT + FURNACE, so it uses the fixed compression duration");

        Recipe easy = route(MachineRecipeMaps.Compressor, item(MaterialPrefix.plate, copper, 9),
                item(MaterialPrefix.plateDense, copper, 1));
        helper.assertTrue(easy != null, "copper dense plate route exists");
        helper.assertTrue(easy.mDuration == 144,
                "GT6 easy-workable materials compress 9 plates in 16*9 = 144 ticks, got " + easy.mDuration);

        // A hard material pays 256 * (1 + toolQuality) ticks per material unit instead.
        GTMaterial hard = null;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (MaterialWorkability.isEasyWorkable(material)) continue;
            if (item(MaterialPrefix.plate, material, 9).isEmpty()) continue;
            if (item(MaterialPrefix.plateDense, material, 1).isEmpty()) continue;
            hard = material;
            break;
        }
        helper.assertTrue(hard != null, "the port has at least one hard material with plate + dense plate");
        Recipe scaled = route(MachineRecipeMaps.Compressor, item(MaterialPrefix.plate, hard, 9),
                item(MaterialPrefix.plateDense, hard, 1));
        helper.assertTrue(scaled != null, "hard material dense plate route exists: " + hard.getName());
        long expected = 9L * 256 * (hard.getToolQuality() + 1);
        helper.assertTrue(scaled.mDuration == expected,
                "GT6 scales hard materials by 9 * 256 * (quality + 1) = " + expected + ", got " + scaled.mDuration);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void workabilityTableMatchesGT6(GameTestHelper helper) {
        List<String> easy = new ArrayList<>();
        for (String name : List.of("Copper", "Tin", "Lead", "Gold", "Silver", "Aluminium", "Bronze", "Brass")) {
            GTMaterial material = GTMaterialRegistry.get(name);
            if (material == null) continue;
            helper.assertTrue(MaterialWorkability.isEasyWorkable(material),
                    "GT6 marks " + name + " SOFT or FURNACE: " + name);
            easy.add(name);
        }
        helper.assertTrue(easy.size() >= 6, "the flagged metals resolve in the port: " + easy);
        for (String name : List.of("Tungsten", "Titanium", "StainlessSteel")) {
            GTMaterial material = GTMaterialRegistry.get(name);
            if (material == null) continue;
            helper.assertTrue(!MaterialWorkability.isEasyWorkable(material),
                    "GT6 does not flag " + name + " as easy workable: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void craftingGridConversions(GameTestHelper helper) {
        List<String> ids = Loader_FormConversionCraftingRecipes.registeredIds();
        helper.assertTrue(ids.size() > 1000,
                "GT6's crafting-grid form conversions were materialised per material: " + ids.size());
        helper.assertTrue(ids.contains("gregtech:form_conversion/ingot_to_blockingot/copper_9"),
                "9 copper ingots -> block of copper ingots is a crafting recipe");
        helper.assertTrue(ids.contains("gregtech:form_conversion/dustsmall_to_dust/copper_4"),
                "4 copper small piles -> one copper dust is a crafting recipe");

        GTMaterial copper = GTMaterialRegistry.get("Copper");
        var manager = helper.getLevel().getRecipeManager();

        // 9 GT ingots in the 3x3 grid -> one GT block of ingots. Vanilla's own 9-ingot recipes keep
        // serving the unified vanilla items (see the loader's formIngredient note).
        var grid = grid();
        for (int slot = 0; slot < 9; slot++) grid.setItem(slot, item(MaterialPrefix.ingot, copper, 1));
        CraftingRecipe packed = manager.getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel()).orElse(null);
        helper.assertTrue(packed != null, "the crafting grid has a recipe for 9 copper ingots");
        ItemStack packedResult = packed.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.isSameItemSameTags(packedResult, block(BlockMaterialPrefix.blockIngot, copper)),
                "9 copper ingots craft into a block of copper ingots, got " + packedResult);

        // 1 block -> 9 ingots.
        var unpackGrid = grid();
        unpackGrid.setItem(0, block(BlockMaterialPrefix.blockIngot, copper));
        CraftingRecipe unpacked = manager.getRecipeFor(RecipeType.CRAFTING, unpackGrid, helper.getLevel()).orElse(null);
        helper.assertTrue(unpacked != null, "the crafting grid has a recipe for a copper ingot block");
        ItemStack unpackedResult = unpacked.assemble(unpackGrid, helper.getLevel().registryAccess());
        helper.assertTrue(unpackedResult.getCount() == 9
                        && MaterialEquivalence.matches(item(MaterialPrefix.ingot, copper, 1), unpackedResult),
                "a copper ingot block crafts back into 9 ingots, got " + unpackedResult);

        // 4 small piles -> one dust (GT6 Loader_Recipes_Handlers:602).
        var smallGrid = grid();
        for (int slot = 0; slot < 4; slot++) smallGrid.setItem(slot, item(MaterialPrefix.dustSmall, copper, 1));
        CraftingRecipe small = manager.getRecipeFor(RecipeType.CRAFTING, smallGrid, helper.getLevel()).orElse(null);
        helper.assertTrue(small != null, "the crafting grid has a 4 small piles -> dust recipe");
        ItemStack smallResult = small.assemble(smallGrid, helper.getLevel().registryAccess());
        helper.assertTrue(smallResult.getCount() == 1
                        && MaterialEquivalence.matches(item(MaterialPrefix.dust, copper, 1), smallResult),
                "4 copper small piles craft into one copper dust, got " + smallResult);

        helper.succeed();
    }

    /** The per-material recipes must not let two materials be mixed in one grid. */
    @GameTest(template = "test_empty")
    public static void craftingGridRejectsMixedMaterials(GameTestHelper helper) {
        GTMaterial copper = GTMaterialRegistry.get("Copper");
        GTMaterial tin = GTMaterialRegistry.get("Tin");
        var grid = grid();
        for (int slot = 0; slot < 8; slot++) grid.setItem(slot, item(MaterialPrefix.ingot, copper, 1));
        grid.setItem(8, item(MaterialPrefix.ingot, tin, 1));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(recipe.isEmpty(), "a mixed copper/tin grid has no form conversion: " + recipe);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void boxinatorNeedsTheMatchingCircuit(GameTestHelper helper) {
        GTMaterial copper = GTMaterialRegistry.get("Copper");
        ItemStack ingots = item(MaterialPrefix.ingot, copper, 9);
        ItemStack block = block(BlockMaterialPrefix.blockIngot, copper);
        Recipe correct = route(MachineRecipeMaps.Boxinator, ingots, block);
        helper.assertTrue(correct != null, "the copper ingot block route exists");
        boolean hasCircuit = contains(correct, correct.mInputs, new ItemStack(GTTechnological.selectorTag(9)));
        helper.assertTrue(hasCircuit, "GT6 gates the packing handler behind ST.tag(9), like the port's circuit");
        helper.succeed();
    }
}
