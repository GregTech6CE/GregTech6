package com.gregtech.gregtech.integration.gameplay;

import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Actual machine API boundary tests; natural coal heating is checked separately. */
@GameTestHolder("gregtech_playflow")
@PrefixGameTestTemplate(false)
public final class ForgeThermalBoundaryTests {
    private ForgeThermalBoundaryTests() {}

    @GameTest(template = "test_bronze_chain")
    public static void realCruciblePreservesPositiveHeatRemainderAndMixing(GameTestHelper helper) {
        var pos = new BlockPos(2, 2, 2);
        var state = GTMachines.SMELTING_CRUCIBLE_CERAMIC.get().defaultBlockState();
        helper.setBlock(pos, state);
        var be = (SmeltingCrucibleBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(be.getTemperature() == 293 && be.getEnergyBuffer() == 0,
                "New formal ceramic crucible starts at 293 K with no HU");
        helper.assertTrue(be.doInject(GregTechTags.Energy.HU, Direction.DOWN, -8, 1, false) == 1
                        && be.getEnergyBuffer() == 0,
                "Valid simulated signed-size HU packet cannot change storage");
        helper.assertTrue(be.doInject(GregTechTags.Energy.HU, Direction.DOWN, -8, 1, true) == 1
                        && be.getEnergyBuffer() == 8,
                "Negative HU packet size means positive heating magnitude");
        SmeltingCrucibleBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), state, be);
        var snapshot = be.saveWithoutMetadata();
        helper.assertTrue(be.getTemperature() == 294 && be.getEnergyBuffer() == 1
                        && snapshot.getLong("gt.temperature.old") == 293 && snapshot.getInt("gt.cooldown") == 100,
                "636kg ceramic shell needs 7HU/K; actual step must retain 1HU and the previous temperature");
        SmeltingCrucibleBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), state, be);
        snapshot = be.saveWithoutMetadata();
        helper.assertTrue(be.getTemperature() == 294 && be.getEnergyBuffer() == 1
                        && snapshot.getLong("gt.temperature.old") == 294 && snapshot.getInt("gt.cooldown") == 99,
                "No new energy cannot erase the sub-Kelvin remainder or advance twice");
        var mixedPos = new BlockPos(6, 2, 2);
        helper.setBlock(mixedPos, state);
        var mixed = (SmeltingCrucibleBlockEntity) helper.getBlockEntity(mixedPos);
        helper.assertTrue(mixed.addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Copper, GTValues.U)), 800)
                        && mixed.getTemperature() == 603 && mixed.getEnergyBuffer() == 0
                        && mixed.getCrucibleContentAmount() == GTValues.U,
                "Actual shell/incoming copper mixing keeps original whole-mass rounding at 603 K");
        helper.succeed();
    }

    @GameTest(template = "test_bronze_chain")
    public static void realCrucibleRejectsUnrepresentablePacketsWithoutMutation(GameTestHelper helper) {
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, GTMachines.SMELTING_CRUCIBLE_CERAMIC.get());
        var be = (SmeltingCrucibleBlockEntity) helper.getBlockEntity(pos);
        for (boolean commit : new boolean[]{false, true}) {
            helper.assertTrue(be.doInject(GregTechTags.Energy.HU, Direction.DOWN, Long.MAX_VALUE, 2, commit) == 0,
                    "Unrepresentable packet product must be rejected before accepting any count");
            helper.assertTrue(be.doInject(GregTechTags.Energy.HU, Direction.DOWN, Long.MIN_VALUE, 1, commit) == 0,
                    "Unrepresentable packet magnitude must be rejected");
            helper.assertTrue(be.doInject(GregTechTags.Energy.HU, Direction.DOWN, 1, -1, commit) == 0
                            && be.doInject(GregTechTags.Energy.HU, Direction.UP, 8, 1, commit) == 0
                            && be.doInject(GregTechTags.Energy.CU, Direction.DOWN, 8, 1, commit) == 0,
                    "Count, bottom face and existing HU-only acceptance remain platform boundaries");
            helper.assertTrue(be.getTemperature() == 293 && be.getEnergyBuffer() == 0
                            && be.getContentView().isEmpty(),
                    "Rejected and simulated packets leave actual machine state unchanged");
        }
        helper.succeed();
    }
}
