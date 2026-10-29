/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;


import gregtech.api.addon.material.*;





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
     * @param side        {@code Direction.ordinal()} of the mold face receiving the pour.
     *                    This core type cannot name {@code Direction}.
     *                    {@code MachineBehavior.fillMoldAtSide} receives a {@code Direction};
     *                    pass {@code side.ordinal()} here
     * @return amount actually consumed
     */
    long fillMold(GregTechMaterials.Material material, long amount, long temperature, int side);
}
