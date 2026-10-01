package com.gregtech.gregtech.api.tool;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** A block declares its tool operation once; interaction and client preview use this declaration. */
public interface ToolInteractionTarget {
    ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool);
    /** Read-only permission shared by the tool overlay and the server interaction. */
    default boolean canUseTool(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                               net.minecraft.world.entity.player.Player player, ItemStack tool) { return true; }
    /** Server-only hook for state such as first-use ownership, after feasibility checks pass. */
    default boolean beginToolUse(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                                 net.minecraft.world.entity.player.Player player, ItemStack tool) { return true; }
    default void toolStateChanged(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, BlockState state) {}
    default void toolStateChanged(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, BlockState state,ToolInteractionSpec operation) {toolStateChanged(level,pos,state);}
}
