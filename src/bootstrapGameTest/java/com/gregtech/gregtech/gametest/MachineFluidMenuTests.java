package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.client.gui.*;
import com.gregtech.gregtech.network.PacketSyncFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.*;
import java.util.ArrayList;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MachineFluidMenuTests {
    private static final class RecordingMenu extends BasicMachineContainerMenu {
        final ArrayList<PacketSyncFluids> sent = new ArrayList<>();
        RecordingMenu(Inventory inventory, BasicMachineBlockEntity machine) { super(41, inventory, machine); }
        @Override protected void sendFluidUpdate(PacketSyncFluids packet) { sent.add(packet); }
    }

    @GameTest(template="test_empty")
    public static void fluidsRefreshWhileMenuStaysOpen(GameTestHelper h) {
        // A single-block machine with an output tank. The distillation tower is multiblock-only in
        // this port (the controller owns that name), so the menu test uses the distillery instead.
        var block = MachineRegistry.basicMachines().stream().map(e -> e.get())
                .filter(b -> b.basicSpec().machineName().equals("distillery")).findFirst().orElseThrow();
        var pos = new BlockPos(1,1,1); h.setBlock(pos, block);
        var machine = (BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        var player = h.makeMockPlayer();
        var menu = new RecordingMenu(player.getInventory(), machine);
        player.containerMenu = menu;
        var buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeUtf(machine.spec().machineName());
        for (int i=0;i<machine.getTanks();i++) buf.writeFluidStack(FluidStack.EMPTY);
        var client = new BasicMachineContainerMenu(41, player.getInventory(), buf); buf.release();
        machine.getTanksOutput()[0].setFluid(new FluidStack(Fluids.WATER,1000));
        menu.broadcastChanges();
        h.assertTrue(menu.sent.size()==1,"open menu sends fluid changes without a ServerPlayer ContainerListener");
        var packet = menu.sent.get(0);
        var wire = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        PacketSyncFluids.encode(packet,wire); PacketSyncFluids.decode(wire).apply(client); wire.release();
        var display = (SlotFluid)client.slots.get(packet.slotIndex());
        h.assertTrue(display.fluid().getAmount()==1000,"decoded clientbound packet updates active menu");
        menu.broadcastChanges(); h.assertTrue(menu.sent.size()==1,"unchanged tank sends no duplicate");
        machine.getTanksOutput()[0].setFluid(new FluidStack(Fluids.WATER,2000));
        menu.broadcastChanges(); menu.sent.get(1).apply(client);
        h.assertTrue(display.fluid().getAmount()==2000,"amount-only change refreshes without reopening");
        var tagged = new FluidStack(Fluids.WATER,2000); tagged.getOrCreateTag().putString("test","changed");
        machine.getTanksOutput()[0].setFluid(tagged); menu.broadcastChanges();
        h.assertTrue(menu.sent.size()==3,"NBT-only change sends");
        new PacketSyncFluids(40,packet.slotIndex(),new FluidStack(Fluids.LAVA,500)).apply(client);
        h.assertTrue(display.fluid().getFluid()==Fluids.WATER,"late packet from old window rejected");
        machine.getTanksOutput()[0].setFluid(FluidStack.EMPTY); menu.broadcastChanges(); menu.sent.get(3).apply(client);
        h.assertTrue(display.fluid().isEmpty(),"empty tank clears displayed fluid");
        var mutable = new FluidStack(Fluids.WATER,300); display.setFluid(mutable); mutable.setAmount(100);
        h.assertTrue(display.fluid().getAmount()==300,"slot snapshot does not alias tank state");
        player.containerMenu = player.inventoryMenu;
        machine.getTanksOutput()[0].setFluid(new FluidStack(Fluids.LAVA,500)); menu.broadcastChanges();
        h.assertTrue(menu.sent.size()==4,"closed menu stops sending");
        h.succeed();
    }
}
