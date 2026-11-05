package gregtech.engine.energy;

import gregtech.content.machine.MachineInputAverage;

/** Transient transfer observations. Completed ticks keep inspection independent of block tick order. */
public final class EnergyConversionMeter {
    private final Window input = new Window();
    private final Window output = new Window();
    private final Window auxiliaryOutput = new Window();

    public void input(long tick, long amount) {
        input.add(tick, amount);
    }

    public void input(long tick, long size, long packets) {
        input.add(tick, packetEnergy(size, packets));
    }

    public void output(long tick, long size, long packets) {
        output.add(tick, packetEnergy(size, packets));
    }

    public void auxiliaryOutput(long tick, long size, long packets) {
        auxiliaryOutput.add(tick, packetEnergy(size, packets));
    }

    private static long packetEnergy(long size, long packets) {
        long magnitude = size == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(size);
        if (magnitude == 0 || packets <= 0) return 0;
        return packets > Long.MAX_VALUE / magnitude ? Long.MAX_VALUE : magnitude * packets;
    }

    public long input(long tick) {
        return input.previous(tick);
    }

    public long output(long tick) {
        return output.previous(tick);
    }

    public long auxiliaryOutput(long tick) {
        return auxiliaryOutput.previous(tick);
    }

    public double averageInput(long tick) {
        return input.average(tick);
    }

    public double averageOutput(long tick) {
        return output.average(tick);
    }

    public double averageAuxiliaryOutput(long tick) {
        return auxiliaryOutput.average(tick);
    }

    private static final class Window {
        private long tick = Long.MIN_VALUE;
        private long current, previous;
        // Idle channels need no history allocation; observations are never saved with the machine.
        private MachineInputAverage average;

        void add(long now, long amount) {
            if (amount <= 0) return;
            if (tick != now) {
                previous = tick == now - 1 ? current : 0;
                current = 0;
                tick = now;
            }
            long accepted = Math.min(amount, Long.MAX_VALUE - current);
            current += accepted;
            if (average == null) average = new MachineInputAverage();
            average.record(now, accepted);
        }

        long previous(long now) {
            return tick == now ? previous : tick == now - 1 ? current : 0;
        }

        double average(long now) {
            return average == null ? 0 : average.average(now);
        }
    }
}
