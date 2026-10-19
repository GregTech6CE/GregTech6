package com.gregtech.gregtech.api.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

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

    default boolean supportsMode() {
        return false;
    }

    default int mode() {
        return 0;
    }

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

    static MachineControl find(BlockEntity entity, Direction side) {
        return entity instanceof Provider provider && !entity.isRemoved() ? provider.machineControl(side) : null;
    }
}
