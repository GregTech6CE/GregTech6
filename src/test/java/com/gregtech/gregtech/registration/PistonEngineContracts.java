package com.gregtech.gregtech.registration;

import com.gregtech.gregtech.api.machine.PistonEngineState;

public final class PistonEngineContracts {
    public static void main(String[] args) {
        sideDispatchContract();
        var electric = new PistonEngineState();
        check(PistonEngineState.voltageTier(8) == 0 && PistonEngineState.voltageTier(32) == 1
                && PistonEngineState.voltageTier(-33) == 2 && PistonEngineState.voltageTier(513) == 4,
                "overvoltage explosion tier boundaries");
        check(electric.accept(32, 2, 64, false) == 2 && electric.energy() == 0, "simulation mutated storage");
        electric.accept(32, 2, 64, true);
        check(electric.tick(1, 32, 16) == 16 && electric.energy() == 32, "32 EU must become 16 KU");
        check(electric.tick(2, 32, 16) == 16 && electric.energy() == 0, "unaccepted output must still cost energy");
        check(electric.tick(3, 32, 16) == 0, "empty engine generated energy");
        var flux = new PistonEngineState();
        flux.accept(1, 128, 256, true);
        check(flux.tick(1, 128, 16) == 16 && flux.energy() == 0, "RF must not convert one-to-one");
        for (int mode = 0; mode < 32; mode++) {
            var engine = new PistonEngineState();
            engine.restore(1000, mode, 0, false, false);
            check(engine.input(17) == (17L * (mode + 1) + 15) / 16, "input rounding");
            check(engine.output(17) == 17L * (mode + 1) / 16, "output rounding");
            if (mode == 0) check(engine.coreColor() == 0x0000ff, "minimum power blue");
            if (mode == 15 || mode == 16) check(engine.coreColor() == 0x00ff00, "rated power green");
            if (mode == 31) check(engine.coreColor() == 0xff0000, "maximum power red");
        }
        electric.restore(1000, 31, 0, true, false);
        check(electric.tick(1, 32, 16) == 32, "first piston half");
        check(electric.tick(2, 32, 16) == -32, "return piston stroke must be negative");
        electric.restore(63, 15, 0, false, false);
        check(electric.accept(32, Long.MAX_VALUE, 64, true) == 1 && electric.energy() == 95, "whole packet capacity boundary");
        check(electric.accept(32, 1, 64, true) == 0, "full buffer accepted more packets");
        electric.toggleStopped();
        check(electric.accept(1, 1, 128, true) == 0, "stopped input");
        check(electric.tick(1, 32, 16) == 16, "stopping input must still use remaining stored energy like GT6");
        System.out.println("PASS GT6 electric/flux piston conversion, mode, sign, simulation and storage contracts");
    }
    private static void sideDispatchContract() {
        var port = (com.gregtech.gregtech.api.energy.IEnergyBlock) java.lang.reflect.Proxy.newProxyInstance(
                PistonEngineContracts.class.getClassLoader(),
                new Class<?>[]{com.gregtech.gregtech.api.energy.IEnergyBlock.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "isEnergyType", "hasEnergySurface" -> true;
                        case "isEnergyAcceptingFrom", "isEnergyEmittingTo" -> args[1] == net.minecraft.core.Direction.SOUTH;
                        case "getEnergySizeInputMin", "getEnergySizeOutputMin" -> 16L;
                        case "doInject", "doExtract" -> (long) args[3];
                        default -> throw new AssertionError("Unexpected call " + method.getName());
                    };
                });
        var eu = com.gregtech.gregtech.data.GregTechTags.Energy.EU;
        check(com.gregtech.gregtech.api.energy.EnergyBlockDefaults.doEnergyInjection(
                port, eu, net.minecraft.core.Direction.NORTH, 1, 1, true) == 0,
                "wrong-side undervoltage must not consume a packet");
        check(com.gregtech.gregtech.api.energy.EnergyBlockDefaults.doEnergyInjection(
                port, eu, net.minecraft.core.Direction.SOUTH, 32, 1, true) == 1, "valid side must inject");
        check(com.gregtech.gregtech.api.energy.EnergyBlockDefaults.doEnergyExtraction(
                port, eu, net.minecraft.core.Direction.NORTH, 32, 1, true) == 0, "wrong-side extraction");
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
