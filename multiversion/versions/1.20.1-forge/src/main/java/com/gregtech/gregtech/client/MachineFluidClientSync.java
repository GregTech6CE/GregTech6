package com.gregtech.gregtech.client;

import com.gregtech.gregtech.network.PacketSyncFluids;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class MachineFluidClientSync {
    private MachineFluidClientSync() {}
    public static void accept(PacketSyncFluids packet) {
        var player = Minecraft.getInstance().player;
        if (player != null) packet.apply(player.containerMenu);
    }
}
