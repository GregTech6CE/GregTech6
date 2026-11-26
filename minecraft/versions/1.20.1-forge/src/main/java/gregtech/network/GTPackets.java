package gregtech.network;

import gregtech.GregTech;
import java.util.Optional;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GTPackets {
    private GTPackets() {}

    private static final String PROTOCOL = "5";
    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(GregTech.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static final String ORE_GENERATION_PROTOCOL = "1";
    public static final SimpleChannel ORE_GENERATION = NetworkRegistry.newSimpleChannel(
            GregTech.id("ore_generation"),
            () -> ORE_GENERATION_PROTOCOL,
            NetworkRegistry.acceptMissingOr(ORE_GENERATION_PROTOCOL::equals),
            NetworkRegistry.acceptMissingOr(ORE_GENERATION_PROTOCOL::equals));

    private static int packetId;

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                PacketSyncComponentCovers.class,
                PacketSyncComponentCovers::encode,
                PacketSyncComponentCovers::decode,
                PacketSyncComponentCovers::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(
                packetId++,
                PacketSyncFluids.class,
                PacketSyncFluids::encode,
                PacketSyncFluids::decode,
                PacketSyncFluids::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(
                packetId++,
                PacketSyncStructureLayouts.class,
                PacketSyncStructureLayouts::encode,
                PacketSyncStructureLayouts::decode,
                PacketSyncStructureLayouts::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        ORE_GENERATION.registerMessage(
                0,
                PacketSyncOreGeneration.class,
                PacketSyncOreGeneration::encode,
                PacketSyncOreGeneration::decode,
                PacketSyncOreGeneration::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
