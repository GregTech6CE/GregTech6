/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import net.minecraft.core.BlockPos;

/** One energy part in a formed behavior-machine structure. */
public interface EnergyPart {
    BlockPos pos();

    /** Registry path of the part block, without the addon namespace. */
    String name();

    long stored();

    long capacity();

    /**
     * Adds as much of {@code amount} as the part can hold.
     * Returns how much was stored. Returns 0 when the part is not bound to this machine.
     */
    long charge(long amount);

    /**
     * Sets the packet this part pushes each tick and clears its stopped state.
     * A request above the declared maximum throws. GregTech does not clamp it.
     */
    void setOutput(long size, long amount);

    /** Stops emission until the next {@link #setOutput(long, long)}. */
    void stop();

    /** True when this part drains a condition each tick. An output part is false. */
    boolean condition();

    /** True when the last condition drain was paid. An output part is false. */
    boolean satisfied();
}
