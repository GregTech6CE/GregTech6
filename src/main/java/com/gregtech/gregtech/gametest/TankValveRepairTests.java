package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.TankControllerBlock;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockTankControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts;
import com.gregtech.gregtech.content.multiblock.TankValveSpec;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTTanks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class TankValveRepairTests {
    private static final BlockPos CONTROL = new BlockPos(5, 5, 5);

    private static MultiblockTankControllerBlockEntity place(GameTestHelper helper, TankValveSpec spec) {
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = 0; z <= 4; z++)
            helper.setBlock(CONTROL.offset(x, y, z), Blocks.AIR);
        Block controller = LargeMachineParts.block(spec.originalId());
        helper.setBlock(CONTROL, controller.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH));
        return (MultiblockTankControllerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CONTROL));
    }

    private static BlockPos buildShell(GameTestHelper helper, TankValveSpec spec) {
        int radius = spec.size() / 2;
        BlockPos center = CONTROL.south(radius);
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++)
            for (int z = -radius; z <= radius; z++) {
                BlockPos cell = center.offset(x, y, z);
                if (cell.equals(CONTROL)) continue;
                if (Math.abs(x) == radius || Math.abs(y) == radius || Math.abs(z) == radius)
                    helper.setBlock(cell, spec.wall());
            }
        return center;
    }

    private static IFluidHandler wallPort(GameTestHelper helper, TankValveSpec spec) {
        var controller = (MultiblockTankControllerBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(CONTROL));
        helper.assertTrue(controller != null && controller.isStructureOk(),
                "tank wall must be bound to its formed controller before querying a port");
        BlockPos side = CONTROL.south(spec.size() / 2).east(spec.size() / 2);
        var port = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(side));
        return port.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.EAST)
                .resolve().orElseThrow();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void allTwentyFiveOriginalValvesFormWithTheirOwnWalls(GameTestHelper helper) {
        helper.assertTrue(TankValveSpec.all().size() == 25, "GT6 registers 25 tank valve designs");
        for (TankValveSpec spec : TankValveSpec.all()) {
            var block = LargeMachineParts.block(spec.originalId());
            helper.assertTrue(block instanceof TankControllerBlock,
                    "original valve item has a real controller: " + spec.originalId());
            var preview = ControllerStructureLayouts.cells(block);
            int radius = spec.size() / 2;
            helper.assertTrue(preview != null && preview.size() == spec.size() * spec.size() * spec.size() - 1
                            && preview.get(new BlockPos(0, 0, radius)) == Blocks.AIR
                            && preview.get(new BlockPos(0, 0, spec.size() - 1)) == spec.wall(),
                    "JEI blueprint exposes the original hollow vessel and material wall " + spec.originalId());
            var tank = place(helper, spec);
            helper.assertTrue(tank.getType() == GTBlockEntities.MULTIBLOCK_TANK.get(), "valve has tank BE");
            helper.assertTrue(tank.valveSpec() == spec && tank.fluidHandler().getTankCapacity(0) == spec.capacity(),
                    "GT6 valve capacity is specific to ID " + spec.originalId());
            helper.assertTrue(!tank.isStructureOk(), "unassembled valve cannot expose its tank");
            BlockPos center = buildShell(helper, spec);
            helper.assertTrue(tank.isStructureOk(), "correct wall forms GT6 valve " + spec.originalId());
            var port = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(
                    helper.absolutePos(center.offset(spec.size() / 2, 0, 0)));
            IFluidHandler wallTank = port.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.EAST)
                    .resolve().orElseThrow();
            helper.assertTrue(wallTank.getTankCapacity(0) == spec.capacity(),
                    "shell port forwards its valve's exact capacity " + spec.originalId());
            helper.assertTrue(wallTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                    "assembled wall accepts fluid " + spec.originalId());
            helper.setBlock(center.offset(spec.size() / 2, 0, 0), Blocks.IRON_BLOCK);
            helper.assertTrue(!tank.isStructureOk() && wallTank.getTanks() == 0,
                    "wrong wall invalidates the controller and old port capability " + spec.originalId());
        }
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void fluidRulesAndDropRestoreUseOriginalValve(GameTestHelper helper) {
        TankValveSpec wood = TankValveSpec.find(17001);
        var woodTank = place(helper, wood);
        buildShell(helper, wood);
        helper.assertTrue(woodTank.isStructureOk(), "treated wood tank forms");
        var helium = new FluidStack(GTFluids.still("Helium").get(), 1000);
        helper.assertTrue(woodTank.fluidHandler().fill(helium, IFluidHandler.FluidAction.EXECUTE) == 1000,
                "GT6 lets gas enter an unproofed valve before its next hazard tick");
        MultiblockTankControllerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CONTROL),
                woodTank.getBlockState(), woodTank);
        helper.assertTrue(woodTank.fluidHandler().getFluidInTank(0).isEmpty()
                        && woodTank.isStructureOk(),
                "the gas vents on the next tick without destroying the wood tank");
        var water = new FluidStack(Fluids.WATER, 1000);
        helper.assertTrue(woodTank.fluidHandler().fill(water, IFluidHandler.FluidAction.EXECUTE) == 1000,
                "wood valve accepts simple water");

        TankValveSpec adamantium = TankValveSpec.find(17065);
        var metalTank = place(helper, adamantium);
        buildShell(helper, adamantium);
        helper.assertTrue(metalTank.isStructureOk() && metalTank.fluidHandler().fill(helium,
                IFluidHandler.FluidAction.EXECUTE) == 1000, "dense Adamantium accepts nonconducting gas");
        var state = metalTank.getBlockState();
        var drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(CONTROL), metalTank);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(state.getBlock().asItem())
                && drops.get(0).getTagElement("BlockEntityTag") != null,
                "breaking a filled valve yields its original item and tank NBT");
        ItemStack valveItem = drops.get(0);
        helper.setBlock(CONTROL, Blocks.AIR);
        helper.setBlock(CONTROL, state);
        state.getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(CONTROL), state,
                helper.makeMockPlayer(), valveItem);
        var restored = (MultiblockTankControllerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CONTROL));
        helper.assertTrue(restored.fluidHandler().getTankCapacity(0) == adamantium.capacity()
                && restored.fluidHandler().getFluidInTank(0).getAmount() == 1000,
                "valve retains material capacity and fluid after drop and placement");
        restored.fluidHandler().drain(1000, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(restored.fluidHandler().fill(water, IFluidHandler.FluidAction.EXECUTE) == 1000,
                "restored controller can refill");
        BlockPos receiverPos = CONTROL.north();
        helper.setBlock(receiverPos, GTTanks.WOOD_BARREL.get());
        MultiblockTankControllerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CONTROL),
                restored.getBlockState(), restored);
        var receiver = helper.getLevel().getBlockEntity(helper.absolutePos(receiverPos));
        var receiverFluids = receiver.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.SOUTH)
                .resolve().orElseThrow();
        helper.assertTrue(receiverFluids.getFluidInTank(0).getAmount() == 1000
                && restored.fluidHandler().getFluidInTank(0).isEmpty(),
                "GT6 front valve automatically emits to an adjacent fluid receiver");
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void originalValvesAcceptThenReactToHazardousFluids(GameTestHelper helper) {
        TankValveSpec wood = TankValveSpec.find(17001);
        var valve = place(helper, wood);
        BlockPos centre = buildShell(helper, wood);
        helper.assertTrue(valve.isStructureOk(), "wood tank forms");
        helper.assertTrue(wallPort(helper, wood).fill(new FluidStack(Fluids.LAVA, 1_000),
                IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "the wall really accepts lava before the next hazard tick");
        MultiblockTankControllerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CONTROL),
                valve.getBlockState(), valve);
        helper.assertTrue(!helper.getBlockState(CONTROL).is(LargeMachineParts.block(17001))
                        && helper.getBlockState(centre).is(Blocks.LAVA),
                "a 3x3x3 wood vessel melts and leaves lava in its hollow centre");

        TankValveSpec invar = TankValveSpec.find(17007);
        valve = place(helper, invar);
        buildShell(helper, invar);
        var acid = new FluidStack(GTFluids.still("GenLiquid_SulfuricAcid").get(), 1_000);
        helper.assertTrue(wallPort(helper, invar).fill(acid, IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "acid enters the original non-acidproof metal valve");
        MultiblockTankControllerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CONTROL),
                valve.getBlockState(), valve);
        helper.assertTrue(helper.getBlockState(CONTROL).isAir(),
                "acid corrosion removes the main valve on its next tick");

        valve = place(helper, wood);
        buildShell(helper, wood);
        var magic = new FluidStack(GTFluids.still("Holywater").get(), 1_000);
        helper.assertTrue(wallPort(helper, wood).fill(magic, IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "magical fluid enters an unproofed wood valve");
        MultiblockTankControllerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CONTROL),
                valve.getBlockState(), valve);
        helper.assertTrue(helper.getBlockState(CONTROL).isAir(),
                "magical contamination destroys the valve; Flux has no registered port block");

        valve = place(helper, wood);
        buildShell(helper, wood);
        var lithium = new FluidStack(GTFluids.still("GenMolten_Lithium").get(), 1_000);
        helper.assertTrue(wallPort(helper, wood).fill(lithium, IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "molten lithium enters the wood valve below its 500 K melting point");
        MultiblockTankControllerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CONTROL),
                valve.getBlockState(), valve);
        helper.assertTrue(valve.isStructureOk() && valve.fluidHandler().getFluidInTank(0).isEmpty(),
                "wood valve vents a non-SIMPLE fluid without corroding or melting");

        TankValveSpec stainless = TankValveSpec.find(17002);
        valve = place(helper, stainless);
        buildShell(helper, stainless);
        helper.assertTrue(wallPort(helper, stainless).fill(acid, IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "acid enters the stainless valve");
        MultiblockTankControllerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CONTROL),
                valve.getBlockState(), valve);
        helper.assertTrue(valve.isStructureOk() && valve.fluidHandler().getFluidInTank(0).getAmount() == 1_000,
                "an acid-proof material keeps its contents and shell");
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void largeValveMeltdownUsesFiveBlockCube(GameTestHelper helper) {
        TankValveSpec invar = TankValveSpec.find(17047);
        var valve = place(helper, invar);
        buildShell(helper, invar);
        BlockPos outside = CONTROL.south(2).east(3);
        helper.setBlock(outside, Blocks.DIAMOND_BLOCK);
        helper.assertTrue(valve.isStructureOk(), "5x5x5 Invar tank forms with the outside marker present");
        var pyrotheum = new FluidStack(GTFluids.still("Pyrotheum").get(), 1_000);
        helper.assertTrue(wallPort(helper, invar).fill(pyrotheum, IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "5x5x5 valve accepts fluid hotter than Invar until the hazard tick");
        MultiblockTankControllerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(CONTROL),
                valve.getBlockState(), valve);
        helper.assertTrue(!helper.getBlockState(CONTROL).is(LargeMachineParts.block(17047))
                        && helper.getBlockState(outside).is(Blocks.DIAMOND_BLOCK),
                "meltdown replaces its main valve while leaving solid blocks outside the 5x5x5 intact");
        helper.succeed();
    }
}
