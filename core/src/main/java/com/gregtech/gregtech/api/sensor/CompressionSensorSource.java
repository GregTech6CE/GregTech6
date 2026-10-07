/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Domain boundary for original gregapi.tileentity.data.ITileEntityGibbl. */
package com.gregtech.gregtech.api.sensor;

/** Raw compressed volume; sensor scaling is separate. Side uses the native six-direction ordinal. */
public interface CompressionSensorSource {
    long gibblValue(int side);
    long gibblMaximum(int side);
}
