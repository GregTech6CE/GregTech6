package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions;
import com.gregtech.gregtech.content.multiblock.TurbineStructure;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//?}

/** Native save/load into fresh entities, not an independent JVM or actual chunk-unload test. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MachinePendingStateTests {
    private static BasicMachineBlock crusher() {
        return (BasicMachineBlock) BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof BasicMachineBlock basic && basic.basicSpec().machineName().equals("crusher"))
                .findFirst().orElseThrow();
    }
    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }
    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag tag) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(tag, h.getLevel().registryAccess());
*///?} else {
        entity.load(tag);
//?}
    }
    private static void tick(GameTestHelper h, BasicMachineBlockEntity machine) {
        BasicMachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }

    /** Restore only explicitly touched disposable-test cells, including any preexisting native data. */
    private static final class Fixture implements AutoCloseable {
        private record Cell(net.minecraft.world.level.block.state.BlockState state, CompoundTag data) {}
        private final GameTestHelper helper;
        private final java.util.Map<BlockPos, Cell> cells = new java.util.LinkedHashMap<>();
        private Fixture(GameTestHelper helper) { this.helper = helper; }
        private void capture(BlockPos pos) {
            if (cells.containsKey(pos)) return;
            var entity = helper.getLevel().getBlockEntity(pos);
            cells.put(pos.immutable(), new Cell(helper.getLevel().getBlockState(pos),
                    entity == null ? null : save(helper, entity)));
        }
        @Override public void close() {
            // Remove test inventories before replacing blocks so cleanup cannot spill test items.
            for (var pos : cells.keySet()) helper.getLevel().removeBlockEntity(pos);
            for (var entry : cells.entrySet()) {
                var pos = entry.getKey(); var cell = entry.getValue();
                helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
                helper.getLevel().setBlock(pos, cell.state(),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
                var restored = helper.getLevel().getBlockEntity(pos);
                if (restored != null && cell.data() != null) load(helper, restored, cell.data());
            }
        }
    }

    @GameTest(template="test_empty")
    public static void partiallyDeliveredMachineJobSurvivesFreshEntityLoad(GameTestHelper h) {
        var block = crusher();
        var pos = h.absolutePos(new BlockPos(2, 3, 2));
        var map = new RecipeMap(null, "gt.recipe.test.pending_" + java.util.UUID.randomUUID(),
                "Pending fixture", "test", 1, 1, 1, 0, 1, 0, 1, false, false, false, false);
        try (var fixture = new Fixture(h)) {
            fixture.capture(pos);
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var machine = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(pos);
            var spec = BasicMachineSpec.builder("pending_fixture", block.basicSpec().material())
                    .machineType("crusher").energy(GregTechTags.Energy.TU, 1).energyRange(1, 1)
                    .processingEfficiency(10000).parallel(1).recipes(map).faces(FaceConfig.ALL_SIDES).build();
            machine.setSpec(spec);
            map.mRecipeList.add(new Recipe(new ItemStack[]{new ItemStack(Items.COBBLESTONE)},
                    new ItemStack[]{new ItemStack(Items.STICK, 4)}, null, null, null,
                    new FluidStack[]{new FluidStack(Fluids.WATER, 100)}, 4, 1, 0));
            machine.inventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 2));
            tick(h, machine); // Actual recipe admission and first TU, while all outputs fit.
            h.assertTrue(machine.inventory().getStackInSlot(0).getCount() == 1
                    && machine.machineControl(null).progressMax() == 4, "one input admitted into real four-TU job");
            // Explicit contention fixture: output space disappears after admission, before completion.
            machine.inventory().setStackInSlot(1, new ItemStack(Items.STICK, 62));
            machine.getTanksOutput()[0].fill(new FluidStack(Fluids.WATER, 7999), FluidAction.EXECUTE);
            for (int i = 0; i < 3; i++) tick(h, machine);
            h.assertTrue(machine.inventory().getStackInSlot(1).getCount() == 64
                    && machine.getTanksOutput()[0].getAmount() == 8000
                    && machine.getProgressPercent() == 100, "completion commits only two items and one mB, retaining remainder");
            var snapshot = save(h, machine);
            h.getLevel().removeBlockEntity(pos);
            var restored = new BasicMachineBlockEntity(machine.getType(), pos, machine.getBlockState());
            restored.setSpec(spec);
            load(h, restored, snapshot);
            h.getLevel().setBlockEntity(restored);
            h.assertTrue(restored.inventory().getStackInSlot(0).getCount() == 1
                    && restored.inventory().getStackInSlot(1).getCount() == 64
                    && restored.getTanksOutput()[0].getAmount() == 8000
                    && restored.getProgressPercent() == 100, "fresh native load retains inputs, committed stock and blocked progress");
            tick(h, restored);
            h.assertTrue(restored.inventory().getStackInSlot(0).getCount() == 1
                    && restored.inventory().getStackInSlot(1).getCount() == 64
                    && restored.getTanksOutput()[0].getAmount() == 8000, "blocked retry neither redrains input nor duplicates committed output");
            h.assertTrue(restored.inventory().extractItem(1, 64, true).getCount() == 64
                    && restored.getTanksOutput()[0].drain(8000, FluidAction.SIMULATE).getAmount() == 8000
                    && restored.inventory().getStackInSlot(1).getCount() == 64
                    && restored.getTanksOutput()[0].getAmount() == 8000, "simulated output extraction is nonmutating");
            int deliveredItems = restored.inventory().extractItem(1, 64, false).getCount();
            int deliveredFluid = restored.getTanksOutput()[0].drain(8000, FluidAction.EXECUTE).getAmount();
            tick(h, restored); // Completed-job retry passes zero work; must not start the second input.
            h.assertTrue(deliveredItems + restored.inventory().getStackInSlot(1).getCount() == 62 + 4
                    && deliveredFluid + restored.getTanksOutput()[0].getAmount() == 7999 + 100
                    && restored.inventory().getStackInSlot(0).getCount() == 1
                    && restored.getProgressPercent() == 0
                    && save(h, restored).getLong("gt.max_progress") == 0, "exact residual flush, no reroll or duplicate input consumption");
        } finally {
            RecipeMap.unregister(map);
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void blockedGasTurbinePulseSurvivesFreshEntityLoad(GameTestHelper h) {
        var block = GasTurbineDefinitions.blocks().iterator().next().get();
        var grade = GasTurbineDefinitions.grade(block);
        var pos = h.absolutePos(new BlockPos(6, 3, 6));
        var front = Direction.NORTH;
        var target = pos.relative(front.getOpposite(), 4);
        try (var fixture = new Fixture(h)) {
            fixture.capture(pos);
            fixture.capture(target);
            for (var cell : TurbineStructure.LAYOUT.cells()) fixture.capture(cell.at(pos, front));
            h.getLevel().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(DirectionalBlock.FACING, front));
            for (var cell : TurbineStructure.LAYOUT.cells())
                h.getLevel().setBlockAndUpdate(cell.at(pos, front), grade.wall().defaultBlockState());
            var machine = (LargeGasTurbineControllerBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(StructureTestSupport.ok(machine), "registered gas turbine forms with native walls");
            var recipe = FuelRecipeMaps.Gas.mRecipeList.stream().filter(r -> r.mFluidInputs.length == 1
                    && r.mFluidOutputs.length > 0 && r.mEUt < 0
                    && r.mFluidInputs[0].getFluid() == GTFluids.still("Methane").get()).findFirst().orElseThrow();
            var fuel = recipe.mFluidInputs[0].copy(); fuel.setAmount(machine.getTankCapacity(0));
            h.assertTrue(machine.fill(fuel, FluidAction.SIMULATE) == fuel.getAmount()
                    && machine.getFluidInTank(0).isEmpty(), "simulated fuel fill does not create stock");
            int filled = machine.fill(fuel, FluidAction.EXECUTE);
            machine.tick();
            int remaining = machine.getFluidInTank(0).getAmount();
            int charges = (filled - remaining) / recipe.mFluidInputs[0].getAmount();
            h.assertTrue(charges > 0 && filled - remaining == charges * recipe.mFluidInputs[0].getAmount()
                    && machine.getEnergyStored(GregTechTags.Energy.RU, null) == grade.outputMaximum(), "real fuel conversion produces one retained RU pulse");
            for (int i = 0; i < recipe.mFluidOutputs.length; i++)
                h.assertTrue(machine.getFluidInTank(i + 1).getAmount() == charges * recipe.mFluidOutputs[i].getAmount(), "exhaust matches consumed charges");
            var snapshot = save(h, machine);
            long chargedEnergy = Math.multiplyExact((long) charges, Math.multiplyExact(-recipe.mEUt, recipe.mDuration));
            h.assertTrue(snapshot.getLong("gt.fuel") + grade.inputMaximum() == chargedEnergy,
                    "consumed fuel energy equals retained fuel energy plus the converted charge");
            h.getLevel().removeBlockEntity(pos);
            var restored = new LargeGasTurbineControllerBlockEntity(pos, machine.getBlockState());
            load(h, restored, snapshot);
            h.getLevel().setBlockEntity(restored);
            StructureTestSupport.ok(restored);
            restored.tick();
            h.assertTrue(restored.getFluidInTank(0).getAmount() == remaining
                    && restored.getEnergyStored(GregTechTags.Energy.RU, null) == grade.outputMaximum()
                    && save(h, restored).getLong("gt.fuel") == snapshot.getLong("gt.fuel"), "blocked restored turbine retains pulse and fuel energy without another charge");
            for (int i = 0; i < recipe.mFluidOutputs.length; i++)
                h.assertTrue(restored.getFluidInTank(i + 1).getAmount() == charges * recipe.mFluidOutputs[i].getAmount(), "reload and blocked retry do not duplicate exhaust");
            var receiverBlock = crusher();
            h.getLevel().setBlockAndUpdate(target, receiverBlock.defaultBlockState());
            var receiver = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(target);
            receiver.setSpec(BasicMachineSpec.builder("pending_ru_receiver", receiverBlock.basicSpec().material())
                    .machineType("test").energy(GregTechTags.Energy.RU, grade.outputMaximum())
                    .recipes(receiverBlock.basicSpec().recipeMap()).faces(FaceConfig.ALL_SIDES).build());
            restored.toggleStopped(); // Existing pulse may drain while stopped, but no new charge may start.
            restored.tick();
            restored.tick();
            h.assertTrue(receiver.getEnergyTick() == grade.outputMaximum()
                    && restored.getEnergyStored(GregTechTags.Energy.RU, null) == 0
                    && restored.getFluidInTank(0).getAmount() == remaining
                    && save(h, restored).getLong("gt.fuel") == snapshot.getLong("gt.fuel"), "saved RU transfers exactly once while stopped, conserving retained fuel energy");
        }
        h.succeed();
    }
}
