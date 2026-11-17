package gregtech.gametest;

import gregtech.content.block.machine.MultiblockPortBlock;
import gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity;
import gregtech.content.blockentity.machine.SmelteryFireHelper;
import gregtech.content.data.generated.GT6Materials;
import gregtech.content.item.GTToolItem;
import gregtech.content.multiblock.LargeMachineParts;
import gregtech.content.registry.GTMultiblocks;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.event.CrucibleInteractionHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
//?}

/**
 * A formed large crucible's mouth and its two hollow layers must reject fire and block placement,
 * while the outer shell, GT tools and the structure contract keep their original behaviour.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class LargeCrucibleCavityTests {
    private static final int CONTROLLER_ID = 17302;
    private static final int WALL_ID = 18002;

    private LargeCrucibleCavityTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 240)
    public static void formedHollowRejectsFireAndPlacement(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var machine = buildCrucible(helper, origin);
        var cavity1 = origin.above();
        var cavity2 = origin.above(2);
        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && formed(level, origin),
                        "Large crucible fixture did not form"))
                .thenExecute(() -> {
                    helper.assertFalse(level.getBlockState(origin).isFaceSturdy(level, origin, Direction.UP),
                            "The formed mouth still supports fire");
                    helper.assertFalse(Blocks.FIRE.defaultBlockState().canSurvive(level, cavity1),
                            "Fire survived on the formed mouth");
                    helper.assertFalse(SmelteryFireHelper.igniteAt(level, cavity1, false),
                            "Smeltery fire was placed in the first hollow layer");
                    helper.assertFalse(SmelteryFireHelper.igniteAt(level, cavity2, false),
                            "Smeltery fire was placed in the second hollow layer");
                    assertHollow(helper, cavity1, cavity2);
                })
                .thenExecute(() -> clickEveryCavityFace(helper, player, origin))
                .thenExecute(() -> assertHollow(helper, cavity1, cavity2))
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk(),
                        "Cavity clicks degraded the formed crucible"))
                .thenExecute(() -> level.setBlockAndUpdate(cavity1, Blocks.DIRT.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertFalse(machine.isStructureOk(),
                        "A stray block in the hollow kept the vessel formed"))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 240)
    public static void topRingPlacementAndMagnifierStayAvailable(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var machine = buildCrucible(helper, origin);
        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && formed(level, origin),
                        "Large crucible fixture did not form"))
                .thenExecute(() -> {
                    var ring = origin.east().above(2);
                    var above = ring.above();
                    var result = click(helper, player, hit(ring, Direction.UP), new ItemStack(Items.STONE), false);
                    helper.assertTrue(result.consumesAction(), "Top ring placement was rejected: " + result);
                    helper.assertTrue(level.getBlockState(above).is(Blocks.STONE),
                            "The top ring did not place a block above the vessel");
                })
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk(),
                        "Placing above the top ring degraded the structure"))
                .thenExecute(() -> {
                    var glass = GTToolItem.create(GTToolType.MAGNIFYING_GLASS,
                            GT6Materials.Compounds.VanadiumSteel, GT6Materials.Woods.Spruce);
                    helper.assertTrue(GTToolHelper.isTool(glass), "Magnifying glass fixture has no tool stats");
                    helper.assertFalse(deniesItemUse(player, hit(origin, Direction.UP), glass),
                            "The magnifier was intercepted on the controller mouth");
                    helper.assertFalse(deniesItemUse(player, hit(origin.west().above(), Direction.EAST), glass),
                            "The magnifier was intercepted on a hollow wall");
                    helper.assertTrue(deniesItemUse(player, hit(origin, Direction.UP), new ItemStack(Items.STONE)),
                            "The mouth stopped denying ordinary block items");
                })
                .thenExecute(player::discard)
                .thenSucceed();
    }

    /**
     * A piston above a loose block must not push it into the hollow, and a piston whose own head
     * would enter the hollow must not extend at all.
     */
    @GameTest(template = "test_empty", timeoutTicks = 240)
    public static void pistonsCannotEnterTheHollow(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 0, 2));
        var machine = buildCrucible(helper, origin);
        var aboveMouth = origin.above(3);
        var cavity1 = origin.above();
        var cavity2 = origin.above(2);
        var pistonAbove = aboveMouth.above();
        var pistonAtMouth = aboveMouth;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && formed(level, origin),
                        "Large crucible fixture did not form"))
                .thenExecute(() -> {
                    level.setBlockAndUpdate(aboveMouth, Blocks.COBBLESTONE.defaultBlockState());
                    placePiston(level, pistonAbove, Direction.DOWN);
                    // The pushed block would land in the second hollow layer.
                    level.setBlockAndUpdate(pistonAbove.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                    helper.assertFalse(extended(level, pistonAbove),
                            "A piston pushed a block towards the hollow");
                    helper.assertTrue(level.getBlockState(aboveMouth).is(Blocks.COBBLESTONE),
                            "The piston moved the block above the mouth");
                    assertHollow(helper, cavity1, cavity2);
                    // Disarm the fixture so the second scenario owns the whole column.
                    level.setBlockAndUpdate(pistonAbove.east(), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(aboveMouth, Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(pistonAbove, Blocks.AIR.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk(),
                        "The piston broke the formed vessel"))
                .thenExecute(() -> {
                    // The head of this piston would occupy the second hollow layer.
                    placePiston(level, pistonAtMouth, Direction.DOWN);
                    level.setBlockAndUpdate(pistonAtMouth.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                    helper.assertFalse(extended(level, pistonAtMouth),
                            "A piston head entered the hollow");
                    assertHollow(helper, cavity1, cavity2);
                    level.setBlockAndUpdate(pistonAtMouth.east(), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(pistonAtMouth, Blocks.AIR.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && formed(level, origin),
                        "The piston head broke the formed vessel"))
                .thenSucceed();
    }

    /** The underside of a block resting on the mouth opens into the hollow and must reject items. */
    @GameTest(template = "test_empty", timeoutTicks = 240)
    public static void clickUnderTheMouthCannotFillTheHollow(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 0, 2));
        var machine = buildCrucible(helper, origin);
        var aboveMouth = origin.above(3);
        var cavity1 = origin.above();
        var cavity2 = origin.above(2);
        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && formed(level, origin),
                        "Large crucible fixture did not form"))
                .thenExecute(() -> level.setBlockAndUpdate(aboveMouth, Blocks.STONE.defaultBlockState()))
                .thenExecute(() -> {
                    var under = hit(aboveMouth, Direction.DOWN);
                    helper.assertTrue(deniesItemUse(player, under, new ItemStack(Items.FLINT_AND_STEEL)),
                            "The underside of the mouth cover still allowed the igniter");
                    helper.assertTrue(deniesItemUse(player, under, new ItemStack(Items.STONE)),
                            "The underside of the mouth cover still allowed block placement");
                    helper.assertTrue(deniesItemUse(player, under, new ItemStack(Items.WATER_BUCKET)),
                            "The underside of the mouth cover still allowed a bucket");
                    click(helper, player, under, new ItemStack(Items.FLINT_AND_STEEL), false);
                    click(helper, player, under, new ItemStack(Items.STONE), false);
                    assertHollow(helper, cavity1, cavity2);
                })
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk(),
                        "The underside click broke the formed vessel"))
                .thenExecute(player::discard)
                .thenSucceed();
    }

    /** Fluids, dispensed contents and falling blocks all stop at the formed hollow. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void fluidsDispensersAndFallingBlocksStayOutOfTheHollow(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 0, 2));
        var machine = buildCrucible(helper, origin);
        var aboveMouth = origin.above(3);
        var cavity1 = origin.above();
        var cavity2 = origin.above(2);
        var sequence = helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && formed(level, origin),
                        "Large crucible fixture did not form"))
                .thenExecute(() -> level.setBlockAndUpdate(aboveMouth, Blocks.WATER.defaultBlockState()))
                .thenIdle(30)
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(cavity2).getFluidState().isEmpty(),
                            "Water flowed into the hollow");
                    assertHollow(helper, cavity1, cavity2);
                    clearAboveMouth(level, aboveMouth);
                })
                .thenIdle(5);
        for (var stack : List.of(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.LAVA_BUCKET),
                new ItemStack(Items.FLINT_AND_STEEL), new ItemStack(Items.SHULKER_BOX)))
            sequence.thenExecute(() -> dispenseFrom(level, aboveMouth, stack))
                    .thenIdle(5)
                    .thenExecute(() -> assertHollow(helper, cavity1, cavity2));
        sequence.thenExecute(() -> {
                    clearAboveMouth(level, aboveMouth);
                    net.minecraft.world.entity.item.FallingBlockEntity.fall(
                            level, aboveMouth, Blocks.SAND.defaultBlockState());
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    assertHollow(helper, cavity1, cavity2);
                    helper.assertTrue(machine.isStructureOk(), "The falling block broke the vessel");
                    helper.assertTrue(sandDropped(level, origin),
                            "The blocked falling block neither placed nor dropped");
                })
                .thenSucceed();
    }

    /**
     * Only a formed controller's bottom and the shell's true outward faces admit covers; every top
     * face, cavity-facing wall and seam between two vessel blocks refuses them.
     */
    @GameTest(template = "test_empty", timeoutTicks = 240)
    public static void coverSidesFollowTheOuterShell(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var machine = buildCrucible(helper, origin);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && formed(level, origin),
                        "Large crucible fixture did not form"))
                .thenExecute(() -> {
                    assertCoverSide(helper, machine, Direction.DOWN, true, "controller bottom");
                    for (var side : List.of(Direction.UP, Direction.NORTH, Direction.SOUTH,
                            Direction.EAST, Direction.WEST))
                        assertCoverSide(helper, machine, side, false, "controller " + side);

                    var topCorner = port(helper, origin.offset(-1, 2, -1));
                    helper.assertTrue(topCorner != null, "The top-ring corner has no port block entity");
                    assertCoverSide(helper, topCorner, Direction.WEST, true, "top corner west");
                    assertCoverSide(helper, topCorner, Direction.NORTH, true, "top corner north");
                    for (var side : List.of(Direction.UP, Direction.DOWN, Direction.EAST, Direction.SOUTH))
                        assertCoverSide(helper, topCorner, side, false, "top corner " + side);

                    var middleCorner = port(helper, origin.offset(-1, 1, -1));
                    helper.assertTrue(middleCorner != null, "The middle-ring corner has no port block entity");
                    assertCoverSide(helper, middleCorner, Direction.WEST, true, "middle corner west");
                    assertCoverSide(helper, middleCorner, Direction.NORTH, true, "middle corner north");
                    for (var side : List.of(Direction.UP, Direction.DOWN, Direction.EAST, Direction.SOUTH))
                        assertCoverSide(helper, middleCorner, side, false, "middle corner " + side);

                    var middleEdge = port(helper, origin.offset(-1, 1, 0));
                    helper.assertTrue(middleEdge != null, "The middle-ring edge has no port block entity");
                    assertCoverSide(helper, middleEdge, Direction.WEST, true, "middle edge west");
                    for (var side : List.of(Direction.UP, Direction.DOWN, Direction.EAST,
                            Direction.NORTH, Direction.SOUTH))
                        assertCoverSide(helper, middleEdge, side, false, "middle edge " + side);

                    var baseCorner = port(helper, origin.offset(-1, 0, -1));
                    helper.assertTrue(baseCorner != null, "The base-ring corner has no port block entity");
                    for (var side : List.of(Direction.WEST, Direction.NORTH, Direction.DOWN))
                        assertCoverSide(helper, baseCorner, side, true, "base corner " + side);
                    for (var side : List.of(Direction.UP, Direction.EAST, Direction.SOUTH))
                        assertCoverSide(helper, baseCorner, side, false, "base corner " + side);
                })
                .thenExecute(() -> {
                    // An unformed vessel admits nothing, controller or wall.
                    var controllerBlock = GTMultiblocks.findPart(CONTROLLER_ID).get();
                    var looseController = origin.offset(3, 0, -2);
                    level.setBlockAndUpdate(looseController, controllerBlock.defaultBlockState());
                    helper.assertTrue(level.getBlockEntity(looseController) instanceof LargeCrucibleControllerBlockEntity,
                            "The unformed controller has no block entity");
                    var unformed = (LargeCrucibleControllerBlockEntity) level.getBlockEntity(looseController);
                    for (var side : Direction.values())
                        assertCoverSide(helper, unformed, side, false, "unformed controller " + side);
                    var looseWall = origin.offset(3, 0, -1);
                    level.setBlockAndUpdate(looseWall, LargeMachineParts.block(WALL_ID).defaultBlockState());
                    var unformedWall = port(helper, looseWall);
                    helper.assertTrue(unformedWall != null, "The unformed wall has no port block entity");
                    for (var side : Direction.values())
                        assertCoverSide(helper, unformedWall, side, false, "unformed wall " + side);
                })
                .thenSucceed();
    }

    /** The cell preview and the real store both refuse seams and tops while taking an outward face. */
    @GameTest(template = "test_empty", timeoutTicks = 240)
    public static void coverCellsFollowTheOuterShell(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var machine = buildCrucible(helper, origin);
        var cover = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "shutter_cover")));
        var topCorner = origin.offset(-1, 2, -1);
        var baseCorner = origin.offset(-1, 0, -1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.isStructureOk() && formed(level, origin),
                        "Large crucible fixture did not form"))
                .thenExecute(() -> {
                    helper.assertTrue(gregtech.content.cover.CoverItems.isCover(cover),
                            "The cover fixture item is not a cover");
                    helper.assertTrue(canPlace(level, origin, Direction.DOWN, cover),
                            "The controller bottom refused the cover cell");
                    helper.assertFalse(canPlace(level, origin, Direction.UP, cover),
                            "The controller mouth offered a cover cell");
                    helper.assertFalse(canPlace(level, origin, Direction.WEST, cover),
                            "A controller seam offered a cover cell");
                    helper.assertTrue(canPlace(level, baseCorner, Direction.DOWN, cover),
                            "The base ring's bottom refused the cover cell");
                    helper.assertFalse(canPlace(level, baseCorner, Direction.UP, cover),
                            "A base wall top offered a cover cell");
                    helper.assertTrue(canPlace(level, topCorner, Direction.WEST, cover),
                            "An outward top-ring face refused the cover cell");
                    helper.assertFalse(canPlace(level, topCorner, Direction.UP, cover),
                            "The mouth ring offered a cover cell");
                    helper.assertFalse(canPlace(level, topCorner, Direction.DOWN, cover),
                            "A vertical seam offered a cover cell");
                    helper.assertFalse(canPlace(level, topCorner, Direction.EAST, cover),
                            "A same-layer seam offered a cover cell");
                })
                .thenExecute(() -> {
                    var host = (gregtech.content.cover.PanelCoverHost) level.getBlockEntity(origin);
                    helper.assertTrue(host.attachCover(Direction.DOWN, cover.copy()),
                            "The controller refused a cover on its bottom");
                    helper.assertFalse(canPlace(level, origin, Direction.DOWN, cover),
                            "The occupied bottom still offered a cover cell");
                    helper.assertFalse(host.attachCover(Direction.UP, cover.copy()),
                            "The formed controller accepted a cover on its mouth");
                    helper.assertTrue(host.getCover(Direction.UP).isEmpty(),
                            "The refused mouth cover was still stored");
                    host.removeCover(Direction.DOWN);
                })
                .thenSucceed();
    }

    private static void assertCoverSide(
            GameTestHelper helper, net.minecraft.world.level.block.entity.BlockEntity owner,
            Direction side, boolean expected, String message) {
        helper.assertTrue(gregtech.content.cover.CoverAdmission.allowCovers(owner, side) == expected,
                message + " should " + (expected ? "accept" : "refuse") + " a cover");
    }

    private static boolean canPlace(
            net.minecraft.world.level.Level level, BlockPos pos, Direction side, ItemStack held) {
        return gregtech.content.cover.CoverPlacement.canPlace(level, pos, side, held);
    }

    private static LargeCrucibleControllerBlockEntity buildCrucible(GameTestHelper helper, BlockPos origin) {
        var level = helper.getLevel();
        var controller = GTMultiblocks.findPart(CONTROLLER_ID).get();
        helper.assertTrue(controller instanceof gregtech.content.block.machine.LargeCrucibleControllerBlock,
                "Large crucible controller is not registered");
        var wall = LargeMachineParts.block(WALL_ID).defaultBlockState();
        level.setBlockAndUpdate(origin, controller.defaultBlockState());
        // Layers 0..2 form a closed 3x3 ring around the open column above the controller.
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                for (int y = 0; y <= 2; y++)
                    if (x != 0 || z != 0) level.setBlockAndUpdate(origin.offset(x, y, z), wall);
        helper.assertTrue(level.getBlockEntity(origin) instanceof LargeCrucibleControllerBlockEntity,
                "Large crucible controller has no block entity");
        return (LargeCrucibleControllerBlockEntity) level.getBlockEntity(origin);
    }

    /** Both hollow layers, the mouth and every hollow-facing wall at y=1 and y=2, plain and sneaking. */
    private static void clickEveryCavityFace(GameTestHelper helper, ServerPlayer player, BlockPos origin) {
        var hits = List.of(
                hit(origin, Direction.UP),
                hit(origin.west().above(), Direction.EAST),
                hit(origin.west().above(2), Direction.EAST),
                hit(origin.east().above(), Direction.WEST),
                hit(origin.east().above(2), Direction.WEST),
                hit(origin.north().above(), Direction.SOUTH),
                hit(origin.north().above(2), Direction.SOUTH),
                hit(origin.south().above(), Direction.NORTH),
                hit(origin.south().above(2), Direction.NORTH));
        for (var stack : cavityItems(helper)) {
            for (boolean sneaking : new boolean[] {false, true}) {
                for (var target : hits) {
                    click(helper, player, target, stack.copy(), sneaking);
                    assertHollow(helper, origin.above(), origin.above(2));
                }
            }
        }
    }

    private static List<ItemStack> cavityItems(GameTestHelper helper) {
        var igniter = GTToolItem.create(GTToolType.FLINT_AND_TINDER,
                GT6Materials.Compounds.VanadiumSteel, GT6Materials.Woods.Spruce);
        helper.assertTrue(GTToolHelper.matchesTool(igniter, GTToolType.FLINT_AND_TINDER),
                "GT flint and tinder fixture has no tool stats");
        return List.of(
                new ItemStack(Items.FLINT_AND_STEEL),
                new ItemStack(Items.FIRE_CHARGE),
                new ItemStack(Items.STONE),
                new ItemStack(Items.STICK),
                igniter);
    }

    private static InteractionResult click(
            GameTestHelper helper, ServerPlayer player, BlockHitResult hit, ItemStack stack, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setPos(hit.getBlockPos().getX() + 0.5D, hit.getBlockPos().getY() + 0.5D,
                hit.getBlockPos().getZ() + 0.5D);
        InteractionResult result = player.gameMode.useItemOn(
                player, helper.getLevel(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, hit);
        player.setShiftKeyDown(false);
        return result;
    }

    /** The denial is the RightClickBlock useItem flag, which a magnifier click does not otherwise reveal. */
    private static boolean deniesItemUse(ServerPlayer player, BlockHitResult hit, ItemStack stack) {
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setPos(hit.getBlockPos().getX() + 0.5D, hit.getBlockPos().getY() + 0.5D,
                hit.getBlockPos().getZ() + 0.5D);
        var event = new PlayerInteractEvent.RightClickBlock(
                player, InteractionHand.MAIN_HAND, hit.getBlockPos(), hit);
        CrucibleInteractionHandler.onRightClickBlock(event);
//? if >=1.21.1 {
        /*return event.getUseItem() == net.neoforged.neoforge.common.util.TriState.FALSE;
*///?} else {
        return event.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY;
//?}
    }

    private static BlockHitResult hit(BlockPos pos, Direction face) {
        var location = new Vec3(
                pos.getX() + 0.5D + face.getStepX() * 0.5D,
                pos.getY() + 0.5D + face.getStepY() * 0.5D,
                pos.getZ() + 0.5D + face.getStepZ() * 0.5D);
        return new BlockHitResult(location, face, pos, false);
    }

    private static boolean formed(net.minecraft.world.level.Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.hasProperty(MultiblockPortBlock.CRUCIBLE_FORMED)
                && state.getValue(MultiblockPortBlock.CRUCIBLE_FORMED);
    }

    private static void placePiston(net.minecraft.world.level.Level level, BlockPos pos, Direction facing) {
        level.setBlockAndUpdate(pos, Blocks.PISTON
                .defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, facing));
    }

    private static boolean extended(net.minecraft.world.level.Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonBaseBlock
                && state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED);
    }

    /** Fills every slot so the dispenser's random pick cannot fall on an empty one, then ticks it. */
    private static void dispenseFrom(net.minecraft.world.level.Level level, BlockPos pos, ItemStack stack) {
        var facing = Blocks.DISPENSER
                .defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.DOWN);
        if (!level.getBlockState(pos).is(Blocks.DISPENSER)) level.setBlockAndUpdate(pos, facing);
        if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity dispenser))
            return;
        for (int slot = 0; slot < dispenser.getContainerSize(); slot++)
            dispenser.setItem(slot, stack.copy());
        dispenser.setChanged();
        level.scheduleTick(pos, Blocks.DISPENSER, 1);
    }

    private static void clearAboveMouth(net.minecraft.world.level.Level level, BlockPos aboveMouth) {
        level.setBlockAndUpdate(aboveMouth, Blocks.AIR.defaultBlockState());
        for (var face : Direction.Plane.HORIZONTAL) {
            var side = aboveMouth.relative(face);
            if (!level.getBlockState(side).getFluidState().isEmpty())
                level.setBlockAndUpdate(side, Blocks.AIR.defaultBlockState());
        }
    }

    /** The falling block drops as an item, or the vessel absorbs that item into its cache. */
    private static boolean sandDropped(net.minecraft.world.level.Level level, BlockPos origin) {
        var area = new net.minecraft.world.phys.AABB(origin).inflate(3.0D);
        for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area))
            if (entity.getItem().is(Blocks.SAND.asItem())) return true;
        return level.getBlockEntity(origin) instanceof LargeCrucibleControllerBlockEntity crucible
                && crucible.getCacheStack().is(Blocks.SAND.asItem());
    }

    private static gregtech.content.blockentity.machine.MultiblockPortBlockEntity port(
            GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(pos)
                        instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity part
                ? part
                : null;
    }

    private static void assertHollow(GameTestHelper helper, BlockPos cavity1, BlockPos cavity2) {
        var level = helper.getLevel();
        helper.assertTrue(level.getBlockState(cavity1).isAir(),
                "A cavity click left " + level.getBlockState(cavity1) + " in " + cavity1);
        helper.assertTrue(level.getBlockState(cavity2).isAir(),
                "A cavity click left " + level.getBlockState(cavity2) + " in " + cavity2);
    }
}