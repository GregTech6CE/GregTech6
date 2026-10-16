package com.gregtech.gregtech.api.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;

/** The pipe's WD.burn(..., false, false), separate from player igniter permissions. */
public final class PipeIgnition {
    private PipeIgnition() {}

    /** Integration hook for externally owned blocks such as protected pollution/magic nodes. */
    public static final TagKey<Block> PROTECTED = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("gregtech", "pipe_fire_protected"));

    public static void igniteNeighbors(Level level, BlockPos source) {
        for (Direction direction : Direction.values()) {
            BlockPos target = source.relative(direction);
            var state = level.getBlockState(target);
            Block block = state.getBlock();
            boolean excluded = state.getFluidState().is(FluidTags.LAVA)
                    || block instanceof BaseFireBlock || state.is(PROTECTED);
            if (excluded) continue;
            boolean eligible = block instanceof CarpetBlock || state.getCollisionShape(level, target).isEmpty();
            if (!eligible) continue;
            // Modern directions have no UNKNOWN face. Check each face for any flammability.
            boolean flammable = false;
            for (Direction face : Direction.values()) {
                if (block.getFlammability(state, level, target, face) > 0) { flammable = true; break; }
            }
            // Original GT block families implement IItemGT. The port registers these under gregtech.
            boolean gtBlock = BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("gregtech");
            if (FluidPipeSafety.canIgnite(false, true, gtBlock, flammable)) {
                level.setBlock(target, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }
}
