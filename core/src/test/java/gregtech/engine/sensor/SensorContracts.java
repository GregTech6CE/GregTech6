package gregtech.engine.sensor;

import static gregtech.engine.sensor.SensorControl.Mode.*;

/** Source sampling arithmetic and redstone boundary cases; no native world coverage. */
public final class SensorContracts {
    public static void main(String[] args) {
        equal(0, SensorControl.evaluate(SCALE, 0, 100, 100).signal(), "empty");
        equal(1, SensorControl.evaluate(SCALE, 1, 100, 100).signal(), "tiny nonzero input remains detectable");
        equal(8, SensorControl.evaluate(SCALE, 50, 100, 100).signal(), "half-full source scale");
        equal(14, SensorControl.evaluate(SCALE, 99, 100, 100).signal(), "not full yet");
        equal(15, SensorControl.evaluate(SCALE, 100, 100, 100).signal(), "full");
        equal(15, SensorControl.evaluate(SCALE, 1, 100, 0).signal(), "positive input with zero threshold");
        equal(50, SensorControl.evaluate(PERCENT, 50, 100, 0).displayed(), "percentage remains an ordinary ratio");
        equal(8, SensorControl.evaluate(PERCENT, 50, 100, 0).signal(), "percentage redstone uses source scale");
        equal(0, SensorControl.evaluate(PERCENT, 1, 0, 0).signal(), "unknown capacity");
        equal(270, SensorMeasurements.environmentKelvin(0), "freezing biome fallback");
        equal(280, SensorMeasurements.environmentKelvin(.5F), "temperate biome fallback");
        equal(1, SensorMeasurements.environmentKelvin(-100), "absolute temperature minimum");
        equal(2000, SensorPanelRules.tpsFromElapsed(1000, 20), "TPS remains sampled over twenty ticks");
        var average = new SensorAverage();
        average.resize(2);
        equal(5, average.sample(10), "source starts with zero-filled average");
        equal(10, average.sample(10), "two samples fill the window");
        equal(5, average.sample(0), "short pulse remains in the window");
        System.out.println("Sensor contracts passed.");
    }
    private static void equal(long expected, long actual, String message) {
        if (expected != actual) throw new AssertionError(message + ": " + actual + " != " + expected);
    }
}
