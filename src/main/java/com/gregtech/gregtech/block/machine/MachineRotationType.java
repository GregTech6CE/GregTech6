package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.util.GTPlacementCode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * GT6 wrench rotation policy: clicking a 3×3 grid position on a block face rotates the machine
 * to the corresponding direction. Uses {@link GTPlacementCode#getSideWrenching} which maps the
 * exact click position to a target direction.
 */
public enum MachineRotationType {
    /** 4-way: only horizontal directions (N/S/E/W) are valid. Clicking the center maps to UP/DOWN → ignored. */
    HORIZONTAL {
        @Override
        public boolean isValid(Direction facing) {
            return facing.getAxis().isHorizontal();
        }
    },
    /** 6-way: all directions are valid. */
    ALL {
        @Override
        public boolean isValid(Direction facing) {
            return true;
        }
    };

    /** Returns {@code true} if the target direction is allowed by this rotation policy. */
    public abstract boolean isValid(Direction facing);

    /**
     * Handle wrench rotation for any machine block. Computes the target direction from the
     * clicked position on the 3×3 grid, validates it against the rotation policy, and applies
     * the rotation if the target differs from the current facing.
     *
     * @return {@code true} if the wrench was handled (caller should return success).
     */
    public static boolean handleWrench(BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit,
                                       DirectionProperty facingProperty, MachineRotationType rotationType) {
        if (!GTToolHelper.isMachineWrench(player.getItemInHand(hand))) return false;
        var declared = ToolInteractions.describe(state, player.getItemInHand(hand));
        if (state.getBlock() instanceof ToolInteractionTarget && declared == null) return false;
        return ToolInteractions.use(
                declared != null ? declared : ToolInteractionSpec.facing(facingProperty, rotationType),
                state, level, pos, player, hand, hit);
    }
}
