package gregtech.engine.energy;

import gregtech.content.data.GregTechTags;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;

/**
 * GregTech energy block API (GT6 {@code ITileEntityEnergy}).
 * <p>
 * Packet model: {@code size} × {@code amount} (e.g. EU voltage × amperage, HU temperature × 1).
 * Use {@code null} {@link net.minecraft.core.Direction} as GT6 {@code SIDE_ANY}.
 */
public interface IEnergyBlock {

    default boolean isFireProof(net.minecraft.core.Direction side) {
        return false;
    }

    default boolean isWaterProof(net.minecraft.core.Direction side) {
        return false;
    }

    default boolean isRainProof(net.minecraft.core.Direction side) {
        return false;
    }

    default boolean isThunderProof(net.minecraft.core.Direction side) {
        return false;
    }

    boolean isEnergyType(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, boolean emitting);

    Collection<GregTechTags.Tag> getEnergyTypes(@Nullable net.minecraft.core.Direction side);

    boolean isEnergyAcceptingFrom(
            GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, boolean theoretical);

    boolean isEnergyEmittingTo(
            GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, boolean theoretical);

    /** Push-based injection. Implementor must check {@link #isEnergyAcceptingFrom}. */
    long doEnergyInjection(
            GregTechTags.Tag energyType,
            @Nullable net.minecraft.core.Direction side,
            long size,
            long amount,
            boolean doInject);

    long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, long size);

    /** Pull-based extraction. Implementor must check {@link #isEnergyEmittingTo}. */
    long doEnergyExtraction(
            GregTechTags.Tag energyType,
            @Nullable net.minecraft.core.Direction side,
            long size,
            long amount,
            boolean doExtract);

    long getEnergyOffered(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side, long size);

    long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side);

    /**
     * Whether this side participates in energy connections (GT6 attachable surface).
     * The default matches {@link gregtech.api.addon.platform.EnergyBlock}: every side,
     * including a null (any-side) query. Addon blocks implement {@code EnergyBlock} with a
     * {@code String} type; built-in blocks implement this interface with {@link GregTechTags.Tag}.
     * The two cannot share one class: {@code getEnergyTypes} erases to the same method.
     */
    default boolean hasEnergySurface(@Nullable net.minecraft.core.Direction side) {
        return true;
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

    /** Inspection hook for real undersized packets consumed by the gate without entering storage. */
    default void onInsufficientEnergy(GregTechTags.Tag energyType, @Nullable net.minecraft.core.Direction side) {}

    /** Subclass hook: store injected energy. Called from {@link #doEnergyInjection} after validation. */
    default long doInject(
            GregTechTags.Tag energyType,
            @Nullable net.minecraft.core.Direction side,
            long size,
            long amount,
            boolean doInject) {
        return 0;
    }

    /** Subclass hook: remove extracted energy. */
    default long doExtract(
            GregTechTags.Tag energyType,
            @Nullable net.minecraft.core.Direction side,
            long size,
            long amount,
            boolean doExtract) {
        return 0;
    }
}
