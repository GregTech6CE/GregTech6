package com.gregtech.gregtech.content.tool;

import net.minecraft.core.Direction;

/** GT6 uses one top recipe map and two side bending maps. */
public final class AnvilRules {
    public enum Surface { TOP, SMALL, BIG }
    private AnvilRules() {}
    public static Surface surface(Direction facing, Direction side, double half) {
        if (side == Direction.UP) return Surface.TOP;
        if (!facing.getAxis().isHorizontal() || !side.getAxis().isHorizontal()) throw new IllegalArgumentException("Anvil face");
        boolean small = side.getAxis() == facing.getAxis()
                ? (facing == Direction.NORTH || facing == Direction.WEST ? half < .5 : half > .5)
                : side == switch (facing) { case WEST -> Direction.NORTH; case EAST -> Direction.SOUTH; case NORTH -> Direction.WEST; default -> Direction.EAST; };
        return small ? Surface.SMALL : Surface.BIG;
    }
    public static long wear(long energyPerTick, long duration, int fatigueLevel, int hasteLevel) {
        // BigInteger keeps high-tier recipe energy and potion multipliers from overflowing.
        var power = java.math.BigInteger.valueOf(energyPerTick).abs().multiply(java.math.BigInteger.valueOf(Math.max(1, duration)));
        var base = power.add(java.math.BigInteger.valueOf(3)).divide(java.math.BigInteger.valueOf(4)).max(java.math.BigInteger.valueOf(10000));
        var divisor = java.math.BigInteger.valueOf(1L + Math.max(0, hasteLevel));
        return base.multiply(java.math.BigInteger.valueOf(1L + Math.max(0, fatigueLevel)))
                .add(divisor.subtract(java.math.BigInteger.ONE)).divide(divisor).min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
}
