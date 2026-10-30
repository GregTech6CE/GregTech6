/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

import gregtech.api.energy.EnergyInput;
import gregtech.api.machine.MachineFace;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/** Shared auxiliary-energy declaration checks for the recipe and behavior templates. */
@ApiStatus.Internal
public final class AuxiliaryEnergy {
    private AuxiliaryEnergy() {}

    /**
     * Checks {@code auxiliary} against {@code main} and returns the stored form.
     * Returns null when the machine declares no auxiliary input.
     *
     * @param kind {@code "Recipe"} or {@code "Behavior"}, used in the diagnostic
     * @param id the casing id, used in the diagnostic
     * @param main the resolved main input, or null when the machine declares none
     * @param auxiliary the auxiliary input as written, or null
     */
    public static @Nullable EnergyInput validate(String kind, String id, @Nullable EnergyInput main,
                                                 @Nullable EnergyInput auxiliary) {
        if (auxiliary == null) return null;
        if (auxiliary.dynamic())
            throw new IllegalArgumentException(kind + " machine '" + id + "' auxiliary energy cannot pick its type");
        if (!auxiliary.anyPacket() && !auxiliary.rangeSpecified())
            throw new IllegalArgumentException(kind + " machine '" + id
                    + "' auxiliary energy needs a packet range or anyPacket");
        if (auxiliary.perTick() < 1 || auxiliary.perTick() > AddonLimits.MAX_PER_TICK)
            throw new IllegalArgumentException("Invalid auxiliary energy per tick for " + id + ": "
                    + auxiliary.perTick());
        if (!auxiliary.capacitySpecified() || auxiliary.capacity() < 1)
            throw new IllegalArgumentException(kind + " machine '" + id
                    + "' auxiliary energy capacity must be positive");
        if (!auxiliary.facesSpecified() || auxiliary.faces().isEmpty())
            throw new IllegalArgumentException(kind + " machine '" + id + "' auxiliary energy needs a face");
        if (auxiliary.rangeSpecified()
                && (auxiliary.perTick() < auxiliary.rangeMin() || auxiliary.perTick() > auxiliary.rangeMax()))
            throw new IllegalArgumentException(kind + " machine '" + id
                    + "' auxiliary energy per tick must sit inside the packet range");
        if (main != null && auxiliary.type().equals(main.type())) {
            for (MachineFace face : auxiliary.faces())
                if (main.faces().contains(face))
                    throw new IllegalArgumentException(kind + " machine '" + id
                            + "' auxiliary energy overlaps an input face");
        }
        return auxiliary;
    }
}
