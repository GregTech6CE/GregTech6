package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Transient binding is rebuilt after chunk load; no duplicated inventory or heat is stored here. */
public final class MultiblockPortBlockEntity extends GTEnergyBlockEntity implements
        com.gregtech.gregtech.content.logistics.LogisticsCoverHost,
        com.gregtech.gregtech.api.machine.ITileEntityCrucible,
        com.gregtech.gregtech.api.multiblock.BoundMachinePort {
    private final com.gregtech.gregtech.content.logistics.LogisticsCovers covers =
            new com.gregtech.gregtech.content.logistics.LogisticsCovers(this, this);
    private BlockPos controller;
    private MultiblockLayout.Role role;
    private LazyOptional<IFluidHandler> fluid=LazyOptional.empty();
    public MultiblockPortBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.MULTIBLOCK_PORT.get(),pos,state);
    }
    private MultiblockPortOwner owner() {
        if(isRemoved() || level==null || controller==null || !level.hasChunkAt(controller)) return null;
        return level.getBlockEntity(controller) instanceof MultiblockPortOwner owner?owner:null;
    }
    /** Original SensorTE reads the cached controller directly, preserving the measured side.
     * This does not expose an item/fluid transport capability or bypass its role restrictions. */
    public net.minecraft.world.level.block.entity.BlockEntity sensorTarget() {
        var target = owner();
        return target instanceof net.minecraft.world.level.block.entity.BlockEntity entity && !entity.isRemoved()
                ? entity : this;
    }
    public boolean isBoundTo(BlockPos owner) {return owner.equals(controller);}
    public boolean canBind(BlockPos owner) { return controller==null || controller.equals(owner) || owner()==null; }
    public void bind(BlockPos owner,MultiblockLayout.Role role) {
        this.controller=owner.immutable(); this.role=role;
        updateCrucibleVisual(owner() instanceof LargeCrucibleControllerBlockEntity);
    }
    @Override public void onLoad() {
        super.onLoad();
        // Bindings are rebuilt by the controller; never keep a saved invisible orphan wall.
        updateCrucibleVisual(false);
    }
    private void updateCrucibleVisual(boolean formed) {
        if (level == null || level.isClientSide) return;
        var property = com.gregtech.gregtech.block.machine.MultiblockPortBlock.CRUCIBLE_FORMED;
        var state = level.getBlockState(worldPosition);
        if (state.hasProperty(property) && state.getValue(property) != formed)
            level.setBlock(worldPosition, state.setValue(property, formed), 3);
    }
    public void release(BlockPos owner) { if(owner.equals(controller)) {controller=null;role=null;updateCrucibleVisual(false);} }
    @Override public boolean canLogistics(Direction side) {
        // GT6 multiblock parts delegate logistics to their owner; only a Logistics Core opts in.
        return (role == MultiblockLayout.Role.LOGISTICS || role == MultiblockLayout.Role.ENERGY_INPUT)
                && owner() instanceof com.gregtech.gregtech.content.logistics.LogisticsHost host
                && host.canLogistics(side);
    }
    /** GT6 crucible's middle wall serves faucets and molds; upper wall serves hand access. */
    public LargeCrucibleControllerBlockEntity crucibleController() {
        var host = owner();
        return host instanceof LargeCrucibleControllerBlockEntity crucible ? crucible : null;
    }
    public boolean isCrucibleUpperPort() { return role == MultiblockLayout.Role.ITEM_FLUID_IO && crucibleController() != null; }
    @Override public boolean fillMoldAtSide(com.gregtech.gregtech.api.machine.ITileEntityMold mold,
                                             int crucibleSide, int moldSide) {
        var crucible = crucibleController();
        return role == MultiblockLayout.Role.CRUCIBLE && crucible != null && crucible.isStructureOk()
                && crucible.fillMoldAtSide(mold, crucibleSide, moldSide);
    }
    @Override public long getCrucibleTemperature() {
        var crucible = crucibleController();
        return role == MultiblockLayout.Role.CRUCIBLE && crucible != null && crucible.isStructureOk()
                ? crucible.getCrucibleTemperature() : 0;
    }
    @Override public long getCrucibleContentAmount() {
        var crucible = crucibleController();
        return role == MultiblockLayout.Role.CRUCIBLE && crucible != null && crucible.isStructureOk()
                ? crucible.getCrucibleContentAmount() : 0;
    }
    @Override public com.gregtech.gregtech.content.logistics.LogisticsCovers logisticsCovers() { return covers; }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        covers.save(tag);
    }
    @Override public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        covers.load(tag);
    }
    private LazyOptional<net.minecraftforge.items.IItemHandler> items=LazyOptional.empty();
    private net.minecraftforge.items.IItemHandler itemTarget() { var owner=owner(); return owner!=null&&owner.isStructureOk()?owner.portItems(role):null; }
    private final net.minecraftforge.items.IItemHandler itemRelay=new net.minecraftforge.items.IItemHandler() {
        public int getSlots() {var t=itemTarget();return t==null?0:t.getSlots();}
        public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {var t=itemTarget();return t==null||slot<0||slot>=t.getSlots()?net.minecraft.world.item.ItemStack.EMPTY:t.getStackInSlot(slot).copy();}
        public net.minecraft.world.item.ItemStack insertItem(int slot,net.minecraft.world.item.ItemStack stack,boolean simulate) {var t=itemTarget();return t==null||slot<0||slot>=t.getSlots()?stack:t.insertItem(slot,stack,simulate);}
        public net.minecraft.world.item.ItemStack extractItem(int slot,int amount,boolean simulate) {var t=itemTarget();return t==null||slot<0||slot>=t.getSlots()?net.minecraft.world.item.ItemStack.EMPTY:t.extractItem(slot,amount,simulate);}
        public int getSlotLimit(int slot) {var t=itemTarget();return t==null||slot<0||slot>=t.getSlots()?0:t.getSlotLimit(slot);}
        public boolean isItemValid(int slot,net.minecraft.world.item.ItemStack stack) {var t=itemTarget();return t!=null&&slot>=0&&slot<t.getSlots()&&t.isItemValid(slot,stack);}
    };
    private IFluidHandler target() { var owner=owner();return owner!=null&&owner.isStructureOk()?owner.portFluids(role):null; }
    private final IFluidHandler relay=new IFluidHandler() {
        public int getTanks() { var t=target();return t==null?0:t.getTanks(); }
        public FluidStack getFluidInTank(int tank) { var t=target();return t==null?FluidStack.EMPTY:t.getFluidInTank(tank).copy(); }
        public int getTankCapacity(int tank) { var t=target();return t==null?0:t.getTankCapacity(tank); }
        public boolean isFluidValid(int tank,FluidStack stack) { var t=target();return t!=null&&t.isFluidValid(tank,stack); }
        public int fill(FluidStack stack,FluidAction action) { var t=target();return t==null?0:t.fill(stack,action); }
        public FluidStack drain(int amount,FluidAction action) { var t=target();return t==null?FluidStack.EMPTY:t.drain(amount,action); }
        public FluidStack drain(FluidStack stack,FluidAction action) { var t=target();return t==null?FluidStack.EMPTY:t.drain(stack,action); }
    };
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side) {
        if(cap==ForgeCapabilities.ITEM_HANDLER&&!isRemoved()) {
            if(!items.isPresent()) items=LazyOptional.of(()->itemRelay);
            return items.cast();
        }
        if(cap==ForgeCapabilities.FLUID_HANDLER&&!isRemoved()) {
            if(!fluid.isPresent()) fluid=LazyOptional.of(() -> relay);
            return fluid.cast();
        }
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() {super.invalidateCaps();fluid.invalidate();items.invalidate();}
    @Override public void reviveCaps() {super.reviveCaps();items=LazyOptional.of(()->itemRelay);fluid=LazyOptional.of(() -> relay);}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) {var o=owner();return o==null?List.of():o.portEnergyTypes(role);}
    @Override public boolean isEnergyType(GregTechTags.Tag t,Direction side,boolean emitting) {var o=owner();return o!=null&&(emitting?o.emitsPortEnergy(role,t):getEnergyTypes(side).contains(t)&&!o.emitsPortEnergy(role,t));}
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag t,Direction side,boolean theoretical) {var o=owner();return isEnergyType(t,side,false)&&o!=null&&o.isStructureOk();}
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag t,Direction side,boolean theoretical) {var o=owner();return o!=null&&o.emitsPortEnergy(role,t)&&o.isStructureOk();}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag t,Direction side) {var o=owner();return o!=null&&isEnergyType(t,side,false)?o.portEnergyInputRecommended(role,t):0;}
    @Override public long getEnergySizeInputMin(GregTechTags.Tag t,Direction side) {var o=owner();return o==null?0:o.portEnergyInputMin(role,t);}
    @Override public long getEnergySizeInputMax(GregTechTags.Tag t,Direction side) {var o=owner();return o==null?0:o.portEnergyInputMax(role,t);}
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag t,Direction side) {var o=owner();return o==null?0:o.portEnergyOutputSize(role,t);}
    @Override public long getEnergyOffered(GregTechTags.Tag t,Direction side,long size) {return 0;}
    @Override public long getEnergyDemanded(GregTechTags.Tag t,Direction side,long size) {var o=owner();return o!=null&&isEnergyAcceptingFrom(t,side,false)?o.portEnergyDemanded(role,t,size):0;}
    @Override public long getEnergyStored(GregTechTags.Tag t,Direction side) {var o=owner();return o==null?0:o.portEnergyStored(role,t);}
    @Override public long getEnergyCapacity(GregTechTags.Tag t,Direction side) {var o=owner();return o==null?0:o.portEnergyCapacity(role,t);}
    @Override public long doInject(GregTechTags.Tag t,Direction side,long size,long amount,boolean execute) {var o=owner();return o!=null&&o.isStructureOk()?o.injectPortEnergy(role,t,size,amount,execute):0;}
}
