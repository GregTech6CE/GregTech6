package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.blockentity.machine.AxialGeneratorBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeDynamoControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeTurbineControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions;
import com.gregtech.gregtech.content.multiblock.AxialStructureTransform;
import com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.TurbineStructure;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Vertical builds use the same parts as horizontal builds, with ports at world-lowest Y. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class SixWayAxialControllerTests {
    private static BlockPos origin(Direction front) {
        return new BlockPos(6, front == Direction.UP ? 4 : 3, 6);
    }

    private static BasicMachineBlockEntity receiver(GameTestHelper helper, BlockPos pos,
                                                    GregTechTags.Tag energy, long packet) {
        var block = MachineRegistry.basicMachines().iterator().next().get();
        helper.setBlock(pos, block);
        var be = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        be.setSpec(BasicMachineSpec.builder("six_way_axial_receiver", block.basicSpec().material())
                .machineType("test").energy(energy, packet).recipes(block.basicSpec().recipeMap())
                .faces(FaceConfig.ALL_SIDES).build());
        return be;
    }

    private static MultiblockPortBlockEntity port(GameTestHelper helper, BlockPos pos) {
        return (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void steamTurbinesFormAndRouteFluidsOnAllSixAxes(GameTestHelper helper) {
        Block controller = LargeMachineParts.block(17211);
        var grade = AxialGeneratorDefinitions.STEAM.get(0);
        for (Direction front : Direction.values()) {
            BlockPos origin = origin(front);
            helper.setBlock(origin, controller.defaultBlockState().setValue(DirectionalBlock.FACING, front));
            grade.cells().forEach((cell, part) -> helper.setBlock(
                    AxialStructureTransform.at(origin, front, cell.getX(), cell.getY(), cell.getZ()), part));
            var machine = (LargeTurbineControllerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(origin));
            helper.assertTrue(machine != null && machine.isStructureOk(), "steam turbine forms facing " + front);
            BlockPos rear = AxialStructureTransform.at(origin, front, 0, 0, 3);
            helper.assertTrue(port(helper, rear).isEnergyEmittingTo(GregTechTags.Energy.RU, front, false),
                    "rear-centre is the RU port facing " + front);
            BlockPos frontFluid = AxialStructureTransform.at(origin, front, 1, 0, 0);
            var input = port(helper, frontFluid).getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
            int amount = grade.input() * 2;
            helper.assertTrue(input.fill(new FluidStack(GTFluids.still("Steam").get(), amount), FluidAction.EXECUTE) == amount,
                    "front accepts steam facing " + front);
            var sink = receiver(helper, origin.relative(front.getOpposite(), 4), GregTechTags.Energy.RU, grade.output());
            machine.tick();
            helper.assertTrue(sink.getEnergyTick() == grade.output(), "RU leaves axis end facing " + front);
            BlockPos lowestFluid = front == Direction.UP
                    ? AxialStructureTransform.at(origin, front, 1, 0, 3)
                    : front == Direction.DOWN ? frontFluid
                    : AxialStructureTransform.at(origin, front, 1, -1, 1);
            var water = port(helper, lowestFluid).getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
            helper.assertTrue(water.drain(1000, FluidAction.SIMULATE).getAmount() == amount / 170,
                    "distilled water drains from world-lowest layer facing " + front);
            if (!lowestFluid.equals(frontFluid))
                helper.assertTrue(water.fill(new FluidStack(GTFluids.still("Steam").get(), 1), FluidAction.SIMULATE) == 0,
                        "output-only layer rejects intake facing " + front);
            helper.setBlock(origin.relative(front.getOpposite(), 4), Blocks.AIR);
            grade.cells().forEach((cell, part) -> helper.setBlock(
                    AxialStructureTransform.at(origin, front, cell.getX(), cell.getY(), cell.getZ()), Blocks.AIR));
            helper.setBlock(origin, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void dynamosAcceptFrontRuAndEmitRearEuOnAllSixAxes(GameTestHelper helper) {
        Block controller = LargeMachineParts.block(17221);
        var grade = AxialGeneratorDefinitions.DYNAMO.get(0);
        for (Direction front : Direction.values()) {
            BlockPos origin = origin(front);
            helper.setBlock(origin, controller.defaultBlockState().setValue(DirectionalBlock.FACING, front));
            grade.cells().forEach((cell, part) -> helper.setBlock(
                    AxialStructureTransform.at(origin, front, cell.getX(), cell.getY(), cell.getZ()), part));
            var machine = (LargeDynamoControllerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(origin));
            helper.assertTrue(machine != null && machine.isStructureOk(), "dynamo forms facing " + front);
            BlockPos rear = AxialStructureTransform.at(origin, front, 0, 0, 3);
            helper.assertTrue(port(helper, rear).isEnergyEmittingTo(GregTechTags.Energy.EU, front, false),
                    "rear-centre is the EU port facing " + front);
            helper.assertTrue(machine.doEnergyInjection(GregTechTags.Energy.RU, front, grade.input(), 1, true) == 1,
                    "controller front accepts RU facing " + front);
            var sink = receiver(helper, origin.relative(front.getOpposite(), 4), GregTechTags.Energy.EU, grade.output());
            machine.tick();
            helper.assertTrue(sink.getEnergyTick() == grade.output(), "EU leaves axis end facing " + front);
            helper.setBlock(origin.relative(front.getOpposite(), 4), Blocks.AIR);
            grade.cells().forEach((cell, part) -> helper.setBlock(
                    AxialStructureTransform.at(origin, front, cell.getX(), cell.getY(), cell.getZ()), Blocks.AIR));
            helper.setBlock(origin, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_large", timeoutTicks = 200)
    public static void gasTurbinesFormAndExposeRearEnergyPortOnAllSixAxes(GameTestHelper helper) {
        Block controller = LargeMachineParts.block(17231);
        var grade = GasTurbineDefinitions.GRADES.get(0);
        for (Direction front : Direction.values()) {
            BlockPos origin = origin(front);
            helper.setBlock(origin, controller.defaultBlockState().setValue(DirectionalBlock.FACING, front));
            for (var cell : TurbineStructure.LAYOUT.cells()) helper.setBlock(
                    AxialStructureTransform.at(origin, front, cell.right(), cell.up(), cell.back()), grade.wall());
            var machine = (LargeGasTurbineControllerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(origin));
            helper.assertTrue(machine != null && machine.isStructureOk(), "gas turbine forms facing " + front);
            BlockPos rear = AxialStructureTransform.at(origin, front, 0, 0, 3);
            helper.assertTrue(port(helper, rear).isEnergyEmittingTo(GregTechTags.Energy.RU, front, false),
                    "rear-centre is the RU port facing " + front);
            BlockPos frontFluid = AxialStructureTransform.at(origin, front, 1, 0, 0);
            var input = port(helper, frontFluid).getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
            var methane = new FluidStack(GTFluids.still("Methane").get(), 1000);
            helper.assertTrue(input.fill(methane, FluidAction.EXECUTE) == 1000,
                    "front gas intake works facing " + front);
            if (front == Direction.UP) {
                var output = port(helper, AxialStructureTransform.at(origin, front, 1, 0, 3))
                        .getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
                helper.assertTrue(output.fill(methane, FluidAction.SIMULATE) == 0,
                        "up-facing turbine exhaust is on its low rear layer");
            }
            for (var cell : TurbineStructure.LAYOUT.cells()) helper.setBlock(
                    AxialStructureTransform.at(origin, front, cell.right(), cell.up(), cell.back()), Blocks.AIR);
            helper.setBlock(origin, Blocks.AIR);
        }
        helper.succeed();
    }
}
