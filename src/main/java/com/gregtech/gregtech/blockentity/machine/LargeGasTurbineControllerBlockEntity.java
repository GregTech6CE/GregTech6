package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.fluid.FluidPort;
import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.api.recipe.FluidFuelBatch;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.multiblock.AxialStructureTransform;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import java.util.*;

/** Stainless-steel GT6 gas turbine: solid 3x3x4 structure, front fuel and bottom exhaust. */
public class LargeGasTurbineControllerBlockEntity extends GTEnergyBlockEntity implements MultiblockPortOwner,IFluidHandler,com.gregtech.gregtech.api.machine.MachineControl.Provider {
    public static final int INPUT_MAX=12288, OUTPUT_MAX=8192; // Legacy lowest-grade constants.
    public com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.Grade grade() {
        return com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.grade(getBlockState().getBlock());
    }
    private long fuelEnergy,ruStored;
    private boolean stopped;
    private boolean converting,emitted;
    private boolean rotorFormed,rotorActive;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,
            ()->!this.stopped,value->{this.stopped=!value;setChanged();},()->this.converting,()->this.emitted);
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    private final FluidTank fuel=new FluidTank(grade().inputMaximum()*4,stack->FuelRecipeMaps.Gas.containsInput(stack)) {
        @Override protected void onContentsChanged(){setChanged();}
    };
    private final FluidTank[] exhaust={tank(),tank(),tank()};
    private FluidTank tank(){return new FluidTank(grade().inputMaximum()*16){@Override protected void onContentsChanged(){setChanged();}};}
    private final IFluidHandler input=new FluidPort(this,true,false),output=new FluidPort(this,false,true);
    private LazyOptional<IFluidHandler> fluids=LazyOptional.of(()->this);
    public LargeGasTurbineControllerBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.LARGE_GAS_TURBINE.get(),pos,state);}
    public Direction front(){return getBlockState().getValue(DirectionalBlock.FACING);}
    public boolean isRotorFormed(){return rotorFormed;}
    public boolean isRotorActive(){return rotorActive;}
    private void updateRotorVisual(boolean formed,boolean active){
        active &= formed;
        if(rotorFormed==formed&&rotorActive==active)return;
        rotorFormed=formed;rotorActive=active;syncToClient();
    }
    public void toggleStopped(){stopped=!stopped;setChanged();}
    @Override public boolean isStructureOk(){
        if(level==null||isRemoved())return false;
        var parts=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(var cell:com.gregtech.gregtech.content.multiblock.TurbineStructure.LAYOUT.cells()){
            var pos=AxialStructureTransform.at(worldPosition,front(),cell.right(),cell.up(),cell.back());
            if(!level.hasChunkAt(pos)||!grade().accepts(level.getBlockState(pos).getBlock())||!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity)){bindings.clear(this::release);return false;}
            parts.put(pos,AxialStructureTransform.role(front(),cell.right(),cell.up(),cell.back(),true));
        }
        return bindings.update(parts,p->((MultiblockPortBlockEntity)level.getBlockEntity(p)).canBind(worldPosition),
                (p,r)->((MultiblockPortBlockEntity)level.getBlockEntity(p)).bind(worldPosition,r),this::release);
    }
    private void release(BlockPos pos){if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)part.release(worldPosition);}
    @Override public void setRemoved(){bindings.clear(this::release);super.setRemoved();}
    public static void serverTick(Level level,BlockPos pos,BlockState state,LargeGasTurbineControllerBlockEntity machine){machine.tick();}
    public void tick(){
        converting=false;emitted=false;
        if(level==null||level.isClientSide)return;
        boolean formed=isStructureOk();
        if(!formed){updateRotorVisual(false,false);return;}
        pushOutput();
        if(stopped||ruStored>0){updateRotorVisual(true,emitted);return;}
        if(fuelEnergy<grade().inputMaximum()&&Arrays.stream(exhaust).allMatch(t->t.getFluidAmount()<t.getCapacity()/2)) {
            for(var recipe:FuelRecipeMaps.Gas.mRecipeList){
                var plan=FluidFuelBatch.plan(recipe,fuel.getFluid(),Arrays.stream(exhaust).map(FluidTank::getFluid).toList(),Arrays.stream(exhaust).mapToInt(FluidTank::getCapacity).toArray(),grade().inputMaximum()-fuelEnergy,10000);
                if(plan==null||plan.energy()>Long.MAX_VALUE-fuelEnergy)continue;
                fuel.setFluid(plan.input());for(int i=0;i<exhaust.length;i++)exhaust[i].setFluid(plan.outputs().get(i));fuelEnergy+=plan.energy();setChanged();break;
            }
        }
        if(fuelEnergy>=grade().inputMaximum()){converting=true;fuelEnergy-=grade().inputMaximum();ruStored=grade().outputMaximum();setChanged();}
        updateRotorVisual(true,converting||emitted);
    }
    private void pushOutput(){
        if(ruStored<=0)return;
        var side=front().getOpposite();var pos=worldPosition.relative(side,4);
        if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof IEnergyBlock receiver
                &&receiver.doEnergyInjection(GregTechTags.Energy.RU,side.getOpposite(),ruStored,1,true)>0){emitted=true;ruStored=0;setChanged();}
    }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role){return switch(role){case FLUID_INPUT->input;case FLUID_OUTPUT->output;case FLUID_IO->this;default->null;};}
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role){return role==MultiblockLayout.Role.ENERGY_OUTPUT?List.of(GregTechTags.Energy.RU):List.of();}
    @Override public boolean emitsPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type){return role==MultiblockLayout.Role.ENERGY_OUTPUT&&type==GregTechTags.Energy.RU;}
    @Override public long portEnergyOutputSize(MultiblockLayout.Role role,GregTechTags.Tag type){return emitsPortEnergy(role,type)?grade().output():0;}
    @Override public int getTanks(){return 4;}
    @Override public FluidStack getFluidInTank(int tank){return tank==0?fuel.getFluid().copy():tank>0&&tank<4?exhaust[tank-1].getFluid().copy():FluidStack.EMPTY;}
    @Override public int getTankCapacity(int tank){return tank==0?fuel.getCapacity():tank>0&&tank<4?exhaust[tank-1].getCapacity():0;}
    @Override public boolean isFluidValid(int tank,FluidStack stack){return tank==0&&!stopped&&fuel.isFluidValid(stack);}
    @Override public int fill(FluidStack stack,FluidAction action){return stopped?0:fuel.fill(stack,action);}
    @Override public FluidStack drain(FluidStack stack,FluidAction action){for(var tank:exhaust){var result=tank.drain(stack,action);if(!result.isEmpty())return result;}return FluidStack.EMPTY;}
    @Override public FluidStack drain(int amount,FluidAction action){for(int i=0;i<3;i++){var result=exhaust[(i+(level==null?0:(int)(level.getGameTime()/20%3)))%3].drain(amount,action);if(!result.isEmpty())return result;}return FluidStack.EMPTY;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.FLUID_HANDLER&&(side==null||side==front())?fluids.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();fluids.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();fluids=LazyOptional.of(()->this);}
    // Rotation exits the rear structural block, never directly from the front controller.
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting){return false;}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side){return List.of();}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side){return 0;}
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side){return 0;}
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size){return 0;}
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size){return 0;}
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute){return 0;}
    @Override public long getEnergyStored(GregTechTags.Tag type,Direction side){return type==GregTechTags.Energy.RU?ruStored:type==GregTechTags.Energy.HU?fuelEnergy:0;}
    @Override public long getEnergyCapacity(GregTechTags.Tag type,Direction side){return type==GregTechTags.Energy.RU?grade().outputMaximum():type==GregTechTags.Energy.HU?grade().inputMaximum():0;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong("gt.ru",ruStored);tag.putLong("gt.fuel",fuelEnergy);tag.putBoolean("gt.stopped",stopped);tag.putBoolean("gt.rotor_formed",rotorFormed);tag.putBoolean("gt.rotor_active",rotorActive);tag.put("gt.tank",fuel.writeToNBT(new CompoundTag()));for(int i=0;i<3;i++)tag.put("gt.exhaust"+i,exhaust[i].writeToNBT(new CompoundTag()));}
    @Override public void load(CompoundTag tag){super.load(tag);ruStored=Math.max(0,Math.min(grade().outputMaximum(),tag.getLong("gt.ru")));fuelEnergy=Math.max(0,tag.getLong("gt.fuel"));stopped=tag.getBoolean("gt.stopped");rotorFormed=tag.getBoolean("gt.rotor_formed");rotorActive=rotorFormed&&tag.getBoolean("gt.rotor_active");var data=tag.getCompound("gt.tank");
        if(data.contains("Fluid")){var fluid=FluidStack.loadFluidStackFromNBT(data.getCompound("Fluid"));fluid.setAmount((int)Math.max(0,Math.min(fuel.getCapacity(),data.getLong("Amount"))));fuel.setFluid(fluid);}else fuel.readFromNBT(data);
        for(int i=0;i<3;i++)exhaust[i].readFromNBT(tag.getCompound("gt.exhaust"+i));
    }
}
