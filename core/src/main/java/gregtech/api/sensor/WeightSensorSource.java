/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.sensor;

/** Physical contents exposed to a weight sensor, in kilograms, before display scaling.
 * {@code side} is {@code Direction.ordinal()}. Implementations must not include their casing.
 */
public interface WeightSensorSource {
    double weightKilograms(int side);
}
