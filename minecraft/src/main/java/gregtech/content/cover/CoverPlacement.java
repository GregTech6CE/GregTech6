package gregtech.content.cover;

import gregtech.engine.tool.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** A full targeting cube for cover installation and wrenching, never physical collision. */
public final class CoverPlacement {
    private CoverPlacement() {}

    public static ItemStack held(Player player) {
        return player.getMainHandItem().isEmpty() ? player.getOffhandItem() : player.getMainHandItem();
    }

    /** Whether the player is using the cover placement gesture; {@code covers.require_sneaking} decides. */
    public static boolean placing(Player player) {
        return !gregtech.GregTechConfig.Rule.COVER_SNEAK.enabled() || player.isShiftKeyDown();
    }

    public static boolean coverGrid(BlockGetter level, BlockPos pos, Player player) {
        if (level == null) return false;
        ItemStack stack = held(player);
        boolean previewing = placing(player) && CoverItems.isCover(stack);
        boolean removing = GTToolHelper.matchesTool(stack, GTToolType.CROWBAR);
        if (!previewing && !removing) return false;
        return level.getBlockEntity(pos) instanceof PanelCoverHost host
                // Existing covers stay reachable for crowbar removal even on hosts which no longer admit them.
                && (previewing ? CoverAdmission.allowAnyCover(host.coverOwner()) : host.hasAttachedCovers());
    }

    /**
     * Whether the cell the player is aiming at would really accept {@code held}. The face overlay
     * colours the grid with this, so occupied slots, refused host faces and covers the host cannot
     * support show as invalid before the click.
     */
    public static boolean canPlace(BlockGetter level, BlockPos pos, Direction side, ItemStack held) {
        if (level == null || side == null || held.isEmpty()) return false;
        if (!(level.getBlockEntity(pos) instanceof PanelCoverHost host)) return false;
        if (gregtech.content.logistics.LogisticsCoverType.of(held) != null)
            return host.coverOwner() instanceof gregtech.content.logistics.LogisticsCoverHost logistics
                    && logistics.canLogistics(side)
                    && logistics.logisticsCovers().get(side).isEmpty()
                    && host.getCover(side).isEmpty();
        return CoverItems.isCover(held)
                && CoverAdmission.allowCover(host.coverOwner(), side, held)
                && host.getCover(side).isEmpty()
                && host.panels().canAttach(side, held);
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
