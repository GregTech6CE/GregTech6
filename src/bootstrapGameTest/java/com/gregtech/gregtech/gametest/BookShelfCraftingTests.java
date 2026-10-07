package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.content.book.BookShelfVariants;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** GT6 Loader_MultiTileEntities:143,181-183 — survival recipes for the represented shelf family. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BookShelfCraftingTests {
    private BookShelfCraftingTests() {}

    private static TransientCraftingContainer grid() {
        var menu = new AbstractContainerMenu(null, 0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return true;
            }
        };
        return new TransientCraftingContainer(menu, 3, 3);
    }

    private static ItemStack usable(ItemStack stack) {
        if (stack.getItem() instanceof GTToolItem) {
            return GTToolItem.create(GTToolHelper.getType(stack), Materials.Steel,
                    GTMaterialRegistry.get("Wood"));
        }
        return stack.copy();
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 600)
    public static void everyRegisteredShelfHasAWorkingOriginalRecipe(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        helper.assertTrue(BookShelfVariants.all().size() == 75, "all represented GT6 shelves covered");
        for (var variant : BookShelfVariants.all()) {
            var id = GregTech.id("bookshelves/" + variant.path());
            var loaded = manager.byKey(id).orElse(null);
            helper.assertTrue(loaded instanceof ToolShapedRecipe, id + " is a tool-aware shaped recipe");
            var recipe = (ToolShapedRecipe) loaded;
            helper.assertTrue(recipe.getWidth() == 3 && recipe.getHeight() == 3 && !recipe.allowMirror(),
                    id + " keeps GT6's 3x3 non-mirrored recipe");
            helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(
                    GTDecorBlocks.bookshelf(variant.path()).get().asItem()), id + " produces its own shelf");
            if (variant.metal()) {
                helper.assertTrue(!GTItems.getStack(MaterialPrefix.plate, variant.material()).isEmpty(),
                        variant.path() + " has the source material's plate");
                helper.assertTrue(!GTItems.getStack(MaterialPrefix.screw, variant.material()).isEmpty(),
                        variant.path() + " has the source material's screw");
            }

            var inventory = grid();
            int toolCount = 0;
            for (int slot = 0; slot < 9; slot++) {
                Ingredient ingredient = recipe.getIngredients().get(slot);
                ItemStack[] options = ingredient.getItems();
                helper.assertTrue(options.length > 0,
                        id + " ingredient slot " + slot + " resolves to a real item or tool");
                ItemStack stack = usable(options[0]);
                if (stack.getItem() instanceof GTToolItem) toolCount++;
                inventory.setItem(slot, stack);
            }
            helper.assertTrue(toolCount == 3, id + " uses exactly three reusable GT6 tools");
            helper.assertTrue(recipe.matches(inventory, helper.getLevel()), id + " crafts from its ingredients");
            var remaining = recipe.getRemainingItems(inventory);
            for (int slot = 0; slot < 9; slot++) {
                if (inventory.getItem(slot).getItem() instanceof GTToolItem) {
                    helper.assertTrue(remaining.get(slot).getItem() == inventory.getItem(slot).getItem(),
                            id + " returns its used tool in slot " + slot);
                }
            }
        }
        helper.succeed();
    }
}
