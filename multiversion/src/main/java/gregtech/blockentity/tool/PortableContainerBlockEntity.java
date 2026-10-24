package gregtech.blockentity.tool;

import gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.*;
*///?} else {
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
//?}
import javax.annotation.Nullable;

/** Stores one vessel item rather than maintaining a second, divergent fluid tank. */
public final class PortableContainerBlockEntity extends BlockEntity {
    private ItemStack vessel = ItemStack.EMPTY;
//? if >=1.21.1 {
//?} else {
    private boolean capabilitiesActive = true;
    private LazyOptional<IFluidHandler> capability = LazyOptional.of(() -> this.handler);
//?}
    public PortableContainerBlockEntity(BlockPos pos, BlockState state) { super(GTBlockEntities.PORTABLE_CONTAINER.get(), pos, state); }
    public ItemStack contents() { return vessel.isEmpty() ? new ItemStack(getBlockState().getBlock()) : vessel.copyWithCount(1); }
    public void setContents(ItemStack stack) {
        vessel = stack.isEmpty()||stack.getItem()!=getBlockState().getBlock().asItem()
                ? new ItemStack(getBlockState().getBlock()) : stack.copyWithCount(1);
//? if >=1.21.1 {
        /*vessel.remove(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if(level!=null)level.invalidateCapabilities(worldPosition);
*///?} else {
        if (vessel.hasTag()) vessel.getTag().remove("BlockEntityTag");
        capability.invalidate();
        capability = capabilitiesActive ? LazyOptional.of(() -> handler) : LazyOptional.empty();
//?}
        setChanged();
    }
    public int adjustLimit(double y,boolean precise){
        if(vessel.isEmpty())vessel=new ItemStack(getBlockState().getBlock());
        var spec=((gregtech.block.tool.PortableContainerBlock)getBlockState().getBlock()).spec();
        int limit=gregtech.content.tool.PortableContainerLimits.adjust(vessel,spec,y,precise);
        setChanged();return limit;
    }
    private IFluidHandlerItem tank() {
        if (vessel.isEmpty()) vessel = new ItemStack(getBlockState().getBlock());
//? if >=1.21.1 {
        /*return FluidUtil.getFluidHandler(vessel).orElseThrow(() -> new IllegalStateException("Vessel lacks fluid capability"));
*///?} else {
        return vessel.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(() -> new IllegalStateException("Vessel lacks fluid capability"));
//?}
    }
    private final IFluidHandler handler = new IFluidHandler() {
        public int getTanks() { return tank().getTanks(); }
        public FluidStack getFluidInTank(int slot) { return tank().getFluidInTank(slot).copy(); }
        public int getTankCapacity(int slot) { return tank().getTankCapacity(slot); }
        public boolean isFluidValid(int slot, FluidStack fluid) { return tank().isFluidValid(slot, fluid); }
        public int fill(FluidStack fluid, FluidAction action) {
            if(isRemoved())return 0;
            int moved = tank().fill(fluid, action); if (action.execute() && moved > 0) setChanged(); return moved;
        }
        public FluidStack drain(FluidStack fluid, FluidAction action) {
            if(isRemoved())return FluidStack.EMPTY;
            FluidStack moved = tank().drain(fluid, action); if (action.execute() && !moved.isEmpty()) setChanged(); return moved;
        }
        public FluidStack drain(int amount, FluidAction action) {
            if(isRemoved())return FluidStack.EMPTY;
            FluidStack moved = tank().drain(amount, action); if (action.execute() && !moved.isEmpty()) setChanged(); return moved;
        }
    };
    public boolean interact(Player player, InteractionHand hand) { return FluidUtil.interactWithFluidHandler(player, hand, handler); }
//? if >=1.21.1 {
    /*public IFluidHandler fluidHandler(){return isRemoved()?null:handler;}
*///?} else {
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && !isRemoved()) {
            return capability.cast();
        }
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        capabilitiesActive = false;
        capability.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        capabilitiesActive = true;
        capability = LazyOptional.of(() -> handler);
    }
//?}
    @Override public void setChanged() {
        super.setChanged();
        if(level!=null&&!level.isClientSide&&!isRemoved())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
//? if >=1.21.1 {
    /*@Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
*///?} else {
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
//?}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
//? if >=1.21.1 {
    /*@Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) { super.saveAdditional(tag,lookup); tag.put("gt.vessel", contents().save(lookup)); }
    @Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) { super.loadAdditional(tag,lookup); setContents(ItemStack.parseOptional(lookup,tag.getCompound("gt.vessel"))); }
*///?} else {
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("gt.vessel", contents().save(new CompoundTag())); }
    @Override public void load(CompoundTag tag) { super.load(tag); setContents(ItemStack.of(tag.getCompound("gt.vessel"))); }
//?}
}
