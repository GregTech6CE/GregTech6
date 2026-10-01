package com.gregtech.gregtech.network;

import com.gregtech.gregtech.client.gui.BasicMachineContainerMenu;
import com.gregtech.gregtech.client.gui.SlotFluid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → client: syncs one fluid stack to a specific SlotFluid in the open menu.
 */
public record PacketSyncFluids(int containerId, int slotIndex, FluidStack fluid) {
    public PacketSyncFluids { fluid = fluid.copy(); }

    public static void encode(PacketSyncFluids pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.containerId);
        buf.writeVarInt(pkt.slotIndex);
        buf.writeFluidStack(pkt.fluid);
    }

    public static PacketSyncFluids decode(FriendlyByteBuf buf) {
        return new PacketSyncFluids(buf.readVarInt(), buf.readVarInt(), buf.readFluidStack());
    }

    public static void handle(PacketSyncFluids pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.gregtech.gregtech.client.MachineFluidClientSync.accept(pkt));
        });
        ctx.get().setPacketHandled(true);
    }

    public void apply(AbstractContainerMenu menu) {
        if (menu instanceof BasicMachineContainerMenu && menu.containerId == containerId
                && slotIndex >= 0 && slotIndex < menu.slots.size()
                && menu.slots.get(slotIndex) instanceof SlotFluid slot) slot.setFluid(fluid);
    }
}
