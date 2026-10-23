package com.gregtech.gregtech.api.sensor;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Absolute temperature a block entity reports to a thermometer sensor.
 * Side uses the native six-direction ordinal, the same as {@link ProgressSensorSource}.
 */
public interface TemperatureSource {
    long temperatureK(int side);

    /** Upper bound for the sensor's percentage and scale modes. */
    long maximumK(int side);
}
