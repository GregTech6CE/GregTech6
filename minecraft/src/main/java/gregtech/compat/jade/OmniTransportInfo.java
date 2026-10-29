package gregtech.compat.jade;

import gregtech.content.blockentity.machine.ItemPipeBlockEntity;
import gregtech.content.blockentity.machine.LogisticsWireBlockEntity;
import gregtech.content.cover.*;
import gregtech.content.logistics.LogisticsCoverHost;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;

/** Inspect local pipes and face attachments without traversing or changing the network. */
final class OmniTransportInfo {
    private OmniTransportInfo() {}

    static void append(JadeInspection info, BlockEntity entity, BlockAccessor accessor) {
        Direction viewed = accessor.getHitResult().getDirection();
        var player = accessor.getPlayer();
        if (gregtech.GregTechConfig.Rule.COVER_SNEAK.enabled()
                && player != null
                && player.mayBuild()
                && entity instanceof PanelCoverHost host
                && entity.getLevel() != null
                && entity.getLevel().mayInteract(player, entity.getBlockPos())) {
            var held = CoverPlacement.held(player);
            var side = gregtech.engine.tool.ToolInteractions.selectedFace(accessor.getHitResult());
            if (CoverItems.isCover(held)
                    && CoverAdmission.allowCover(entity, side, held)
                    && host.getCover(side).isEmpty()
                    && host.panels().canAttach(side, held)) info.line("sneak_attach_cover");
        }
        if (entity instanceof ItemPipeBlockEntity pipe) {
            info.groupedInventory();
            info.inventory("contents", pipe, 0, pipe.getSlots());
        } else if (entity instanceof LogisticsWireBlockEntity wire) {
            int connections = 0;
            for (Direction face : Direction.values()) if (wire.canLogistics(face)) connections++;
            info.line("pipe_connections", connections);
            info.line(
                    "pipe_face",
                    side(viewed),
                    JadeInspection.label(wire.canLogistics(viewed) ? "connected" : "disconnected"));
        }
        // Put the pointed face first; name every installed cover, with settings for the pointed one.
        appendCover(info, entity, viewed, true);
        for (Direction face : Direction.values()) if (face != viewed) appendCover(info, entity, face, false);
    }

    private static void appendCover(JadeInspection info, BlockEntity entity, Direction face, boolean details) {
        ItemStack cover = entity instanceof PanelCoverHost host ? host.getCover(face) : ItemStack.EMPTY;
        if (cover.isEmpty() && entity instanceof LogisticsCoverHost host)
            cover = host.logisticsCovers().get(face);
        if (cover.isEmpty()) return;
        info.line("cover_face", side(face), cover);
        if (!details) return;
        for (var line : CoverDisplayInfo.describe(cover, entity.getLevel()))
            info.translated(line.key(), line.arguments());
        if (entity instanceof PanelCoverHost host) {
            info.flag("covers_enabled", !host.panels().stopped());
            if (PanelCover.of(cover) != null || MachineCoverSpec.of(cover) != null)
                info.line("redstone_signal", host.panels().signal(face));
        }
    }

    static JadeInspection.Label side(Direction face) {
        return new JadeInspection.Label("jade.gt6ce.direction." + face.getSerializedName());
    }
}
