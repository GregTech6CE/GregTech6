/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.sensor;

import gregtech.api.machine.BlockFace;

/**
 * Absolute temperature a block entity reports to a thermometer sensor.
 * {@code side} is the absolute face the sensor reads, the same as {@link ProgressSensorSource}.
 * {@code SensorContext.side} is the {@code Direction} an addon sensor receives.
 */
public interface TemperatureSource {
    long temperatureK(BlockFace side);

    /** Upper bound for the sensor's percentage and scale modes. */
    long maximumK(BlockFace side);
}
