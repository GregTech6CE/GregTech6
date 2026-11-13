package gregtech.engine.energy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Guards battery-box item charging: a sink cell charges at matched voltage and refuses overvoltage. */
class BatteryBoxStateTest {

    /** Mirrors ElectricToolItem energy rules: injection only within [voltage/2, voltage*2], no extraction. */
    private static final class Cell implements BatteryBoxState.Battery {
        private final long tierVoltage;
        private final boolean source;
        private final long capacity;
        private long stored;

        Cell(long tierVoltage, boolean source, long capacity, long stored) {
            this.tierVoltage = tierVoltage;
            this.source = source;
            this.capacity = capacity;
            this.stored = stored;
        }

        public boolean accepts() { return true; }
        public boolean canInject(long v) { return v >= tierVoltage / 2 && v <= tierVoltage * 2; }
        public boolean canExtract(long v) { return source && canInject(v); }
        public long stored() { return stored; }
        public long capacity() { return capacity; }

        public long extract(long v, long p) {
            if (!canExtract(v)) return 0;
            long take = Math.min(p, stored / v);
            stored -= take * v;
            return take;
        }

        public long inject(long v, long p) {
            if (!canInject(v)) return 0;
            long space = (capacity - stored) / v;
            long take = Math.min(Math.min(64, p), Math.max(1, space));
            stored += take * v;
            return take;
        }
    }

    @Test
    void chargesSinkAtMatchedVoltage() {
        Cell battery = new Cell(32, true, 32 * 10000, 32 * 5000);
        Cell wrench = new Cell(32, false, 32 * 10000, 0);
        BatteryBoxState state = new BatteryBoxState(List.of(battery, wrench), 32);
        // High buffer band (>=6) drives charging on the once-per-second tick.
        state.restore(new BatteryBoxState.Snapshot(18000, false, 0));
        state.tick(1);
        assertTrue(wrench.stored() > 0, "wrench should charge at matched 32V; stored=" + wrench.stored());
    }

    @Test
    void refusesOvervoltage() {
        Cell battery = new Cell(128, true, 128 * 10000, 128 * 5000);
        Cell wrench = new Cell(32, false, 32 * 10000, 0);
        BatteryBoxState state = new BatteryBoxState(List.of(battery, wrench), 128);
        state.restore(new BatteryBoxState.Snapshot(70000, false, 0));
        state.tick(1);
        assertEquals(0, wrench.stored(), "LV wrench must not charge from a 128V box");
    }
}