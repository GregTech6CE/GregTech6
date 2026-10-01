package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.plant.BaleBlock;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Crop and grass bale behavior and survival loop from GT6 BlockBaseBale/MultiItemFood. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BaleRepairTests {
    private BaleRepairTests() {}

    private static final String[][] BALES = {
            {"grass", "grass_2"}, {"grass_dry", "dry_grass"},
            {"grass_moldy", "moldy_grass"}, {"grass_rotten", "rotten_grass"},
            {"barley", "barley"}, {"oat", "oats"}, {"rice", "rice"}, {"rye", "rye"}
    };

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("gregtech", path);
    }

    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null, 0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) {
                return ItemStack.EMPTY;
            }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        }, 3, 3);
    }

    @GameTest(template = "test_empty")
    public static void everyBaleHasOriginalFireHarvestModelAndSurvivalRecipe(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (String[] pair : BALES) {
            String name = "bale_" + pair[0];
            var block = ForgeRegistries.BLOCKS.getValue(id(name));
            Item crop = ForgeRegistries.ITEMS.getValue(id(pair[1]));
            helper.assertTrue(block instanceof BaleBlock && crop != null && crop != Items.AIR,
                    name + " and its input crop are registered");
            BaleBlock bale = (BaleBlock) block;
            var state = bale.defaultBlockState();
            var position = helper.absolutePos(new BlockPos(0, 0, 0));
            helper.assertTrue(BlockHarvestPolicy.tool(bale) == BlockHarvestPolicy.Tool.SWORD
                            && state.is(BlockTags.SWORD_EFFICIENT)
                            && bale.getFlammability(state, helper.getLevel(), position, Direction.NORTH) == 150
                            && bale.getFireSpreadSpeed(state, helper.getLevel(), position, Direction.NORTH) == 150,
                    name + " has GT6 sword and fire properties");
            helper.assertTrue(Block.getDrops(state, helper.getLevel(), position, null).stream()
                            .anyMatch(stack -> stack.is(bale.asItem())), name + " drops itself");
            for (String path : new String[]{"blockstates/" + name + ".json",
                    "models/block/iconsets/" + name + ".json", "models/item/" + name + ".json"}) {
                helper.assertTrue(BaleRepairTests.class.getClassLoader()
                                .getResource("assets/gregtech/" + path) != null, name + " has " + path);
            }

            var pack = manager.byKey(id("bales/" + name + "_pack")).orElse(null);
            var unpack = manager.byKey(id("bales/" + name + "_unpack")).orElse(null);
            helper.assertTrue(pack instanceof CraftingRecipe && unpack instanceof CraftingRecipe,
                    name + " has both GT6 packing directions");
            var packed = grid();
            for (int slot = 0; slot < 9; slot++) packed.setItem(slot, new ItemStack(crop));
            helper.assertTrue(((CraftingRecipe) pack).matches(packed, helper.getLevel())
                            && ((CraftingRecipe) pack).assemble(packed, helper.getLevel().registryAccess())
                            .is(bale.asItem()), name + " packs nine crops");
            var unpacked = grid();
            unpacked.setItem(0, new ItemStack(bale));
            var result = ((CraftingRecipe) unpack).assemble(unpacked, helper.getLevel().registryAccess());
            helper.assertTrue(((CraftingRecipe) unpack).matches(unpacked, helper.getLevel())
                            && result.is(crop) && result.getCount() == 9,
                    name + " unpacks to nine crops");
        }
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space")
    public static void wetGrassBaleAgesWithoutLosingItsAxis(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        BlockPos position = helper.absolutePos(relative);
        BlockPos water = helper.absolutePos(relative.east());
        var level = helper.getLevel();
        var fresh = (BaleBlock) ForgeRegistries.BLOCKS.getValue(id("bale_grass"));
        var moldy = (BaleBlock) ForgeRegistries.BLOCKS.getValue(id("bale_grass_moldy"));
        var rotten = (BaleBlock) ForgeRegistries.BLOCKS.getValue(id("bale_grass_rotten"));
        level.setBlock(position, fresh.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                Direction.Axis.X), 3);
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        RandomSource random = RandomSource.create(0xBA1E);
        for (int i = 0; i < 1000 && level.getBlockState(position).is(fresh); i++) {
            fresh.tick(level.getBlockState(position), level, position, random);
        }
        helper.assertTrue(level.getBlockState(position).is(moldy)
                        && level.getBlockState(position).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
                "wet fresh grass becomes moldy and keeps its rotation");
        for (int i = 0; i < 1000 && level.getBlockState(position).is(moldy); i++) {
            moldy.tick(level.getBlockState(position), level, position, random);
        }
        helper.assertTrue(level.getBlockState(position).is(rotten)
                        && level.getBlockState(position).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
                "wet moldy grass becomes rotten and keeps its rotation");
        helper.succeed();
    }
}
