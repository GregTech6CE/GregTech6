package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.EnergyPackets;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMath;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.recipe.MachineWorkCost;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Shared-core linkage and game bootstrap checks; no gameplay completion claim. */
@GameTestHolder("gregtech_bootstrap")
@PrefixGameTestTemplate(false)
public final class IntegrationBootstrapTests {
    private IntegrationBootstrapTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void sharedMachineWorkCost(GameTestHelper helper) {
        var normal = MachineWorkCost.calculate(32, 192, 1, true, 10_000, 128, 512, false);
        helper.assertTrue(normal != null && normal.minimumPower() == 128
                        && normal.totalWork() == 12_288,
                "Ordinary overclock must require 128 power and 12288 work");
        var cheap = MachineWorkCost.calculate(32, 192, 4, true, 5_000, 512, 4_096, true);
        helper.assertTrue(cheap != null && cheap.minimumPower() == 32
                        && cheap.totalWork() == 49_152,
                "Cheap parallel work must retain 32 power and require 49152 work");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void sharedMaterialUnits(GameTestHelper helper) {
        helper.assertTrue(GTValues.U == 648_648_000L, "GT6 U precision must remain unchanged");
        helper.assertTrue(CrucibleMath.units(144, 144, GTValues.U, false) == GTValues.U,
                "144 L must be one ingot in GT6 U");
        helper.assertTrue(CrucibleMath.units(16, 144, GTValues.U, false) == GTValues.U9,
                "16 L must be one ninth of an ingot");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void sharedSignedEnergyPackets(GameTestHelper helper) {
        helper.assertTrue(EnergyPackets.magnitude(-32, true) == 32,
                "Signed rotation must retain the 32-unit magnitude");
        helper.assertTrue(EnergyPackets.magnitude(-32, false) == 0,
                "Unsigned energy must reject negative packets");
        helper.assertTrue(EnergyPackets.magnitude(Long.MIN_VALUE, true) == 0,
                "Unrepresentable signed magnitude must not become usable energy");
        helper.assertTrue(EnergyPackets.fitting(8, 100, 32) == 3,
                "A 100-unit buffer must fit exactly three 32-unit packets");
        helper.assertTrue(EnergyPackets.fitting(8, 31, 32) == 0,
                "A buffer smaller than one packet must accept none");
        helper.succeed();
    }
}
