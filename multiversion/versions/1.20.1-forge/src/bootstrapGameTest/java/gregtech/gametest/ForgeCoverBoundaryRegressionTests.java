package gregtech.gametest;

import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.cover.CoverConnections;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.CoverUtilityBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gt6ce_forge_cover_boundaries")
@PrefixGameTestTemplate(false)
public final class ForgeCoverBoundaryRegressionTests {
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void pressureValveMovesAFullTankIntoTheTankInFront(GameTestHelper h) {
        CoverBehaviorTests.pressureValveMovesAFullTankIntoTheTankInFront(h);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void theFilterCoversFilterThePipesOnTheirOwnFace(GameTestHelper h) {
        CoverBehaviorTests.theFilterCoversFilterThePipesOnTheirOwnFace(h);
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void theFilterOnTheSendingFaceGatesOutgoingItems(GameTestHelper h) {
        ItemPipeCoverTests.theFilterOnTheSendingFaceGatesOutgoingItems(h);
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void emptyValveDisconnectsAndRejectsLaterPipeConnections(GameTestHelper h) {
        var level = h.getLevel();
        var pos = new BlockPos(174000, 180, 174000);
        level.getChunkAt(pos);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "pipe_medium_steel"));
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var pipe = (FluidPipeBlockEntity) level.getBlockEntity(pos);
        FluidPipeBlockEntity.serverTick(level, pos, pipe.getBlockState(), pipe);
        var valve = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "pressure_value")));
        h.assertTrue(CoverUtilityBehaviors.PRESSURE_VALVE.equals(CoverItems.behavior(valve)), "registered pressure valve");
        h.assertTrue(pipe.attachCover(Direction.EAST, valve), "valve attaches before neighbour exists");
        level.setBlockAndUpdate(pos, pipe.getBlockState().setValue(FluidPipeBlock.EAST, true));
        pipe.panels().restoreStopped(true);
        for (int i=0; i<4; i++) FluidPipeBlockEntity.serverTick(level, pos, pipe.getBlockState(), pipe);
        h.assertTrue(pipe.getBlockState().getValue(FluidPipeBlock.EAST), "stopped covers do not disconnect");
        pipe.panels().restoreStopped(false);
        FluidPipeBlockEntity.serverTick(level, pos, pipe.getBlockState(), pipe);
        h.assertTrue(!pipe.getBlockState().getValue(FluidPipeBlock.EAST), "eligible empty valve disconnects");
        level.setBlockAndUpdate(pos.east(), block.defaultBlockState());
        var neighbour = (FluidPipeBlockEntity) level.getBlockEntity(pos.east());
        h.assertTrue(!CoverConnections.canConnect(level, pos, Direction.EAST)
                && !CoverConnections.canConnect(level, pos.east(), Direction.WEST), "both tool directions reject pipe connection");
        level.setBlockAndUpdate(pos, pipe.getBlockState().setValue(FluidPipeBlock.EAST, true));
        level.setBlockAndUpdate(pos.east(), neighbour.getBlockState().setValue(FluidPipeBlock.WEST, true));
        FluidPipeBlockEntity.serverTick(level, pos, pipe.getBlockState(), pipe);
        h.assertTrue(!pipe.getBlockState().getValue(FluidPipeBlock.EAST)
                && !neighbour.getBlockState().getValue(FluidPipeBlock.WEST), "tick clears both connection bits");
        h.assertTrue(pipe.getFluidInTank(0).isEmpty(), "empty valve does not create fluid");
        h.succeed();
    }
}
