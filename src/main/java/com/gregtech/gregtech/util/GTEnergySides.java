package com.gregtech.gregtech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

/** GT6 byte-side conventions mapped to modern {@link Direction}. */
public final class GTEnergySides {
    private GTEnergySides() {}

    public static final Set<Direction> ALL = EnumSet.allOf(Direction.class);

    /** GT6 {@code SIDE_ANY}: skip side checks when {@code null}. */
    @Nullable
    public static Direction anySide(@Nullable Direction side) {
        return side;
    }

    public static boolean isAnySide(@Nullable Direction side) {
        return side == null;
    }

    public static boolean matches(@Nullable Direction required, @Nullable Direction actual) {
        return required == null || required == actual;
    }

    public static BlockPos offset(BlockPos pos, Direction direction) {
        return pos.relative(direction);
    }

    @Nullable
    public static BlockEntity getNeighbor(Level level, BlockPos pos, Direction from) {
        return level.getBlockEntity(offset(pos, from));
    }

    /** Side of the receiver block that faces the emitter. */
    public static Direction receiverSide(Direction emitterToReceiver) {
        return emitterToReceiver.getOpposite();
    }

    public static Iterable<Direction> allBut(@Nullable Direction excluded) {
        if (excluded == null) {
            return ALL;
        }
        return () -> ALL.stream().filter(d -> d != excluded).iterator();
    }
}
