package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.data.GregTechTags;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * GregTech energy block API (GT6 {@code ITileEntityEnergy}).
 * <p>
 * Packet model: {@code size} × {@code amount} (e.g. EU voltage × amperage, HU temperature × 1).
 * Use {@code null} {@link net.minecraft.core.Direction} as GT6 {@code SIDE_ANY}.
 */
public interface IEnergyBlock {

    boolean isEnergyType(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, boolean emitting);

    Collection<GregTechTags.Tag> getEnergyTypes(@Nullable net.minecraft.core.Direction side);

    boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, boolean theoretical);

    boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, boolean theoretical);

    /** Push-based injection. Implementor must check {@link #isEnergyAcceptingFrom}. */
    long doEnergyInjection(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side,
                           long size, long amount, boolean doInject);

    long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, long size);

    /** Pull-based extraction. Implementor must check {@link #isEnergyEmittingTo}. */
    long doEnergyExtraction(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side,
                            long size, long amount, boolean doExtract);

    long getEnergyOffered(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, long size);

    long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    /** Whether this side participates in energy connections (GT6 attachable surface). */
    default boolean hasEnergySurface(@Nullable net.minecraft.core.Direction side) {
        return side == null || true;
    }

    default boolean isEnergyCapacitorType(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side) {
        return false;
    }

    default long getEnergyStored(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side) {
        return 0;
    }

    default long getEnergyCapacity(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side) {
        return 0;
    }

    default Collection<GregTechTags.Tag> getEnergyCapacitorTypes(@Nullable net.minecraft.core.Direction side) {
        return Collections.emptyList();
    }

    /** Subclass hook: store injected energy. Called from {@link #doEnergyInjection} after validation. */
    default long doInject(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side,
                          long size, long amount, boolean doInject) {
        return 0;
    }

    /** Subclass hook: remove extracted energy. */
    default long doExtract(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side,
                           long size, long amount, boolean doExtract) {
        return 0;
    }
}
