package gregtech.engine.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MachineWorkCostTest {
    @Test
    void advancesProgressPerTick() {
        assertEquals(30, MachineWorkCost.advance(0, 100, 30));
        assertEquals(100, MachineWorkCost.advance(30, 100, 80));
        assertEquals(100, MachineWorkCost.advance(90, 100, Long.MAX_VALUE));
    }

    @Test
    void ignoresNegativeInputsAndEmptyWork() {
        assertEquals(10, MachineWorkCost.advance(-5, 100, 10));
        assertEquals(0, MachineWorkCost.advance(0, 0, 10));
        assertEquals(50, MachineWorkCost.advance(50, 100, -20));
    }

    @Test
    void singleRunCost() {
        var cost = MachineWorkCost.calculate(8, 100, 1, true, 10000, 0, Long.MAX_VALUE, false);
        assertEquals(new MachineWorkCost.Cost(8, 800), cost);
    }

    @Test
    void powerScaledParallelMultipliesPower() {
        var cost = MachineWorkCost.calculate(8, 100, 4, false, 10000, 0, Long.MAX_VALUE, false);
        assertEquals(new MachineWorkCost.Cost(32, 3200), cost);
    }

    @Test
    void durationScaledParallelKeepsPower() {
        var cost = MachineWorkCost.calculate(8, 100, 4, true, 10000, 0, Long.MAX_VALUE, false);
        assertEquals(new MachineWorkCost.Cost(8, 3200), cost);
    }

    @Test
    void timeEnergyKeepsPowerAndDuration() {
        var cost = MachineWorkCost.calculate(8, 100, 4, false, 10000, 0, Long.MAX_VALUE, false, true);
        assertEquals(new MachineWorkCost.Cost(8, 800), cost);
    }

    @Test
    void overclockRaisesPowerAndHalvesWork() {
        var cost = MachineWorkCost.calculate(8, 100, 1, true, 10000, 32, Long.MAX_VALUE, false);
        assertEquals(new MachineWorkCost.Cost(32, 1600), cost);
    }

    @Test
    void cheapOverclockingSkipsTheRamp() {
        var cost = MachineWorkCost.calculate(8, 100, 1, true, 10000, 32, Long.MAX_VALUE, true);
        assertEquals(new MachineWorkCost.Cost(8, 800), cost);
    }

    @Test
    void refusedInputsReturnNull() {
        assertNull(MachineWorkCost.calculate(-1, 100, 1, true, 10000, 0, Long.MAX_VALUE, false));
        assertNull(MachineWorkCost.calculate(8, 0, 1, true, 10000, 0, Long.MAX_VALUE, false));
        assertNull(MachineWorkCost.calculate(8, 100, 0, true, 10000, 0, Long.MAX_VALUE, false));
        assertNull(MachineWorkCost.calculate(8, 100, 1, true, 0, 0, Long.MAX_VALUE, false));
        assertNull(MachineWorkCost.calculate(9, 100, 1, true, 10000, 0, 8, false));
    }

    @Test
    void unrepresentableWorkReturnsNull() {
        assertNull(MachineWorkCost.calculate(Long.MAX_VALUE, Long.MAX_VALUE, 1, true, 1, 0, Long.MAX_VALUE, false));
    }
}
