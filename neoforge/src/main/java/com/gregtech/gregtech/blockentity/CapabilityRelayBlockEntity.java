package com.gregtech.gregtech.blockentity;

import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.*;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.*;
import java.util.function.Function;

/** Live, sided forwarding without local storage, chunk loading or recursive capability loops. */
public abstract class CapabilityRelayBlockEntity extends BlockEntity {
    public record Target(BlockPos position,Direction side,net.minecraft.world.level.Level level) {
        public Target(BlockPos position,Direction side) { this(position,side,null); }
    }
    protected final net.minecraft.world.level.Level targetLevel(Target target) {
        return target.level()==null?level:target.level();
    }
    private static final com.gregtech.gregtech.content.logistics.RelayVisitSet<CapabilityRelayBlockEntity> ACTIVE=new com.gregtech.gregtech.content.logistics.RelayVisitSet<>();
    private final Map<Direction,IItemHandler> itemCaps=new EnumMap<>(Direction.class);
    private final Map<Direction,IFluidHandler> fluidCaps=new EnumMap<>(Direction.class);
    protected CapabilityRelayBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    protected abstract Target target(Direction side);
    protected boolean permitsItem(ItemStack stack) { return true; }
    protected boolean permitsFluid(FluidStack stack) { return true; }
    protected boolean permitsItem(Direction side,ItemStack stack) { return permitsItem(stack); }
    protected boolean permitsFluid(Direction side,FluidStack stack) { return permitsFluid(stack); }
    protected boolean supportsItems() { return true; }
    protected boolean supportsFluids() { return true; }
    protected boolean exposes(Direction side) { return side!=null; }
    protected final <T,R> R forward(Direction side,BlockCapability<T,Direction> capability,R unavailable,Function<T,R> operation) {
        var active=ACTIVE.get();
        if(level==null || isRemoved() || !active.add(this)) return unavailable;
        try {
            var target=target(side);
            if(target==null) return unavailable;
            var destinationLevel=targetLevel(target);
            if(!destinationLevel.hasChunkAt(target.position())) return unavailable;
            var entity=destinationLevel.getBlockEntity(target.position());
            if(entity==null || entity==this || entity.isRemoved()) return unavailable;
            var handler=destinationLevel.getCapability(capability,target.position(),target.side());return handler==null?unavailable:operation.apply(handler);
        } finally { active.remove(this);if(active.isEmpty()) ACTIVE.remove(); }
    }
    public IItemHandler itemHandler(Direction side){return !isRemoved()&&exposes(side)&&supportsItems()?itemCaps.computeIfAbsent(side,ItemRelay::new):null;}
    public IFluidHandler fluidHandler(Direction side){return !isRemoved()&&exposes(side)&&supportsFluids()?fluidCaps.computeIfAbsent(side,FluidRelay::new):null;}
    private final class ItemRelay implements IItemHandler {
        private final Direction side;
        ItemRelay(Direction side) { this.side=side; }
        @Override public int getSlots() { return forward(side,Capabilities.ItemHandler.BLOCK,0,IItemHandler::getSlots); }
        @Override public ItemStack getStackInSlot(int slot) { return forward(side,Capabilities.ItemHandler.BLOCK,ItemStack.EMPTY,h->slot>=0&&slot<h.getSlots()?h.getStackInSlot(slot).copy():ItemStack.EMPTY); }
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) { return forward(side,Capabilities.ItemHandler.BLOCK,stack,h->slot>=0&&slot<h.getSlots()&&permitsItem(side,stack)?h.insertItem(slot,stack,simulate):stack); }
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate) { return forward(side,Capabilities.ItemHandler.BLOCK,ItemStack.EMPTY,h->slot>=0&&slot<h.getSlots()&&permitsItem(side,h.getStackInSlot(slot))?h.extractItem(slot,amount,simulate):ItemStack.EMPTY); }
        @Override public int getSlotLimit(int slot) { return forward(side,Capabilities.ItemHandler.BLOCK,0,h->slot>=0&&slot<h.getSlots()?h.getSlotLimit(slot):0); }
        @Override public boolean isItemValid(int slot,ItemStack stack) { return forward(side,Capabilities.ItemHandler.BLOCK,false,h->slot>=0&&slot<h.getSlots()&&permitsItem(side,stack)&&h.isItemValid(slot,stack)); }
    }
    private final class FluidRelay implements IFluidHandler {
        private final Direction side;
        FluidRelay(Direction side) { this.side=side; }
        @Override public int getTanks() { return forward(side,Capabilities.FluidHandler.BLOCK,0,IFluidHandler::getTanks); }
        @Override public FluidStack getFluidInTank(int tank) { return forward(side,Capabilities.FluidHandler.BLOCK,FluidStack.EMPTY,h->tank>=0&&tank<h.getTanks()?h.getFluidInTank(tank).copy():FluidStack.EMPTY); }
        @Override public int getTankCapacity(int tank) { return forward(side,Capabilities.FluidHandler.BLOCK,0,h->tank>=0&&tank<h.getTanks()?h.getTankCapacity(tank):0); }
        @Override public boolean isFluidValid(int tank,FluidStack stack) { return forward(side,Capabilities.FluidHandler.BLOCK,false,h->tank>=0&&tank<h.getTanks()&&permitsFluid(side,stack)&&h.isFluidValid(tank,stack)); }
        @Override public int fill(FluidStack stack,FluidAction action) { return forward(side,Capabilities.FluidHandler.BLOCK,0,h->permitsFluid(side,stack)?h.fill(stack,action):0); }
        @Override public FluidStack drain(FluidStack stack,FluidAction action) { return forward(side,Capabilities.FluidHandler.BLOCK,FluidStack.EMPTY,h->permitsFluid(side,stack)?h.drain(stack,action):FluidStack.EMPTY); }
        @Override public FluidStack drain(int amount,FluidAction action) { return forward(side,Capabilities.FluidHandler.BLOCK,FluidStack.EMPTY,h->{var candidate=h.drain(amount,FluidAction.SIMULATE);return permitsFluid(side,candidate)?h.drain(candidate,action):FluidStack.EMPTY;}); }
    }
}
