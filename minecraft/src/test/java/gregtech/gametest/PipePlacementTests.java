package gregtech.gametest;

import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.ItemPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * Connections a player gets from placing a pipe or a hopper, driven through the real
 * {@link BlockItem} placement path rather than by writing block states.
 *
 * <p>A pipe links to the block it was placed against (GT6 {@code TileEntityBase09Connector.onPlaced},
 * which connects the clicked side through {@code canConnect}), and a hopper set down with its nozzle
 * on an item pipe links that pipe to itself (GT6 {@code MultiTileEntityHopper.onPlaced}).</p>
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class PipePlacementTests {
    private static final String BATCH = "pipe_placement";

    private PipePlacementTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void pipePlacedAgainstAChestLinksTheClickFace(GameTestHelper helper) {
        BlockPos chest = at(helper, 1, 1, 1);
        helper.getLevel().setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        BlockPos pipe = chest.above();

        helper.assertTrue(place(helper, chest, Direction.UP, "item_pipe_medium_electrum"),
                "the pipe item is placed against the chest");
        helper.assertTrue(connected(helper, pipe, ItemPipeBlock.DOWN),
                "a pipe placed against an item container connects the face it was placed on");
        clear(helper, chest, pipe);
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void fluidPipePlacedAgainstATankLinksTheClickFace(GameTestHelper helper) {
        BlockPos tank = at(helper, 1, 1, 3);
        helper.getLevel().setBlockAndUpdate(tank, block("drum_bronze").defaultBlockState());
        BlockPos pipe = tank.above();

        helper.assertTrue(place(helper, tank, Direction.UP, "pipe_medium_steel"),
                "the fluid pipe item is placed against the tank");
        helper.assertTrue(connected(helper, pipe, FluidPipeBlock.DOWN),
                "a fluid pipe placed against a fluid container connects the face it was placed on");
        clear(helper, tank, pipe);
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void hopperPlacedFacingAPipeLinksThatFace(GameTestHelper helper) {
        BlockPos pipe = at(helper, 2, 1, 1);
        helper.getLevel().setBlockAndUpdate(pipe, block("item_pipe_medium_electrum").defaultBlockState());

        helper.assertTrue(place(helper, pipe, Direction.EAST, "hopper_bronze"),
                "the hopper is placed against the pipe");
        helper.assertTrue(helper.getLevel().getBlockState(pipe.east())
                        .getValue(net.minecraft.world.level.block.DirectionalBlock.FACING) == Direction.WEST,
                "the hopper turns its nozzle toward the pipe");
        helper.assertTrue(connected(helper, pipe, ItemPipeBlock.EAST),
                "a hopper placed with its nozzle on a pipe connects that pipe face");
        clear(helper, pipe, pipe.east());
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void queueHopperPlacedFacingAPipeLinksThatFace(GameTestHelper helper) {
        BlockPos pipe = at(helper, 4, 1, 1);
        helper.getLevel().setBlockAndUpdate(pipe, block("item_pipe_medium_electrum").defaultBlockState());

        helper.assertTrue(place(helper, pipe, Direction.NORTH, "queue_hopper_lead"),
                "the queue hopper is placed against the pipe");
        helper.assertTrue(connected(helper, pipe, ItemPipeBlock.NORTH),
                "a queue hopper placed with its nozzle on a pipe connects that pipe face");
        clear(helper, pipe, pipe.north());
    }

    /** Places {@code item} the way a player does: the item's own use-on path against {@code clicked}. */
    private static boolean place(GameTestHelper helper, BlockPos clicked, Direction face, String item) {
//? if >=1.21.1 {
        /*Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        Player player = helper.makeMockPlayer();
//?}
        player.setShiftKeyDown(true);
        BlockPos target = clicked.relative(face);
        Vec3 hit = Vec3.atCenterOf(clicked).add(
                face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D);
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id(item)));
        BlockPlaceContext context = new BlockPlaceContext(
                helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(hit, face, clicked, false));
        ((BlockItem) stack.getItem()).useOn(context);
        return helper.getLevel().getBlockState(target).is(BuiltInRegistries.BLOCK.get(id(item)));
    }

    private static boolean connected(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.state.properties.BooleanProperty face) {
        BlockState state = helper.getLevel().getBlockState(pos);
        return state.hasProperty(face) && state.getValue(face);
    }

    private static net.minecraft.world.level.block.Block block(String path) {
        return BuiltInRegistries.BLOCK.get(id(path));
    }

    private static ResourceLocation id(String path) {
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
*///?} else {
        return new ResourceLocation("gt6ce", path);
//?}
    }

    private static BlockPos at(GameTestHelper helper, int x, int y, int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    private static void clear(GameTestHelper helper, BlockPos... positions) {
        for (BlockPos pos : positions) {
            var entity = helper.getLevel().getBlockEntity(pos);
            if (entity instanceof net.minecraft.world.Container container) container.clearContent();
            helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }
}
