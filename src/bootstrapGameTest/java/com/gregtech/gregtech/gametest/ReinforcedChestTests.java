package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.registry.GTStorage;
import com.gregtech.gregtech.registry.GTStorageMetals;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTLootChests;
import com.gregtech.gregtech.block.inventory.MetalChestBlock;
import com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity;
import com.gregtech.gregtech.api.material.MaterialProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ReinforcedChestTests {
    @GameTest(template = "test_empty")
    public static void reinforcedChestFamilyHasStoragePropertiesAndRealCrafting(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.assertTrue(GTStorage.REINFORCED_WOOD_CHESTS.size() == GTStorageMetals.ALL.size(), "all storage materials have reinforced chests");
        var player = h.makeMockPlayer();
        for (var spec : GTStorageMetals.ALL) {
            var block = GTStorage.reinforcedChest(spec.material());
            var state = block.defaultBlockState();
            h.assertTrue(block.shell() == MetalChestBlock.Shell.REINFORCED_WOOD, "wood shell: " + spec.suffix());
            h.assertTrue(GTBlockEntities.METAL_CHEST.get().isValid(state), "registered chest entity accepts " + spec.suffix());
            h.assertTrue(state.getDestroySpeed(level, pos) == spec.hardness() / 2 && block.getExplosionResistance() == spec.resistance() / 2,
                    "GT6 half hardness and resistance: " + spec.suffix());
            h.assertTrue(block.getFlammability(state, level, pos, Direction.UP) == (spec.material().has(MaterialProperty.UNBURNABLE) ? 0 : 100),
                    "reinforcement controls flammability: " + spec.suffix());
            h.assertTrue(new ItemStack(block).getMaxStackSize() == 16, "GT6 chest item stack limit");
            h.assertTrue(state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE), "wood shell receives axe tag");
            level.setBlock(pos, state, 3);
            var chest = (MetalChestBlockEntity) level.getBlockEntity(pos);
            h.assertTrue(chest.inventory().getSlots() == 54, "54 storage slots");
            chest.inventory().setStackInSlot(53, new ItemStack(Items.DIAMOND, 7));
            chest.load(chest.saveWithoutMetadata());
            h.assertTrue(chest.inventory().getStackInSlot(53).getCount() == 7, "last slot survives reload");
            chest.inventory().setStackInSlot(53, ItemStack.EMPTY);

            var id = ResourceLocation.fromNamespaceAndPath("gregtech", "hand/storage/reinforced_wood_chest_" + spec.suffix());
            var recipe = level.getRecipeManager().byKey(id).orElseThrow();
            h.assertTrue(recipe instanceof ShapedRecipe, "real shaped recipe registered: " + spec.suffix());
            var shaped = (ShapedRecipe) recipe;
            var grid = new TransientCraftingContainer(new CraftingMenu(0, player.getInventory()), 3, 3);
            for (int i = 0; i < 9; i++) {
                var stacks = shaped.getIngredients().get(i).getItems();
                h.assertTrue(stacks.length > 0, "all chest ingredients resolve: " + spec.suffix());
                var input = stacks[0].copy();
                if (input.getItem() instanceof com.gregtech.gregtech.item.GTToolItem) {
                    input = com.gregtech.gregtech.item.GTToolItem.create(
                            com.gregtech.gregtech.api.tool.GTToolHelper.getType(input),
                            com.gregtech.gregtech.content.material.Materials.Steel,
                            com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
                }
                grid.setItem(i, input);
            }
            grid.setItem(4, new ItemStack(Items.CHEST));
            h.assertTrue(shaped.matches(grid, level), "GT6 pattern matches: " + spec.suffix());
            h.assertTrue(shaped.assemble(grid, level.registryAccess()).is(block.asItem()), "crafting yields correct reinforcement");
            var remaining = shaped.getRemainingItems(grid);
            h.assertTrue(!remaining.get(0).isEmpty() && !remaining.get(2).isEmpty(), "saw and wrench are returned");
            h.assertTrue(remaining.get(4).isEmpty(), "wooden chest input is consumed");
        }
        for (var loot : GTLootChests.LOOT_CHESTS) {
            h.assertTrue(GTBlockEntities.METAL_CHEST.get().isValid(loot.get().defaultBlockState()), "loot chest belongs to entity type");
            h.assertTrue(loot.get().shell() == MetalChestBlock.Shell.LOOT, "loot chest uses its own original shell");
        }
        h.succeed();
    }
}
