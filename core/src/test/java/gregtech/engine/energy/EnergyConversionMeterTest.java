package gregtech.engine.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class EnergyConversionMeterTest {
    @Test
    void completedTickIsStableBeforeAndAfterTheNextTransfer() {
        var meter = new EnergyConversionMeter();
        meter.input(100, 32);
        meter.input(100, 96);
        meter.output(100, 64, 2);
        meter.auxiliaryOutput(100, 32, 1);
        assertEquals(0, meter.input(100));
        assertEquals(128, meter.input(101));
        assertEquals(128, meter.output(101));
        assertEquals(32, meter.auxiliaryOutput(101));

        meter.input(101, 256);
        meter.output(101, 32, 1);
        meter.auxiliaryOutput(101, 16, 1);
        assertEquals(128, meter.input(101));
        assertEquals(128, meter.output(101));
        assertEquals(256, meter.input(102));
        assertEquals(32, meter.output(102));
        assertEquals(16, meter.auxiliaryOutput(102));
        assertEquals(0, meter.auxiliaryOutput(103));
    }

    @Test
    void idleTicksExpireAndSignedOutputCountsAsTransferredPower() {
        var meter = new EnergyConversionMeter();
        meter.input(100, 128);
        meter.output(100, -64, 1);
        meter.output(100, 64, 1);
        assertEquals(128, meter.output(101));
        assertEquals(0, meter.input(102));
        assertEquals(0, meter.output(102));

        meter.input(105, 256);
        meter.output(105, 128, 0);
        assertEquals(0, meter.input(105));
        assertEquals(256, meter.input(106));
        assertEquals(0, meter.output(106));
    }

    @Test
    void largeTransfersSaturateInsteadOfWrappingNegative() {
        var meter = new EnergyConversionMeter();
        meter.input(100, Long.MAX_VALUE - 1);
        meter.input(100, 2);
        meter.output(100, Long.MAX_VALUE, 2);
        assertEquals(Long.MAX_VALUE, meter.input(101));
        assertEquals(Long.MAX_VALUE, meter.output(101));
        meter.output(101, Long.MIN_VALUE, 1);
        assertEquals(Long.MAX_VALUE, meter.output(102));
        meter.input(101, -Long.MAX_VALUE, 2);
        assertEquals(Long.MAX_VALUE, meter.input(102));
    }
}
