package com.gregtech.gregtech.api.multiblock.tracking;

import com.gregtech.gregtech.content.multiblock.PlainStructureBlocks;
import com.gregtech.gregtech.block.OreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/** Compares the part of a block change that a structure cell actually reads. */
@FunctionalInterface
public interface Projection {
    boolean equivalent(Level level, BlockPos pos, BlockState oldState, BlockState newState);

    Projection BLOCK = (level, pos, oldState, newState) -> oldState.getBlock() == newState.getBlock();

    Projection AIR = (level, pos, oldState, newState) -> oldState.isAir() == newState.isAir();

    /** Controller facing only. Running, lit, rotor and crucible visuals do not invalidate. */
    Projection FACING = (level, pos, oldState, newState) -> facing(oldState) == facing(newState);

    static Projection opaque() {
        return (level, pos, oldState, newState) ->
                oldState.isSolidRender(level, pos) == newState.isSolidRender(level, pos);
    }

    static Projection fluid() {
        return (level, pos, oldState, newState) -> sameFluid(oldState.getFluidState(), newState.getFluidState());
    }

    static Projection plain(String name) {
        if ("anyopaque".equals(name)) return opaque();
        if (name != null && name.startsWith("anyfluid")) return (level, pos, oldState, newState) -> {
            boolean oldMatch = PlainStructureBlocks.matches(level, pos, name, oldState);
            boolean newMatch = PlainStructureBlocks.matches(level, pos, name, newState);
            if (oldMatch != newMatch) return false;
            return !oldMatch || sameFluid(oldState.getFluidState(), newState.getFluidState());
        };
        return (level, pos, oldState, newState) ->
                PlainStructureBlocks.matches(level, pos, name, oldState)
                        == PlainStructureBlocks.matches(level, pos, name, newState);
    }

    /** Only floor membership is structural; the drill reads ore identity at the start of each cycle. */
    static Projection bedrockFloor() {
        return (level, pos, oldState, newState) -> bedrock(oldState) == bedrock(newState);
    }

    static Projection and(Projection left, Projection right) {
        return (level, pos, oldState, newState) ->
                left.equivalent(level, pos, oldState, newState) && right.equivalent(level, pos, oldState, newState);
    }

    private static boolean sameFluid(FluidState oldFluid, FluidState newFluid) {
        if (oldFluid.isEmpty() || newFluid.isEmpty()) return oldFluid.isEmpty() && newFluid.isEmpty();
        return source(oldFluid.getType()) == source(newFluid.getType());
    }

    /** Source and flowing states are one fluid. Level is not part of the comparison. */
    private static Fluid source(Fluid fluid) {
        return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    }

    private static Object facing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) return state.getValue(BlockStateProperties.FACING);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return null;
    }

    private static boolean bedrock(BlockState state) {
        return state.is(Blocks.BEDROCK) || state.getBlock() instanceof OreBlock && OreBlock.isBedrockOre(state);
    }
}
