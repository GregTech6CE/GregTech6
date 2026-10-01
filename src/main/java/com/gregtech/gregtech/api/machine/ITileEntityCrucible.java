package com.gregtech.gregtech.api.machine;

/**
 * GT6 {@code gregapi.tileentity.machines.ITileEntityCrucible} — implemented by any tile entity
 * that can contain and pour molten material into molds (smelting crucible, multi-block crucible,
 * and crossing).
 */
public interface ITileEntityCrucible {
    /**
     * Attempt to fill a mold from this crucible on the given side.
     *
     * @param mold         the target mold to fill
     * @param crucibleSide the side of this crucible the pour originates from
     * @param moldSide     the side of the mold receiving the pour
     * @return true if material was transferred
     */
    boolean fillMoldAtSide(ITileEntityMold mold, int crucibleSide, int moldSide);

    /** Current internal temperature in Kelvin. */
    long getCrucibleTemperature();

    /** Total amount of material (in GT units) currently stored. */
    long getCrucibleContentAmount();
}
