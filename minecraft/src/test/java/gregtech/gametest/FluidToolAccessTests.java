package gregtech.gametest;

import gregtech.content.block.tool.FluidAttachmentBlock;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.FuelGeneratorBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.content.recipe.RecipeMapSpec;
import gregtech.content.registry.GTFluids;
import gregtech.content.world.GarbageData;
import gregtech.engine.machine.MachineInventory;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Tap and nozzle access searches output tanks, then input tanks, without opening the facing capability. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class FluidToolAccessTests {

    private FluidToolAccessTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void basicMachineUpdatesRedstoneAndFluidAccessWhenCoversChange(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "mixer_bronze"));
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var machine = (BasicMachineBlockEntity) level.getBlockEntity(pos);
        var water = new FluidStack(Fluids.WATER, 50);
        var simulate = IFluidHandler.FluidAction.SIMULATE;
        for (Direction side : Direction.values()) {
            var open = fluidCapability(level, pos, side);
            if (open == null || open.fill(water, simulate) == 0) continue;

            var detector = new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("gt6ce", "activity_detector_running")));
            gregtech.content.cover.CoverStackData.putBoolean(detector, "gt.cover.inverted", true);
            helper.assertTrue(machine.attachCover(side, detector)
                            && machine.hasRedstoneCover() && machine.getRedstoneSignal(side) == 15,
                    "The inverted activity detector did not signal on the idle mixer");
            helper.assertTrue(machine.configureControlCover(side, true) && machine.getStrongRedstoneSignal(side) == 15,
                    "The detector did not switch to a strong redstone output");
            machine.removeCover(side);
            helper.assertTrue(machine.getRedstoneSignal(side) == 0, "Cover removal left a redstone signal behind");

            var shutter = new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("gt6ce", "shutter_cover")));
            gregtech.content.cover.CoverStackData.putBoolean(shutter, "gt.cover.inverted", true);
            helper.assertTrue(machine.attachCover(side, shutter), "Could not attach the shutter");
            var closed = fluidCapability(level, pos, side);
            helper.assertTrue(closed != null && closed.fill(water, simulate) == 0 && open.fill(water, simulate) == 0,
                    "The shutter did not block both new and cached fluid handlers");
            machine.removeCover(side);
            var reopened = fluidCapability(level, pos, side);
            helper.assertTrue(reopened != null && reopened.fill(water, simulate) == water.getAmount(),
                    "Cover removal did not reopen the input face");
            helper.succeed();
            return;
        }
        helper.assertTrue(false, "The mixer fixture has no fluid input face");
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void toolAccessDrainsOutputTanksBeforeInputs(GameTestHelper helper) {
        var map = new RecipeMap(RecipeMapSpec.builder("gt6ce_test_fluid_tool", "Tool Access", "textures/gui/test")
                .items(1, 0, 0)
                .fluids(1, 1, 0)
                .build());
        try {
            var inventory = new MachineInventory(map, 1, () -> {});
            inventory.tanksOutput()[0].setFluid(new FluidStack(Fluids.LAVA, 500));
            inventory.tanksInput()[0].setFluid(new FluidStack(Fluids.WATER, 500));

            var drain = inventory.toolDrainHandler();
            helper.assertTrue(drain.getTanks() == 2
                            && drain.getFluidInTank(0).getFluid() == Fluids.LAVA
                            && drain.getFluidInTank(1).getFluid() == Fluids.WATER,
                    "The tool view does not list output tanks before input tanks");
            helper.assertTrue(drain.drain(100, IFluidHandler.FluidAction.SIMULATE).getFluid() == Fluids.LAVA,
                    "A simulated tool drain did not prefer the output tank");
            helper.assertTrue(drain.drain(600, IFluidHandler.FluidAction.EXECUTE).getAmount() == 500,
                    "The tool drain did not empty the output tank first");
            helper.assertTrue(inventory.tanksOutput()[0].isEmpty()
                            && drain.drain(100, IFluidHandler.FluidAction.EXECUTE).getFluid() == Fluids.WATER,
                    "The tool drain did not fall back to the input tank");

            helper.assertTrue(inventory.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "The facing capability drained an input tank");

            var fill = inventory.toolFillHandler();
            helper.assertTrue(fill.getTanks() == 1 && fill.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "The fill view exposed output tanks or a drain path");
            helper.assertTrue(fill.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 100,
                    "The fill view could not fill the input tank");
        } finally {
            RecipeMap.unregister(map);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void attachmentsReachInputTanksWithoutOpeningTheFacingCapability(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        Block machineBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "mixer_bronze"));
        level.setBlockAndUpdate(pos, machineBlock.defaultBlockState());
        var machine = (BasicMachineBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(machine != null && machine.getTanksInput().length > 0,
                "The mixer fixture has no input tank");

        machine.getTanksInput()[0].setFluid(new FluidStack(Fluids.WATER, 600));

        var tap = (FluidAttachmentBlock) BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "tap_ceramic"));
        var tapAccess = tap.access(machine.fluidToolHandler(Direction.UP, false));
        helper.assertTrue(tapAccess.drain(200, IFluidHandler.FluidAction.SIMULATE).getAmount() == 200,
                "A tap did not drain a liquid from an input tank");
        helper.assertTrue(tapAccess.drain(200, IFluidHandler.FluidAction.EXECUTE).getAmount() == 200,
                "Executing the tap drain changed nothing");
        helper.assertTrue(tapAccess.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0,
                "A tap filled a tank");

        boolean inputFace = false;
        for (var side : Direction.values()) {
            var facing = fluidCapability(level, pos, side);
            if (facing == null) continue;
            if (facing.fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.SIMULATE) > 0) {
                inputFace = true;
                helper.assertTrue(facing.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                        "The facing capability drained a recipe input tank");
            }
        }
        helper.assertTrue(inputFace, "No facing fluid capability delivered to an input tank");

        var funnel = (FluidAttachmentBlock) BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_funnel"));
        var funnelAccess = funnel.access(machine.fluidToolHandler(Direction.UP, true));
        helper.assertTrue(funnelAccess.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "A funnel drained the machine");

        var steam = GTFluids.stack("Steam", 400);
        helper.assertTrue(steam != null, "Steam is not registered");
        machine.getTanksInput()[0].setEmpty();
        machine.getTanksInput()[0].setFluid(steam);
        helper.assertTrue(tapAccess.drain(300, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "A tap drained a gas");
        var nozzle = (FluidAttachmentBlock) BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "nozzle_steel"));
        var nozzleAccess = nozzle.access(machine.fluidToolHandler(Direction.UP, false));
        helper.assertTrue(nozzleAccess.drain(300, IFluidHandler.FluidAction.EXECUTE).getFluid() == steam.getFluid(),
                "A nozzle did not drain a gas from an input tank");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void reactorTapRecoversOutputThenInputCoolant(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "reactor_core"));
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var reactor = (ReactorCoreBlockEntity) level.getBlockEntity(pos);
        var distilled = GTFluids.stack("DistW", 1000);
        var steam = GTFluids.stack("Steam", 500);
        helper.assertTrue(reactor != null && distilled != null && steam != null, "The reactor fixture is incomplete");
        reactor.input.setFluid(distilled);
        reactor.output.setFluid(steam);

        var tap = reactor.fluidToolHandler(Direction.UP, false);
        helper.assertTrue(tap != null && tap.drain(100, IFluidHandler.FluidAction.SIMULATE).getFluid() == steam.getFluid(),
                "A tap did not prefer the reactor's output tank");
        helper.assertTrue(tap.drain(600, IFluidHandler.FluidAction.EXECUTE).getAmount() == 500
                        && reactor.output.isEmpty(),
                "The tool drain did not empty the reactor output tank first");
        helper.assertTrue(tap.drain(100, IFluidHandler.FluidAction.EXECUTE).getFluid() == distilled.getFluid(),
                "A tap did not recover leftover coolant from the input tank");
        helper.assertTrue(reactor.fluids.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "The facing capability drained reactor coolant");

        var fill = reactor.fluidToolHandler(Direction.UP, true);
        helper.assertTrue(fill != null && fill.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "A funnel drained the reactor");
        helper.assertTrue(fill.isFluidValid(0, distilled)
                        && !fill.isFluidValid(0, new FluidStack(Fluids.WATER, 100)),
                "The reactor fill view did not restrict itself to coolant");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fuelGeneratorTapDrainsTheFuelTank(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "fuel_api_test"));
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var generator = (FuelGeneratorBlockEntity) level.getBlockEntity(pos);
        var diesel = GTFluids.stack("Diesel", 800);
        helper.assertTrue(generator != null && diesel != null, "The fuel generator fixture is incomplete");
        helper.assertTrue(generator.fill(diesel, IFluidHandler.FluidAction.EXECUTE) == 800,
                "The fixture generator rejected its fuel");

        helper.assertTrue(generator.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "The facing capability drained the generator's fuel tank");
        var tap = generator.fluidToolHandler(Direction.UP, false);
        helper.assertTrue(tap != null
                        && tap.drain(300, IFluidHandler.FluidAction.EXECUTE).getFluid() == diesel.getFluid(),
                "A tap did not drain the generator's fuel tank");
        helper.assertTrue(tap.fill(diesel, IFluidHandler.FluidAction.EXECUTE) == 0,
                "A tap filled the generator");

        var fill = generator.fluidToolHandler(Direction.UP, true);
        helper.assertTrue(fill != null && fill.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "A funnel drained the generator");
        helper.assertTrue(fill.isFluidValid(0, diesel)
                        && fill.fill(diesel, IFluidHandler.FluidAction.SIMULATE) > 0,
                "The generator fill view did not reach the fuel tank");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void tankTapBypassesAutoOutputFacesAndClosedCovers(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var drum = (TankBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(drum != null && drum.fill(new FluidStack(Fluids.WATER, 1000),
                        IFluidHandler.FluidAction.EXECUTE) == 1000,
                "The drum fixture rejected water");

        var shutter = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "shutter_cover")));
        gregtech.content.cover.CoverStackData.putBoolean(shutter, "gt.cover.inverted", true);
        helper.assertTrue(drum.attachCover(Direction.UP, shutter) && drum.panels().shuttered(Direction.UP),
                "The drum shutter fixture did not close");
        var covered = fluidCapability(level, pos, Direction.UP);
        helper.assertTrue(covered != null && covered.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "The closed cover did not block the facing capability");
        var tap = drum.fluidToolHandler(Direction.UP, false);
        helper.assertTrue(tap != null && tap.drain(200, IFluidHandler.FluidAction.SIMULATE).getAmount() == 200,
                "A tap did not bypass the closed cover");

        drum.removeCover(Direction.UP);
        drum.toggleAutoOutput();
        helper.assertTrue(fluidCapability(level, pos, Direction.DOWN) == null,
                "The auto-output face still exposed a fluid handler");
        var autoTap = drum.fluidToolHandler(Direction.DOWN, false);
        helper.assertTrue(autoTap != null && autoTap.drain(200, IFluidHandler.FluidAction.EXECUTE).getAmount() == 200,
                "A tap on the auto-output face could not drain");

        drum.toggleSoftHammerState();
        helper.assertTrue(drum.getSoftHammerState(), "The drum fixture did not seal");
        helper.assertTrue(drum.fluidToolHandler(Direction.DOWN, false)
                        .drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "A sealed drum drained through a tap");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void garbageDumpTapRecoversStoredFluids(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "ender_garbage_dump"));
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var dump = (EnderGarbageDumpBlockEntity) level.getBlockEntity(pos);
        var water = new FluidStack(Fluids.WATER, 1000);
        var steam = GTFluids.stack("Steam", 400);
        helper.assertTrue(dump != null && steam != null, "The garbage dump fixture is incomplete");
        GarbageData.get(level).trash(water);
        GarbageData.get(level).trash(steam);

        var facing = fluidCapability(level, pos, Direction.UP);
        helper.assertTrue(facing != null && facing.drain(water, IFluidHandler.FluidAction.SIMULATE).getAmount() > 0,
                "The garbage dump exposed no fluid capability");
        var tap = dump.fluidToolHandler(Direction.UP, false);
        helper.assertTrue(tap != null && tap.drain(water, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000,
                "A tap did not recover garbage water");
        helper.assertTrue(tap.drain(steam, IFluidHandler.FluidAction.EXECUTE).getAmount() == 400,
                "A tap did not recover garbage steam");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fluidContainerInteractionAlignsWithVanillaCreativeAndSurvival(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        Block tankBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        level.setBlockAndUpdate(pos, tankBlock.defaultBlockState());
        var be = level.getBlockEntity(pos);
        helper.assertTrue(be instanceof TankBlockEntity, "The tank fixture did not create a TankBlockEntity");
        var tank = (TankBlockEntity) be;

        var creative = mockCreativePlayer(helper);
        creative.setItemInHand(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        helper.assertTrue(
                gregtech.engine.fluid.FluidContainerInteraction.use(
                        creative, net.minecraft.world.InteractionHand.MAIN_HAND, tank),
                "Creative water bucket did not fill the tank");
        helper.assertTrue(
                creative.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                        && countItem(creative, net.minecraft.world.item.Items.WATER_BUCKET) == 0
                        && tank.getFluidInTank(0).getAmount() == 1000,
                "Creative fill did not swap to an empty bucket or fill 1000 mB");

        creative.setItemInHand(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(net.minecraft.world.item.Items.BUCKET));
        helper.assertTrue(
                gregtech.engine.fluid.FluidContainerInteraction.use(
                        creative, net.minecraft.world.InteractionHand.MAIN_HAND, tank),
                "Creative empty bucket did not drain the tank");
        helper.assertTrue(
                creative.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET)
                        && countItem(creative, net.minecraft.world.item.Items.BUCKET) == 0
                        && tank.getFluidInTank(0).isEmpty(),
                "Creative drain did not swap to a water bucket and empty the tank");

        creative.setItemInHand(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        creative.getAbilities().instabuild = false;
        helper.assertTrue(
                gregtech.engine.fluid.FluidContainerInteraction.use(
                        creative, net.minecraft.world.InteractionHand.MAIN_HAND, tank),
                "Survival water bucket did not fill the tank");
        helper.assertTrue(
                creative.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                        && tank.getFluidInTank(0).getAmount() == 1000,
                "Survival fill did not swap to an empty bucket");

        helper.assertTrue(
                gregtech.engine.fluid.FluidContainerInteraction.use(
                        creative, net.minecraft.world.InteractionHand.MAIN_HAND, tank),
                "Survival empty bucket did not drain the tank");
        helper.assertTrue(
                creative.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET)
                        && tank.getFluidInTank(0).isEmpty(),
                "Survival drain did not swap to a water bucket");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void creativeEmptyBucketDrainsTankAndPipeEvenWithExistingWaterBucket(GameTestHelper helper) {
        var level = helper.getLevel();
        var drumPos = helper.absolutePos(new BlockPos(1, 1, 2));
        var pipePos = helper.absolutePos(new BlockPos(3, 1, 2));
        Block drumBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        Block pipeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "pipe_large_steel"));
        level.setBlockAndUpdate(drumPos, drumBlock.defaultBlockState());
        level.setBlockAndUpdate(
                pipePos, pipeBlock.defaultBlockState().setValue(gregtech.content.transport.pipe.fluid.FluidPipeBlock.UP, true));

        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        var pipe = (gregtech.content.transport.pipe.fluid.FluidPipeBlockEntity) level.getBlockEntity(pipePos);
        helper.assertTrue(drum != null && pipe != null, "Drum or pipe fixture is missing");
        helper.assertTrue(
                drum.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE) == 2000
                        && pipe.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE) == 2000,
                "Fixtures did not accept initial water");

        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(false);
        try {
            player.initInventoryMenu();
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
            player.getInventory().selected = 1;
            player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.BUCKET, 2));

            var drumHit = new net.minecraft.world.phys.BlockHitResult(
                    drumPos.getCenter(), Direction.UP, drumPos, false);
            var drumRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, drumHit);
            helper.assertTrue(
                    drumRes.consumesAction()
                            && drum.getFluidInTank(0).getAmount() == 1000
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                            && player.getMainHandItem().getCount() == 1
                            && countItem(player, net.minecraft.world.item.Items.WATER_BUCKET) == 2,
                    "Creative drum drain did not shrink stacked bucket and deliver a second water bucket");

            var pipeHit = new net.minecraft.world.phys.BlockHitResult(
                    pipePos.getCenter(), Direction.UP, pipePos, false);
            var pipeRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, pipeHit);
            helper.assertTrue(
                    pipeRes.consumesAction()
                            && pipe.getFluidInTank(0).getAmount() == 1000
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET)
                            && countItem(player, net.minecraft.world.item.Items.BUCKET) == 0
                            && countItem(player, net.minecraft.world.item.Items.WATER_BUCKET) == 3,
                    "Creative pipe drain did not swap last bucket to water bucket");
        } finally {
            gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(null);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void creativeStackedGlassBottlesExtractRepeatedlyIntoInventory(GameTestHelper helper) {
        var level = helper.getLevel();
        var drumPos = helper.absolutePos(new BlockPos(1, 1, 2));
        var pipePos = helper.absolutePos(new BlockPos(3, 1, 2));
        Block drumBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        Block pipeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "pipe_large_steel"));
        level.setBlockAndUpdate(drumPos, drumBlock.defaultBlockState());
        level.setBlockAndUpdate(
                pipePos, pipeBlock.defaultBlockState().setValue(gregtech.content.transport.pipe.fluid.FluidPipeBlock.UP, true));

        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        var pipe = (gregtech.content.transport.pipe.fluid.FluidPipeBlockEntity) level.getBlockEntity(pipePos);
        helper.assertTrue(drum != null && pipe != null, "Drum or pipe fixture is missing");
        drum.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        pipe.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(false);
        try {
            player.initInventoryMenu();
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 4));
            var drumHit = new net.minecraft.world.phys.BlockHitResult(
                    drumPos.getCenter(), Direction.UP, drumPos, false);
            var pipeHit = new net.minecraft.world.phys.BlockHitResult(
                    pipePos.getCenter(), Direction.UP, pipePos, false);

            helper.assertTrue(
                    player.gameMode
                                    .useItemOn(
                                            player,
                                            level,
                                            player.getMainHandItem(),
                                            net.minecraft.world.InteractionHand.MAIN_HAND,
                                            drumHit)
                                    .consumesAction()
                            && player.gameMode
                                    .useItemOn(
                                            player,
                                            level,
                                            player.getMainHandItem(),
                                            net.minecraft.world.InteractionHand.MAIN_HAND,
                                            drumHit)
                                    .consumesAction()
                            && player.gameMode
                                    .useItemOn(
                                            player,
                                            level,
                                            player.getMainHandItem(),
                                            net.minecraft.world.InteractionHand.MAIN_HAND,
                                            pipeHit)
                                    .consumesAction(),
                    "Repeated creative glass bottle extractions failed");

            helper.assertTrue(
                    player.getMainHandItem().is(net.minecraft.world.item.Items.GLASS_BOTTLE)
                            && player.getMainHandItem().getCount() == 1
                            && drum.getFluidInTank(0).getAmount() == 500
                            && pipe.getFluidInTank(0).getAmount() == 750
                            && countWaterBottles(player) == 3,
                    "Stacked creative glass bottles did not consume 3 bottles to count 1 and deliver 3 water bottles");
        } finally {
            gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(null);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void clientPredictionConsumesFluidContainersAndPassesOthers(GameTestHelper helper) {
        var player = mockCreativePlayer(helper);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;

        for (var item : java.util.List.of(
                net.minecraft.world.item.Items.BUCKET,
                net.minecraft.world.item.Items.WATER_BUCKET,
                net.minecraft.world.item.Items.GLASS_BOTTLE)) {
            player.setItemInHand(hand, new ItemStack(item));
            helper.assertTrue(
                    gregtech.engine.fluid.FluidContainerInteraction.canInteract(player, hand)
                            && gregtech.engine.fluid.FluidContainerInteraction.clientResult(player, hand)
                                    == net.minecraft.world.InteractionResult.SUCCESS,
                    "Fluid container " + item + " must return SUCCESS on client prediction");
        }

        player.setItemInHand(hand, ItemStack.EMPTY);
        helper.assertTrue(
                !gregtech.engine.fluid.FluidContainerInteraction.canInteract(player, hand)
                        && gregtech.engine.fluid.FluidContainerInteraction.clientResult(player, hand)
                                == net.minecraft.world.InteractionResult.PASS,
                "Empty hand must return PASS on client prediction");

        player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.DIRT));
        helper.assertTrue(
                !gregtech.engine.fluid.FluidContainerInteraction.canInteract(player, hand)
                        && gregtech.engine.fluid.FluidContainerInteraction.clientResult(player, hand)
                                == net.minecraft.world.InteractionResult.PASS,
                "Unrelated item must return PASS on client prediction");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void serverFluidContainerUseImmediatelySyncsInventoryMenuToRemote(GameTestHelper helper) {
        var level = helper.getLevel();
        var drumPos = helper.absolutePos(new BlockPos(2, 1, 2));
        Block drumBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        level.setBlockAndUpdate(drumPos, drumBlock.defaultBlockState());
        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        helper.assertTrue(drum != null, "The tank fixture did not create a TankBlockEntity");
        drum.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

        var sent = new java.util.ArrayList<net.minecraft.network.protocol.Packet<?>>();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "sync-test");
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
//? if >=1.21.1 {
        /*var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
*///?} else {
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player) {
//?}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { sent.add(packet); }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) { sent.add(packet); }
        };
        gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(false);
        try {
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.initInventoryMenu();
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
            player.getInventory().selected = 1;
            player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            player.inventoryMenu.broadcastChanges();
            sent.clear();

            var hit = new net.minecraft.world.phys.BlockHitResult(
                    drumPos.getCenter(), Direction.UP, drumPos, false);
            var res = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(res.consumesAction(), "useItemOn did not consume the container click");

            boolean syncedWaterBucket = false;
            for (var pkt : sent) {
                if (pkt instanceof net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket slotPkt
                        && slotPkt.getItem().is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                    syncedWaterBucket = true;
                    break;
                }
                if (pkt instanceof net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket contentPkt
                        && contentPkt.getItems().stream()
                                .anyMatch(s -> s.is(net.minecraft.world.item.Items.WATER_BUCKET))) {
                    syncedWaterBucket = true;
                    break;
                }
            }
            helper.assertTrue(
                    syncedWaterBucket && countItem(player, net.minecraft.world.item.Items.WATER_BUCKET) == 2,
                    "Server useItemOn did not immediately flush the added water bucket to the remote client");

            sent.clear();
            player.inventoryMenu.broadcastChanges();
            boolean extraSlotSync = sent.stream()
                    .anyMatch(p -> p instanceof net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket);
            helper.assertTrue(!extraSlotSync, "InventoryMenu still had unsynced slot diffs after immediate sync");
        } finally {
            gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(null);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void partialPortableContainerTopsUpOnTapNozzleAndGasTank(GameTestHelper helper) {
        var level = helper.getLevel();
        var drumPos = helper.absolutePos(new BlockPos(2, 1, 2));
        var tapPos = drumPos.north();
        var nozzlePos = drumPos.east();
        var gasPos = helper.absolutePos(new BlockPos(4, 1, 2));

        Block drumBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        Block tapBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "tap_ceramic"));
        Block nozzleBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "nozzle_steel"));
        Block gasBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "compressed_gas_api_test"));
        level.setBlockAndUpdate(drumPos, drumBlock.defaultBlockState());
        level.setBlockAndUpdate(tapPos, tapBlock.defaultBlockState().setValue(FluidAttachmentBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(
                nozzlePos, nozzleBlock.defaultBlockState().setValue(FluidAttachmentBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(gasPos, gasBlock.defaultBlockState());

        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        var gasTank = (gregtech.content.blockentity.machine.CompressedGasTankBlockEntity) level.getBlockEntity(gasPos);
        var gas = GTFluids.stack("Nitrogen", 4000);
        helper.assertTrue(drum != null && gasTank != null && gas != null, "Tap/nozzle/gas tank fixture incomplete");

        var measuringPot = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_measuring_pot"));
        var gasCylinder = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_barometer_gas_cylinder"));

        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        try {
            player.initInventoryMenu();
            // 1. Failed tap interaction with non-drink measuring pot must NOT place the pot block.
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(measuringPot), Fluids.WATER, 400));
            var tapHit = new net.minecraft.world.phys.BlockHitResult(tapPos.getCenter(), Direction.NORTH, tapPos, false);
            var emptyTapRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, tapHit);
            helper.assertTrue(
                    !emptyTapRes.consumesAction() && level.getBlockState(tapPos.north()).isAir(),
                    "Failed non-drink measuring pot tap interaction unexpectedly placed a block");

            // 2. Survival partial measuring pot clicking water tap tops up to 1000 mB.
            helper.assertTrue(
                    drum.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE) == 2000,
                    "Drum did not accept water");
            var survivalTapRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, tapHit);
            helper.assertTrue(
                    survivalTapRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).getAmount() == 1000
                            && drum.getFluidInTank(0).getAmount() == 1400,
                    "Survival partial measuring pot did not top up via tap");

            // Full measuring pot cannot empty into tap and fallback fill fails cleanly.
            var fullTapRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, tapHit);
            helper.assertTrue(
                    !fullTapRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).getAmount() == 1000
                            && drum.getFluidInTank(0).getAmount() == 1400
                            && level.getBlockState(tapPos.north()).isAir(),
                    "Full measuring pot unexpectedly mutated tap or placed block");

            // 3. Creative partial measuring pot clicking tap tops up directly in hand (no creative cloning).
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(measuringPot), Fluids.WATER, 1000));
            player.getInventory().selected = 1;
            player.getInventory().setItem(1, containerWithFluid(new ItemStack(measuringPot), Fluids.WATER, 400));
            var creativeTapRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, tapHit);
            helper.assertTrue(
                    creativeTapRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).getAmount() == 1000
                            && drum.getFluidInTank(0).getAmount() == 800
                            && countFilledContainerAmount(player, measuringPot, 1000) == 2,
                    "Creative partial measuring pot tap fallback did not swap hand container directly to 1000 mB");

            // 4. Survival & Creative partial gas cylinder on nozzle_steel (default rule=true).
            drum.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(drum.fill(gas.copy(), IFluidHandler.FluidAction.EXECUTE) == 4000,
                    "Drum did not accept Nitrogen");
            var nozzleHit = new net.minecraft.world.phys.BlockHitResult(
                    nozzlePos.getCenter(), Direction.EAST, nozzlePos, false);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(gasCylinder), gas.getFluid(), 1000));
            var survivalNozzleRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, nozzleHit);
            helper.assertTrue(
                    survivalNozzleRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).getAmount() == 5000
                            && drum.getFluidInTank(0).isEmpty(),
                    "Survival partial gas cylinder did not top up via nozzle: consumes="
                            + survivalNozzleRes.consumesAction()
                            + " res=" + survivalNozzleRes
                            + " hand=" + containedFluid(player.getMainHandItem()).getAmount()
                            + " drum=" + drum.getFluidInTank(0).getAmount());

            // 5. Direct CompressedGasTankBlock with overrideTanksRequireFluidToolsForTesting(false), Survival & Creative.
            gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(false);
            helper.assertTrue(gasTank.fillCompressed(GTFluids.stack("Nitrogen", 5000)) == 5000,
                    "CompressedGasTank did not accept Nitrogen");
            var gasHit = new net.minecraft.world.phys.BlockHitResult(gasPos.getCenter(), Direction.UP, gasPos, false);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(gasCylinder), gas.getFluid(), 1000));
            var survivalGasRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, gasHit);
            helper.assertTrue(
                    survivalGasRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).getAmount() == 6000
                            && gasTank.getFluidInTank(0).isEmpty(),
                    "Survival partial gas cylinder did not top up directly from CompressedGasTankBlock");

            helper.assertTrue(gasTank.fillCompressed(GTFluids.stack("Nitrogen", 5000)) == 5000,
                    "CompressedGasTank did not refill Nitrogen");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(gasCylinder), gas.getFluid(), 1000));
            var creativeGasRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, gasHit);
            helper.assertTrue(
                    creativeGasRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).getAmount() == 6000
                            && gasTank.getFluidInTank(0).isEmpty()
                            && countFilledContainerAmount(player, gasCylinder, 6000) == 1,
                    "Creative partial gas cylinder on CompressedGasTankBlock did not swap hand item to 6000 mB");
        } finally {
            gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(null);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void partialPortableContainerFunnelFullTankAndNearlyFullTank(GameTestHelper helper) {
        var level = helper.getLevel();
        var drumPos = helper.absolutePos(new BlockPos(2, 1, 2));
        var funnelPos = drumPos.north();
        var pipePos = helper.absolutePos(new BlockPos(4, 1, 2));

        Block drumBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        Block funnelBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_funnel"));
        Block pipeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "pipe_large_steel"));
        level.setBlockAndUpdate(drumPos, drumBlock.defaultBlockState());
        level.setBlockAndUpdate(
                funnelPos, funnelBlock.defaultBlockState().setValue(FluidAttachmentBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(
                pipePos, pipeBlock.defaultBlockState().setValue(gregtech.content.transport.pipe.fluid.FluidPipeBlock.UP, true));

        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        var pipe = (gregtech.content.transport.pipe.fluid.FluidPipeBlockEntity) level.getBlockEntity(pipePos);
        helper.assertTrue(drum != null && pipe != null, "Drum or pipe fixture missing");

        var measuringPot = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_measuring_pot"));
        var portableTankItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));

        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        try {
            player.initInventoryMenu();
            var funnelHit = new net.minecraft.world.phys.BlockHitResult(
                    funnelPos.getCenter(), Direction.NORTH, funnelPos, false);

            // 1. Funnel (input-only): Survival partial pot pours into drum; Creative keeps template & adds water.
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(measuringPot), Fluids.WATER, 400));
            var funnelSurvRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, funnelHit);
            helper.assertTrue(
                    funnelSurvRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).isEmpty()
                            && drum.getFluidInTank(0).getAmount() == 400,
                    "Survival partial pot did not pour into funnel");

            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(measuringPot), Fluids.WATER, 400));
            var funnelCreatRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, funnelHit);
            helper.assertTrue(
                    funnelCreatRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).isEmpty()
                            && drum.getFluidInTank(0).getAmount() == 800,
                    "Creative partial pot did not empty hand container into funnel");

            // 2. Full bidirectional drum (rule=false) & PortableTankHandler: partial container falls back to fill.
            gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(false);
            int fullCap = drum.getTankCapacity(0);
            drum.fill(new FluidStack(Fluids.WATER, fullCap), IFluidHandler.FluidAction.EXECUTE);
            var drumHit = new net.minecraft.world.phys.BlockHitResult(drumPos.getCenter(), Direction.UP, drumPos, false);

            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(measuringPot), Fluids.WATER, 400));
            var fullTankSurvRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, drumHit);
            helper.assertTrue(
                    fullTankSurvRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).getAmount() == 1000
                            && drum.getFluidInTank(0).getAmount() == fullCap - 600,
                    "Survival partial pot did not fall back to top up from full drum");

            // Refill drum and test PortableTankHandler (drum_bronze item) partial fallback in Creative.
            drum.fill(new FluidStack(Fluids.WATER, fullCap), IFluidHandler.FluidAction.EXECUTE);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(portableTankItem), Fluids.WATER, 500));
            var portableTankCreatRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, drumHit);
            helper.assertTrue(
                    portableTankCreatRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).getAmount() == fullCap
                            && drum.getFluidInTank(0).getAmount() == 500
                            && countFilledContainerAmount(player, portableTankItem, fullCap) == 1,
                    "Creative partial PortableTankHandler did not swap hand container to full via full drum");

            // 3. Nearly-full drum (200 mB space left): filled-first pours 200 mB before any fallback.
            drum.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            drum.fill(new FluidStack(Fluids.WATER, fullCap - 200), IFluidHandler.FluidAction.EXECUTE);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(measuringPot), Fluids.WATER, 500));
            var nearlyFullRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, drumHit);
            helper.assertTrue(
                    nearlyFullRes.consumesAction()
                            && drum.getFluidInTank(0).getAmount() == fullCap
                            && containedFluid(player.getMainHandItem()).getAmount() == 300,
                    "Nearly-full bidirectional drum did not pour partial 200 mB first");

            // 4. Direct FluidPipeBlock click with PortableFluidContainerItem yields onItemUseFirst to FluidPipeBlock.
            int pipeCap = pipe.getTankCapacity(0);
            pipe.fill(new FluidStack(Fluids.WATER, pipeCap), IFluidHandler.FluidAction.EXECUTE);
            var pipeHit = new net.minecraft.world.phys.BlockHitResult(pipePos.getCenter(), Direction.UP, pipePos, false);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(measuringPot), Fluids.WATER, 400));
            var pipeCreatRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, pipeHit);
            helper.assertTrue(
                    pipeCreatRes.consumesAction()
                            && containedFluid(player.getMainHandItem()).getAmount() == 1000
                            && pipe.getFluidInTank(0).getAmount() == pipeCap - 600
                            && countFilledContainerAmount(player, measuringPot, 1000) == 1,
                    "Creative partial measuring pot on full FluidPipeBlock failed to swap hand item to 1000 mB");
        } finally {
            gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(null);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void woodenBucketUnifiesCreativeSurvivalAndMoldExclusion(GameTestHelper helper) {
        var level = helper.getLevel();
        var drumPos = helper.absolutePos(new BlockPos(2, 1, 2));
        var tapPos = drumPos.north();
        var moldPos = helper.absolutePos(new BlockPos(4, 2, 2));

        Block drumBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        Block tapBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "tap_ceramic"));
        Block moldBlock = BuiltInRegistries.BLOCK.stream()
                .filter(candidate -> candidate instanceof gregtech.content.block.machine.MoldBlock)
                .findFirst()
                .orElseThrow();
        level.setBlockAndUpdate(drumPos, drumBlock.defaultBlockState());
        level.setBlockAndUpdate(tapPos, tapBlock.defaultBlockState().setValue(FluidAttachmentBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(moldPos, moldBlock.defaultBlockState());

        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        var mold = (gregtech.content.blockentity.machine.MoldBlockEntity) level.getBlockEntity(moldPos);
        helper.assertTrue(drum != null && mold != null, "Drum or mold fixture missing");
        drum.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);

        var band = gregtech.content.tool.WoodenBucketCatalog.BANDS.get(0);
        var emptyWoodItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                "gt6ce",
                gregtech.content.tool.WoodenBucketCatalog.itemId(band, gregtech.content.tool.WoodenBucketCatalog.empty())));
        var waterWoodItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                "gt6ce",
                gregtech.content.tool.WoodenBucketCatalog.itemId(band, gregtech.content.tool.WoodenBucketCatalog.water())));

        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        try {
            player.initInventoryMenu();
            var tapHit = new net.minecraft.world.phys.BlockHitResult(tapPos.getCenter(), Direction.NORTH, tapPos, false);

            // Creative empty wooden bucket on tap swaps directly to water wooden bucket (no cloning).
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().setItem(0, new ItemStack(emptyWoodItem));
            var creativeTapRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, tapHit);
            helper.assertTrue(
                    creativeTapRes.consumesAction()
                            && player.getMainHandItem().is(waterWoodItem)
                            && countItem(player, emptyWoodItem) == 0
                            && drum.getFluidInTank(0).getAmount() == 1000,
                    "Creative wooden bucket tap extract did not swap directly to water wooden bucket");

            // Survival empty wooden bucket swaps to water wooden bucket.
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(emptyWoodItem));
            var survivalTapRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, tapHit);
            helper.assertTrue(
                    survivalTapRes.consumesAction()
                            && player.getMainHandItem().is(waterWoodItem)
                            && drum.getFluidInTank(0).isEmpty(),
                    "Survival wooden bucket did not swap to water bucket via tap");

            // Mold bottom face must ignore wooden bucket fluid transfer.
            var moldBottomHit = new net.minecraft.world.phys.BlockHitResult(
                    moldPos.getCenter(), Direction.DOWN, moldPos, false);
            var moldRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, moldBottomHit);
            helper.assertTrue(
                    !moldRes.consumesAction() && player.getMainHandItem().is(waterWoodItem),
                    "Wooden bucket unexpectedly poured into mold bottom face");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void migratedHandlersAndMachinesKeepCreativeStackedAndOneWayPour(GameTestHelper helper) {
        var level = helper.getLevel();
        var burningPos = helper.absolutePos(new BlockPos(1, 1, 2));
        var boilerPos = helper.absolutePos(new BlockPos(2, 1, 2));
        var potPos = helper.absolutePos(new BlockPos(3, 1, 2));
        var mixerPos = helper.absolutePos(new BlockPos(4, 1, 2));
        var behaviorPos = helper.absolutePos(new BlockPos(5, 1, 2));

        Block burningBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "burning_box_liquid_tungsten"));
        Block boilerBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "large_boiler_bronze"));
        Block potBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_measuring_pot"));
        Block mixerBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "mixer_bronze"));
        Block behaviorBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce_api_test", "ports_api_receiver"));
        level.setBlockAndUpdate(burningPos, burningBlock.defaultBlockState());
        level.setBlockAndUpdate(boilerPos, boilerBlock.defaultBlockState());
        level.setBlockAndUpdate(potPos, potBlock.defaultBlockState());
        level.setBlockAndUpdate(mixerPos, mixerBlock.defaultBlockState());
        level.setBlockAndUpdate(behaviorPos, behaviorBlock.defaultBlockState());

        var burning = (gregtech.content.blockentity.machine.BurningBoxBlockEntity) level.getBlockEntity(burningPos);
        var mixer = (BasicMachineBlockEntity) level.getBlockEntity(mixerPos);
        var behavior = (gregtech.content.blockentity.machine.BehaviorMachineBlockEntity) level.getBlockEntity(behaviorPos);
        helper.assertTrue(burning != null && mixer != null && behavior != null, "Machine fixtures incomplete");

        var burningCap = fluidCapability(level, burningPos, Direction.UP);
        var potCap = fluidCapability(level, potPos, Direction.UP);
        helper.assertTrue(burningCap != null && potCap != null, "Missing burning box or pot capability");
        burningCap.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        potCap.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

        var measuringPot = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_measuring_pot"));

        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        try {
            player.initInventoryMenu();
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);

            // 1. Migrated BurningBoxBlockEntity + PortableContainerBlockEntity creative extraction matches survival.
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.BUCKET, 2));
            var burningHit = new net.minecraft.world.phys.BlockHitResult(
                    burningPos.getCenter(), Direction.UP, burningPos, false);
            var burningRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, burningHit);
            helper.assertTrue(
                    burningRes.consumesAction()
                            && burningCap.getFluidInTank(0).isEmpty()
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                            && player.getMainHandItem().getCount() == 1
                            && countItem(player, net.minecraft.world.item.Items.WATER_BUCKET) == 1,
                    "Migrated BurningBox did not shrink stacked creative bucket and deliver water bucket");

            var potHit = new net.minecraft.world.phys.BlockHitResult(potPos.getCenter(), Direction.UP, potPos, false);
            var potRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, potHit);
            helper.assertTrue(
                    potRes.consumesAction()
                            && potCap.getFluidInTank(0).isEmpty()
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET)
                            && countItem(player, net.minecraft.world.item.Items.BUCKET) == 0
                            && countItem(player, net.minecraft.world.item.Items.WATER_BUCKET) == 2,
                    "Migrated PortableContainer did not swap last creative bucket to second water bucket");

            // Unformed LargeBoiler passes cleanly without eating held bucket.
            var boilerHit = new net.minecraft.world.phys.BlockHitResult(
                    boilerPos.getCenter(), Direction.UP, boilerPos, false);
            var boilerRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, boilerHit);
            helper.assertTrue(
                    !boilerRes.consumesAction()
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET),
                    "Unformed LargeBoiler unexpectedly consumed bucket");

            // 2. BasicMachineBlock & BehaviorMachineBlock creative pour matches survival + one-way GUI.
            var behaviorHit = new net.minecraft.world.phys.BlockHitResult(
                    behaviorPos.getCenter(), Direction.UP, behaviorPos, false);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
            var creativeBehavRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, behaviorHit);
            helper.assertTrue(
                    creativeBehavRes.consumesAction()
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                            && behavior.fluidHandler(Direction.UP).getFluidInTank(0).getAmount() == 1000,
                    "Creative BehaviorMachineBlock pour did not swap water bucket to empty bucket");

            var behavior2Pos = behaviorPos.south();
            level.setBlockAndUpdate(behavior2Pos, behaviorBlock.defaultBlockState());
            var behavior2 = (gregtech.content.blockentity.machine.BehaviorMachineBlockEntity) level.getBlockEntity(behavior2Pos);
            helper.assertTrue(behavior2 != null, "Second behavior fixture missing");
            var behavior2Hit = new net.minecraft.world.phys.BlockHitResult(
                    behavior2Pos.getCenter(), Direction.UP, behavior2Pos, false);

            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, containerWithFluid(new ItemStack(measuringPot, 3), Fluids.WATER, 400));
            var mixerHit = new net.minecraft.world.phys.BlockHitResult(mixerPos.getCenter(), Direction.UP, mixerPos, false);
            var mixerRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, mixerHit);
            helper.assertTrue(
                    mixerRes.consumesAction()
                            && player.getMainHandItem().getCount() == 2
                            && containedFluid(player.getMainHandItem()).getAmount() == 400
                            && mixer.getTanksInput()[0].getAmount() == 400
                            && countEmptyContainers(player, measuringPot) == 1,
                    "BasicMachineBlock stacked survival pour did not consume only one item and keep remaining stack intact");

            var behaviorRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, behavior2Hit);
            helper.assertTrue(
                    behaviorRes.consumesAction()
                            && player.getMainHandItem().getCount() == 1
                            && containedFluid(player.getMainHandItem()).getAmount() == 400
                            && behavior2.fluidHandler(Direction.UP).getFluidInTank(0).getAmount() == 400
                            && countEmptyContainers(player, measuringPot) == 2,
                    "BehaviorMachineBlock stacked survival pour did not consume only one item");

            // Empty bucket must NOT reverse-drain BasicMachineBlock or BehaviorMachineBlock and must open GUI.
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            player.closeContainer();
            var mixerGuiRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, mixerHit);
            helper.assertTrue(
                    mixerGuiRes.consumesAction()
                            && mixer.getTanksInput()[0].getAmount() == 400
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                            && player.containerMenu != player.inventoryMenu,
                    "Empty bucket on BasicMachineBlock drained input or failed to open GUI");
            player.closeContainer();
            var behaviorGuiRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, behavior2Hit);
            helper.assertTrue(
                    behaviorGuiRes.consumesAction()
                            && behavior2.fluidHandler(Direction.UP).getFluidInTank(0).getAmount() == 400
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                            && player.containerMenu != player.inventoryMenu,
                    "Empty bucket on BehaviorMachineBlock drained input or failed to open GUI");
        } finally {
            player.closeContainer();
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void genericFluidDisplayResolvesWaterLavaAndCreativeCatalog(GameTestHelper helper) {
        var level = helper.getLevel();
        var lookup = level.registryAccess();
        var generic = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_display"));
        helper.assertTrue(
                generic instanceof gregtech.content.item.FluidDisplayItemLike
                        && generic == gregtech.content.registry.GTFluidItems.generic(),
                "gt6ce:fluid_display is not registered as the generic FluidDisplayItemLike carrier");

        var bare = new ItemStack(generic);
        helper.assertTrue(
                !gregtech.content.item.CatalogVisibility.visible(bare),
                "Bare gt6ce:fluid_display shell must remain hidden from CatalogVisibility");

        var waterDisplay = gregtech.engine.fluid.FluidDisplayBinding.display(
                new FluidStack(Fluids.WATER, 1000), lookup);
        var lavaDisplay = gregtech.engine.fluid.FluidDisplayBinding.display(
                new FluidStack(Fluids.LAVA, 1000), lookup);
        var flowingWaterDisplay = gregtech.engine.fluid.FluidDisplayBinding.display(
                new FluidStack(Fluids.FLOWING_WATER, 1000), lookup);
        helper.assertTrue(
                waterDisplay.is(generic)
                        && lavaDisplay.is(generic)
                        && flowingWaterDisplay.is(generic),
                "Vanilla water/lava/fallback fluids did not resolve onto gt6ce:fluid_display");

        var resolvedWater = gregtech.engine.fluid.FluidDisplayBinding.resolve(waterDisplay, lookup);
        var resolvedLava = gregtech.engine.fluid.FluidDisplayBinding.resolve(lavaDisplay, lookup);
        var nitrogen = GTFluids.stack("Nitrogen", 1000);
        helper.assertTrue(nitrogen != null, "Nitrogen test fluid missing");
        var nitrogenDisplay = gregtech.engine.fluid.FluidDisplayBinding.display(nitrogen, lookup);
        helper.assertTrue(
                resolvedWater.getFluid() == Fluids.WATER
                        && resolvedLava.getFluid() == Fluids.LAVA
                        && gregtech.content.item.CatalogVisibility.visible(waterDisplay)
                        && BuiltInRegistries.ITEM.getKey(nitrogenDisplay.getItem())
                                .getPath()
                                .equals("fluid_item_nitrogen"),
                "Payload resolution or dedicated GT fluid_item_* lookup failed");

        var fluidsPage = gregtech.bootstrap.registration.OriginCreativeContents.contents("fluids");
        var techPage = gregtech.bootstrap.registration.OriginCreativeContents.contents("technology");
        boolean hasBareInCatalog = fluidsPage.stream()
                        .anyMatch(s -> s.is(generic) && !gregtech.engine.fluid.FluidDisplayBinding.hasPayload(s))
                || techPage.stream().anyMatch(s -> s.is(generic));
        boolean hasWaterInFluids = fluidsPage.stream()
                .anyMatch(s -> s.is(generic)
                        && gregtech.engine.fluid.FluidDisplayBinding.resolve(s, lookup).getFluid() == Fluids.WATER);
        helper.assertTrue(
                !hasBareInCatalog && hasWaterInFluids,
                "Creative fluids tab must contain water-loaded gt6ce:fluid_display and never bare shell");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void creativeFluidDisplayFillsAttachmentsAndCrucibleWithoutConsuming(GameTestHelper helper) {
        var level = helper.getLevel();
        var lookup = level.registryAccess();
        var drumPos = helper.absolutePos(new BlockPos(1, 1, 2));
        var tapPos = drumPos.north();
        var funnelPos = drumPos.east();
        var gasDrumPos = helper.absolutePos(new BlockPos(3, 1, 2));
        var nozzlePos = gasDrumPos.north();
        var cruciblePos = helper.absolutePos(new BlockPos(5, 1, 2));

        Block drumBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        Block tapBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "tap_ceramic"));
        Block funnelBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_funnel"));
        Block capNozzleBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "cap_nozzle"));
        Block crucibleBlock = BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof gregtech.content.block.machine.SmeltingCrucibleBlock)
                .findFirst()
                .orElseThrow();

        level.setBlockAndUpdate(drumPos, drumBlock.defaultBlockState());
        level.setBlockAndUpdate(tapPos, tapBlock.defaultBlockState().setValue(FluidAttachmentBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(funnelPos, funnelBlock.defaultBlockState().setValue(FluidAttachmentBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(gasDrumPos, drumBlock.defaultBlockState());
        level.setBlockAndUpdate(nozzlePos, capNozzleBlock.defaultBlockState().setValue(FluidAttachmentBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(cruciblePos, crucibleBlock.defaultBlockState());

        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        var gasDrum = (TankBlockEntity) level.getBlockEntity(gasDrumPos);
        var crucible = (gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity) level.getBlockEntity(cruciblePos);
        helper.assertTrue(drum != null && gasDrum != null && crucible != null, "Attachment/crucible fixture missing");

        var waterDisplay = gregtech.engine.fluid.FluidDisplayBinding.display(
                new FluidStack(Fluids.WATER, 1000), lookup);
        var nitrogen = GTFluids.stack("Nitrogen", 1000);
        helper.assertTrue(nitrogen != null, "Nitrogen fluid missing");
        var nitrogenDisplay = gregtech.engine.fluid.FluidDisplayBinding.display(nitrogen, lookup);
        var lavaDisplay = gregtech.engine.fluid.FluidDisplayBinding.display(
                new FluidStack(Fluids.LAVA, 1000), lookup);

        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        try {
            player.initInventoryMenu();
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);

            // Tap must never accept creative display fills; fluid_funnel fills full tank and preserves carrier.
            player.getInventory().setItem(0, waterDisplay.copy());
            var tapHit = new net.minecraft.world.phys.BlockHitResult(tapPos.getCenter(), Direction.NORTH, tapPos, false);
            player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, tapHit);
            helper.assertTrue(drum.getFluidInTank(0).isEmpty(), "Creative display carrier unexpectedly filled tap");

            var funnelHit = new net.minecraft.world.phys.BlockHitResult(
                    funnelPos.getCenter(), Direction.EAST, funnelPos, false);
            var funnelRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, funnelHit);
            helper.assertTrue(
                    funnelRes.consumesAction()
                            && drum.getFluidInTank(0).getAmount() == drum.getTankCapacity(0)
                            && player.getMainHandItem().is(waterDisplay.getItem())
                            && gregtech.engine.fluid.FluidDisplayBinding
                                            .resolve(player.getMainHandItem(), lookup)
                                            .getFluid()
                                    == Fluids.WATER,
                    "Creative display carrier failed to fill tank through fluid_funnel");

            // cap_nozzle rejects liquid water and fills full tank with gas Nitrogen.
            var capNozzleHit = new net.minecraft.world.phys.BlockHitResult(
                    nozzlePos.getCenter(), Direction.NORTH, nozzlePos, false);
            player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, capNozzleHit);
            helper.assertTrue(gasDrum.getFluidInTank(0).isEmpty(), "cap_nozzle accepted liquid water");

            player.getInventory().setItem(0, nitrogenDisplay.copy());
            var capGasRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, capNozzleHit);
            helper.assertTrue(
                    capGasRes.consumesAction()
                            && gasDrum.getFluidInTank(0).getAmount() == gasDrum.getTankCapacity(0)
                            && gasDrum.getFluidInTank(0).getFluid() == nitrogen.getFluid()
                            && player.getMainHandItem().is(nitrogenDisplay.getItem())
                            && gregtech.engine.fluid.FluidDisplayBinding
                                            .resolve(player.getMainHandItem(), lookup)
                                            .getFluid()
                                    == nitrogen.getFluid(),
                    "Creative display carrier failed to fill gas through cap_nozzle");

            // Creative regular water bucket on crucible swaps cleanly to empty bucket (survival parity),
            // whereas creative lava display carrier fills crucible without consuming itself.
            var crucibleHit = new net.minecraft.world.phys.BlockHitResult(
                    cruciblePos.getCenter(), Direction.UP, cruciblePos, false);
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
            var bucketCrucibleRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, crucibleHit);
            helper.assertTrue(
                    bucketCrucibleRes.consumesAction()
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET),
                    "Creative water bucket on crucible did not swap to empty bucket");

            player.getInventory().setItem(0, lavaDisplay.copy());
            var lavaCrucibleRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, crucibleHit);
            helper.assertTrue(
                    lavaCrucibleRes.consumesAction()
                            && player.getMainHandItem().is(lavaDisplay.getItem())
                            && gregtech.engine.fluid.FluidDisplayBinding
                                            .resolve(player.getMainHandItem(), lookup)
                                            .getFluid()
                                    == Fluids.LAVA
                            && crucible.getContentView().stream()
                                    .anyMatch(s -> (s.material == gregtech.content.material.Materials.Lava
                                                    || s.material == gregtech.content.material.Materials.Obsidian)
                                            && s.amount > 0),
                    "Creative lava display carrier failed to pour into crucible cleanly");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void survivalFluidDisplayFillsBlocksDisablesDrinkingAndFilterRouting(GameTestHelper helper) {
        var level = helper.getLevel();
        var lookup = level.registryAccess();
        var drumPos = helper.absolutePos(new BlockPos(1, 1, 2));
        var funnelPos = drumPos.north();
        var tapDrumPos = helper.absolutePos(new BlockPos(1, 1, 4));
        var tapPos = tapDrumPos.north();
        var barePipePos = helper.absolutePos(new BlockPos(2, 1, 2));
        var fluidFilteredPipePos = helper.absolutePos(new BlockPos(3, 1, 2));
        var creativeFilteredPipePos = helper.absolutePos(new BlockPos(3, 1, 4));
        var itemPipePos = helper.absolutePos(new BlockPos(4, 1, 4));
        var mixerPos = helper.absolutePos(new BlockPos(4, 1, 2));
        var cruciblePos = helper.absolutePos(new BlockPos(5, 1, 2));

        Block drumBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "drum_bronze"));
        Block funnelBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_funnel"));
        Block tapBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "tap_ceramic"));
        Block fluidPipeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "pipe_large_steel"));
        Block itemPipeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "item_pipe_medium_brass"));
        Block mixerBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "mixer_bronze"));
        Block crucibleBlock = BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof gregtech.content.block.machine.SmeltingCrucibleBlock)
                .findFirst()
                .orElseThrow();

        level.setBlockAndUpdate(drumPos, drumBlock.defaultBlockState());
        level.setBlockAndUpdate(funnelPos, funnelBlock.defaultBlockState().setValue(FluidAttachmentBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(tapDrumPos, drumBlock.defaultBlockState());
        level.setBlockAndUpdate(tapPos, tapBlock.defaultBlockState().setValue(FluidAttachmentBlock.FACING, Direction.SOUTH));
        var openPipeState = fluidPipeBlock.defaultBlockState().setValue(gregtech.content.transport.pipe.fluid.FluidPipeBlock.UP, true);
        level.setBlockAndUpdate(barePipePos, openPipeState);
        level.setBlockAndUpdate(fluidFilteredPipePos, openPipeState);
        level.setBlockAndUpdate(creativeFilteredPipePos, openPipeState);
        level.setBlockAndUpdate(itemPipePos, itemPipeBlock.defaultBlockState());
        level.setBlockAndUpdate(mixerPos, mixerBlock.defaultBlockState());
        level.setBlockAndUpdate(cruciblePos, crucibleBlock.defaultBlockState());

        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        var tapDrum = (TankBlockEntity) level.getBlockEntity(tapDrumPos);
        var barePipe = (gregtech.content.transport.pipe.fluid.FluidPipeBlockEntity) level.getBlockEntity(barePipePos);
        var fluidFilteredPipe = (gregtech.content.transport.pipe.fluid.FluidPipeBlockEntity) level.getBlockEntity(fluidFilteredPipePos);
        var creativeFilteredPipe = (gregtech.content.transport.pipe.fluid.FluidPipeBlockEntity) level.getBlockEntity(creativeFilteredPipePos);
        var itemPipe = (gregtech.content.transport.pipe.item.ItemPipeBlockEntity) level.getBlockEntity(itemPipePos);
        var mixer = (BasicMachineBlockEntity) level.getBlockEntity(mixerPos);
        var crucible = (gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity) level.getBlockEntity(cruciblePos);
        helper.assertTrue(
                drum != null && tapDrum != null && barePipe != null && fluidFilteredPipe != null
                        && creativeFilteredPipe != null && itemPipe != null && mixer != null && crucible != null,
                "Fluid display fixture incomplete");

        var fluidFilterItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_filter"));
        var itemFilterItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "item_filter"));
        var retrieverItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "item_retriever_cover"));
        helper.assertTrue(
                fluidFilteredPipe.attachCover(Direction.UP, new ItemStack(fluidFilterItem))
                        && creativeFilteredPipe.attachCover(Direction.UP, new ItemStack(fluidFilterItem))
                        && itemPipe.attachCover(Direction.UP, new ItemStack(itemFilterItem))
                        && itemPipe.attachCover(Direction.DOWN, new ItemStack(retrieverItem)),
                "Failed to attach test filter covers");

        var waterDisplay = gregtech.engine.fluid.FluidDisplayBinding.display(
                new FluidStack(Fluids.WATER, 1000), lookup);
        var lavaDisplay = gregtech.engine.fluid.FluidDisplayBinding.display(
                new FluidStack(Fluids.LAVA, 1000), lookup);
        var drinkDisplayItem = gregtech.content.registry.GTFluidItems.allEntries().stream()
                .filter(gregtech.content.registry.RegisteredItem::isPresent)
                .map(gregtech.content.registry.RegisteredItem::get)
                .filter(item -> item instanceof gregtech.content.item.FluidDisplayItemLike display
                        && gregtech.content.food.GTDrinks.rowFor(
                                        gregtech.content.food.GTDrinks.fluidOf(display.fluidEntry()))
                                != null)
                .findFirst()
                .orElseThrow();

        var player = MultiblockBehaviorApiTests.menuPlayer(helper);
        gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(true);
        try {
            player.initInventoryMenu();
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

            // 1. Survival right-click with held display carrier fills funnel, pipe, mixer, crucible without consuming it,
            //    even when TANKS_REQUIRE_FLUID_TOOLS is enabled; tap stays drain-only.
            player.getInventory().setItem(0, waterDisplay.copy());
            for (BlockPos target : java.util.List.of(tapPos, funnelPos, barePipePos, mixerPos)) {
                var hit = new net.minecraft.world.phys.BlockHitResult(target.getCenter(), Direction.UP, target, false);
                player.gameMode.useItemOn(
                        player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            }
            helper.assertTrue(
                    tapDrum.getFluidInTank(0).isEmpty()
                            && drum.getFluidInTank(0).getFluid() == Fluids.WATER
                            && drum.getFluidInTank(0).getAmount() == drum.getTankCapacity(0)
                            && barePipe.getFluidInTank(0).getFluid() == Fluids.WATER
                            && mixer.getTanksInput()[0].getFluid().getFluid() == Fluids.WATER
                            && player.getMainHandItem().is(waterDisplay.getItem())
                            && gregtech.engine.fluid.FluidDisplayBinding
                                            .resolve(player.getMainHandItem(), lookup)
                                            .getFluid()
                                    == Fluids.WATER,
                    "Survival display carrier failed to fill targets without consuming itself");

            // Drum direct click (not just funnel) also works in Survival even with TANKS_REQUIRE_FLUID_TOOLS=true.
            drum.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            var drumHit = new net.minecraft.world.phys.BlockHitResult(drumPos.getCenter(), Direction.UP, drumPos, false);
            player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, drumHit);
            helper.assertTrue(
                    drum.getFluidInTank(0).getFluid() == Fluids.WATER
                            && drum.getFluidInTank(0).getAmount() == drum.getTankCapacity(0),
                    "Survival display carrier failed to fill drum directly with TANKS_REQUIRE_FLUID_TOOLS=true");

            player.getInventory().setItem(0, lavaDisplay.copy());
            var crucibleHit = new net.minecraft.world.phys.BlockHitResult(
                    cruciblePos.getCenter(), Direction.UP, cruciblePos, false);
            var crucibleRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, crucibleHit);
            helper.assertTrue(
                    crucibleRes.consumesAction()
                            && crucible.getContentView().stream()
                                    .anyMatch(s -> (s.material == gregtech.content.material.Materials.Lava
                                                    || s.material == gregtech.content.material.Materials.Obsidian)
                                            && s.amount > 0)
                            && player.getMainHandItem().is(lavaDisplay.getItem()),
                    "Survival lava display carrier failed to fill crucible cleanly");

            // 2. Survival air-use with drink display item must pass without drinking or producing glass bottles.
            player.getInventory().setItem(0, new ItemStack(drinkDisplayItem));
            var drinkRes = player.getMainHandItem().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(
                    drinkRes.getResult() == net.minecraft.world.InteractionResult.PASS
                            && player.getMainHandItem().is(drinkDisplayItem)
                            && countItem(player, net.minecraft.world.item.Items.GLASS_BOTTLE) == 0,
                    "FluidDisplayItemLike unexpectedly allowed drinking on right-click");

            // 3. Survival AND Creative clicks on fluid_filter cover sample 1000 mB without filling the pipe.
            player.getInventory().setItem(0, waterDisplay.copy());
            var sampled = gregtech.content.cover.UtilityCoverInteraction.sample(waterDisplay);
            helper.assertTrue(
                    sampled.getFluid() == Fluids.WATER && sampled.getAmount() == 1000,
                    "UtilityCoverInteraction.sample did not normalize display carrier to 1000 mB");
            var coverHit = new net.minecraft.world.phys.BlockHitResult(
                    fluidFilteredPipePos.getCenter(), Direction.UP, fluidFilteredPipePos, false);
            var coverRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, coverHit);
            var storedFilter = gregtech.content.cover.CoverUtilityBehaviors.fluidFilter(
                    fluidFilteredPipe.getCover(Direction.UP), lookup);
            helper.assertTrue(
                    coverRes.consumesAction()
                            && storedFilter.getFluid() == Fluids.WATER
                            && storedFilter.getAmount() == 1000
                            && fluidFilteredPipe.getFluidInTank(0).isEmpty(),
                    "Survival display click on fluid_filter cover failed to sample or leaked into pipe tank");

            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var creativeCoverHit = new net.minecraft.world.phys.BlockHitResult(
                    creativeFilteredPipePos.getCenter(), Direction.UP, creativeFilteredPipePos, false);
            var creativeCoverRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, creativeCoverHit);
            var creativeStoredFilter = gregtech.content.cover.CoverUtilityBehaviors.fluidFilter(
                    creativeFilteredPipe.getCover(Direction.UP), lookup);
            helper.assertTrue(
                    creativeCoverRes.consumesAction()
                            && creativeStoredFilter.getFluid() == Fluids.WATER
                            && creativeStoredFilter.getAmount() == 1000
                            && creativeFilteredPipe.getFluidInTank(0).isEmpty(),
                    "Creative display click on fluid_filter cover failed to sample or leaked into pipe tank");

            // 4. Item filter & retriever covers must reject FluidDisplayItemLike in both Survival and Creative.
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            for (Direction face : java.util.List.of(Direction.UP, Direction.DOWN)) {
                var itemFilterHit = new net.minecraft.world.phys.BlockHitResult(
                        itemPipePos.getCenter(), face, itemPipePos, false);
                var itemFilterRes = player.gameMode.useItemOn(
                        player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, itemFilterHit);
                helper.assertTrue(
                        !itemFilterRes.consumesAction()
                                && gregtech.content.cover.CoverUtilityBehaviors.itemFilter(
                                                itemPipe.getCover(face), lookup)
                                        .isEmpty(),
                        "Item filter/retriever accepted FluidDisplayItemLike on " + face);
            }
            helper.assertTrue(
                    !gregtech.content.cover.CoverUtilityBehaviors.setItemFilter(
                                    new ItemStack(itemFilterItem), waterDisplay, lookup)
                            && !gregtech.content.cover.CoverUtilityBehaviors.setItemFilter(
                                    new ItemStack(itemFilterItem), new ItemStack(drinkDisplayItem), lookup),
                    "CoverUtilityBehaviors.setItemFilter unexpectedly accepted FluidDisplayItemLike");
        } finally {
            player.closeContainer();
            gregtech.GregTechConfig.overrideTanksRequireFluidToolsForTesting(null);
            player.discard();
        }
        helper.succeed();
    }

    private static ItemStack containerWithFluid(
            ItemStack stack, net.minecraft.world.level.material.Fluid fluid, int amount) {
        return containerWithFluid(stack, new FluidStack(fluid, amount));
    }

    private static ItemStack containerWithFluid(ItemStack stack, FluidStack fluid) {
        var handler = gregtech.engine.stack.StackCompat.fluidHandlerOrThrow(stack.copyWithCount(1));
        int filled = handler.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (filled != fluid.getAmount()) {
            throw new IllegalStateException("Container " + stack + " only accepted " + filled + " of " + fluid.getAmount());
        }
        return handler.getContainer().copyWithCount(stack.getCount());
    }

    private static FluidStack containedFluid(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
    }

    private static int countFilledContainerAmount(
            net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item, int amount) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(item) && containedFluid(stack).getAmount() == amount) count += stack.getCount();
        }
        return count;
    }

    private static int countEmptyContainers(
            net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(item) && containedFluid(stack).isEmpty()) count += stack.getCount();
        }
        return count;
    }

    private static int countItem(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static int countWaterBottles(net.minecraft.world.entity.player.Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (!gregtech.engine.fluid.PotionBottleHandler.contents(stack).isEmpty()) count += stack.getCount();
        }
        return count;
    }

    private static net.minecraft.world.entity.player.Player mockCreativePlayer(GameTestHelper helper) {
//? if >=1.21.1 {
/*        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
*///?} else {
        var player = helper.makeMockPlayer();
//?}
        player.getAbilities().instabuild = true;
        return player;
    }

    private static IFluidHandler fluidCapability(net.minecraft.server.level.ServerLevel level, BlockPos pos, Direction side) {
//? if >=1.21.1 {
/*        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
*///?} else {
        return level.getBlockEntity(pos)
                .getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
//?}
    }
}
