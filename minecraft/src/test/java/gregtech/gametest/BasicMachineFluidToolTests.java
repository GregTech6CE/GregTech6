package gregtech.gametest;

import gregtech.content.block.tool.FluidAttachmentBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.registry.GTFluids;
import gregtech.engine.machine.MachineInventory;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
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
public final class BasicMachineFluidToolTests {

    private BasicMachineFluidToolTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void toolAccessDrainsOutputTanksBeforeInputs(GameTestHelper helper) {
        var map = new RecipeMap(null, "gt6ce_test_fluid_tool", "Tool Access", "textures/gui/test",
                1, 0, 0, 1, 1, 0, 0, false, false, false, false);
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

    private static IFluidHandler fluidCapability(net.minecraft.server.level.ServerLevel level, BlockPos pos, Direction side) {
//? if >=1.21.1 {
/*        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
*///?} else {
        return level.getBlockEntity(pos)
                .getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
//?}
    }
}
