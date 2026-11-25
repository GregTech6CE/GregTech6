package gregtech.gametest;

import gregtech.content.transport.pipe.fluid.FluidPipeBlock;
import gregtech.content.transport.pipe.item.ItemPipeBlock;
import gregtech.content.transport.pipe.MaterialPipeBlock;
import gregtech.content.blockentity.machine.LogisticsWireBlockEntity;
import gregtech.content.cover.PipeCoverHost;
import gregtech.content.data.generated.GT6Materials;
import gregtech.content.item.GTToolItem;
import gregtech.content.tool.ElectricWrenchHarvest;
import gregtech.engine.machine.PipeGeometry;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.engine.transport.PipeConnections;
import java.util.Arrays;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void connectorGeometryKeepsPipeWireAndAxleWidthsSeparate(GameTestHelper helper) {
        double none = PipeGeometry.NONE;
        var fluid = block("pipe_tiny_steel").defaultBlockState();
        var item = block("item_pipe_medium_brass").defaultBlockState();
        checkWidths(helper, fluid, 2, none, none);
        checkWidths(helper, item, 4, none, none);
        checkWidths(helper, block("wire_02_copper").defaultBlockState(), none, 1.5, none);
        checkWidths(helper, block("wire_01_redalloy").defaultBlockState(), none, 1, none);
        checkWidths(helper, block("cable_01_redalloy").defaultBlockState(), none, 2, none);
        checkWidths(helper, block("axle_steel_2").defaultBlockState(), none, none, 4.5);
        checkWidths(helper, Blocks.STONE.defaultBlockState(), none, none, none);

        double thin = PipeGeometry.pipeHalfOf(fluid);
        double thick = PipeGeometry.fluidNeighborHalf(item, Direction.EAST);
        helper.assertTrue(Arrays.equals(PipeGeometry.armBox(Direction.EAST, thin, thick),
                        new double[] {10, 6, 6, 20, 10, 10}),
                "the thin fluid pipe renders its arm up to the thick item pipe's core");
        helper.assertTrue(PipeGeometry.armBox(Direction.WEST, thick, thin) == null,
                "the thick item pipe leaves rendering to the thin fluid pipe");
        helper.assertTrue(Arrays.equals(PipeGeometry.localArmBox(Direction.WEST, thick, thin),
                        new double[] {0, 6, 6, 4, 10, 10}),
                "the thick pipe's collision arm fills its own cell at the thin pipe's width");
        helper.succeed();
    }

    private static void checkWidths(GameTestHelper helper, BlockState state, double pipe, double wire, double axle) {
        helper.assertTrue(PipeGeometry.pipeHalfOf(state) == pipe, "pipe half-width for " + state);
        helper.assertTrue(PipeGeometry.wireHalfOf(state) == wire, "wire half-width for " + state);
        helper.assertTrue(PipeGeometry.axleHalfOf(state) == axle, "axle half-width for " + state);
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void steamEngineVirtualSocketsFollowTheirFaces(GameTestHelper helper) {
        var engine = block("engine_steam_bronze").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH);
        helper.assertTrue(PipeGeometry.pipeHalfOf(engine) == PipeGeometry.NONE,
                "a machine socket supplies only face-specific fluid geometry");
        helper.assertTrue(PipeGeometry.fluidNeighborHalf(engine, Direction.SOUTH) == PipeGeometry.NONE,
                "a pipe facing the engine's kinetic output sees no socket");
        helper.assertTrue(PipeGeometry.fluidNeighborHalf(engine, Direction.NORTH) == PipeGeometry.virtual(4),
                "a pipe facing the steam input sees a medium socket");
        helper.assertTrue(PipeGeometry.fluidNeighborHalf(engine, Direction.DOWN) == PipeGeometry.virtual(2),
                "a pipe facing the water return sees a tiny socket");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void pipeHarvestUsesMaterialToolAndWrenchQuality(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        var level = helper.getLevel();
        BlockPos pos = at(helper, 1, 1, 1);
        var wood = (FluidPipeBlock) block("pipe_medium_wood");
        var rubber = (FluidPipeBlock) block("pipe_medium_rubber");
        checkSoftPipeTool(helper, player, pos, wood, new ItemStack(Items.IRON_AXE));
        checkSoftPipeTool(helper, player, pos, rubber, new ItemStack(Items.SHEARS));

        ItemStack wrench = GTToolItem.create(GTToolType.WRENCH, GT6Materials.Compounds.Brass, GT6Materials.Woods.Spruce);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        var brass = (ItemPipeBlock) block("item_pipe_medium_brass");
        var hard = (ItemPipeBlock) block("item_pipe_medium_vibranium_silver");
        helper.assertTrue(brass.canHarvestBlock(brass.defaultBlockState(), level, pos, player)
                && brass.getDestroyProgress(brass.defaultBlockState(), player, level, pos) > 0,
                "a brass wrench mines a brass item pipe");
        helper.assertFalse(hard.canHarvestBlock(hard.defaultBlockState(), level, pos, player),
                "a brass wrench cannot harvest the higher-quality pipe");
        helper.assertTrue(hard.getDestroyProgress(hard.defaultBlockState(), player, level, pos) == 0,
                "an underqualified wrench makes no mining progress");
        helper.succeed();
    }

    private static void checkSoftPipeTool(GameTestHelper helper, Player player, BlockPos pos,
                                          MaterialPipeBlock pipe, ItemStack tool) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var state = pipe.defaultBlockState();
        var level = helper.getLevel();
        float speed = tool.getDestroySpeed(state) / state.getDestroySpeed(level, pos) / 30.0F;
        helper.assertTrue(pipe.canHarvestBlock(state, level, pos, player)
                && pipe.getDestroyProgress(state, player, level, pos) == speed,
                "the matching vanilla tool harvests the pipe at its tool speed");
        helper.assertFalse(ElectricWrenchHarvest.target(state), "wood and rubber are excluded from electric wrench targets");
    }

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
    public static void pipePlacedBetweenTwoClosedPipesLinksOnlyTheClickedPair(GameTestHelper helper) {
        String item = "item_pipe_medium_electrum";
        BlockPos clicked = at(helper, 1, 1, 2);
        BlockPos middle = clicked.east();
        BlockPos other = middle.east();
        helper.getLevel().setBlockAndUpdate(clicked, block(item).defaultBlockState());
        helper.getLevel().setBlockAndUpdate(other, block(item).defaultBlockState());

        helper.assertTrue(place(helper, clicked, Direction.EAST, item), "the middle pipe places against the first pipe");
        helper.assertTrue(connected(helper, clicked, ItemPipeBlock.EAST) && connected(helper, middle, ItemPipeBlock.WEST),
                "the placed pipe and the clicked neighbor open their shared faces");
        helper.assertFalse(connected(helper, middle, ItemPipeBlock.EAST) || connected(helper, other, ItemPipeBlock.WEST),
                "the second neighbor and the placed pipe keep their shared faces closed");
        clear(helper, clicked, middle, other);
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void installedCoversBlockLaterSameFamilyPipePlacement(GameTestHelper helper) {
        BlockPos itemPos = at(helper, 1, 1, 1);
        BlockPos fluidPos = at(helper, 1, 1, 3);
        checkCoveredPipePlacement(helper, itemPos, "item_pipe_medium_electrum", "item_filter",
                ItemPipeBlock.EAST, ItemPipeBlock.WEST);
        checkCoveredPipePlacement(helper, fluidPos, "pipe_medium_steel", "pressure_value",
                FluidPipeBlock.EAST, FluidPipeBlock.WEST);
        clear(helper, itemPos, itemPos.east(), fluidPos, fluidPos.east());
    }

    private static void checkCoveredPipePlacement(GameTestHelper helper, BlockPos pos, String pipeId, String coverId,
            BooleanProperty east, BooleanProperty west) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, block(pipeId).defaultBlockState());
        var host = (PipeCoverHost) level.getBlockEntity(pos);
        var cover = new ItemStack(BuiltInRegistries.ITEM.get(id(coverId)));
        helper.assertTrue(host.attachCover(Direction.EAST, cover), "the cover attaches on an open face");
        helper.assertTrue(place(helper, pos, Direction.EAST, pipeId), "the second pipe can be placed");
        helper.assertFalse(connected(helper, pos, east) || connected(helper, pos.east(), west),
                "neither pipe opens across the covered seam");

        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        helper.assertFalse(host.removeCover(Direction.EAST).isEmpty(), "the cover can be removed");
        helper.assertTrue(place(helper, pos, Direction.EAST, pipeId), "the second pipe can be placed again");
        helper.assertTrue(connected(helper, pos, east) && connected(helper, pos.east(), west),
                "the same placement connects both faces after cover removal");
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

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void wireCutterConnectsAndDisconnectsAdjacentLogisticsWires(GameTestHelper helper) {
        BlockPos first = at(helper, 1, 1, 2);
        BlockPos second = first.east();
        helper.getLevel().setBlockAndUpdate(first, block("logistics_wire").defaultBlockState());
        helper.getLevel().setBlockAndUpdate(second, block("logistics_wire").defaultBlockState());

        helper.assertFalse(connected(helper, first, logisticsProp(Direction.EAST)),
                "adjacent logistics wires start disconnected on the shared face");
        helper.assertFalse(connected(helper, second, logisticsProp(Direction.WEST)),
                "adjacent neighbor wire starts disconnected on the opposite face");

        ItemStack cutter = GTToolHelper.displayTool(GTToolType.WIRE_CUTTER);
        helper.assertTrue(clickWithTool(helper, first, Direction.EAST, cutter),
                "wire cutter connects two disconnected adjacent logistics wires");
        helper.assertTrue(connected(helper, first, logisticsProp(Direction.EAST)),
                "clicked logistics wire opens its face toward the neighbor wire");
        helper.assertTrue(connected(helper, second, logisticsProp(Direction.WEST)),
                "neighbor logistics wire opens its opposite face synchronously");
        helper.assertTrue(helper.getLevel().getBlockEntity(first) instanceof LogisticsWireBlockEntity firstWire
                        && firstWire.canLogistics(Direction.EAST),
                "clicked wire reports open logistics connectivity after connecting");
        helper.assertTrue(helper.getLevel().getBlockEntity(second) instanceof LogisticsWireBlockEntity secondWire
                        && secondWire.canLogistics(Direction.WEST),
                "neighbor wire reports open logistics connectivity after connecting");

        helper.assertTrue(clickWithTool(helper, first, Direction.EAST, cutter),
                "clicking the connected face again with a wire cutter disconnects both wires");
        helper.assertFalse(connected(helper, first, logisticsProp(Direction.EAST)),
                "clicked logistics wire closes its face when disconnected");
        helper.assertFalse(connected(helper, second, logisticsProp(Direction.WEST)),
                "neighbor logistics wire closes its opposite face when disconnected");
        clear(helper, first, second);
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void wireCutterRespectsLogisticsCoversAndSolidBlocks(GameTestHelper helper) {
        BlockPos solid = at(helper, 1, 1, 4);
        BlockPos first = solid.east();
        BlockPos second = first.east();
        helper.getLevel().setBlockAndUpdate(solid, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(first, block("logistics_wire").defaultBlockState());
        helper.getLevel().setBlockAndUpdate(second, block("logistics_wire").defaultBlockState());

        var secondEntity = helper.getLevel().getBlockEntity(second);
        helper.assertTrue(secondEntity instanceof LogisticsWireBlockEntity, "neighbor logistics wire has block entity");
        LogisticsWireBlockEntity secondWire = (LogisticsWireBlockEntity) secondEntity;
        ItemStack bus = new ItemStack(BuiltInRegistries.ITEM.get(id("generic_logistics_import_bus")));
        helper.assertTrue(secondWire.logisticsCovers().attach(Direction.WEST, bus),
                "logistics cover attaches to the neighbor wire face");

        ItemStack cutter = GTToolHelper.displayTool(GTToolType.WIRE_CUTTER);
        helper.assertFalse(clickWithTool(helper, first, Direction.EAST, cutter),
                "wire cutter refuses to connect across a neighbor logistics cover");
        helper.assertFalse(connected(helper, first, logisticsProp(Direction.EAST)),
                "blocked connection keeps the clicked face closed");
        helper.assertFalse(connected(helper, second, logisticsProp(Direction.WEST)),
                "blocked connection keeps the covered neighbor face closed");

        helper.assertFalse(clickWithTool(helper, first, Direction.WEST, cutter),
                "wire cutter refuses to connect into an unrelated solid block");
        helper.assertFalse(connected(helper, first, logisticsProp(Direction.WEST)),
                "refused solid-block connection keeps the wire face closed");
        clear(helper, solid, first, second);
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void shutterCoverBlocksAndDisconnectsLogisticsWires(GameTestHelper helper) {
        BlockPos first = at(helper, 1, 1, 2);
        BlockPos second = first.east();
        helper.getLevel().setBlockAndUpdate(first, block("logistics_wire").defaultBlockState());
        helper.getLevel().setBlockAndUpdate(second, block("logistics_wire").defaultBlockState());

        var secondEntity = helper.getLevel().getBlockEntity(second);
        helper.assertTrue(secondEntity instanceof gregtech.content.cover.PanelCoverHost,
                "neighbor logistics wire accepts panel covers");
        var secondHost = (gregtech.content.cover.PanelCoverHost) secondEntity;
        ItemStack shutter = new ItemStack(BuiltInRegistries.ITEM.get(id("shutter_cover")));
        gregtech.content.cover.CoverStackData.putBoolean(shutter, "gt.cover.inverted", true);
        helper.assertTrue(secondHost.attachCover(Direction.WEST, shutter.copy()) && secondHost.panels().shuttered(Direction.WEST),
                "closed shutter cover attaches to the neighbor wire face");

        ItemStack cutter = GTToolHelper.displayTool(GTToolType.WIRE_CUTTER);
        helper.assertFalse(clickWithTool(helper, first, Direction.EAST, cutter),
                "wire cutter refuses to connect across a closed shutter cover");
        helper.assertFalse(connected(helper, first, logisticsProp(Direction.EAST)),
                "shuttered seam keeps clicked wire face closed");
        helper.assertFalse(connected(helper, second, logisticsProp(Direction.WEST)),
                "shuttered seam keeps covered neighbor wire face closed");

        helper.getLevel().setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
        var firstEntity = helper.getLevel().getBlockEntity(first);
        helper.assertTrue(firstEntity instanceof gregtech.content.cover.PanelCoverHost,
                "first wire accepts panel covers");
        var firstHost = (gregtech.content.cover.PanelCoverHost) firstEntity;
        helper.assertTrue(firstHost.attachCover(Direction.EAST, shutter.copy()) && firstHost.panels().shuttered(Direction.EAST),
                "closed shutter cover attaches to the first wire face");
        helper.assertTrue(place(helper, first, Direction.EAST, "logistics_wire"),
                "placing a wire against a shuttered wire places the block");
        helper.assertFalse(connected(helper, first, logisticsProp(Direction.EAST)),
                "placing against a shuttered wire does not open the covered face");
        helper.assertFalse(connected(helper, second, logisticsProp(Direction.WEST)),
                "newly placed wire does not open into a shuttered face");

        firstHost.removeCover(Direction.EAST);
        helper.assertTrue(clickWithTool(helper, first, Direction.EAST, cutter),
                "removing the shutter lets the wire cutter connect both wires");
        helper.assertTrue(connected(helper, first, logisticsProp(Direction.EAST))
                        && connected(helper, second, logisticsProp(Direction.WEST)),
                "both wires open before re-attaching a closed shutter");
        helper.assertTrue(firstHost.attachCover(Direction.EAST, shutter.copy()) && firstHost.panels().shuttered(Direction.EAST),
                "re-attaching a closed shutter closes cleanly");
        helper.assertFalse(connected(helper, first, logisticsProp(Direction.EAST)),
                "attaching a closed shutter disconnects the host wire face");
        helper.assertFalse(connected(helper, second, logisticsProp(Direction.WEST)),
                "attaching a closed shutter disconnects the neighbor wire face");
        clear(helper, first, second);
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void logisticsWirePlacedOnSolidGroundStaysDisconnectedDownward(GameTestHelper helper) {
        BlockPos stone = at(helper, 2, 1, 2);
        BlockPos wire = stone.above();
        helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());

        helper.assertTrue(place(helper, stone, Direction.UP, "logistics_wire"),
                "logistics wire places on top of solid stone ground");
        helper.assertFalse(connected(helper, wire, logisticsProp(Direction.DOWN)),
                "logistics wire placed against stone ground never opens its downward face");
        clear(helper, stone, wire);
    }

    private static Player mockPlayer(GameTestHelper helper) {
//? if >=1.21.1 {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        return helper.makeMockPlayer();
//?}
    }

    private static boolean clickWithTool(GameTestHelper helper, BlockPos pos, Direction face, ItemStack tool) {
        Player player = mockPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        Vec3 hit = Vec3.atCenterOf(pos).add(
                face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D);
        BlockHitResult blockHit = new BlockHitResult(hit, face, pos, false);
        BlockState state = helper.getLevel().getBlockState(pos);
//? if >=1.21.1 {
        /*return state.useItemOn(tool, helper.getLevel(), player, InteractionHand.MAIN_HAND, blockHit).consumesAction();
*///?} else {
        return state.use(helper.getLevel(), player, InteractionHand.MAIN_HAND, blockHit).consumesAction();
//?}
    }

    private static net.minecraft.world.level.block.state.properties.BooleanProperty logisticsProp(Direction side) {
        return PipeConnections.propFor(side);
    }

    /** Places {@code item} the way a player does: the item's own use-on path against {@code clicked}. */
    private static boolean place(GameTestHelper helper, BlockPos clicked, Direction face, String item) {
        Player player = mockPlayer(helper);
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
