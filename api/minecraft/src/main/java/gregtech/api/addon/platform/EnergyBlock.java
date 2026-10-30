/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import gregtech.api.energy.EnergyType;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Energy block an addon implements. {@code energy} is an {@link EnergyType}, such as
 * {@link EnergyType#EU}. A null side means every side. Packet model: {@code size} times {@code amount}.
 */
public interface EnergyBlock {
    boolean isEnergyType(EnergyType energy, @Nullable Direction side, boolean emitting);

    Collection<EnergyType> getEnergyTypes(@Nullable Direction side);

    default boolean isEnergyAcceptingFrom(EnergyType energy, @Nullable Direction side, boolean theoretical) {
        return isEnergyType(energy, side, false) && hasEnergySurface(side);
    }

    default boolean isEnergyEmittingTo(EnergyType energy, @Nullable Direction side, boolean theoretical) {
        return isEnergyType(energy, side, true) && hasEnergySurface(side);
    }

    /** Push-based injection. The default applies GregTech's packet gate and then {@link #doInject}. */
    default long doEnergyInjection(EnergyType energy, @Nullable Direction side, long size, long amount, boolean doInject) {
        return AddonEnergy.inject(this, energy, side, size, amount, doInject);
    }

    long getEnergyDemanded(EnergyType energy, @Nullable Direction side, long size);

    default long doEnergyExtraction(EnergyType energy, @Nullable Direction side, long size, long amount, boolean doExtract) {
        return AddonEnergy.extract(this, energy, side, size, amount, doExtract);
    }

    long getEnergyOffered(EnergyType energy, @Nullable Direction side, long size);

    long getEnergySizeInputRecommended(EnergyType energy, @Nullable Direction side);

    long getEnergySizeOutputRecommended(EnergyType energy, @Nullable Direction side);

    default long getEnergySizeInputMin(EnergyType energy, @Nullable Direction side) {
        long recommended = getEnergySizeInputRecommended(energy, side);
        return recommended <= 0 ? 0 : Math.max(1, recommended / 2);
    }

    default long getEnergySizeInputMax(EnergyType energy, @Nullable Direction side) {
        return doubled(getEnergySizeInputRecommended(energy, side));
    }

    default long getEnergySizeOutputMin(EnergyType energy, @Nullable Direction side) {
        long recommended = getEnergySizeOutputRecommended(energy, side);
        return recommended <= 0 ? 0 : Math.max(1, recommended / 2);
    }

    default long getEnergySizeOutputMax(EnergyType energy, @Nullable Direction side) {
        return doubled(getEnergySizeOutputRecommended(energy, side));
    }

    /** Twice {@code recommended}, or {@link Long#MAX_VALUE} when that product does not fit in a long. */
    private static long doubled(long recommended) {
        if (recommended <= 0) return 0;
        return recommended > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : recommended * 2;
    }

    default boolean hasEnergySurface(@Nullable Direction side) { return true; }

    default long doInject(EnergyType energy, @Nullable Direction side, long size, long amount, boolean doInject) { return 0; }

    default long doExtract(EnergyType energy, @Nullable Direction side, long size, long amount, boolean doExtract) { return 0; }

    default boolean isEnergyCapacitorType(EnergyType energy, @Nullable Direction side) { return false; }

    default long getEnergyStored(EnergyType energy, @Nullable Direction side) { return 0; }

    default long getEnergyCapacity(EnergyType energy, @Nullable Direction side) { return 0; }

    default Collection<EnergyType> getEnergyCapacitorTypes(@Nullable Direction side) { return List.of(); }
}
