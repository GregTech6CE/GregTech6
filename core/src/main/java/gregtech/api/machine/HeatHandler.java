/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

/**
 * Signed heat facade over a machine's own HU buffer.
 *
 * <p>Amounts are signed heat units: a positive value heats, a negative value cools. Each
 * implementation keeps its own limits. A boiler still explodes on the next tick when its buffer
 * exceeds its rated capacity, a crucible still converts its stored HU into Kelvin on its next
 * tick, and an unformed large boiler still refuses input. A simulated call must return what the
 * executed call would return and must not change any state.
 */
public interface HeatHandler {
    /** Signed stored heat in HU; cooling energy is negative. */
    long heatStored();

    /** Absolute ceiling of the heat buffer; {@link Long#MAX_VALUE} when the buffer is unbounded. */
    long heatCapacity();

    /** Tile temperature in K, never negative. Machines without a thermal model report their tile environment. */
    long temperatureK();

    /** Adds {@code signedHU}; returns the signed amount accepted (0 when refused). */
    long insertHeat(long signedHU, boolean simulate);

    /** Removes {@code signedHU}; returns the signed amount removed (0 when refused). */
    long extractHeat(long signedHU, boolean simulate);
}
