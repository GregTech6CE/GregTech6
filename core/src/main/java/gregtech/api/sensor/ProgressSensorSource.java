/* SPDX-License-Identifier: LGPL-3.0-or-later
 * GregTech-6 Team / Gregorius Techneticies.
 * Domain boundary for original gregapi.tileentity.data.ITileEntityProgress. */
package gregtech.api.sensor;

/**
 * Raw progress and its maximum, before the sensor's selectable percentage mode.
 * {@code side} is {@code Direction.ordinal()}. This core type cannot name {@code Direction}.
 */
public interface ProgressSensorSource {
    long progressValue(int side);
    long progressMaximum(int side);
}
