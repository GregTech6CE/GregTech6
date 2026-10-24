package gregtech.recipe;

import gregtech.content.multiblock.StructureLayoutOverlayRuntime;
import gregtech.network.PacketSyncStructureLayouts;
import net.minecraft.server.level.ServerPlayer;

//? if >=1.21.1 {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} else {
import gregtech.GregTech;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
//?}

/**
 * Applies addon structure layouts after tags bind, then sends them to clients.
 * A formed machine reads the layout again on its next structure check.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID)
//?}
public final class StructureLayoutLifecycle {
    private StructureLayoutLifecycle() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void tags(TagsUpdatedEvent event) {
        if (!event.shouldUpdateStaticData()) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        StructureLayoutOverlayRuntime.apply(server);
        gregtech.internal.multiblock.tracking.StructureEpoch.bump();
        PacketSyncStructureLayouts.send(server);
    }

    /** Integrated startup binds tags before the server handle exists. {@code /reload} uses {@link #tags}. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void started(ServerStartedEvent event) {
        StructureLayoutOverlayRuntime.apply(event.getServer());
        gregtech.internal.multiblock.tracking.StructureEpoch.bump();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PacketSyncStructureLayouts.send(player);
    }
}
