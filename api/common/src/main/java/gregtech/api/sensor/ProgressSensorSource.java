/* SPDX-License-Identifier: LGPL-3.0-or-later
 * GregTech-6 Team / Gregorius Techneticies.
 * Domain boundary for original gregapi.tileentity.data.ITileEntityProgress. */
package gregtech.api.sensor;

import gregtech.api.machine.BlockFace;

/**
 * Raw progress and its maximum, before the sensor's selectable percentage mode.
 * {@code side} is the absolute face the sensor reads; the host maps it to {@code Direction}.
 */
public interface ProgressSensorSource {
    long progressValue(BlockFace side);
    long progressMaximum(BlockFace side);
}
