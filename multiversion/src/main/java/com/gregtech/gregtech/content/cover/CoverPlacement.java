package com.gregtech.gregtech.content.cover;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import com.gregtech.gregtech.api.tool.*;

/** A full targeting cube for cover installation and wrenching, never physical collision. */
public final class CoverPlacement {
    private CoverPlacement() {}
    public static ItemStack held(Player player) {
        return player.getMainHandItem().isEmpty() ? player.getOffhandItem() : player.getMainHandItem();
    }
    public static boolean coverGrid(BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = held(player);
        boolean placing = CoverItems.isCover(stack);
        boolean removing = GTToolHelper.matchesTool(stack, GTToolType.CROWBAR);
        if (!placing && !removing) return false;
        return level.getBlockEntity(pos) instanceof PanelCoverHost host
                && (ComponentCoverFallback.ownClass(host.coverOwner()) || ComponentCoverFallback.eligible(host.coverOwner()))
                && (placing || host.hasAttachedCovers());
    }
    public static boolean fullSelection(BlockGetter level, BlockPos pos, BlockState state, Player player) {
        return coverGrid(level, pos, player) || ToolInteractions.describe(state, held(player)) != null;
    }
}
