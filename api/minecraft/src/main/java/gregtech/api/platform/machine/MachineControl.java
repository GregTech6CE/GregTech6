/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.platform.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Optional machine controls. Relays resolve the target on every operation, never cache a machine.
 * This boundary currently exposes running/active state but has no explicit work-possible
 * or just-completed state; callers must not infer full original detector support from it.
 * <p>
 * Capability bits: {@link #available()} reports whether controls work at all this tick.
 * {@link #supportsSwitch()} and {@link #supportsProgress()} default to true for machines that
 * expose the matching state; an implementation without that state should override them to false.
 * {@link #supportsMode()} defaults to false. Callers must check the matching bit instead of
 * inferring support from {@link #available()}. Progress and energy are read-only on this surface.
 */
public interface MachineControl {
    default boolean available() {
        return true;
    }

    default boolean supportsSwitch() {
        return true;
    }

    default boolean supportsProgress() {
        return true;
    }

    /** Buffered energy in the machine's own unit; 0 when it has no buffer or is unavailable. */
    default long energy() {
        return 0;
    }

    /** Ceiling {@link #energy()} is capped at; 0 when the machine does not report one. */
    default long energyCapacity() {
        return 0;
    }

    /** Work the next active tick adds to {@link #progress()}; 0 while idle. */
    default long progressPerTick() {
        return 0;
    }

    /** External control signal on {@code side}, 0–15. It is combined with that face's cover signal. */
    default int externalSignal(Direction side) {
        return 0;
    }

    /**
     * Control input, not a state write: accepted regardless of the read-only progress and energy state.
     * Returns whether the value was accepted; a machine may clamp it to 0–15.
     */
    default boolean setExternalSignal(Direction side, int level) {
        return false;
    }

    default boolean supportsMode() {
        return false;
    }

    default int mode() {
        return 0;
    }

    /**
     * Applies {@code mode} when {@link #supportsMode()} is true.
     * Returns the mode now in effect, which may be clamped. Returns 0 when modes are not supported.
     */
    default int setMode(int mode) {
        return 0;
    }

    boolean enabled();

    boolean setEnabled(boolean enabled);

    boolean running();

    boolean active();

    long progress();

    long progressMax();

    interface Provider {
        /** Null means that this face has no control interface. */
        @Nullable MachineControl machineControl(Direction side);
    }

    @Nullable
    static MachineControl find(BlockEntity entity, Direction side) {
        return entity instanceof Provider provider && !entity.isRemoved() ? provider.machineControl(side) : null;
    }
}
