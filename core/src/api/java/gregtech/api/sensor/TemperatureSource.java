/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.sensor;

/**
 * Absolute temperature a block entity reports to a thermometer sensor.
 * {@code side} is {@code Direction.ordinal()}, the same as {@link ProgressSensorSource}.
 * This core type cannot name {@code Direction}. {@code SensorContext.side} is the {@code Direction}
 * an addon sensor receives.
 */
public interface TemperatureSource {
    long temperatureK(int side);

    /** Upper bound for the sensor's percentage and scale modes. */
    long maximumK(int side);
}
