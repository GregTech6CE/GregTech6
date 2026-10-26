package gregtech.content.cover;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import gregtech.engine.tool.*;

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
                // Loose piles consume plates as contents, never as machine covers.
                && !(host.coverOwner() instanceof gregtech.content.blockentity.misc.PileBlockEntity)
                && (ComponentCoverFallback.ownClass(host.coverOwner()) || ComponentCoverFallback.eligible(host.coverOwner()))
                && (placing || host.hasAttachedCovers());
    }
    public static boolean fullSelection(BlockGetter level, BlockPos pos, BlockState state, Player player) {
        return coverGrid(level, pos, player) || ToolInteractions.describe(state, held(player)) != null;
    }
    public static int attachedFaces(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PanelCoverHost host)) return 0;
        int faces = 0;
        for (var side : net.minecraft.core.Direction.values()) {
            boolean attached = !host.getCover(side).isEmpty();
            if (host.coverOwner() instanceof gregtech.content.logistics.LogisticsCoverHost logistics)
                attached |= !logistics.logisticsCovers().get(side).isEmpty();
            if (attached) faces |= 1 << side.ordinal();
        }
        return faces;
    }
}
