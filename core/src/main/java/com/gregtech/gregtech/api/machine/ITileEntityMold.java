package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;

/**
 * GT6 {@code gregapi.tileentity.machines.ITileEntityMold} — implemented by any tile entity
 * that can receive molten material and produce shaped output (mold, basin, faucet, smeltery).
 */
public interface ITileEntityMold {
    /** Whether the given side can accept molten material input. */
    boolean isMoldInputSide(int side);

    /** Maximum temperature (K) this mold can withstand before melting down. */
    long getMoldMaxTemperature();

    /** Amount of material units required to fully fill this mold. */
    long getMoldRequiredMaterialUnits();

    /**
     * Fill the mold with molten material.
     *
     * @param material    the material being poured
     * @param amount      available amount (units)
     * @param temperature temperature of the incoming material (K)
     * @param side        the side from which material enters
     * @return amount actually consumed, or 0 if the pour was rejected
     */
    long fillMold(GTMaterial material, long amount, long temperature, int side);

    /** Current amount of material stored in the mold (units). */
    long getMoldContentAmount();

    /** Material currently in the mold, or null if empty. */
    GTMaterial getMoldContentMaterial();
}
