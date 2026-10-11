package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.tool.ScaffoldBlock;
import com.gregtech.gregtech.registry.GTToolBlocks;
import com.gregtech.gregtech.registry.GTStorageMetals;
import com.gregtech.gregtech.content.material.Materials;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ScaffoldFamilyTests {
    @GameTest(template="test_empty")
    public static void allOriginalMetalScaffoldsHaveUsableRecipes(GameTestHelper h) {
        h.assertTrue(GTToolBlocks.SCAFFOLDS.size() == 60, "all GT6 metalset scaffolds registered");
        for (var spec : GTStorageMetals.ALL) {
            var block = GTToolBlocks.scaffold(spec.material());
            String id = spec.suffix().equals("steel") ? "scaffold" : "scaffold_" + spec.suffix();
            h.assertTrue(block.tintRgb() == spec.material().getColor(), "material tint: " + id);
            h.assertTrue(new ItemStack(block).getMaxStackSize() == 64, "GT6 stack limit: " + id);
            var recipe = (CraftingRecipe) h.getLevel().getRecipeManager().byKey(GregTech.id("scaffolds/" + id)).orElseThrow();
            var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, 0) {
                @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
                @Override public boolean stillValid(Player p) { return true; }
            }, 3, 3);
            for (int i = 0; i < recipe.getIngredients().size(); i++) {
                var options = recipe.getIngredients().get(i).getItems();
                h.assertTrue(options.length > 0, "crafting material available: " + id + " slot " + i);
                grid.setItem(i, options[0].copy());
            }
            grid.setItem(4, com.gregtech.gregtech.item.GTToolItem.create(
                    com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER, Materials.Steel,
                    com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood")));
            h.assertTrue(recipe.matches(grid, h.getLevel()), "recipe matches actual materials: " + id);
            h.assertTrue(recipe.assemble(grid,h.getLevel().registryAccess()).is(block.asItem()), "correct output: " + id);
            h.assertTrue(!recipe.getRemainingItems(grid).get(4).isEmpty(), "screwdriver survives: " + id);
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void differentMaterialsShareStructuralSupport(GameTestHelper h) {
        var lower = GTToolBlocks.scaffold(Materials.Brass);
        var upper = GTToolBlocks.scaffold(Materials.Steel);
        var pos = h.absolutePos(new BlockPos(2,2,2));
        var level = h.getLevel();
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, lower.defaultBlockState(), 3);
        level.setBlock(pos.above(), upper.defaultBlockState(), 3);
        lower.tick(level.getBlockState(pos), level, pos, level.random);
        upper.tick(level.getBlockState(pos.above()), level, pos.above(), level.random);
        h.assertTrue(level.getBlockState(pos).getValue(ScaffoldBlock.DESIGN) == 2, "brass opens below steel");
        h.assertTrue(level.getBlockState(pos.above()).getValue(ScaffoldBlock.DESIGN) == 1, "steel is supported by brass");
        level.removeBlock(pos, false);
        upper.tick(level.getBlockState(pos.above()), level, pos.above(), level.random);
        h.assertTrue(level.getBlockState(pos.above()).isAir(), "mixed material stack collapses when support is removed");
        h.succeed();
    }
}
