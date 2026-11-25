package gregtech.client;

import gregtech.content.multiblock.StructureLayoutOverlayRuntime;
import gregtech.network.PacketSyncStructureLayouts;

/** Applies a synced layout overlay and asks recipe viewers to rebuild structure pages. */
public final class StructureLayoutClient {
    private StructureLayoutClient() {}

    public static void accept(PacketSyncStructureLayouts packet) {
        // Login also sends an empty/unchanged overlay. Rebuilding every recipe for that packet
        // duplicates EMI's normal recipe/tag synchronization and allocates another working index.
        if (StructureLayoutOverlayRuntime.documents().equals(packet.layouts())) return;
        StructureLayoutOverlayRuntime.applySynced(packet.layouts());
        refresh();
    }

    public static void refresh() {
        gregtech.content.multiblock.ControllerStructureLayouts.invalidate();
        RecipeViewerRefresh.refresh();
    }
}
