package gregtech.compat.jade;

import gregtech.content.block.machine.ItemPipeBlock;
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
        if (entity instanceof ItemPipeBlockEntity pipe) {
            info.groupedInventory();
            info.line("pipe_capacity", pipe.spec().invSize());
            info.line("pipe_routing", pipe.spec().stepSize());
            info.line("pipe_transferred", pipe.getTransferredThisSecond());
            info.inventory("contents", pipe, 0, pipe.getSlots());
            info.line(
                    "pipe_face",
                    side(viewed),
                    JadeInspection.label(
                            pipe.getBlockState().getValue(ItemPipeBlock.propFor(viewed))
                                    ? "connected"
                                    : "disconnected"));
            info.flag("pipe_input", (pipe.getDisabledInputs() & (1 << viewed.ordinal())) == 0);
            info.flag("pipe_output", (pipe.getDisabledOutputs() & (1 << viewed.ordinal())) == 0);
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
