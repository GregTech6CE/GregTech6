/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.sensor;

import gregtech.api.machine.BlockFace;

/** Physical contents exposed to a weight sensor, in kilograms, before display scaling.
 * {@code side} is the absolute face the sensor reads; the host maps it to {@code Direction}.
 * Implementations must not include their casing.
 */
public interface WeightSensorSource {
    double weightKilograms(BlockFace side);
}
