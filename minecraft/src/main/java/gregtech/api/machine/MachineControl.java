/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * Optional machine controls. Relays resolve the target on every operation, never cache a machine.
 * This boundary currently exposes running/active state but has no explicit work-possible
 * or just-completed state; callers must not infer full original detector support from it.
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

    /**
     * Whether {@link #setProgress(long)} and {@link #setEnergy(long)} are honoured. Built-in
     * machines answer {@code false}: this surface is read-only until a later decision opens writes.
     * Callers must check this bit instead of inferring write support from {@link #available()}.
     */
    default boolean supportsWrite() {
        return false;
    }

    /** Does nothing unless {@link #supportsWrite()} and the machine's own checks allow the value. */
    default boolean setProgress(long value) {
        return false;
    }

    /** Does nothing unless {@link #supportsWrite()} and the machine's own checks allow the value. */
    default boolean setEnergy(long value) {
        return false;
    }

    /** External control signal on {@code side}, 0–15. It is combined with that face's cover signal. */
    default int externalSignal(Direction side) {
        return 0;
    }

    /**
     * Control input, not a state write: accepted even when {@link #supportsWrite()} is {@code false}.
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
        MachineControl machineControl(Direction side);
    }

    @Nullable
    static MachineControl find(BlockEntity entity, Direction side) {
        return entity instanceof Provider provider && !entity.isRemoved() ? provider.machineControl(side) : null;
    }
}
