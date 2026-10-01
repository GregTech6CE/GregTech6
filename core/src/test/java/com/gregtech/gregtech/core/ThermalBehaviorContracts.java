package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.machine.crucible.ThermalState;
import com.gregtech.gregtech.api.machine.crucible.ThermalStep;
import com.gregtech.gregtech.api.machine.crucible.ThermalStep.EnergyKind;

/** Fixed upstream/brokestar fixtures and the seven thermal/two mixing comparison groups. */
public final class ThermalBehaviorContracts {
    private static int assertions;
    private ThermalBehaviorContracts() {}
    public static void main(String[] args) {
        positiveSupply();
        remainderAcrossTicks();
        signedCooling();
        signedPacketKinds();
        passiveWindow();
        kelvinFloor();
        inputAndOverflowPolicy();
        integralMixing();
        quantizedMixing();
        System.out.println("Thermal behavior contracts passed: " + assertions + " assertions in 7 thermal and 2 mixing groups; immutable integer Kelvin");
    }

    private static void positiveSupply() {
        // GT6 Smeltery at pinned revision:301-313; comparison thermal group1.
        expect(ThermalStep.advance(state(1300, 8, 7), 293, 250), 1302, 1300, 2, 100, "8HU at required3");
        // Original brokestar CruciblePhysicsTest:83-89,145-147 numeric expectations.
        check(ThermalStep.requiredEnergy(0) == 1 && ThermalStep.requiredEnergy(99.99) == 1
                && ThermalStep.requiredEnergy(100) == 2 && ThermalStep.requiredEnergy(222.222222) == 3,
                "Original whole100kg threshold table");
        expect(ThermalStep.advance(state(500, 3, 0), 300, 222.222222), 501, 500, 0, 100, "Original oneKelvin heat tick");
    }
    private static void remainderAcrossTicks() {
        // Comparison group2: do not adopt CC's zero-incoming remainder erasure.
        ThermalState prior = state(1300, 2, 7);
        ThermalState waiting = ThermalStep.advance(prior, 293, 250);
        expect(waiting, 1300, 1300, 2, 6, "Subdegree remainder survives");
        ThermalState paid = ThermalStep.receive(waiting, EnergyKind.HEAT, 1, 1);
        expect(ThermalStep.advance(paid, 293, 250), 1301, 1300, 0, 100, "A later1HU joins retained2HU");
        check(prior.equals(state(1300, 2, 7)) && waiting.energyHU() == 2, "Input state is never mutated");
    }
    private static void signedCooling() {
        // Comparison group3, GT6's nonzero conversion branch; Forge CU acceptance is unchanged elsewhere.
        var cooling = ThermalStep.receive(state(1300, 0, 7), EnergyKind.COOLING, 8, 1);
        expect(ThermalStep.advance(cooling, 293, 250), 1298, 1300, -2, 100, "Cooling consumes whole signed conversions");
        expect(ThermalStep.advance(state(1300, -2, 7), 293, 250), 1300, 1300, -2, 6, "Negative subdegree remainder survives");
        var cancelled = ThermalStep.receive(state(1300, -2, 7), EnergyKind.HEAT, 2, 1);
        expect(cancelled, 1300, 1299, 0, 7, "Opposite energy kinds cancel exactly without touching other fields");
    }
    private static void signedPacketKinds() {
        // Comparison group4, pinned GT6 doInject:693: HU/CU kind decides the sign, size supplies magnitude.
        expect(ThermalStep.advance(ThermalStep.receive(state(1300, 0, 7), EnergyKind.HEAT, -8, 1), 293, 250),
                1302, 1300, 2, 100, "Negative HU packet still heats");
        expect(ThermalStep.advance(ThermalStep.receive(state(1300, 0, 7), EnergyKind.COOLING, -8, 1), 293, 250),
                1298, 1300, -2, 100, "Negative CU packet still cools");
        expect(ThermalStep.receive(state(1300, 2, 7), EnergyKind.HEAT, 8, 0), 1300, 1299, 2, 7, "Zero packet count is a no-op");
    }
    private static void passiveWindow() {
        // Comparison group5 and original brokestar tests:154-161: oneK each ten ticks after supply expires.
        ThermalState drift = ThermalStep.advance(state(1300, 0, 1), 293, 250);
        expect(drift, 1299, 1300, 0, 10, "Expired countdown begins oneK drift");
        for (int tick = 1; tick <= 9; tick++) drift = ThermalStep.advance(drift, 293, 250);
        expect(drift, 1299, 1299, 0, 1, "No second drift during the nine waiting ticks");
        expect(ThermalStep.advance(drift, 293, 250), 1298, 1299, 0, 10, "Tenth tick drifts again");
        expect(ThermalStep.advance(state(500, 0, 0), 600, 222.222222), 501, 500, 0, 10, "Hot ambient warms oneK");
        expect(ThermalStep.advance(state(293, 0, 1), 293, 250), 293, 293, 0, 10, "At ambient the original timer remains10");
        expect(ThermalStep.advance(state(500, 0, 100), 300, 222.222222), 500, 500, 0, 99, "Supply window blocks drift");
        expect(ThermalStep.advance(state(500, 0, Integer.MIN_VALUE), 300, 222.222222), 499, 500, 0, 10,
                "An expired negative legacy countdown is normalized");
    }
    private static void kelvinFloor() {
        // Comparison group6 and original brokestar tests:168-173; the ceiling on the floor is200K, not200C.
        expect(ThermalStep.advance(state(200, 0, 100), 293, 250), 200, 200, 0, 99, "200K remains200K with normal ambient");
        expect(ThermalStep.advance(state(200, 0, 100), 600, 250), 200, 200, 0, 99, "Hot ambient does not raise the floor above200K");
        expect(ThermalStep.advance(state(1, 0, 1), 300, 222.222222), 200, 1, 0, 10, "Original belowfloor state clamps to200K");
        expect(ThermalStep.advance(state(150, 0, 1), 150, 222.222222), 150, 150, 0, 10, "Cold ambient sets its own floor");
    }
    private static void inputAndOverflowPolicy() {
        // Comparison group7: explicit new policy, rather than treating nonfinite mass as required1.
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1})
            throwsType(IllegalArgumentException.class, () -> ThermalStep.requiredEnergy(invalid), "Invalid mass rejected");
        throwsType(ArithmeticException.class, () -> ThermalStep.requiredEnergy(Double.MAX_VALUE), "Required energy overflow rejected");
        throwsType(IllegalArgumentException.class, () -> new ThermalState(-1, 0, 0, 0), "Negative absolute Kelvin rejected");
        throwsType(IllegalArgumentException.class, () -> new ThermalState(0, -1, 0, 0), "Negative previous Kelvin rejected");
        throwsType(IllegalArgumentException.class, () -> ThermalStep.advance(state(200, 0, 1), -1, 250), "Negative ambient Kelvin rejected");
        throwsType(IllegalArgumentException.class, () -> ThermalStep.receive(state(200, 0, 1), EnergyKind.HEAT, 8, -1), "Negative packet count rejected");
        throwsType(ArithmeticException.class, () -> ThermalStep.receive(state(200, 0, 1), EnergyKind.HEAT, Long.MIN_VALUE, 1), "Unrepresentable packet magnitude rejected");
        throwsType(ArithmeticException.class, () -> ThermalStep.receive(state(200, 0, 1), EnergyKind.HEAT, Long.MAX_VALUE, 2), "Packet product overflow rejected");
        throwsType(ArithmeticException.class, () -> ThermalStep.receive(state(200, Long.MAX_VALUE, 1), EnergyKind.HEAT, 1, 1), "Buffer overflow rejected");
        throwsType(ArithmeticException.class, () -> ThermalStep.receive(state(200, Long.MIN_VALUE, 1), EnergyKind.COOLING, 1, 1), "Buffer underflow rejected");
        throwsType(ArithmeticException.class, () -> ThermalStep.advance(state(Long.MAX_VALUE, 1, 1), 293, 0), "Temperature overflow rejected");
        var minimum = ThermalStep.receive(state(200, -Long.MAX_VALUE, 1), EnergyKind.COOLING, 1, 1);
        check(minimum.energyHU() == Long.MIN_VALUE, "A representable MIN_VALUE signed buffer is retained");
        expect(ThermalStep.advance(new ThermalState(0, 0, Long.MIN_VALUE, 7), 293, 0), 200, 0, 0, 100,
                "Signed extreme cooling is consumed with the original Kelvin floor");
        expect(ThermalStep.advance(new ThermalState(0, 0, Long.MAX_VALUE, 7), 293, 0), Long.MAX_VALUE, 0, 0, 100,
                "Exactly representable extreme heating is retained");
    }
    private static void integralMixing() {
        // Comparison mixing1: the Celsius float path would show796K; authoritative integers must be797K.
        check(ThermalStep.mixTemperature(1301, 293, 300, 300) == 797, "Equal300kg mixing yields797K");
        // Original brokestar test:104-118 (two density1 water units / empty shell).
        check(ThermalStep.mixTemperature(500, 300, 111.111111, 111.111111) == 400, "Original equalwater masses yield400K");
        check(ThermalStep.mixTemperature(500, 300, 0, 111.111111) == 300, "Zero existing mass takes incoming temperature");
        check(ThermalStep.mixTemperature(500, 300, 0, 0) == 500, "Zero total mass retains existing temperature");
        expect(ThermalStep.mix(state(1301, -2, 7), 293, 300, 300), 797, 1300, -2, 7,
                "Mixing preserves prior-temperature, signed energy and cooldown");
    }
    private static void quantizedMixing() {
        // Comparison mixing2: long-cast masses250/426 yield883K, not a continuous weighted average.
        check(ThermalStep.mixTemperature(1300, 293, 250.75, 175.5) == 883, "Mass quantization remains883K");
        check(ThermalStep.mixTemperature(293, 1300, 250.75, 175.5) == 710, "Reverse-direction mix retains its original floor scaling");
        throwsType(IllegalArgumentException.class, () -> ThermalStep.mixTemperature(500, 300, Double.NaN, 300), "Nonfinite existing mass rejected");
        throwsType(IllegalArgumentException.class, () -> ThermalStep.mixTemperature(500, 300, 300, -1), "Negative incoming mass rejected");
        throwsType(IllegalArgumentException.class, () -> ThermalStep.mixTemperature(500, 300, 0.25, 0.25), "Positive total mass below1kg has no valid integer divisor");
        throwsType(IllegalArgumentException.class, () -> ThermalStep.mixTemperature(500, 300, Double.MAX_VALUE, Double.MAX_VALUE), "Nonfinite mass sum rejected");
        throwsType(ArithmeticException.class, () -> ThermalStep.mixTemperature(500, 300, 0x1.0p63, 0), "Mass cast overflow rejected");
        throwsType(ArithmeticException.class, () -> ThermalStep.mixTemperature(Long.MAX_VALUE, 0, 2, 2), "Scale product overflow rejected before CrucibleMath");
    }

    private static ThermalState state(long temperature, long energy, int cooldown) {
        return new ThermalState(temperature, Math.max(0, temperature - 1), energy, cooldown);
    }
    private static void expect(ThermalState actual, long temperature, long previous, long energy, int cooldown, String message) {
        check(actual.equals(new ThermalState(temperature, previous, energy, cooldown)), message + ": " + actual);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static void throwsType(Class<? extends Throwable> type, Runnable action, String message) {
        assertions++;
        try { action.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(message, failure);
        }
        throw new AssertionError(message);
    }
}
