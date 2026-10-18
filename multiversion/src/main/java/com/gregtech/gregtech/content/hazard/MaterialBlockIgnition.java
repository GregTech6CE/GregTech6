package com.gregtech.gregtech.content.hazard;

import com.gregtech.gregtech.block.MaterialBlockLike;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Native heat trigger and explosion boundary for the original PrefixBlock reactions. */
public final class MaterialBlockIgnition {
    private MaterialBlockIgnition() {}
    private record Blast(BlockPos pos, float power) {}
    private static final java.util.Map<Level, java.util.ArrayDeque<Blast>> CHAINS = new java.util.IdentityHashMap<>();
    private static boolean hot(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            var neighbor = level.getBlockState(pos.relative(side));
            if (neighbor.is(BlockTags.FIRE) || neighbor.getFluidState().is(FluidTags.LAVA)) return true;
        }
        return false;
    }
    public static void schedule(Level level, BlockPos pos, BlockState state, MaterialBlockLike block) {
        if (!level.isClientSide && MaterialBlockHazards.ignitionPower(block.prefix(), block.material()) > 0 && hot(level, pos))
            level.scheduleTick(pos, state.getBlock(), 2);
    }
    public static boolean tick(Level level, BlockPos pos, MaterialBlockLike block) {
        float power = MaterialBlockHazards.ignitionPower(block.prefix(), block.material());
        if (power <= 0 || !hot(level, pos)) return false;
        detonate(level, pos, power); return true;
    }
    public static void detonate(Level level, BlockPos pos, float power) {
        if (level.isClientSide || power <= 0 || level.getBlockState(pos).isAir()) return;
        // Remove the reacting container before callbacks so a chain cannot explode it twice.
        level.removeBlock(pos, false);
        var queue = CHAINS.get(level);
        if (queue != null) { queue.addLast(new Blast(pos.immutable(), power)); return; }
        queue = new java.util.ArrayDeque<>(); CHAINS.put(level, queue);
        queue.addLast(new Blast(pos.immutable(), power));
        try {
            // Source catches recursive stack overflow. Process the same uncapped chain without recursion.
            while (!queue.isEmpty()) {
                var blast = queue.removeFirst(); var target = blast.pos();
                level.explode(null, target.getX() + .5, target.getY() + .5, target.getZ() + .5,
                        blast.power(), true, Level.ExplosionInteraction.TNT);
            }
        } finally { CHAINS.remove(level); }
    }
}
