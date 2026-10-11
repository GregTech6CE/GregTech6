package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.tool.PlantPotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6's plant pot supports any plant above it, including plants that reject ordinary dirt. */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class PlantPotTests {
    @GameTest(template = "test_empty")
    public static void universalTopAndOriginalShape(GameTestHelper helper) {
        Block block = ForgeRegistries.BLOCKS.getValue(
                com.gregtech.gregtech.GregTech.id("plant_pot"));
        helper.assertTrue(block instanceof PlantPotBlock, "plant_pot has its GT6 plant support behavior");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        level.setBlock(pos, block.defaultBlockState(), 3);
        BlockState state = level.getBlockState(pos);

        for (Block plant : new Block[]{Blocks.CACTUS, Blocks.SUGAR_CANE, Blocks.OAK_SAPLING, Blocks.WHEAT}) {
            helper.assertTrue(state.canSustainPlant(level, pos, Direction.UP, (IPlantable) plant),
                    plant + " survives on top of the universal pot");
            helper.assertTrue(!state.canSustainPlant(level, pos, Direction.NORTH, (IPlantable) plant)
                            && !state.canSustainPlant(level, pos, Direction.DOWN, (IPlantable) plant),
                    "plants cannot attach to side/bottom of pot");
        }

        // Forge's cactus/cane code consults canSustainPlant directly; BushBlock's live check also
        // consults the dirt tag, which the port keeps for saplings and flowers.
        for (Block plant : new Block[]{Blocks.CACTUS, Blocks.SUGAR_CANE, Blocks.OAK_SAPLING}) {
            level.setBlock(pos.above(), plant.defaultBlockState(), 3);
            helper.assertTrue(level.getBlockState(pos.above()).is(plant)
                            && level.getBlockState(pos.above()).canSurvive(level, pos.above()),
                    plant + " remains planted over a real pot");
            level.removeBlock(pos.above(), false);
        }

        var shape = state.getCollisionShape(level, pos);
        helper.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(0.5, 0.5, 0.5)),
                "the pot's ceramic base collides");
        helper.assertTrue(shape.toAabbs().stream().noneMatch(box -> box.contains(0.02, 0.5, 0.02)),
                "the base remains inset one pixel");
        helper.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(0.02, 0.9, 0.02)),
                "the upper rim fills the full top surface");
        var drops = Block.getDrops(state, level, pos, null);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1,
                "breaking the pot returns one plant_pot");
        helper.succeed();
    }
}
