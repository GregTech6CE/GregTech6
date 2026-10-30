/* SPDX-License-Identifier: LGPL-3.0-or-later
 * GregTech-6 Team / Gregorius Techneticies.
 * Domain boundary for original gregapi.tileentity.data.ITileEntityGibbl. */
package gregtech.api.sensor;

import gregtech.api.machine.BlockFace;

/**
 * Raw compressed volume; sensor scaling is separate.
 * {@code side} is the absolute face the sensor reads; the host maps it to {@code Direction}.
 */
public interface CompressionSensorSource {
    /** Stored compressed volume in milligibbl; not necessarily fluid millibuckets. Crucibles use 1000 per U/9 of material. */
    long gibblValue(BlockFace side);

    /** Upper bound in the same unit as {@link #gibblValue(BlockFace)}. */
    long gibblMaximum(BlockFace side);
}
