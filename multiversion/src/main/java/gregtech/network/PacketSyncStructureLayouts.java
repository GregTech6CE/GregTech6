package gregtech.network;

import gregtech.content.multiblock.StructureLayoutOverlayRuntime;

import java.util.Map;

//? if >=1.21.1 {
/*import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/^* Server to client: addon structure layouts that replace the startup baseline. ^/
public record PacketSyncStructureLayouts(Map<String, String> layouts) implements CustomPacketPayload {
    public static final Type<PacketSyncStructureLayouts> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("gt6ce", "sync_structure_layouts"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSyncStructureLayouts> STREAM_CODEC = StreamCodec.of(PacketSyncStructureLayouts::encode, PacketSyncStructureLayouts::decode);

    private static void encode(RegistryFriendlyByteBuf buf, PacketSyncStructureLayouts packet) { StructureLayoutSync.write(buf, packet.layouts); }

    private static PacketSyncStructureLayouts decode(RegistryFriendlyByteBuf buf) { return new PacketSyncStructureLayouts(StructureLayoutSync.read(buf)); }

    @Override public Type<PacketSyncStructureLayouts> type() { return TYPE; }

    public void apply() { gregtech.client.StructureLayoutClient.accept(this); }

    public static void send(ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new PacketSyncStructureLayouts(StructureLayoutOverlayRuntime.documents()));
    }

    public static void send(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player);
    }
}
*///?} else {
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Server to client: addon structure layouts that replace the startup baseline. */
public record PacketSyncStructureLayouts(Map<String, String> layouts) {
    public static void encode(PacketSyncStructureLayouts packet, net.minecraft.network.FriendlyByteBuf buf) {
        StructureLayoutSync.write(buf, packet.layouts);
    }

    public static PacketSyncStructureLayouts decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new PacketSyncStructureLayouts(StructureLayoutSync.read(buf));
    }

    public static void handle(PacketSyncStructureLayouts packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> gregtech.client.StructureLayoutClient.accept(packet)));
        ctx.get().setPacketHandled(true);
    }

    public static void send(ServerPlayer player) {
        GTPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncStructureLayouts(StructureLayoutOverlayRuntime.documents()));
    }

    public static void send(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player);
    }
}
//?}
