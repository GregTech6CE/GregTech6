package gregtech.gametest;

import gregtech.internal.energy.FaceConfig;
import gregtech.internal.multiblock.tracking.StructureScheduler;
import gregtech.internal.machine.BasicMachineSpec;
//? if >=1.21.1 {
/**///?} else {
import gregtech.internal.machine.MachineRegistry;
//?}
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.block.machine.FuelGeneratorBlock;
import gregtech.blockentity.machine.BasicMachineBlockEntity;
import gregtech.blockentity.machine.FuelGeneratorBlockEntity;
import gregtech.blockentity.machine.FuelTurbineBlockEntity;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.data.GregTechTags;
import gregtech.jei.MultiblockInfoData;
import gregtech.jei.RecipeMachines;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.machine.BasicMachineRegistries;
*///?} else {
//?}
import gregtech.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
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

/** Sample engine and turbine: datapack fuel, efficiency, delivery, preview, and save. */
@GameTestHolder("gregtech_card17_fuel")
@PrefixGameTestTemplate(false)
public final class FuelGeneratorTests {
    private FuelGeneratorTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void sampleFuelRegistersAndJoinsTheViewers(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("addon_sample:sample_engine")) instanceof FuelGeneratorBlock,
                "sample engine block");
        helper.assertTrue(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("addon_sample:sample_turbine")) instanceof FuelGeneratorBlock,
                "sample turbine block");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("addon_sample:be_sample_engine")),
                "engine block entity");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("addon_sample:be_sample_turbine")),
                "turbine block entity");
        var map = RecipeMap.RECIPE_MAPS.get("addon_sample.fuel.gas");
        helper.assertTrue(map != null, "sample fuel map");
        Recipe lava = lava(map);
        helper.assertTrue(lava != null && lava.mEUt == -16 && lava.mDuration == 20, "datapack lava is stored as produced energy");
        var engine = BuiltInRegistries.ITEM.get(ResourceLocation.parse("addon_sample:sample_engine"));
        var turbine = BuiltInRegistries.ITEM.get(ResourceLocation.parse("addon_sample:sample_turbine"));
        boolean engineCatalyst = false;
        boolean turbineCatalyst = false;
        for (var stack : RecipeMachines.collect().getOrDefault(map, java.util.List.of())) {
            engineCatalyst |= stack.is(engine);
            turbineCatalyst |= stack.is(turbine);
        }
        helper.assertTrue(engineCatalyst && turbineCatalyst, "both generators are catalysts on the sample fuel map");
        boolean preview = false;
        for (var info : MultiblockInfoData.recipes()) {
            if (!info.controller().is(turbine)) continue;
            preview = true;
            helper.assertTrue(MultiblockInfoData.expectedPart(info, new BlockPos(0, 0, 1)).is(GTMultiblocks.TANK_WALL.get().asItem()),
                    "structure preview shows the tank wall");
        }
        helper.assertTrue(preview, "the turbine is in the structure preview");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void engineBurnsAFullChargeAndFeedsTheFront(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var engine = place(helper, "addon_sample:sample_engine", origin);
        helper.assertTrue(engine.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 100, "engine accepts lava");
        engine.tick();
        helper.assertTrue(engine.storedEnergy() == 320, "100 percent efficiency stores the whole 320 charge");
        helper.assertTrue(engine.fuelAmount() == 0, "the lava charge is consumed");
        helper.assertTrue(engine.getFluidInTank(1).getFluid() == Fluids.WATER && engine.getFluidInTank(1).getAmount() == 10,
                "exhaust water is kept");
        helper.assertTrue(!engine.getBlockState().getValue(FuelGeneratorBlock.LIT), "nothing accepted the packet");

        engine.machineControl(null).setEnabled(false);
        helper.assertTrue(engine.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 100, "stopped engine still holds fuel");
        long held = engine.storedEnergy();
        engine.tick();
        helper.assertTrue(engine.isStopped() && engine.fuelAmount() == 100 && engine.storedEnergy() == held, "the stop switch burns nothing");
        engine.machineControl(null).setEnabled(true);

        var front = origin.relative(Direction.NORTH);
        var receiver = receiver(helper, front, 32);
        engine.tick();
        helper.assertTrue(receiver.getEnergyTick() == 32, "the front machine receives one 32 RU packet");
        helper.assertTrue(engine.storedEnergy() == held - 32, "the buffer spends the packet after it is accepted");
        helper.getLevel().removeBlock(origin, false);
        helper.getLevel().removeBlock(front, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void fourAmpereMotorSendsFourPackets(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var motor = place(helper, "addon_sample:sample_motor", origin);
        helper.assertTrue(motor.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 100, "motor accepts lava");
        motor.tick();
        helper.assertTrue(motor.storedEnergy() == 320, "the charge is stored before a receiver exists");
        var front = origin.relative(Direction.NORTH);
        var receiver = receiver(helper, front, 32, 256);
        motor.tick();
        helper.assertTrue(receiver.getEnergyTick() == 128, "four 32 RU packets leave in one tick");
        helper.assertTrue(motor.storedEnergy() == 192, "the buffer spends all four packets");
        helper.getLevel().removeBlock(origin, false);
        helper.getLevel().removeBlock(front, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void turbineWaitsForStructureAndFeedsFourBehind(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var turbine = (FuelTurbineBlockEntity) place(helper, "addon_sample:sample_turbine", origin);
        helper.assertTrue(turbine.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 100, "turbine accepts lava");
        turbine.tick();
        helper.assertTrue(!turbine.isFormed() && turbine.fuelAmount() == 100 && turbine.storedEnergy() == 0,
                "an unformed turbine does not burn");
        var layout = LargeMachineLayouts.cells("sample_turbine");
        helper.assertTrue(layout != null && layout.size() == 1, "sample turbine layout");
        BlockPos wall = null;
        for (var cell : layout) {
            wall = cell.at(origin, Direction.NORTH);
            helper.getLevel().setBlockAndUpdate(wall, cell.block().defaultBlockState());
        }
        StructureScheduler.flush(helper.getLevel());
        turbine.tick();
        helper.assertTrue(turbine.isFormed(), "the tank wall forms the turbine");
        helper.assertTrue(turbine.storedEnergy() == 160, "half efficiency stores 160");
        helper.assertTrue(turbine.fuelAmount() == 0, "formed turbine consumes the charge");
        var behind = origin.relative(Direction.SOUTH, 4);
        var receiver = receiver(helper, behind, 32);
        turbine.tick();
        helper.assertTrue(receiver.getEnergyTick() == 32 && turbine.storedEnergy() == 128,
                "one packet arrives four blocks behind");
        helper.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
        turbine.tick();
        helper.assertTrue(!turbine.isFormed() && turbine.storedEnergy() == 128, "breaking the wall stops the burn");
        helper.getLevel().removeBlock(origin, false);
        helper.getLevel().removeBlock(behind, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void restartKeepsFuelBufferAndStructure(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var turbine = (FuelTurbineBlockEntity) place(helper, "addon_sample:sample_turbine", origin);
        var layout = LargeMachineLayouts.cells("sample_turbine");
        BlockPos wall = layout.get(0).at(origin, Direction.NORTH);
        helper.getLevel().setBlockAndUpdate(wall, layout.get(0).block().defaultBlockState());
        StructureScheduler.flush(helper.getLevel());
        turbine.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE);
        turbine.tick();
        turbine.machineControl(null).setEnabled(false);
        turbine.fill(new FluidStack(Fluids.LAVA, 50), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(turbine.isFormed() && turbine.isStopped() && turbine.fuelAmount() == 50 && turbine.storedEnergy() == 160,
                "state ready to save");
//? if >=1.21.1 {
/*        var lookup = helper.getLevel().registryAccess();
        var saved = turbine.saveWithoutMetadata(lookup);
*///?} else {
        var saved = turbine.saveWithoutMetadata();
//?}
        helper.getLevel().removeBlock(origin, false);
        var restored = (FuelTurbineBlockEntity) place(helper, "addon_sample:sample_turbine", origin);
//? if >=1.21.1 {
/*        restored.loadWithComponents(saved, lookup);
*///?} else {
        restored.load(saved);
//?}
        helper.assertTrue(restored.fuelAmount() == 50, "fuel reloads");
        helper.assertTrue(restored.getFluidInTank(1).getFluid() == Fluids.WATER && restored.getFluidInTank(1).getAmount() == 10,
                "exhaust reloads");
        helper.assertTrue(restored.storedEnergy() == 160, "energy buffer reloads");
        helper.assertTrue(restored.isStopped(), "stop switch reloads");
        helper.assertTrue(restored.isFormed(), "structure state reloads");
        helper.getLevel().removeBlock(origin, false);
        helper.getLevel().removeBlock(wall, false);
        helper.succeed();
    }

//? if >=1.21.1 {
/**///?} else {
    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void builtInGasTurbineGradesStillConsumeAndDeliver(GameTestHelper helper) {
        GasTurbineGradeTests.structuralPartsDropThemselves(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 800)
    public static void builtInGasTurbineGradesStillBindEveryFacing(GameTestHelper helper) {
        GasTurbineGradeTests.fourGradesConsumeConserveAndBind(helper);
    }

    @GameTest(template = "test_blueprint_empty", timeoutTicks = 400)
    public static void builtInGasTurbineWallsKeepTheirWelds(GameTestHelper helper) {
        GasTurbineGradeTests.newWallsHaveOriginalWeldingRecipes(helper);
    }

//?}
    private static FuelGeneratorBlockEntity place(GameTestHelper helper, String id, BlockPos pos) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(FuelGeneratorBlock.FACING, Direction.NORTH));
        var entity = helper.getLevel().getBlockEntity(pos);
        if (!(entity instanceof FuelGeneratorBlockEntity machine)) throw new AssertionError("No fuel generator at " + id);
        return machine;
    }

    private static BasicMachineBlockEntity receiver(GameTestHelper helper, BlockPos pos, long packet) {
        return receiver(helper, pos, packet, 0);
    }

    private static BasicMachineBlockEntity receiver(GameTestHelper helper, BlockPos pos, long packet, long maximum) {
//? if >=1.21.1 {
/*        var block = BasicMachineRegistries.all().iterator().next().get();
*///?} else {
        var block = MachineRegistry.basicMachines().iterator().next().get();
//?}
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(pos);
        var spec = BasicMachineSpec.builder("fuel_receiver", block.basicSpec().material())
                .machineType("test").energy(GregTechTags.Energy.RU, packet).recipes(block.basicSpec().recipeMap())
                .faces(FaceConfig.ALL_SIDES);
        if (maximum > 0) spec.energyRange(1, maximum);
        machine.setSpec(spec.build());
        return machine;
    }

    private static Recipe lava(RecipeMap map) {
        for (Recipe recipe : map.mRecipeList)
            if (recipe.mFluidInputs.length == 1 && recipe.mFluidInputs[0].getFluid() == Fluids.LAVA) return recipe;
        return null;
    }
}
