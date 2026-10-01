package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.GTGrassBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** The six GT6 BlockGrass colours share soil behaviour but do not spread. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class GTGrassRepairTests {
    private GTGrassRepairTests() {}

    @GameTest(template = "coin_pile_space")
    public static void colouredGrassDropsDirtAndSupportsPlants(GameTestHelper h) {
        for (String colour : new String[]{"medium", "light", "dark", "normal", "yellow", "brown"}) {
            Block block = ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", "grassblock_" + colour));
            h.assertTrue(block instanceof GTGrassBlock, colour + " is a GT6 grass block");
            var state = block.defaultBlockState();
            var drops = block.getDrops(state, new LootParams.Builder(h.getLevel()));
            h.assertTrue(drops.size() == 1 && drops.get(0).is(Blocks.DIRT.asItem()),
                    colour + " grass drops one dirt without Silk Touch");
            h.assertTrue(state.is(BlockTags.MINEABLE_WITH_SHOVEL), colour + " grass uses a shovel");
            h.assertTrue(!state.isRandomlyTicking(), colour + " grass does not spread");
            var recipes = h.getLevel().getRecipeManager();
            h.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath("gregtech",
                            "grass/grassblock_" + colour + "_from_dye")).isPresent()
                            && recipes.byKey(ResourceLocation.fromNamespaceAndPath("gregtech",
                                    "grass/grassblock_" + colour + "_to_vanilla")).isPresent(),
                    colour + " grass has the original dye and reverse crafting routes");
        }

        BlockPos soil = h.absolutePos(new BlockPos(2, 1, 2));
        var state = ForgeRegistries.BLOCKS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "grassblock_yellow"))
                .defaultBlockState();
        h.getLevel().setBlockAndUpdate(soil, state);
        GTGrassBlock grass = (GTGrassBlock) state.getBlock();
        h.assertTrue(grass.canSustainPlant(state, h.getLevel(), soil, Direction.UP,
                        (IPlantable) Blocks.DANDELION),
                "GT6 grass supports Plains flowers");
        h.assertTrue(!grass.canSustainPlant(state, h.getLevel(), soil, Direction.UP,
                        (IPlantable) Blocks.SUGAR_CANE),
                "GT6 grass needs adjacent water for Beach plants");
        h.getLevel().setBlockAndUpdate(soil.east(), Blocks.WATER.defaultBlockState());
        h.assertTrue(grass.canSustainPlant(state, h.getLevel(), soil, Direction.UP,
                        (IPlantable) Blocks.SUGAR_CANE),
                "GT6 grass supports Beach plants next to water");
        h.succeed();
    }
}
