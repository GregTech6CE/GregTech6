package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.network.PacketSyncFluids;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/** Container for basic machine GUI — creates slot layout from RecipeMap counts. */
public class BasicMachineContainerMenu extends AbstractContainerMenu {
    private final BasicMachineBlockEntity blockEntity;
    private final Player viewer;
    private final String machineName;
    private final RecipeMap recipeMap;
    private final ContainerData progressData;
    private int firstPlayerSlot;

    /** Server-side constructor. */
    public BasicMachineContainerMenu(int containerId, Inventory playerInv, BasicMachineBlockEntity blockEntity) {
        super(GTMenuTypes.BASIC_MACHINE.get(), containerId);
        this.blockEntity = blockEntity;
        this.viewer = playerInv.player;
        this.machineName = blockEntity != null && blockEntity.spec() != null
                ? blockEntity.spec().machineName() : "default";
        this.recipeMap = blockEntity != null ? blockEntity.recipeMap() : MachineRecipeMaps.DidYouKnow;
        this.progressData = new ContainerData() {
            @Override public int get(int key) {
                return blockEntity != null ? blockEntity.getProgressPercent() : 0;
            }
            @Override public void set(int key, int value) {}
            @Override public int getCount() { return 1; }
        };
        addDataSlots(progressData);
        if (blockEntity != null && blockEntity.inventory() != null) {
            addMachineSlots(blockEntity.inventory());
            int totalMachineSlots = recipeMap.mInputItemsCount + recipeMap.mOutputItemsCount
                    + recipeMap.mInputFluidCount + recipeMap.mOutputFluidCount;
            addFluidSlots(new ItemStackHandler(totalMachineSlots));
        }
        addPlayerSlots(playerInv);
    }

    /** Client-side constructor used by Forge MenuType factory. */
    public BasicMachineContainerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        super(GTMenuTypes.BASIC_MACHINE.get(), containerId);
        this.blockEntity = null;
        this.viewer = playerInv.player;
        this.machineName = buf != null ? buf.readUtf() : "default";
        this.recipeMap = MachineRecipeMaps.byMachineName(machineName);
        this.progressData = new SimpleContainerData(1);
        addDataSlots(progressData);
        int totalMachineSlots = recipeMap.mInputItemsCount + recipeMap.mOutputItemsCount
                + recipeMap.mInputFluidCount + recipeMap.mOutputFluidCount;
        ItemStackHandler dummy = new ItemStackHandler(totalMachineSlots);
        addMachineSlots(dummy);
        addFluidSlots(dummy);
        addPlayerSlots(playerInv);
        if (buf != null) {
            readFluidSync(buf);
        }
    }

    /** Read fluid tank data from a buffer and apply to SlotFluid instances. */
    public void readFluidSync(RegistryFriendlyByteBuf buf) {
        int total = recipeMap.mInputFluidCount + recipeMap.mOutputFluidCount;
        if (total == 0) return;
        net.neoforged.neoforge.fluids.FluidStack[] fluids = new net.neoforged.neoforge.fluids.FluidStack[total];
        for (int i = 0; i < total; i++) {
            fluids[i] = net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_STREAM_CODEC.decode(buf);
        }
        for (Slot slot : slots) {
            if (slot instanceof SlotFluid sf) {
                int globalIdx = sf.tankIndex() + (sf.isInput() ? 0 : recipeMap.mInputFluidCount);
                if (globalIdx >= 0 && globalIdx < total) {
                    sf.setFluid(fluids[globalIdx]);
                }
            }
        }
    }

    // ── Slot grid ─────────────────────────────────────────────────────────

    private void addMachineSlots(net.neoforged.neoforge.items.IItemHandler inv) {
        int inputCount  = recipeMap.mInputItemsCount;
        int outputCount = recipeMap.mOutputItemsCount;
        int totalFluids = recipeMap.mInputFluidCount + recipeMap.mOutputFluidCount;

        addGrid(inv,0,inputCount,true,totalFluids);
        addGrid(inv,inputCount,outputCount,false,totalFluids);
    }

    private void addGrid(net.neoforged.neoforge.items.IItemHandler inv,int start,int count,boolean input,int fluids) {
        for(int i=0;i<count;i++) {
            var point=com.gregtech.gregtech.api.recipe.MachineGuiLayout.item(input,i,count,fluids);
            addSlot(input?new SlotInput(inv,start+i,point.x(),point.y()):new SlotOutput(inv,start+i,point.x(),point.y()));
        }
    }

    private void addFluidSlots(ItemStackHandler dummy) {
        int baseIndex = recipeMap.mInputItemsCount + recipeMap.mOutputItemsCount;
        // Input fluid slots (left, X=53/35/17, Y from bottom up: 63, 45, 27)
        for (int i = 0; i < recipeMap.mInputFluidCount; i++) {
            var point=com.gregtech.gregtech.api.recipe.MachineGuiLayout.fluid(true,i);
            int x=point.x(),y=point.y();
            addSlot(new SlotFluid(dummy, baseIndex + i, x, y, i, true));
        }
        // Output fluid slots (right, X=107/125/143, Y from bottom up)
        for (int i = 0; i < recipeMap.mOutputFluidCount; i++) {
            var point=com.gregtech.gregtech.api.recipe.MachineGuiLayout.fluid(false,i);
            int x=point.x(),y=point.y();
            addSlot(new SlotFluid(dummy, baseIndex + recipeMap.mInputFluidCount + i, x, y, i, false));
        }
    }

    private void addPlayerSlots(Inventory playerInv) {
        this.firstPlayerSlot = this.slots.size();
        int playerX = 8;
        int invY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, playerX + col * 18, invY + row * 18));
            }
        }
        int hotbarY = 142;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, playerX + col * 18, hotbarY));
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public BasicMachineBlockEntity blockEntity() { return blockEntity; }
    public String machineName() { return machineName; }
    public RecipeMap recipeMap() { return recipeMap; }
    public int progressPercent() { return progressData.get(0); }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || !blockEntity.isRemoved();
    }

    // ── Fluid sync ──────────────────────────────────────────────────────

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity == null || viewer.containerMenu != this) return;
        for (Slot slot : slots) {
            if (!(slot instanceof SlotFluid sf)) continue;
            var current = getTankFluid(sf);
            if (current.getAmount()!=sf.fluid().getAmount() || !net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(current,sf.fluid())) {
                sf.setFluid(current);
                sendFluidUpdate(new PacketSyncFluids(containerId, slot.index, current));
            }
        }
    }

    protected void sendFluidUpdate(PacketSyncFluids packet) {
        if (viewer instanceof ServerPlayer player)
            PacketDistributor.sendToPlayer(player,packet);
    }

    private net.neoforged.neoforge.fluids.FluidStack getTankFluid(SlotFluid sf) {
        if (blockEntity == null) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        int tank = sf.isInput() ? sf.tankIndex() : blockEntity.getTanks() - recipeMap.mOutputFluidCount + sf.tankIndex();
        if (tank < 0 || tank >= blockEntity.getTanks()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        return blockEntity.getFluidInTank(tank);
    }

    // ── Shift-click ──────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (firstPlayerSlot == 0) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int machineEnd = firstPlayerSlot;
        if (index < firstPlayerSlot) {
            // Machine slot → player inventory
            if (!moveItemStackTo(stack, firstPlayerSlot, slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            // Player inventory → machine input slots (0..inputCount-1)
            int inputCount = recipeMap.mInputItemsCount;
            if (inputCount > 0) {
                if (!moveItemStackTo(stack, 0, inputCount, false))
                    return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }
}
