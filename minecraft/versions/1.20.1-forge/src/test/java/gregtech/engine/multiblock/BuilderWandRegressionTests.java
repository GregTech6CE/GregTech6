package gregtech.engine.multiblock;

import gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.material.Materials;
import gregtech.content.multiblock.LargeMachineParts;
import gregtech.content.registry.GTMultiblocks;
import gregtech.content.registry.GTToolItems;
import gregtech.engine.multiblock.tracking.StructureScheduler;
import gregtech.engine.multiblock.tracking.StructureTracking;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class BuilderWandRegressionTests {
    private BuilderWandRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void cokeOvenBuildsFromControllerAndIncompleteWalls(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        Player player = helper.makeMockSurvivalPlayer();
        player.moveTo(Vec3.atCenterOf(origin.above(8)));
        var brick = LargeMachineParts.block(18000);
        StructureTracking.consistencyCheck(true);
        try {
            for (Direction front : Direction.Plane.HORIZONTAL) {
                clear(level, origin);
                player.getInventory().clearContent();
                player.setItemInHand(InteractionHand.MAIN_HAND,
                        GTToolHelper.write(GTToolItems.empty(GTToolType.BUILDER_WAND), Materials.Steel, Materials.Steel));
                player.getInventory().setItem(1, new ItemStack(brick, 25));
                level.setBlockAndUpdate(origin, GTMultiblocks.COKE_OVEN_MAIN.get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, front));
                var controller = (CokeOvenControllerBlockEntity) level.getBlockEntity(origin);

                click(helper, player, origin);
                StructureScheduler.flush(level);
                helper.assertFalse(controller.isStructureOk(), "First click must leave the back wall unfinished: " + front);
                helper.assertTrue(player.getInventory().getItem(1).getCount() == 9,
                        "First click must consume exactly 16 bricks within its 3x3x3 reach: " + front);
                helper.assertTrue(level.isEmptyBlock(origin.relative(front)), "Do not copy the controller onto its front");
                BlockPos right = origin.relative(front.getOpposite()).relative(front.getClockWise());
                var wall = (MultiblockPortBlockEntity) level.getBlockEntity(right);
                helper.assertTrue(wall != null && wall.isBoundTo(origin), "Incomplete walls must route tools to the controller");
                var items = wall.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
                helper.assertTrue(items == null || items.getSlots() == 0, "Incomplete walls must not expose machine inventory");

                click(helper, player, right);
                StructureScheduler.flush(level);
                click(helper, player, origin.relative(front.getOpposite()).relative(front.getCounterClockWise()));
                StructureScheduler.flush(level);
                helper.assertTrue(controller.isStructureOk(), "Wall clicks must complete the coke oven: " + front);
                helper.assertTrue(player.getInventory().getItem(1).isEmpty(), "A completed oven consumes exactly 25 bricks");
                helper.assertTrue(level.isEmptyBlock(origin.relative(front.getOpposite())), "The centre must stay hollow");

                // A missing brick and empty inventory must leave the oven incomplete, retaining the other tool ports.
                BlockPos missing = origin.relative(front.getOpposite(), 2);
                level.setBlockAndUpdate(missing, Blocks.AIR.defaultBlockState());
                StructureScheduler.flush(level);
                click(helper, player, right);
                StructureScheduler.flush(level);
                helper.assertFalse(controller.isStructureOk(), "Survival construction requires the actual brick item");
                helper.assertTrue(level.isEmptyBlock(missing) && wall.isBoundTo(origin), "Missing supplies must not lose tool routing");
                level.setBlockAndUpdate(missing, Blocks.DIAMOND_BLOCK.defaultBlockState());
                player.getInventory().setItem(1, new ItemStack(brick));
                click(helper, player, right);
                StructureScheduler.flush(level);
                helper.assertTrue(level.getBlockState(missing).is(Blocks.DIAMOND_BLOCK)
                        && player.getInventory().getItem(1).getCount() == 1, "Never replace an occupied cell or consume its brick");
            }
        } finally {
            StructureTracking.consistencyCheck(false);
            clear(level, origin);
        }
        helper.succeed();
    }

    private static void click(GameTestHelper helper, Player player, BlockPos pos) {
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        var stack = player.getMainHandItem();
        helper.assertTrue(stack.getItem().onItemUseFirst(stack, context).consumesAction(),
                "The normal item-use entry must handle the multiblock click at " + pos);
    }

    private static void clear(ServerLevel level, BlockPos origin) {
        level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3)))
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}
