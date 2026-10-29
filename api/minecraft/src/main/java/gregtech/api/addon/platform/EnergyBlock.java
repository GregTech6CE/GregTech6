/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.core.Direction;

import java.util.Collection;
import java.util.List;

/**
 * Energy block an addon implements. {@code energy} is the short name, such as {@code EU}.
 * A null side means every side. Packet model: {@code size} times {@code amount}.
 */
public interface EnergyBlock {
    boolean isEnergyType(String energy, Direction side, boolean emitting);

    Collection<String> getEnergyTypes(Direction side);

    default boolean isEnergyAcceptingFrom(String energy, Direction side, boolean theoretical) {
        return isEnergyType(energy, side, false) && hasEnergySurface(side);
    }

    default boolean isEnergyEmittingTo(String energy, Direction side, boolean theoretical) {
        return isEnergyType(energy, side, true) && hasEnergySurface(side);
    }

    /** Push-based injection. The default applies GregTech's packet gate and then {@link #doInject}. */
    default long doEnergyInjection(String energy, Direction side, long size, long amount, boolean doInject) {
        return AddonEnergy.inject(this, energy, side, size, amount, doInject);
    }

    long getEnergyDemanded(String energy, Direction side, long size);

    default long doEnergyExtraction(String energy, Direction side, long size, long amount, boolean doExtract) {
        return AddonEnergy.extract(this, energy, side, size, amount, doExtract);
    }

    long getEnergyOffered(String energy, Direction side, long size);

    long getEnergySizeInputRecommended(String energy, Direction side);

    long getEnergySizeOutputRecommended(String energy, Direction side);

    default long getEnergySizeInputMin(String energy, Direction side) {
        long recommended = getEnergySizeInputRecommended(energy, side);
        return recommended <= 0 ? 0 : Math.max(1, recommended / 2);
    }

    default long getEnergySizeInputMax(String energy, Direction side) {
        return doubled(getEnergySizeInputRecommended(energy, side));
    }

    default long getEnergySizeOutputMin(String energy, Direction side) {
        long recommended = getEnergySizeOutputRecommended(energy, side);
        return recommended <= 0 ? 0 : Math.max(1, recommended / 2);
    }

    default long getEnergySizeOutputMax(String energy, Direction side) {
        return doubled(getEnergySizeOutputRecommended(energy, side));
    }

    /** Twice {@code recommended}, or {@link Long#MAX_VALUE} when that product does not fit in a long. */
    private static long doubled(long recommended) {
        if (recommended <= 0) return 0;
        return recommended > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : recommended * 2;
    }

    default boolean hasEnergySurface(Direction side) { return true; }

    default long doInject(String energy, Direction side, long size, long amount, boolean doInject) { return 0; }

    default long doExtract(String energy, Direction side, long size, long amount, boolean doExtract) { return 0; }

    default boolean isEnergyCapacitorType(String energy, Direction side) { return false; }

    default long getEnergyStored(String energy, Direction side) { return 0; }

    default long getEnergyCapacity(String energy, Direction side) { return 0; }

    default Collection<String> getEnergyCapacitorTypes(Direction side) { return List.of(); }
}
