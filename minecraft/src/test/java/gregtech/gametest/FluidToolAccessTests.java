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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
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
                creative.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET)
                        && !creative.getInventory().contains(new ItemStack(net.minecraft.world.item.Items.BUCKET))
                        && tank.getFluidInTank(0).getAmount() == 1000,
                "Creative fill did not preserve the water bucket or fill 1000 mB");

        creative.setItemInHand(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(net.minecraft.world.item.Items.BUCKET));
        helper.assertTrue(
                gregtech.engine.fluid.FluidContainerInteraction.use(
                        creative, net.minecraft.world.InteractionHand.MAIN_HAND, tank),
                "Creative empty bucket did not drain the tank");
        helper.assertTrue(
                creative.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                        && creative.getInventory().contains(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET))
                        && tank.getFluidInTank(0).isEmpty(),
                "Creative drain did not keep the empty bucket, stow a water bucket, and empty the tank");

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
                pipePos, pipeBlock.defaultBlockState().setValue(gregtech.content.block.machine.FluidPipeBlock.UP, true));

        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        var pipe = (gregtech.content.blockentity.machine.FluidPipeBlockEntity) level.getBlockEntity(pipePos);
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
            player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.BUCKET));

            var drumHit = new net.minecraft.world.phys.BlockHitResult(
                    drumPos.getCenter(), Direction.UP, drumPos, false);
            var drumRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, drumHit);
            helper.assertTrue(
                    drumRes.consumesAction()
                            && drum.getFluidInTank(0).getAmount() == 1000
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                            && countItem(player, net.minecraft.world.item.Items.WATER_BUCKET) == 2,
                    "Creative drum drain did not unconditionally deliver a second water bucket");

            var pipeHit = new net.minecraft.world.phys.BlockHitResult(
                    pipePos.getCenter(), Direction.UP, pipePos, false);
            var pipeRes = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, pipeHit);
            helper.assertTrue(
                    pipeRes.consumesAction()
                            && pipe.getFluidInTank(0).getAmount() == 1000
                            && player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)
                            && countItem(player, net.minecraft.world.item.Items.WATER_BUCKET) == 3,
                    "Creative pipe drain did not unconditionally deliver a third water bucket");
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
                pipePos, pipeBlock.defaultBlockState().setValue(gregtech.content.block.machine.FluidPipeBlock.UP, true));

        var drum = (TankBlockEntity) level.getBlockEntity(drumPos);
        var pipe = (gregtech.content.blockentity.machine.FluidPipeBlockEntity) level.getBlockEntity(pipePos);
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
                            && player.getMainHandItem().getCount() == 4
                            && drum.getFluidInTank(0).getAmount() == 500
                            && pipe.getFluidInTank(0).getAmount() == 750
                            && countWaterBottles(player) == 3,
                    "Stacked creative glass bottles did not keep count 4 or deliver 3 water bottles");
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
