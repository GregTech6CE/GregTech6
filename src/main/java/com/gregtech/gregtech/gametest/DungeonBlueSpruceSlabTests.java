package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.registry.GTWoods;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** GT6's Blue Spruce support slab used by normal dungeon libraries. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DungeonBlueSpruceSlabTests {
    private DungeonBlueSpruceSlabTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void blueSpruceLibrarySupportIsARealSlabWithSurvivalRecipe(GameTestHelper helper) {
        var slab = GTWoods.SLAB_BLUE_SPRUCE.get();
        var top = slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, top, 3);
        var bounds = top.getCollisionShape(helper.getLevel(), pos, CollisionContext.empty()).bounds();
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(slab)
                        && bounds.minY == 0.5 && bounds.maxY == 1.0,
                "library support uses the GT Blue Spruce upper half slab, not vanilla spruce");
        helper.assertTrue(top.is(BlockTags.WOODEN_SLABS) && slab.asItem().getDefaultInstance()
                        .is(ItemTags.WOODEN_SLABS), "the slab joins Minecraft's wood slab interoperability tags");
        var loot = new LootParams.Builder(helper.getLevel());
        helper.assertTrue(slab.getDrops(top, loot).get(0).getCount() == 1
                        && slab.getDrops(slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), loot)
                        .get(0).getCount() == 2,
                "one upper slab drops one item and a combined double slab drops two");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                        "gregtech", "wood/slab_bluespruce")).isPresent(),
                "GT6's saw-and-plank survival recipe for two slabs is loaded");
        helper.succeed();
    }

    /** GT6's recipe letter v is craftingToolSawAxe: axe and saw both work, and remain after crafting. */
    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void blueSpruceSlabAcceptsGt6SawAxeFamily(GameTestHelper helper) {
        var id = ResourceLocation.fromNamespaceAndPath("gregtech", "wood/slab_bluespruce");
        var loaded = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        helper.assertTrue(loaded instanceof ToolShapedRecipe, "Blue Spruce slab uses the GT tool recipe");
        var recipe = (ToolShapedRecipe) loaded;

        for (GTToolType type : new GTToolType[]{GTToolType.SAW, GTToolType.AXE,
                GTToolType.DOUBLE_AXE, GTToolType.UNIVERSAL_SPADE}) {
            ItemStack tool = GTToolItem.create(type, Materials.Steel, GTMaterialRegistry.get("Wood"));
            var menu = new AbstractContainerMenu(null, 0) {
                @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) {
                    return ItemStack.EMPTY;
                }
                @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                    return true;
                }
            };
            var grid = new TransientCraftingContainer(menu, 3, 3);
            grid.setItem(0, tool);
            grid.setItem(1, new ItemStack(GTWoods.planks(WoodSpecies.BLUE_SPRUCE)));
            helper.assertTrue(recipe.matches(grid, helper.getLevel()),
                    "GT6 craftingToolSawAxe accepts " + type.id());
            var remaining = recipe.getRemainingItems(grid).get(0);
            helper.assertTrue(remaining.is(tool.getItem()) && remaining.getCount() == 1
                            && remaining.getDamageValue() == type.damagePerCraft(),
                    "the " + type.id() + " is damaged once and returned");
        }
        helper.succeed();
    }
}
