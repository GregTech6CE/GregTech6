/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Domain boundary for original gregapi.tileentity.data.ITileEntityGibbl. */
package com.gregtech.gregtech.api.sensor;

/**
 * Raw compressed volume; sensor scaling is separate.
 * {@code side} is {@code Direction.ordinal()}. This core type cannot name {@code Direction}.
 */
public interface CompressionSensorSource {
    long gibblValue(int side);
    long gibblMaximum(int side);
}
