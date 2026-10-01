package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.recipe.DiggableRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Processing and material registrations of GT6 BlockDiggable and MultiItemFood. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DiggableRecipesTests {
    private static ItemStack stack(String id, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(GregTech.id(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static Recipe find(com.gregtech.gregtech.api.recipe.RecipeMap map, ItemStack... items) {
        return map.findRecipe(List.of(items), List.of(), false, items.length, map.mOutputItemsCount);
    }

    private static boolean outputs(Recipe recipe, ItemStack expected) {
        return recipe != null && recipe.mOutputs.length == 1
                && recipe.mOutputs[0].getCount() == expected.getCount()
                && ItemStack.isSameItemSameTags(recipe.mOutputs[0], expected);
    }

    private static boolean sameStack(ItemStack actual, ItemStack expected) {
        return actual.getCount() == expected.getCount()
                && ItemStack.isSameItemSameTags(actual, expected);
    }

    @GameTest(template = "test_empty")
    public static void dryingAndFurnaceKeepTheSevenMetadataRules(GameTestHelper h) {
        h.assertTrue(outputs(find(MachineRecipeMaps.Drying, new ItemStack(Blocks.CLAY)),
                new ItemStack(Blocks.TERRACOTTA)), "vanilla clay also dries into terracotta");
        for (var row : DiggableRecipes.packable()) {
            ItemStack input = stack(row.block(), 1);
            ItemStack expected = row.material() == null
                    ? new ItemStack(Blocks.COARSE_DIRT) : new ItemStack(Blocks.TERRACOTTA);
            Recipe dryer = find(MachineRecipeMaps.Drying, input);
            Recipe furnace = find(MachineRecipeMaps.Furnace, input);
            h.assertTrue(outputs(dryer, expected) && dryer.mEUt == 16 && dryer.mDuration == 64,
                    row.block() + " has GT6's 16 EU/t, 64 tick drying row");
            h.assertTrue(outputs(furnace, expected), row.block() + " has GT6's furnace result");
        }
        h.assertTrue(find(MachineRecipeMaps.Drying, stack("turf", 1)) == null
                        && find(MachineRecipeMaps.Furnace, stack("turf", 1)) == null,
                "turf has no GT6 drying or furnace row");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fourBallsPackAndUnpackInAllThreeMachines(GameTestHelper h) {
        ItemStack vanillaBalls = new ItemStack(Items.CLAY_BALL, 4);
        ItemStack vanillaClay = new ItemStack(Blocks.CLAY);
        h.assertTrue(outputs(find(MachineRecipeMaps.Boxinator, vanillaBalls,
                        new ItemStack(GTTechnological.selectorTag(4))), vanillaClay)
                        && outputs(find(MachineRecipeMaps.Compressor, vanillaBalls), vanillaClay)
                        && outputs(find(MachineRecipeMaps.Unboxinator, vanillaClay), vanillaBalls),
                "ordinary vanilla clay retains GT6's three machine packing routes");
        for (var row : DiggableRecipes.packable()) {
            ItemStack balls = stack(row.ball(), 4);
            ItemStack block = stack(row.block(), 1);
            Recipe boxed = find(MachineRecipeMaps.Boxinator, balls,
                    new ItemStack(GTTechnological.selectorTag(4)));
            Recipe compressed = find(MachineRecipeMaps.Compressor, balls);
            Recipe unboxed = find(MachineRecipeMaps.Unboxinator, block);
            h.assertTrue(outputs(boxed, block) && boxed.mDuration == 16 && boxed.mEUt == 16
                            && boxed.isCatalystInput(1), row.block() + " boxes with selector 4");
            h.assertTrue(outputs(compressed, block) && compressed.mDuration == 16
                            && compressed.mEUt == 16, row.block() + " compresses from four balls");
            h.assertTrue(outputs(unboxed, balls) && unboxed.mDuration == 16
                            && unboxed.mEUt == 16, row.block() + " unboxes into four balls");
        }
        h.assertTrue(find(MachineRecipeMaps.Unboxinator, stack("turf", 1)) == null,
                "peat turf has no four-ball unpacking route");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void gridRecipesAreCraftableAndReversible(GameTestHelper h) {
        RecipeManager manager = h.getLevel().getRecipeManager();
        CraftingRecipe vanillaUnpack = (CraftingRecipe) manager.byKey(GregTech.id("diggable/vanilla_clay_unpack"))
                .orElseThrow();
        TransientCraftingContainer vanillaGrid = grid();
        vanillaGrid.setItem(0, new ItemStack(Blocks.CLAY));
        h.assertTrue(vanillaUnpack.matches(vanillaGrid, h.getLevel())
                        && sameStack(vanillaUnpack.assemble(vanillaGrid, h.getLevel().registryAccess()),
                        new ItemStack(Items.CLAY_BALL, 4)),
                "ordinary vanilla clay block crafts back into four balls");
        for (var row : DiggableRecipes.packable()) {
            CraftingRecipe pack = (CraftingRecipe) manager.byKey(GregTech.id("diggable/" + row.block() + "_pack"))
                    .orElseThrow();
            CraftingRecipe unpack = (CraftingRecipe) manager.byKey(GregTech.id("diggable/" + row.block() + "_unpack"))
                    .orElseThrow();
            TransientCraftingContainer grid = grid();
            for (int slot = 0; slot < 4; slot++) grid.setItem(slot, stack(row.ball(), 1));
            h.assertTrue(pack.matches(grid, h.getLevel())
                            && sameStack(pack.assemble(grid, h.getLevel().registryAccess()),
                            stack(row.block(), 1)), row.block() + " crafts from a 2x2 square");
            for (int slot = 0; slot < 4; slot++) grid.setItem(slot, ItemStack.EMPTY);
            grid.setItem(0, stack(row.block(), 1));
            h.assertTrue(unpack.matches(grid, h.getLevel())
                            && sameStack(unpack.assemble(grid, h.getLevel().registryAccess()),
                            stack(row.ball(), 4)), row.block() + " crafts back into four balls");
        }
        h.assertTrue(manager.byKey(GregTech.id("diggable/turf_pack")).isEmpty(),
                "GT6 has no turf packing recipe");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void sixClayBallsRollToPlatesAndCompressFromDust(GameTestHelper h) {
        assertClayBallForms(h, Materials.Clay, new ItemStack(Items.CLAY_BALL), "vanilla clay");
        for (var row : DiggableRecipes.packable()) if (row.material() != null)
            assertClayBallForms(h, row.material(), stack(row.ball(), 1), row.ball());
        h.succeed();
    }

    private static void assertClayBallForms(GameTestHelper h, com.gregtech.gregtech.api.material.GTMaterial material,
                                            ItemStack ball, String name) {
        ItemStack plate = GTItems.getStack(MaterialPrefix.plate, material);
        ItemStack dust = GTItems.getStack(MaterialPrefix.dust, material);
        Recipe rolling = find(MachineRecipeMaps.RollingMill, ball);
        Recipe compressing = find(MachineRecipeMaps.Compressor, dust);
        h.assertTrue(!plate.isEmpty() && outputs(rolling, plate)
                        && rolling.mEUt == 16 && rolling.mDuration == 32,
                name + " rolls into one specific clay plate");
        h.assertTrue(!dust.isEmpty() && outputs(compressing, ball)
                        && compressing.mEUt == 16 && compressing.mDuration == 16,
                name + " dust compresses into one clay ball");
    }

    @GameTest(template = "test_empty")
    public static void clayAndPeatCompositionAndDirectedGenerification(GameTestHelper h) {
        for (var row : DiggableRecipes.packable()) {
            ItemStack block = stack(row.block(), 1);
            ItemStack ball = stack(row.ball(), 1);
            if (row.material() == null) {
                h.assertTrue(ItemMaterialRegistry.get(block).isEmpty()
                                && ItemMaterialRegistry.get(ball).isEmpty(),
                        "GT6 mud has no material composition");
                continue;
            }
            var blockData = ItemMaterialRegistry.get(block).orElseThrow();
            var ballData = ItemMaterialRegistry.get(ball).orElseThrow();
            h.assertTrue(blockData.material() == row.material() && blockData.amount() == GTValues.U * 4,
                    row.block() + " contains four units of its specific clay");
            h.assertTrue(ballData.material() == row.material() && ballData.amount() == GTValues.U,
                    row.ball() + " contains one unit of its specific clay");
            h.assertTrue(outputs(find(MachineRecipeMaps.Generifier, block), new ItemStack(Blocks.CLAY))
                            && outputs(find(MachineRecipeMaps.Generifier, ball), new ItemStack(Items.CLAY_BALL)),
                    row.block() + " and its ball generify toward vanilla clay");
            h.assertTrue(!com.gregtech.gregtech.api.material.MaterialEquivalence.matches(
                            new ItemStack(Blocks.CLAY), block),
                    row.block() + " keeps its own material in ordinary recipes");
            h.assertTrue(block.is(ItemTags.create(GregTech.id("oredict/block_clay")))
                            && ball.is(ItemTags.create(GregTech.id("oredict/item_clay"))),
                    row.block() + " and its ball carry the GT6 ore-dictionary groups");
        }
        var peat = ItemMaterialRegistry.get(stack("turf", 1)).orElseThrow();
        h.assertTrue(peat.material() == Materials.Peat && peat.amount() == GTValues.U * 4,
                "turf contains four units of peat");
        h.assertTrue(ItemMaterialRegistry.get(stack("diggable_clay", 1)).orElseThrow().amount()
                        == GTValues.U * 4
                        && ItemMaterialRegistry.get(stack("diggable_peat", 1)).orElseThrow().amount()
                        == GTValues.U * 4, "saved-world alias blocks retain their compositions");
        h.assertTrue(stack("mud", 1).is(ItemTags.create(GregTech.id("oredict/block_mud")))
                        && stack("mud_2", 1).is(ItemTags.create(GregTech.id("oredict/item_mud"))),
                "mud block and mud ball carry their GT6 ore-dictionary groups");
        h.assertTrue(Blocks.CLAY.defaultBlockState().is(BlockTags.create(GregTech.id("oredict/block_clay"))),
                "block clay tag also contains the vanilla source block");
        h.succeed();
    }

    private static TransientCraftingContainer grid() {
        AbstractContainerMenu menu = new AbstractContainerMenu(null, 0) {
            @Override public boolean stillValid(Player player) { return true; }
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
        };
        return new TransientCraftingContainer(menu, 2, 2);
    }
}
