package com.gregtech.gregtech.network;

import com.gregtech.gregtech.GregTech;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GTPackets {
    private GTPackets() {}

    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            GregTech.id("main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int packetId;

    public static void register() {
        CHANNEL.registerMessage(packetId++, PacketSyncComponentCovers.class, PacketSyncComponentCovers::encode, PacketSyncComponentCovers::decode, PacketSyncComponentCovers::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(packetId++, PacketSyncFluids.class,
                PacketSyncFluids::encode, PacketSyncFluids::decode, PacketSyncFluids::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
    }
}
