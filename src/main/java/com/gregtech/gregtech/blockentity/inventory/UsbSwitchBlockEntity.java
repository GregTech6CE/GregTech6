package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.api.data.UsbDataPort;
import com.gregtech.gregtech.api.machine.MachineControl;
import com.gregtech.gregtech.block.inventory.UsbSwitchBlock;
import com.gregtech.gregtech.client.gui.DataSwitchMenu;
import com.gregtech.gregtech.content.cover.PanelCover;
import com.gregtech.gregtech.content.cover.PanelCoverHost;
import com.gregtech.gregtech.content.cover.PanelCoverRuntime;
import com.gregtech.gregtech.content.data.UsbDataMedia;
import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** GT6's 19000 USB switch (16 sticks) and 19001 HDD switch (one drive, 16 files). */
public class UsbSwitchBlockEntity extends BlockEntity
        implements UsbDataPort, PanelCoverHost, MachineControl.Provider, MenuProvider {
    public static final int USB_SLOTS = com.gregtech.gregtech.content.data.UsbDataRules.FILES;
    public static final int HDD_SLOTS = 1;

    private final UsbSwitchBlock.Kind kind;
    private int mode;
    private final ItemStackHandler items;
    private final java.util.Map<Direction,LazyOptional<IItemHandler>> componentCaps=new java.util.EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> itemCapability;
    private final ItemStack[] covers = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final PanelCoverRuntime panels = new PanelCoverRuntime(this);
    private final MachineControl control = new MachineControl() {
        @Override public boolean available() { return !isRemoved(); }
        @Override public boolean supportsProgress() { return false; }
        @Override public boolean supportsMode() { return true; }
        @Override public int mode() { return UsbSwitchBlockEntity.this.mode; }
        @Override public int setMode(int value) { return UsbSwitchBlockEntity.this.setMode(value); }
        @Override public boolean enabled() { return available(); }
        @Override public boolean setEnabled(boolean value) { return enabled(); }
        @Override public boolean running() { return false; }
        @Override public boolean active() { return false; }
        @Override public long progress() { return 0; }
        @Override public long progressMax() { return 0; }
    };

    public UsbSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.USB_SWITCH.get(), pos, state);
        this.kind = ((UsbSwitchBlock) state.getBlock()).kind();
        this.items = new ItemStackHandler(kind == UsbSwitchBlock.Kind.USB ? USB_SLOTS : HDD_SLOTS) {
            @Override protected void onContentsChanged(int slot) {
                setChanged();
                sync();
            }
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return kind == UsbSwitchBlock.Kind.USB ? UsbDataMedia.isStick(stack) : UsbDataMedia.isDrive(stack);
            }
        };
        this.itemCapability = LazyOptional.of(() -> items);
    }

    @Override public net.minecraftforge.items.IItemHandler componentItems(Direction side) { return items; }
    public UsbSwitchBlock.Kind kind() { return kind; }
    public ItemStackHandler items() { return items; }
    public int mode() { return mode; }

    /** Selector cover modes are 0..15; changing mode affects the next adjacent USB read. */
    public int setMode(int value) {
        int next = com.gregtech.gregtech.content.data.UsbDataRules.mode(value);
        if (mode != next && !isRemoved() && (level == null || !level.isClientSide)) {
            mode = next;
            setChanged();
            sync();
            if (level != null) for (Direction side : Direction.values()) {
                BlockPos neighbor = worldPosition.relative(side);
                if (level.hasChunkAt(neighbor)) level.neighborChanged(neighbor, getBlockState().getBlock(), worldPosition);
            }
        }
        return mode;
    }

    @Override @Nullable
    public CompoundTag readUsbData(Direction side, int requestedTier) {
        ItemStack medium = items.getStackInSlot(kind == UsbSwitchBlock.Kind.USB ? mode : 0);
        return kind == UsbSwitchBlock.Kind.USB
                ? UsbDataMedia.readStick(medium, requestedTier)
                : UsbDataMedia.readDrive(medium, mode, requestedTier);
    }

    @Override
    public boolean writeUsbData(Direction side, int requestedTier, @Nullable CompoundTag data) {
        if (level != null && level.isClientSide) return false;
        int slot = kind == UsbSwitchBlock.Kind.USB ? mode : 0;
        ItemStack medium = items.getStackInSlot(slot).copy();
        boolean success = kind == UsbSwitchBlock.Kind.USB
                ? UsbDataMedia.writeStick(medium, requestedTier, data)
                : UsbDataMedia.writeDrive(medium, mode, requestedTier, data);
        if (success) items.setStackInSlot(slot, medium);
        return success;
    }

    /** GT6's bare-hand switch opens a GUI; holding any USB stick copies or clears its file. */
    public boolean writeFromHeldStick(ItemStack held, Direction side) {
        if (!UsbDataMedia.isStick(held)) return false;
        CompoundTag file = GTMaterialDataRecipes.usbData(held);
        int tier = file == null ? 0 : UsbDataMedia.stickFileTier(held);
        return writeUsbData(side, tier, file);
    }

    @Override public ItemStack getCover(Direction side) { return covers[side.ordinal()]; }
    @Override public PanelCoverRuntime panels() { return panels; }
    @Override public MachineControl machineControl(Direction side) { return control; }
    @Override public boolean attachCover(Direction side, ItemStack stack) {
        PanelCover panel = PanelCover.of(stack);
        if ((panel == null || !panel.selector()) && com.gregtech.gregtech.content.cover.ComponentCoverRuntime.kind(stack)==null
                || !covers[side.ordinal()].isEmpty()
                || !panels.canAttach(side, stack)) return false;
        covers[side.ordinal()] = stack.copyWithCount(1);
        panels.attached(side);
        sync();
        return true;
    }
    @Override public ItemStack removeCover(Direction side) {
        ItemStack cover = covers[side.ordinal()];
        if (cover.isEmpty()) return ItemStack.EMPTY;
        covers[side.ordinal()] = ItemStack.EMPTY;
        // GT6's selector attachment resets the shared mode when it is removed.
        if(PanelCover.of(cover)!=null)setMode(0);
        setChanged();
        sync();
        return cover;
    }
    public void serverTick() {
        panels.beforeTick();
        for(var side:Direction.values())com.gregtech.gregtech.content.cover.ComponentCoverRuntime.tick(this,side,level.getGameTime());
        panels.afterTick();
    }

    @Override public Component getDisplayName() {
        return Component.translatable(kind == UsbSwitchBlock.Kind.USB
                ? "block.gregtech.usb_switch" : "block.gregtech.hdd_switch");
    }
    @Override @Nullable public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new DataSwitchMenu(id, playerInventory, this);
    }

    private void sync() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override @Nullable public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("gt.mode", mode);
        tag.put("gt_switch", items.serializeNBT());
        for (Direction side : Direction.values()) {
            ItemStack cover = covers[side.ordinal()];
            if (!cover.isEmpty()) tag.put("gt_cover_" + side.ordinal(), cover.save(new CompoundTag()));
        }
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        mode = com.gregtech.gregtech.content.data.UsbDataRules.mode(tag.getInt("gt.mode"));
        CompoundTag inventory = tag.getCompound("gt_switch").copy();
        inventory.putInt("Size", items.getSlots());
        items.deserializeNBT(inventory);
        for (Direction side : Direction.values()) {
            String key = "gt_cover_" + side.ordinal();
            covers[side.ordinal()] = tag.contains(key) ? ItemStack.of(tag.getCompound(key)) : ItemStack.EMPTY;
        }
        panels.loaded();
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if(capability==ForgeCapabilities.ITEM_HANDLER)return side==null?itemCapability.cast():componentCaps.computeIfAbsent(side,face->LazyOptional.of(()->com.gregtech.gregtech.content.cover.ComponentCoverAccess.items(this,face,items))).cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate();componentCaps.values().forEach(LazyOptional::invalidate);componentCaps.clear(); }
    @Override public void reviveCaps() { super.reviveCaps(); itemCapability = LazyOptional.of(() -> items); }
}
