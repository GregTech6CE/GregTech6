package com.gregtech.gregtech.api.tool;

import com.gregtech.gregtech.util.GTPlacementCode;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared tool face selection, validation and execution. Client overlays consume the same spec. */
public final class ToolInteractions {
    private ToolInteractions() {}

    public static Direction selectedFace(BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        // Subtract in double precision before narrowing: essential near the world border.
        return GTPlacementCode.getSideWrenching(hit.getDirection(),
                (float) (hit.getLocation().x - pos.getX()),
                (float) (hit.getLocation().y - pos.getY()),
                (float) (hit.getLocation().z - pos.getZ()));
    }

    public static ToolInteractionSpec describe(BlockState state, ItemStack tool) {
        return state.getBlock() instanceof ToolInteractionTarget target ? target.toolInteraction(state, tool) : null;
    }

    /** Read-only feasibility check used on both sides, including neighbor axle constraints. */
    public static boolean canApply(ToolInteractionSpec spec, BlockState state, Level level, BlockPos pos,
                                   Player player, ItemStack tool, Direction side) {
        if (tool.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric
                && !electric.canInteract(tool)) return false;
        if (!spec.allows(state, side) || !player.mayBuild() || !level.mayInteract(player, pos)) return false;
        if (state.getBlock() instanceof ToolInteractionTarget target && !target.canUseTool(level, pos, player, tool)) return false;
        if (spec.connection() == null) return true;
        BlockPos otherPos = pos.relative(side);
        if (!level.hasChunkAt(otherPos) || !level.mayInteract(player, otherPos)) return false;
        BlockState other = level.getBlockState(otherPos);
        var otherSpec = describe(other, tool);
        return otherSpec == null || otherSpec.connection() != spec.connection()
                || spec.connection() != ToolInteractionSpec.ConnectionKind.AXLE
                || com.gregtech.gregtech.block.energy.AxleBlock.isValidConnectionState(other.setValue(
                    spec.connection().property(side.getOpposite()), !state.getValue(spec.connection().property(side))));
    }

    public static boolean use(BlockState state, Level level, BlockPos pos, Player player,
                              InteractionHand hand, BlockHitResult hit) {
        var spec = describe(state, player.getItemInHand(hand));
        return spec != null && use(spec, state, level, pos, player, hand, hit);
    }

    public static boolean use(ToolInteractionSpec spec, BlockState state, Level level, BlockPos pos,
                              Player player, InteractionHand hand, BlockHitResult hit) {
        Direction side = selectedFace(hit);
        if (!canApply(spec, state, level, pos, player, player.getItemInHand(hand), side)) return true;
        if (level.isClientSide) return true;
        if (state.getBlock() instanceof ToolInteractionTarget target
                && !target.beginToolUse(level, pos, player, player.getItemInHand(hand))) return true;
        BlockState next;
        if (spec.connection() == null) {
            if (state.getValue(spec.facing()) == side) {
                if(state.getBlock() instanceof ToolInteractionTarget target)target.toolStateChanged(level,pos,state,spec);
                // GT6 facing clicks return 10000 even when selecting the current valid side.
                if (player.getItemInHand(hand).getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem)
                    GTToolHelper.damageForToolClickReturn(player.getItemInHand(hand), 10000, player);
                return true;
            }
            next = state.setValue(spec.facing(), side);
        } else {
            BlockPos otherPos = pos.relative(side);
            var property = spec.connection().property(side);
            boolean connect = !state.getValue(property);
            next = state.setValue(property, connect);
            BlockState other = level.getBlockState(otherPos);
            var otherSpec = describe(other, player.getItemInHand(hand));
            boolean matching = otherSpec != null && otherSpec.connection() == spec.connection();
            if (!level.setBlockAndUpdate(pos, next)) return true;
            if (matching) {
                level.setBlockAndUpdate(otherPos, other.setValue(spec.connection().property(side.getOpposite()), connect));
                changed(level, otherPos);
            }
            changed(level, pos);
        }
        if (spec.connection() == null && !level.setBlockAndUpdate(pos, next)) return true;
        if (state.getBlock() instanceof ToolInteractionTarget target) target.toolStateChanged(level, pos, next,spec);
        level.playSound(null, pos, com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        GTToolHelper.damageForUse(player.getItemInHand(hand), 1, player);
        return true;
    }

    private static void changed(Level level, BlockPos pos) {
        level.invalidateCapabilities(pos);
        if (level.getBlockEntity(pos) instanceof ElectricWireBlockEntity wire) wire.onConnectionChanged();
        if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.energy.AxleBlockEntity axle) axle.onConnectionChanged();
    }
}
