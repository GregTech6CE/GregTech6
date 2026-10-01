package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidPort;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/** GT6 17211–17214: a steam batch drives two consecutive RU pulses; 170 L condense to 1 L. */
public class LargeTurbineControllerBlockEntity extends AxialGeneratorBlockEntity implements IFluidHandler {
    private long pending;
    private int steamCounter;
    private final FluidTank steam=new FluidTank(grade().inputMaximum()*4,LargeTurbineControllerBlockEntity::isSteam) {
        @Override protected void onContentsChanged(){setChanged();}
    };
    private final FluidTank water=new FluidTank(grade().inputMaximum()*4) {
        @Override protected void onContentsChanged(){setChanged();}
    };
    private final IFluidHandler input=new FluidPort(this,true,false),output=new FluidPort(this,false,true);
    private LazyOptional<IFluidHandler> fluids=LazyOptional.of(()->this);
    public LargeTurbineControllerBlockEntity(BlockPos pos,BlockState state) { super(GTBlockEntities.LARGE_TURBINE.get(),pos,state); }
    private static boolean isSteam(FluidStack stack) {
        var fluid=GTFluids.still("Steam");return !stack.isEmpty()&&fluid!=null&&fluid.isPresent()&&stack.getFluid()==fluid.get();
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,LargeTurbineControllerBlockEntity machine) { machine.tick(); }
    public void tick() {
        if(level==null||level.isClientSide)return;
        boolean formed=isStructureOk();
        if(!formed||overloaded){updateRotorVisual(formed,false);return;}
        if(pending>0) { energy+=pending;pending=0;setChanged(); }
        else if(!stopped&&steam.getFluidAmount()>=grade().input()) {
            int amount=steam.getFluidAmount();steam.drain(amount,FluidAction.EXECUTE);
            energy+=amount/2;pending=amount/2;
            int condensed=(steamCounter+amount)/com.gregtech.gregtech.content.multiblock.OriginalGeneratorParameters.STEAM_CONDENSATION_RATIO;steamCounter=(steamCounter+amount)%com.gregtech.gregtech.content.multiblock.OriginalGeneratorParameters.STEAM_CONDENSATION_RATIO;
            var distilled=GTFluids.still("DistW");
            if(condensed>0&&distilled!=null&&distilled.isPresent())water.fill(new FluidStack(distilled.get(),condensed),FluidAction.EXECUTE);
            // As in GT6 the distilled-water tank voids excess; a full output never blocks the shaft.
            setChanged();
        }
        convertAndEmit();if(overloaded)pending=0;
        updateRotorVisual(true,!overloaded&&isConvertingThisTick());
    }
    /** GT6 plunger empties the input tank first, then the condensed water tank. */
    public int purgeFluid() {
        var tank=steam.isEmpty()?water:steam;
        return tank.drain(tank.getFluidAmount(),FluidAction.EXECUTE).getAmount();
    }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) { return switch(role) { case FLUID_INPUT->input;case FLUID_OUTPUT->output;case FLUID_IO->this;default->null; }; }
    @Override public int getTanks(){return 2;}
    @Override public FluidStack getFluidInTank(int tank){return tank==0?steam.getFluid().copy():tank==1?water.getFluid().copy():FluidStack.EMPTY;}
    @Override public int getTankCapacity(int tank){return tank==0?steam.getCapacity():tank==1?water.getCapacity():0;}
    @Override public boolean isFluidValid(int tank,FluidStack stack){return tank==0&&!stopped&&!overloaded&&isSteam(stack);}
    @Override public int fill(FluidStack stack,FluidAction action){return isFluidValid(0,stack)?steam.fill(stack,action):0;}
    @Override public FluidStack drain(FluidStack stack,FluidAction action){return water.drain(stack,action);}
    @Override public FluidStack drain(int amount,FluidAction action){return water.drain(amount,action);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.FLUID_HANDLER&&(side==null||side==front())?fluids.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();fluids.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();fluids=LazyOptional.of(()->this);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong("gt.pending",pending);tag.putInt("gt.steam_counter",steamCounter);tag.put("gt.steam",steam.writeToNBT(new CompoundTag()));tag.put("gt.water",water.writeToNBT(new CompoundTag()));}
    private static void readTank(FluidTank tank,CompoundTag tag) {
        var fluid=tag.contains("Fluid")?FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid")):FluidStack.loadFluidStackFromNBT(tag);
        if(fluid.isEmpty()) {tank.setFluid(FluidStack.EMPTY);return;}
        long amount=tag.contains("Fluid")?tag.getLong("Amount"):fluid.getAmount();
        fluid.setAmount((int)Math.max(0,Math.min(tank.getCapacity(),amount)));tank.setFluid(fluid);
    }
    @Override public void load(CompoundTag tag){super.load(tag);pending=Math.max(0,Math.min(grade().inputMaximum(),tag.getLong("gt.pending")));steamCounter=Math.floorMod(tag.getInt("gt.steam_counter"),com.gregtech.gregtech.content.multiblock.OriginalGeneratorParameters.STEAM_CONDENSATION_RATIO);readTank(steam,tag.getCompound("gt.steam"));if(!steam.isEmpty()&&!isSteam(steam.getFluid()))steam.setFluid(FluidStack.EMPTY);readTank(water,tag.getCompound("gt.water"));}
}
