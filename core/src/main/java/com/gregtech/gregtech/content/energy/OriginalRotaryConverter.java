/* Copyright (c) 2021-2023 GregTech-6 Team; Gregorius Techneticies.
 * LGPL-3.0-or-later. Adapted from TileEntityBase10EnergyConverter,
 * TileEntityBase11Motor, TE_Behavior_Energy_Stats/Converter/Active_Trinary. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.data.GregTechTags;
import java.math.BigInteger;
import java.util.function.LongBinaryOperator;

/** Source motor/dynamo conversion. Storage, faces, capabilities and NBT remain native boundaries. */
public final class OriginalRotaryConverter {
    private OriginalRotaryConverter() {}
    public static boolean handles(EnergyNodeSpec spec) {
        return spec.id().startsWith("electric_motor_") || spec.id().startsWith("flux_motor_")
                || spec.id().startsWith("electric_dynamo_") || spec.id().startsWith("flux_dynamo_");
    }
    public static boolean motor(EnergyNodeSpec spec) {
        return spec.id().startsWith("electric_motor_") || spec.id().startsWith("flux_motor_");
    }
    public static long capacity(EnergyNodeSpec spec) { return spec.inputRate() * 2; }
    public static long inputMinimum(EnergyNodeSpec spec) { return spec.inputRate() <= 16 ? 1 : spec.inputRate() / 2; }
    public static int efficiency(EnergyNodeSpec spec) {
        long input = spec.inputRate(), output = spec.outputRate();
        if (spec.inType() == GregTechTags.Energy.RF) output *= 4;
        if (spec.outType() == GregTechTags.Energy.RF) input *= 4;
        return (int) units(10000, input, output, false);
    }
    public static float overloadPower(long packet, GregTechTags.Tag type) {
        if (!GregTechTags.Energy.ALL_EXPLODING.contains(type)) return 0.1F;
        long magnitude = packet == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(packet), voltage = 8;
        for (int tier = 0; tier < 16; tier++, voltage *= 4) if (magnitude <= voltage) return tier;
        return 16;
    }

    private static long units(long amount, long from, long to, boolean roundUp) {
        if (amount <= 0 || to == 0) return 0;
        try {
            long product = Math.multiplyExact(amount, to);
            return product / from + (roundUp && product % from != 0 ? 1 : 0);
        } catch (ArithmeticException overflow) {
            var parts = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(to)).divideAndRemainder(BigInteger.valueOf(from));
            var value = parts[0].add(roundUp && parts[1].signum() > 0 ? BigInteger.ONE : BigInteger.ZERO);
            return value.min(BigInteger.valueOf(Long.MAX_VALUE)).longValueExact();
        }
    }

    public record Injection(long energy, long consumed, boolean overloaded) {}
    public record Tick(long energy, boolean overloaded, boolean possible, boolean emitted, boolean fast, int visual) {}
    public record Snapshot(int mode, boolean counterClockwise, boolean negativeInput,
                           boolean possible, boolean emitted, boolean fast) {}

    public static final class State {
        private final EnergyNodeSpec spec;
        private long timer, history;
        private int mode;
        private boolean counterClockwise, negativeInput, possible, emitted, fast;
        public State(EnergyNodeSpec spec) {
            // The same source base conversion also drives thermal devices. Twin outputs use one
            // native emission callback for both channels, so fixed input waste is applied once.
            if (!handles(spec) && !OriginalThermalConverter.handles(spec)) throw new IllegalArgumentException("Not an original registered converter: " + spec.id());
            this.spec = spec;
        }
        public int mode() { return mode; }
        public int mode(int value) { return mode = value & 15; }
        public boolean counterClockwise() { return counterClockwise; }
        public boolean reverse() { return counterClockwise = !counterClockwise; }
        public boolean possible() { return possible; }
        public boolean emitted() { return emitted; }
        public boolean fast() { return fast; }
        public int visual(boolean stopped) { return stopped || history == 0 ? 0 : history == -1L ? 1 : 2; }

        /** Caller applies the shared root minimum/type/face gate before injection. */
        public Injection inject(long energy, long size, long amount, boolean execute) {
            if (size == 0 || size == Long.MIN_VALUE || amount <= 0) return new Injection(energy, 0, false);
            if (execute) negativeInput = size < 0;
            long magnitude = Math.abs(size);
            if (magnitude > spec.inputRate() * 2) return new Injection(energy, amount, execute);
            long room = Math.max(0, capacity(spec) - energy);
            if (room == 0) return new Injection(energy, 0, false);
            // Like source Stats: the final whole packet may overshoot capacity, without amount*size overflow.
            long used = Math.min(amount, room / magnitude + (room % magnitude == 0 ? 0 : 1));
            return new Injection(execute ? energy + used * magnitude : energy, used, false);
        }

        public Tick tick(long energy, boolean stopped, LongBinaryOperator emit) {
            timer++;
            long output = units(energy, spec.inputRate(), spec.outputRate(), false);
            if (mode > 0) output = Math.min(output, units(spec.outputRate() * 2, 16, 16 - mode, false));
            possible = output >= spec.outputRate() / 2;
            fast = output > spec.outputRate();
            emitted = false;
            boolean overloaded = false;
            if (possible) {
                if (output > spec.outputRate() * 2) {
                    if (GregTechTags.Energy.ALL_COMSUMPTION_LIMITED.contains(spec.inType())) output = spec.outputRate() * 2;
                    else {
                        // Original first two chunk-load ticks clear saved excess without an overload event.
                        overloaded = timer > 2;
                        return finish(0, stopped, overloaded);
                    }
                }
                boolean negative = negativeInput && GregTechTags.Energy.ALL_NEGATIVE_ALLOWED.contains(spec.inType())
                        && GregTechTags.Energy.ALL_NEGATIVE_ALLOWED.contains(spec.outType());
                long size = motor(spec) && counterClockwise ? -output : output;
                if (negative) size = -size;
                emitted = (GregTechTags.Energy.isSizeIrrelevant(spec.outType())
                        ? emit.applyAsLong(negative ? -1 : 1, output) : emit.applyAsLong(size, 1)) > 0;
            }
            // These original electric/flux motors, dynamos and thermal devices are WASTE_ENERGY=T.
            // Stopped blocks still convert their remaining buffer; only acceptance/visuals stop.
            energy = Math.max(0, energy - units(spec.inputRate() * 2, 16, 16 - mode, true));
            return finish(energy, stopped, overloaded);
        }
        private Tick finish(long energy, boolean stopped, boolean overloaded) {
            history = (history << 1) | (possible ? 1 : 0);
            return new Tick(energy, overloaded, possible, emitted, fast, visual(stopped));
        }
        public Snapshot snapshot() { return new Snapshot(mode, counterClockwise, negativeInput, possible, emitted, fast); }
        public void restore(Snapshot saved) {
            mode(saved.mode()); counterClockwise = saved.counterClockwise(); negativeInput = saved.negativeInput();
            possible = saved.possible(); emitted = saved.emitted(); fast = saved.fast();
            timer = 0; history = 0;
        }
    }
}
