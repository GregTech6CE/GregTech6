package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.machine.LargeDynamoControllerBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.AxialGeneratorDefinitions;
import com.gregtech.gregtech.content.multiblock.AxialStructureTransform;
import com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions;
import com.gregtech.gregtech.content.multiblock.OriginalGeneratorTooltipData;
import com.gregtech.gregtech.content.multiblock.TurbineStructure;
import com.gregtech.gregtech.content.cover.PanelCoverHost;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** DRAFT: real registered Hydrogen and a real large dynamo; no recipe or receiver-spec substitution. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class GasTurbinePartialFuelTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }

    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag data) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(data, h.getLevel().registryAccess());
*///?} else {
        entity.load(data);
//?}
    }

    private static IFluidHandler inlet(GameTestHelper h, BlockPos pos) {
//? if >=1.21.1 {
/*        var result = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, Direction.NORTH);
        if (result == null) throw new IllegalStateException("missing gas inlet");
        return result;
*///?} else {
        return h.getLevel().getBlockEntity(pos).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                .orElseThrow(() -> new IllegalStateException("missing gas inlet"));
//?}
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void tick(GameTestHelper h, T entity) {
        var type = (BlockEntityType<T>) (BlockEntityType<?>) entity.getType();
        var ticker = ((EntityBlock) entity.getBlockState().getBlock()).getTicker(h.getLevel(), entity.getBlockState(), type);
        h.assertTrue(ticker != null, "actual registered controller supplies native ticker");
        ticker.tick(h.getLevel(), entity.getBlockPos(), entity.getBlockState(), entity);
    }

    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        final GameTestHelper h;
        final BlockPos pos;
        final java.util.Map<BlockPos, Saved> saved = new java.util.LinkedHashMap<>();
        LargeGasTurbineControllerBlockEntity gas;
        LargeDynamoControllerBlockEntity dynamo;
        int supplied;

        Fixture(GameTestHelper h, int index) {
            this.h = h;
            pos = new BlockPos(21_900_000 + index * 128, 200, 21_900_000);
        }

        void set(BlockPos at, BlockState state) {
            if (!saved.containsKey(at)) {
                var entity = h.getLevel().getBlockEntity(at);
                saved.put(at.immutable(), new Saved(h.getLevel().getBlockState(at), entity == null ? null : save(h, entity)));
            }
            h.getLevel().removeBlockEntity(at);
            h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(at, state, FLAGS);
        }

        void build() {
            // Both disjoint 3x3x4 structures, front/rear neighbors, and parallel-suite isolation.
            for (var at : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 10)))
                set(at, Blocks.AIR.defaultBlockState());
            var gasBlock = GasTurbineDefinitions.blocks().get(0).get();
            var gasGrade = GasTurbineDefinitions.grade(gasBlock);
            set(pos, gasBlock.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH));
            for (var cell : TurbineStructure.LAYOUT.cells())
                set(cell.at(pos, Direction.NORTH), gasGrade.wall().defaultBlockState());
            gas = (LargeGasTurbineControllerBlockEntity) h.getLevel().getBlockEntity(pos);
            var receiverPos = pos.south(4);
            var dynamoBlock = AxialGeneratorDefinitions.blocks(false).get(0).get();
            var dynamoGrade = AxialGeneratorDefinitions.grade(dynamoBlock);
            set(receiverPos, dynamoBlock.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.NORTH));
            for (var entry : dynamoGrade.cells().entrySet()) {
                var cell = entry.getKey();
                set(AxialStructureTransform.at(receiverPos, Direction.NORTH, cell.getX(), cell.getY(), cell.getZ()),
                        entry.getValue().defaultBlockState());
            }
            dynamo = (LargeDynamoControllerBlockEntity) h.getLevel().getBlockEntity(receiverPos);
            h.assertTrue(gas.isStructureOk() && dynamo.isStructureOk(), "registered gas turbine and actual adjacent native dynamo both form");
            var shown = OriginalGeneratorTooltipData.find(gasGrade.id());
            h.assertTrue(gasGrade.input() == 6144 && gasGrade.output() == 4096
                            && shown.outputMinimum() == 2048 && shown.outputMaximum() == 8192 && shown.efficiency() == 6666,
                    "current declared output range and efficiency include tested partial packets");
            h.assertTrue(dynamo.getEnergyDemanded(GregTechTags.Energy.RU, Direction.NORTH, 2048) > 0
                            && dynamo.getEnergyDemanded(GregTechTags.Energy.RU, Direction.NORTH, 8192) > 0,
                    "unmodified built-in receiver accepts minimum and maximum turbine RU packets");
            var recipe = FuelRecipeMaps.Gas.mRecipeList.stream().filter(r -> r.mFluidInputs.length == 1
                    && r.mFluidInputs[0].getFluid() == GTFluids.still("Hydrogen").get()).findFirst().orElseThrow();
            h.assertTrue(recipe.mFluidInputs[0].getAmount() == 2 && recipe.mEUt == -16 && recipe.mDuration == 2
                            && recipe.mFluidOutputs.length == 1 && recipe.mFluidOutputs[0].getAmount() == 3
                            && recipe.mFluidOutputs[0].getFluid() == net.minecraft.world.level.material.Fluids.WATER,
                    "actual registered Hydrogen charge remains 2 mB -> 32 HU + 3 mB water");
        }

        void fill(int amount) {
            var fluids = inlet(h, pos);
            var fuel = GTFluids.stack("Hydrogen", amount);
            // The native covered capability calls this same public host access on its first fill.
            // Initialize its lazy empty storage before snapshotting so gt.component_covers is
            // already represented. Keep the complete native NBT comparison; discard no fields.
            h.assertTrue((Object) gas instanceof PanelCoverHost, "registered gas controller has its normal native cover host");
            var host = (PanelCoverHost) (Object) gas;
            h.assertTrue(!host.hasAttachedCovers(), "isolated gas fixture has no attached covers");
            host.panels();
            var before = save(h, gas);
            int simulated = fluids.fill(fuel, IFluidHandler.FluidAction.SIMULATE);
            var after = save(h, gas);
            h.assertTrue(simulated == amount,
                    "native fuel-fill simulation accepts requested " + amount + " mB; actual accepted=" + simulated);
            h.assertTrue(before.equals(after),
                    "native fuel-fill simulation preserves complete initialized NBT; before=" + before + "; after=" + after);
            int accepted = fluids.fill(fuel, IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(accepted == amount,
                    "native gas inlet executes requested " + amount + " mB; actual accepted=" + accepted);
            supplied += accepted;
        }

        void gasTicks(int count) {
            // Do not tick the dynamo before querying accepted RU: its own conversion legitimately spends that buffer.
            for (int i = 0; i < count; i++) tick(h, gas);
        }

        void amounts(long hu, long pendingRu, long receivedRu) {
            int remaining = gas.getFluidInTank(0).getAmount();
            int charges = (supplied - remaining) / 2;
            h.assertTrue(supplied - remaining == charges * 2, "fuel consumption stays on exact registered charge boundary");
            h.assertTrue(gas.getFluidInTank(1).getAmount() == charges * 3
                            && (charges == 0 || gas.getFluidInTank(1).getFluid() == net.minecraft.world.level.material.Fluids.WATER)
                            && gas.getFluidInTank(2).isEmpty() && gas.getFluidInTank(3).isEmpty(),
                    "all real byproduct corresponds exactly to consumed Hydrogen charges");
            long actualHu = gas.getEnergyStored(GregTechTags.Energy.HU, null);
            long actualPending = gas.getEnergyStored(GregTechTags.Energy.RU, null);
            long actualReceived = dynamo.getEnergyStored(GregTechTags.Energy.RU, null);
            h.assertTrue(actualHu == hu && actualPending == pendingRu && actualReceived == receivedRu,
                    "expected HU/pendingRU/receiverRU=" + hu + "/" + pendingRu + "/" + receivedRu
                            + ", actual=" + actualHu + "/" + actualPending + "/" + actualReceived);
            long output = pendingRu + receivedRu;
            long usedHu = (output * 6144 + 4095) / 4096;
            h.assertTrue(charges * 32L == hu + usedHu,
                    "consumed fuel HU equals retained HU plus ceil(actual RU * nominalHU / nominalRU)");
        }

        void freshGasEntity() {
            var old = gas;
            var data = save(h, old);
            var state = old.getBlockState();
            h.getLevel().removeBlockEntity(pos);
            var replacement = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
            h.assertTrue(replacement instanceof LargeGasTurbineControllerBlockEntity && replacement.getType() == old.getType(),
                    "native registered block creates a fresh same-type controller");
            load(h, replacement, data);
            h.getLevel().setBlockEntity(replacement);
            gas = (LargeGasTurbineControllerBlockEntity) replacement;
            h.assertTrue(old.isRemoved() && gas != old && gas.isStructureOk(), "fresh native entity rebinds actual shell after removal");
        }

        @Override public void close() {
            for (var at : saved.keySet()) {
                h.getLevel().removeBlockEntity(at);
                h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            }
            for (var entry : saved.entrySet()) {
                h.getLevel().setBlock(entry.getKey(), entry.getValue().state(), FLAGS);
                var entity = h.getLevel().getBlockEntity(entry.getKey());
                if (entity != null && entry.getValue().data() != null) load(h, entity, entry.getValue().data());
            }
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void minimumOperationalHydrogenBatchReachesRealDynamo(GameTestHelper h) {
        try (var f = new Fixture(h, 0)) {
            f.build(); f.fill(192); f.gasTicks(2);
            h.assertTrue(f.gas.getFluidInTank(0).isEmpty(), "192 mB input was actually consumed");
            f.amounts(0, 0, 2048);
            f.gasTicks(20); f.amounts(0, 0, 2048);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void fractionalPartialHydrogenBatchRoundsWithoutCreatingEnergy(GameTestHelper h) {
        try (var f = new Fixture(h, 1)) {
            f.build(); f.fill(256); f.gasTicks(2);
            h.assertTrue(f.gas.getFluidInTank(0).isEmpty(), "256 mB input was actually consumed");
            // Source output=floor(4096*4096/6144)=2730; debit=ceil(2730*6144/4096)=4095.
            // Port's existing resource protection retains the single unspent HU instead of source no-fuel waste.
            f.amounts(1, 0, 2730);
            f.gasTicks(20); f.amounts(1, 0, 2730);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void belowMinimumHydrogenBatchRetainsHeatWithoutRu(GameTestHelper h) {
        try (var f = new Fixture(h, 2)) {
            f.build(); f.fill(128); f.gasTicks(22);
            h.assertTrue(f.gas.getFluidInTank(0).isEmpty(), "below-minimum fuel still combusts through registered recipe");
            f.amounts(2048, 0, 0);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void emptyGasTurbineCreatesNoEnergyOrExhaust(GameTestHelper h) {
        try (var f = new Fixture(h, 3)) {
            f.build(); f.gasTicks(22); f.amounts(0, 0, 0);
            h.assertTrue(f.gas.getFluidInTank(0).isEmpty(), "empty fuel stays empty");
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void fullHydrogenBatchPreservesMaximumOutputRate(GameTestHelper h) {
        try (var f = new Fixture(h, 4)) {
            f.build(); f.fill(768); f.gasTicks(2);
            h.assertTrue(f.gas.getFluidInTank(0).isEmpty(), "maximum batch consumes exactly 768 mB Hydrogen");
            // Two-tick allowance intentionally preserves current delivery timing, rather than asserting a new throughput policy.
            f.amounts(0, 0, 8192);
            f.gasTicks(20); f.amounts(0, 0, 8192);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void blockedPartialGasPulseSurvivesFreshEntityLoadAndTransfersOnce(GameTestHelper h) {
        try (var f = new Fixture(h, 5)) {
            f.build();
            f.dynamo.machineControl(null).setEnabled(false); // Real configured receiver rejects input, not a mocked handler.
            f.fill(192); f.gasTicks(2); f.amounts(0, 2048, 0);
            f.gas.machineControl(null).setEnabled(false);
            f.freshGasEntity();
            h.assertTrue(!f.gas.machineControl(null).enabled(), "native stopped state survives fresh entity loading");
            f.gasTicks(20); f.amounts(0, 2048, 0);
            f.dynamo.machineControl(null).setEnabled(true);
            f.gasTicks(2); f.amounts(0, 0, 2048);
            f.gasTicks(20); f.amounts(0, 0, 2048);
        }
        h.succeed();
    }
}
