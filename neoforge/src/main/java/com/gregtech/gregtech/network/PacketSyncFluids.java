package com.gregtech.gregtech.network;
import com.gregtech.gregtech.client.gui.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.fluids.FluidStack;
/** Original menu/slot addressed fluid sync using 1.21 native registry-aware component codecs. */
public record PacketSyncFluids(int containerId,int slotIndex,FluidStack fluid) implements CustomPacketPayload {
 public static final Type<PacketSyncFluids> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("gregtech","sync_fluids"));
 public static final StreamCodec<RegistryFriendlyByteBuf,PacketSyncFluids> STREAM_CODEC=StreamCodec.of(PacketSyncFluids::encode,PacketSyncFluids::decode);
 public PacketSyncFluids{fluid=fluid.copy();}
 private static void encode(RegistryFriendlyByteBuf buf,PacketSyncFluids packet){buf.writeVarInt(packet.containerId);buf.writeVarInt(packet.slotIndex);FluidStack.OPTIONAL_STREAM_CODEC.encode(buf,packet.fluid);}
 private static PacketSyncFluids decode(RegistryFriendlyByteBuf buf){return new PacketSyncFluids(buf.readVarInt(),buf.readVarInt(),FluidStack.OPTIONAL_STREAM_CODEC.decode(buf));}
 @Override public Type<PacketSyncFluids> type(){return TYPE;}
 public void apply(AbstractContainerMenu menu){if(menu instanceof BasicMachineContainerMenu&&menu.containerId==containerId&&slotIndex>=0&&slotIndex<menu.slots.size()&&menu.slots.get(slotIndex) instanceof SlotFluid slot)slot.setFluid(fluid);}
}
