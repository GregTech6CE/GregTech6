package gregtech.gametest;

import gregtech.content.registry.GTMultiblocks;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.engine.tool.MachineRotationType;
import gregtech.engine.tool.ToolInteractionSpec;
import gregtech.engine.tool.ToolInteractions;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ToolRotationTests {
    private ToolRotationTests() {}

    @GameTest(template = "test_empty", batch = "tool_rotation", timeoutTicks = 20)
    public static void rotationPreviewsRespectMachineAndVanillaFacingLimits(GameTestHelper helper) {
        var wrench = GTToolHelper.displayTool(GTToolType.WRENCH);
        var horizontal = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
        checkPreview(helper, wrench, Blocks.BARREL.defaultBlockState(), horizontal);
        checkPreview(helper, wrench, Blocks.HOPPER.defaultBlockState(), EnumSet.complementOf(EnumSet.of(Direction.UP)));
        checkPreview(helper, wrench, Blocks.OBSERVER.defaultBlockState(), EnumSet.allOf(Direction.class));
        checkPreview(helper, wrench, GTMultiblocks.HEAT_EXCHANGER_MAIN.get().defaultBlockState(), EnumSet.of(Direction.DOWN));

        var horizontalFacing = ToolInteractionSpec.facing(BlockStateProperties.HORIZONTAL_FACING, MachineRotationType.ALL);
        for (var side : Direction.values())
            helper.assertTrue(horizontalFacing.allows(side) == horizontal.contains(side),
                    "even the all-faces policy respects a horizontal facing property on " + side);
        helper.succeed();
    }

    private static void checkPreview(GameTestHelper helper, ItemStack wrench, BlockState state, EnumSet<Direction> faces) {
        var preview = ToolInteractions.preview(state, wrench);
        helper.assertTrue(preview != null, "the wrench offers a face grid on " + state);
        for (var side : Direction.values())
            helper.assertTrue(preview.allows(state, side) == faces.contains(side),
                    "wrench preview for " + state + " on " + side);
    }

    @GameTest(template = "test_empty", batch = "tool_rotation", timeoutTicks = 20)
    public static void wrenchUsesDeclaredPolicyBeforeTheFallback(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.5, 0, 0), Direction.EAST, pos, false);
//? if >=1.21.1 {
        /*var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        var player = helper.makeMockPlayer();
//?}
        player.setItemInHand(InteractionHand.MAIN_HAND, GTToolHelper.displayTool(GTToolType.WRENCH));
        var exchanger = GTMultiblocks.HEAT_EXCHANGER_MAIN.get().defaultBlockState();
        level.setBlockAndUpdate(pos, exchanger);
        helper.assertTrue(MachineRotationType.handleWrench(exchanger, level, pos, player, InteractionHand.MAIN_HAND,
                hit, BlockStateProperties.FACING, MachineRotationType.ALL), "the controller handles the tool");
        helper.assertTrue(level.getBlockState(pos).getValue(BlockStateProperties.FACING) == Direction.DOWN,
                "the controller's bottom-only declaration rejects an eastward turn");

        var dispenser = Blocks.DISPENSER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, dispenser);
        helper.assertTrue(MachineRotationType.handleWrench(dispenser, level, pos, player, InteractionHand.MAIN_HAND,
                hit, BlockStateProperties.FACING, MachineRotationType.ALL), "the fallback handles the dispenser");
        helper.assertTrue(level.getBlockState(pos).getValue(BlockStateProperties.FACING) == Direction.EAST,
                "the fallback applies the selected face to an undeclared block");
        helper.succeed();
    }
}
