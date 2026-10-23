/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Domain boundary for original gregapi.tileentity.data.ITileEntityProgress. */
package com.gregtech.gregtech.api.sensor;

/** Raw progress and its maximum, before the sensor's selectable percentage mode. */
public interface ProgressSensorSource {
    long progressValue(int side);
    long progressMaximum(int side);
}
