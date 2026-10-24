package gregtech.gametest;

import gregtech.content.block.machine.EngineBlock;
import gregtech.content.blockentity.machine.KineticDieselEngineBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.content.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
@net.neoforged.neoforge.gametest.GameTestHolder("gt6ce_diesel_repair")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
@net.minecraftforge.gametest.GameTestHolder("gt6ce_diesel_repair")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class DieselEngineRepairTests {
    private static final BlockPos CENTER = new BlockPos(2, 2, 2);

    private static KineticDieselEngineBlockEntity place(GameTestHelper helper, Direction front) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:engine_diesel_bronze"));
        helper.setBlock(CENTER, Blocks.AIR);
        helper.setBlock(CENTER, block.defaultBlockState().setValue(EngineBlock.FACING, front));
        return (KineticDieselEngineBlockEntity) helper.getBlockEntity(CENTER);
    }

    private static void tick(GameTestHelper helper, KineticDieselEngineBlockEntity engine) {
        KineticDieselEngineBlockEntity.serverTick(helper.getLevel(), engine.getBlockPos(), engine.getBlockState(), engine);
    }

    @GameTest(template = "test_empty")
    public static void fuelCycleFrontOnlyAndHeavyGasExhaustInAllOrientations(GameTestHelper helper) {
        for (Direction front : Direction.values()) {
            var engine = place(helper, front);
            var rear = engine.getBlockPos().relative(front.getOpposite());
            for (Direction side : Direction.values()) {
                helper.assertTrue(engine.isEnergyEmittingTo(GregTechTags.Energy.RU, side, false) == (side == front), "Fixed RU front " + front + "/" + side);
            }
            engine.setFaceConfig(gregtech.internal.energy.FaceConfig.builder().energyOut(gregtech.internal.energy.FaceConfig.BACK).build());
            helper.assertTrue(engine.isEnergyEmittingTo(GregTechTags.Energy.RU, front, false), "Old saved masks cannot redirect RU");
            engine.fuelTank().fill(new FluidStack(GTFluids.still("Diesel").get(), 1), IFluidHandler.FluidAction.EXECUTE);
            tick(helper, engine);
            long energy = engine.getEnergyStored(GregTechTags.Energy.RU, null);
            helper.assertTrue(energy > 16 && engine.fuelTank().isEmpty(), "Complete real fuel cycle credited");
            helper.assertTrue(engine.getBlockState().getValue(EngineBlock.LIT), "Fuel cycle enables active model");
            engine.setStopped(true);
            tick(helper, engine);
            helper.assertTrue(engine.getEnergyStored(GregTechTags.Energy.RU, null) == energy - 16, "One rated packet spent while residual fuel energy remains");
            helper.assertTrue(!engine.getBlockState().getValue(EngineBlock.LIT), "Stopped visual state");
            engine.exhaustTank().setEmpty();
            engine.exhaustTank().fill(new FluidStack(GTFluids.still("CarbonDioxide").get(), 20), IFluidHandler.FluidAction.EXECUTE);
            helper.getLevel().setBlockAndUpdate(rear, Blocks.STONE.defaultBlockState());
            tick(helper, engine);
            helper.assertTrue(engine.exhaustTank().getAmount() == 20, "Solid rear blocks venting");
            helper.getLevel().setBlockAndUpdate(rear, Blocks.AIR.defaultBlockState());
            tick(helper, engine);
            helper.assertTrue(engine.exhaustTank().isEmpty(), "Heavy gas vents from rear " + front);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void tapRecoversFuelAfterExhaustAndJadeReportsDiesel(GameTestHelper helper) {
        var engine = place(helper, Direction.NORTH);
        var diesel = new FluidStack(GTFluids.still("Diesel").get(), 20);
        var funnel = engine.fluidToolHandler(Direction.UP, true);
        helper.assertTrue(funnel.fill(diesel, IFluidHandler.FluidAction.EXECUTE) == 20, "Funnel accepts engine fuel");
        helper.assertTrue(funnel.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Funnel cannot drain");
        engine.exhaustTank().fill(new FluidStack(GTFluids.still("CarbonDioxide").get(), 3), IFluidHandler.FluidAction.EXECUTE);
        var tap = engine.fluidToolHandler(Direction.UP, false);
        helper.assertTrue(tap.drain(3, IFluidHandler.FluidAction.EXECUTE).getFluid() != diesel.getFluid(), "Tap drains output first");
        helper.assertTrue(tap.drain(2, IFluidHandler.FluidAction.EXECUTE).getFluid() == diesel.getFluid(), "Tap can then recover fuel");
        var data = new net.minecraft.nbt.CompoundTag();
        var accessor = (snownee.jade.api.BlockAccessor) java.lang.reflect.Proxy.newProxyInstance(
                snownee.jade.api.BlockAccessor.class.getClassLoader(), new Class<?>[]{snownee.jade.api.BlockAccessor.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBlockEntity" -> engine;
                    case "getBlock" -> engine.getBlockState().getBlock();
                    case "getBlockState" -> engine.getBlockState();
                    case "getLevel" -> helper.getLevel();
                    default -> null;
                });
        gregtech.integration.jade.EngineJadeProvider.INSTANCE.appendServerData(data, accessor);
        helper.assertTrue(data.getString("gt_eng_type").equals("diesel") && data.getLong("gt_eng_out_rate") == 16, "Jade reports rated diesel RU");
        helper.assertTrue(data.getLong("gt_diesel_fuel_capacity") == 160 && data.contains("gt_diesel_fuel"), "Jade includes real fluid contents and capacity");
        helper.succeed();
    }
}
