/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.machine.BlockFace;

/**
 * The addon-facing target of a molten-material pour.
 *
 * <p>This is the SDK view of GT6CE's internal {@code ITileEntityMold}. GregTech
 * adapts it when a behavior machine is called by a real mold, basin or faucet.
 * The target validates the receiving side, temperature and material rules.</p>
 */
@FunctionalInterface
public interface CrucibleMold {
    /**
     * Offers molten material to this mold.
     *
     * @param material    GregTech material to pour; an invalid material is rejected
     * @param amount      available material amount in GT units
     * @param temperature temperature of the material in Kelvin
     * @param side        absolute face of the mold receiving the pour.
     *                    {@code MachineBehavior.fillMoldAtSide} receives a {@code Direction};
     *                    the host maps it to {@link BlockFace}
     * @return amount actually consumed
     */
    long fillMold(GregTechMaterials.Material material, long amount, long temperature, BlockFace side);
}
