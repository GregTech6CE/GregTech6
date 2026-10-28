package gregtech.network;
//? if >=1.21.1 {
/*import gregtech.client.gui.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
*///?} else {

import gregtech.client.gui.BasicMachineContainerMenu;
import gregtech.client.gui.SlotFluid;
import net.minecraft.network.FriendlyByteBuf;
//?}
import net.minecraft.world.inventory.AbstractContainerMenu;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
/^* Original menu/slot addressed fluid sync using 1.21 native registry-aware component codecs. ^/
public record PacketSyncFluids(int containerId,int slotIndex,FluidStack fluid,long amount,long capacity) implements CustomPacketPayload {
 public static final Type<PacketSyncFluids> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("gt6ce","sync_fluids"));
 public static final StreamCodec<RegistryFriendlyByteBuf,PacketSyncFluids> STREAM_CODEC=StreamCodec.of(PacketSyncFluids::encode,PacketSyncFluids::decode);
 public PacketSyncFluids{fluid=fluid.copy();}
 private static void encode(RegistryFriendlyByteBuf buf,PacketSyncFluids packet){buf.writeVarInt(packet.containerId);buf.writeVarInt(packet.slotIndex);FluidStack.OPTIONAL_STREAM_CODEC.encode(buf,packet.fluid);buf.writeVarLong(packet.amount);buf.writeVarLong(packet.capacity);}
 private static PacketSyncFluids decode(RegistryFriendlyByteBuf buf){return new PacketSyncFluids(buf.readVarInt(),buf.readVarInt(),FluidStack.OPTIONAL_STREAM_CODEC.decode(buf),buf.readVarLong(),buf.readVarLong());}
 @Override public Type<PacketSyncFluids> type(){return TYPE;}
 public void apply(AbstractContainerMenu menu){if(menu instanceof BasicMachineContainerMenu&&menu.containerId==containerId&&slotIndex>=0&&slotIndex<menu.slots.size()&&menu.slots.get(slotIndex) instanceof SlotFluid slot)slot.setFluid(fluid, amount, capacity);}
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → client: syncs one fluid stack to a specific SlotFluid in the open menu.
 */
public record PacketSyncFluids(int containerId, int slotIndex, FluidStack fluid, long amount, long capacity) {
    public PacketSyncFluids { fluid = fluid.copy(); }

    public static void encode(PacketSyncFluids pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.containerId);
        buf.writeVarInt(pkt.slotIndex);
        buf.writeFluidStack(pkt.fluid);
        buf.writeVarLong(pkt.amount);
        buf.writeVarLong(pkt.capacity);
    }

    public static PacketSyncFluids decode(FriendlyByteBuf buf) {
        return new PacketSyncFluids(buf.readVarInt(), buf.readVarInt(), buf.readFluidStack(), buf.readVarLong(), buf.readVarLong());
    }

    public static void handle(PacketSyncFluids pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> gregtech.client.MachineFluidClientSync.accept(pkt));
        });
        ctx.get().setPacketHandled(true);
    }

    public void apply(AbstractContainerMenu menu) {
        if (menu instanceof BasicMachineContainerMenu && menu.containerId == containerId
                && slotIndex >= 0 && slotIndex < menu.slots.size()
                && menu.slots.get(slotIndex) instanceof SlotFluid slot) slot.setFluid(fluid, amount, capacity);
    }
//?}
}
