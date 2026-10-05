package com.gregtech.gregtech.network;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
/** Common registration avoids loading client renderer/screen classes on dedicated servers. */
public final class MachinePayloads {
 private MachinePayloads(){}
 public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToClient(PacketSyncComponentCovers.TYPE,PacketSyncComponentCovers.STREAM_CODEC,(packet,context)->context.enqueueWork(()->packet.apply(context.player().level())));event.registrar("1").playToClient(PacketSyncFluids.TYPE,PacketSyncFluids.STREAM_CODEC,(packet,context)->context.enqueueWork(()->packet.apply(context.player().containerMenu)));}
}
