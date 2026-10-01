package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticRotationEngineBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTMiscBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class RotationEngineRepairTests {
    private static final BlockPos CENTER = new BlockPos(2, 1, 2);

    private static EngineBlock block(String id) {
        return MachineRegistry.rotationEngines().stream().map(registered -> registered.get())
                .filter(block -> block.engineSpec(com.gregtech.gregtech.api.machine.RotationEngineSpec.class)
                        .id().equals(id))
                .findFirst().orElseThrow();
    }

    private static EngineBlock bronzeBlock() { return block("engine_rotation_bronze"); }

    private static KineticRotationEngineBlockEntity place(GameTestHelper helper, Direction facing) {
        helper.setBlock(CENTER, bronzeBlock().defaultBlockState().setValue(EngineBlock.FACING, facing));
        return (KineticRotationEngineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CENTER));
    }

    private static final class KuSink extends GTEnergyBlockEntity {
        long packets;
        long lastSize;
        long totalMagnitude;

        KuSink(BlockPos pos, BlockState state) {
            super(GTBlockEntities.CHARGING_CRAFTING_TABLE.get(), pos, state);
        }

        @Override public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) {
            return !emitting && type == GregTechTags.Energy.KU;
        }
        @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
            return type == GregTechTags.Energy.KU;
        }
        @Override public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) { return Long.MAX_VALUE; }
        @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) { return 0; }
        @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) { return 16; }
        @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) { return 0; }
        @Override public long doEnergyInjection(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
            if (type != GregTechTags.Energy.KU) return 0;
            if (execute) {
                packets += amount;
                lastSize = size;
                totalMagnitude += Math.abs(size) * amount;
            }
            return amount;
        }
    }

    private static KuSink sink(GameTestHelper helper, BlockPos relativePos) {
        helper.setBlock(relativePos, GTMiscBlocks.CHARGING_CRAFTING_TABLE.get());
        BlockPos absolute = helper.absolutePos(relativePos);
        KuSink sink = new KuSink(absolute, helper.getLevel().getBlockState(absolute));
        helper.getLevel().setBlockEntity(sink);
        return sink;
    }

    private static void tick(GameTestHelper helper, KineticRotationEngineBlockEntity engine) {
        KineticRotationEngineBlockEntity.serverTick(helper.getLevel(), engine.getBlockPos(), engine.getBlockState(), engine);
    }

    @GameTest(template = "test_empty")
    public static void fourRadialInputsAndTwoAxialOutputsIncludeVerticalPlacement(GameTestHelper helper) {
        for (Direction facing : Direction.values()) {
            KineticRotationEngineBlockEntity engine = place(helper, facing);
            for (Direction side : Direction.values()) {
                boolean axial = side.getAxis() == facing.getAxis();
                helper.assertTrue(engine.isEnergyAcceptingFrom(GregTechTags.Energy.RU, side, false) == !axial,
                        facing + " accepts RU only around the shaft: " + side);
                helper.assertTrue(engine.isEnergyEmittingTo(GregTechTags.Energy.KU, side, false) == axial,
                        facing + " emits KU only from the two shaft ends: " + side);
                helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, side, 32, 1, false) == (axial ? 0 : 1),
                        "simulation obeys fixed face geometry");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void originalInputSizeBoundsIncludeSmallWoodenEngine(GameTestHelper helper) {
        KineticRotationEngineBlockEntity bronze = place(helper, Direction.NORTH);
        helper.assertTrue(bronze.getEnergySizeInputMin(GregTechTags.Energy.RU, Direction.WEST) == 16
                        && bronze.getEnergySizeInputMax(GregTechTags.Energy.RU, Direction.WEST) == 64,
                "bronze RU input allows sizes 16 through 64 around its rated 32 RU");
        helper.assertTrue(bronze.getEnergyDemanded(GregTechTags.Energy.RU, Direction.WEST, 8) == 0
                        && bronze.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 8, 1, true) == 1
                        && bronze.storedRu() == 0,
                "below-minimum packets are consumed without entering the capacitor, as in GT6 root injection");

        EngineBlock woodBlock = block("engine_rotation_wood");
        helper.setBlock(CENTER, woodBlock.defaultBlockState().setValue(EngineBlock.FACING, Direction.NORTH));
        KineticRotationEngineBlockEntity wood =
                (KineticRotationEngineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CENTER));
        helper.assertTrue(wood.getEnergySizeInputMin(GregTechTags.Energy.RU, Direction.WEST) == 1
                        && wood.getEnergySizeInputMax(GregTechTags.Energy.RU, Direction.WEST) == 16
                        && wood.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 1, 1, true) == 1
                        && wood.storedRu() == 1,
                "the original 8 RU wooden engine accepts size-one RU");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void singleAndDoubleShaftOutputsFollowBipolarSixteenTickCycle(GameTestHelper helper) {
        KineticRotationEngineBlockEntity engine = place(helper, Direction.NORTH);
        KuSink south = sink(helper, CENTER.south());
        helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 32, 1, true) == 1,
                "one bronze-rated RU packet is accepted from the side");
        tick(helper, engine);
        helper.assertTrue(south.packets == 1 && south.lastSize == 16 && engine.storedRu() == 0,
                "one connected end receives +16 KU and the unused opposite output is wasted");

        KuSink north = sink(helper, CENTER.north());
        engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.EAST, 32, 1, true);
        tick(helper, engine);
        helper.assertTrue(south.packets == 2 && south.lastSize == 16
                        && north.packets == 1 && north.lastSize == -16
                        && south.totalMagnitude + north.totalMagnitude == 48 && engine.storedRu() == 0,
                "both ends emit opposite signs and at most 32 KU total per 32 RU input");

        for (int i = 0; i < 14; i++) tick(helper, engine);
        engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, -32, 1, true);
        tick(helper, engine);
        helper.assertTrue(north.packets == 2 && north.lastSize == 16
                        && south.packets == 3 && south.lastSize == -16,
                "after sixteen ticks the polarity swaps even for negative RU input");
        helper.assertTrue(engine.negativeInput() && engine.storedRu() == 0,
                "negative input is recorded but cannot create additional energy");
        helper.assertTrue(KineticRotationEngineBlockEntity.positiveOutputSide(Direction.NORTH, 0) == Direction.SOUTH
                        && KineticRotationEngineBlockEntity.positiveOutputSide(Direction.NORTH, 15) == Direction.SOUTH
                        && KineticRotationEngineBlockEntity.positiveOutputSide(Direction.NORTH, 16) == Direction.NORTH
                        && KineticRotationEngineBlockEntity.positiveOutputSide(Direction.NORTH, 32) == Direction.SOUTH,
                "phase boundaries match original GT6 bipolar converter");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void simulationAndRejectedOutputNeverChangeEnergyOrPolarity(GameTestHelper helper) {
        KineticRotationEngineBlockEntity engine = place(helper, Direction.NORTH);
        CompoundTag before = engine.saveWithoutMetadata();
        helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, -32, 1, false) == 1
                        && before.equals(engine.saveWithoutMetadata()) && engine.storedRu() == 0
                        && !engine.negativeInput(),
                "simulated signed RU injection cannot mutate buffer or input sign");
        helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.NORTH, 32, 1, true) == 0
                        && engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, Long.MIN_VALUE, 1, true) == 0
                        && engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 128, 1, false) == 1,
                "axial and invalid packets are rejected; simulated overvoltage is reported but harmless");
        helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, -32, 1, true) == 1
                        && engine.storedRu() == 32 && engine.negativeInput(),
                "executed negative RU is stored by magnitude");
        CompoundTag saved = engine.saveWithoutMetadata();
        KineticRotationEngineBlockEntity loaded = new KineticRotationEngineBlockEntity(
                bronzeBlock().getBeType(), engine.getBlockPos(), engine.getBlockState());
        loaded.setSpec(engine.spec());
        loaded.load(saved);
        helper.assertTrue(loaded.storedRu() == 32 && loaded.negativeInput(), "RU buffer and sign persist through NBT");
        helper.assertTrue(engine.doEnergyExtraction(GregTechTags.Energy.KU, Direction.SOUTH, 16, 1, true) == 0,
                "bipolar output is pushed on tick and cannot be withdrawn twice");
        tick(helper, engine);
        helper.assertTrue(engine.storedRu() == 0 && engine.isActive(),
                "disconnected WASTE_ENERGY converter spends its input rather than stockpiling KU");
        tick(helper, engine);
        helper.assertTrue(!engine.isActive(), "engine becomes inactive after input stops");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void overvoltageConsumesPacketsAndProtectsFirstHundredIncidents(GameTestHelper helper) {
        KineticRotationEngineBlockEntity engine = place(helper, Direction.NORTH);
        BlockPos absolute = helper.absolutePos(CENTER);
        helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 32, 1, true) == 1,
                "bronze engine first stores valid RU");
        CompoundTag before = engine.saveWithoutMetadata();
        helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, -128, 3, false) == 3
                        && engine.overvoltagePreventionCount() == 0
                        && before.equals(engine.saveWithoutMetadata()),
                "simulation consumes three oversized packets without recording damage or changing stored RU");

        for (int i = 1; i <= 100; i++) {
            helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 128, 3, true) == 3,
                    "each actual overvoltage incident consumes the complete packet count");
            helper.assertTrue(engine.overvoltagePreventionCount() == i && engine.storedRu() == 0
                            && helper.getLevel().getBlockEntity(absolute) == engine,
                    "GT6 startup protection clears the buffer and keeps the engine for incident " + i);
        }

        // GT6 does not persist this transient protection byte when the chunk reloads.
        CompoundTag saved = engine.saveWithoutMetadata();
        KineticRotationEngineBlockEntity loaded = new KineticRotationEngineBlockEntity(
                bronzeBlock().getBeType(), absolute, engine.getBlockState());
        loaded.setSpec(engine.spec());
        loaded.load(saved);
        helper.assertTrue(loaded.overvoltagePreventionCount() == 0,
                "reloading the engine resets the transient protection counter");

        helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 128, 1, true) == 1,
                "the 101st oversized packet is still consumed");
        if (com.gregtech.gregtech.GregTechConfig.machineOvervoltageExplosions()) {
            helper.assertTrue(helper.getLevel().getBlockEntity(absolute) == null,
                    "the 101st overvoltage removes the unprotected engine");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void idleEngineRecoversOneProtectionIncidentAtSixHundredTickCadence(GameTestHelper helper) {
        KineticRotationEngineBlockEntity engine = place(helper, Direction.NORTH);
        helper.assertTrue(engine.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 128, 1, true) == 1
                        && engine.overvoltagePreventionCount() == 1,
                "one overvoltage incident is recorded");
        for (int i = 0; i < 4; i++) tick(helper, engine);
        helper.assertTrue(engine.overvoltagePreventionCount() == 1, "protection remains until GT6's fifth idle tick");
        tick(helper, engine);
        helper.assertTrue(engine.overvoltagePreventionCount() == 0, "an idle engine recovers one incident");
        helper.succeed();
    }
}
