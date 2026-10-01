package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.fluid.FluidPort;
import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.api.recipe.FluidFuelBatch;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import java.util.*;

/** Original two-layer heat exchanger, hot-fluid recipes and eight upward heat outlets. */
public class LargeHeatExchangerControllerBlockEntity extends GTEnergyBlockEntity implements MultiblockPortOwner,IFluidHandler {
    public static final int RATE=com.gregtech.gregtech.content.multiblock.OriginalGeneratorParameters.HEAT_EXCHANGER_RATE;
    private long heat;
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    private final FluidTank hot=new FluidTank(RATE*10,stack->FuelRecipeMaps.Hot.containsInput(stack)) {
        @Override protected void onContentsChanged(){setChanged();}
    };
    private final FluidTank cold=new FluidTank(Integer.MAX_VALUE) {
        @Override protected void onContentsChanged(){setChanged();}
    };
    private final IFluidHandler input=new FluidPort(hot,true,false);
    public LargeHeatExchangerControllerBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.LARGE_HEAT_EXCHANGER.get(),pos,state);}
    @Override public boolean isStructureOk(){
        if(level==null||isRemoved())return false;
        var parts=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(var cell:com.gregtech.gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS){
            var pos=worldPosition.offset(cell.right(),cell.up(),cell.back());var expected=LargeMachineParts.block(cell.part());
            if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(expected)||!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity)){bindings.clear(this::release);return false;}
            parts.put(pos,MultiblockLayout.Role.valueOf(cell.role().name()));
        }
        return bindings.update(parts,p->((MultiblockPortBlockEntity)level.getBlockEntity(p)).canBind(worldPosition),
                (p,r)->((MultiblockPortBlockEntity)level.getBlockEntity(p)).bind(worldPosition,r),this::release);
    }
    private void release(BlockPos pos){if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)part.release(worldPosition);}
    @Override public void setRemoved(){bindings.clear(this::release);super.setRemoved();}
    public static void serverTick(Level level,BlockPos pos,BlockState state,LargeHeatExchangerControllerBlockEntity machine){machine.tick();}
    public void tick(){
        if(level==null||level.isClientSide||!isStructureOk())return;
        // Heat dissipates equally at all eight outlets, including unconnected ones, as in GT6.
        long perOutlet=Math.min(RATE/8,heat/8);
        if(perOutlet>0){
            heat-=perOutlet*8;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0){
                var target=worldPosition.offset(x,2,z);
                if(level.hasChunkAt(target)&&level.getBlockEntity(target) instanceof IEnergyBlock receiver)receiver.doEnergyInjection(GregTechTags.Energy.HU,Direction.DOWN,1,perOutlet,true);
            }
            setChanged();
        }
        if(heat<RATE*2L&&cold.getFluidAmount()<RATE*20){
            for(var recipe:FuelRecipeMaps.Hot.mRecipeList){
                var plan=FluidFuelBatch.plan(recipe,hot.getFluid(),cold.getFluid(),cold.getCapacity(),RATE*2L-heat,10000);
                if(plan==null||plan.energy()>Long.MAX_VALUE-heat)continue;
                hot.setFluid(plan.input());cold.setFluid(plan.output());heat+=plan.energy();setChanged();break;
            }
        }
        var below=worldPosition.below();
        if(!cold.isEmpty()&&level.hasChunkAt(below)&&level.getBlockEntity(below)!=null){
            var target=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,below,Direction.UP);if(target!=null){
                int accepted=target.fill(cold.getFluid().copy(),FluidAction.EXECUTE);if(accepted>0)cold.drain(accepted,FluidAction.EXECUTE);
            }
        }
    }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role){return role==MultiblockLayout.Role.FLUID_INPUT?input:null;}
    @Override public int getTanks(){return 2;}
    @Override public FluidStack getFluidInTank(int tank){return tank==0?hot.getFluid().copy():tank==1?cold.getFluid().copy():FluidStack.EMPTY;}
    @Override public int getTankCapacity(int tank){return tank==0?hot.getCapacity():tank==1?cold.getCapacity():0;}
    @Override public boolean isFluidValid(int tank,FluidStack stack){return tank==0&&hot.isFluidValid(stack);}
    @Override public int fill(FluidStack stack,FluidAction action){return hot.fill(stack,action);}
    @Override public FluidStack drain(FluidStack stack,FluidAction action){return cold.drain(stack,action);}
    @Override public FluidStack drain(int amount,FluidAction action){return cold.drain(amount,action);}
    public IFluidHandler fluidCapability(Direction side){return !isRemoved()?this:null;}
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting){return false;}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side){return List.of();}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side){return 0;}
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side){return 0;}
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size){return 0;}
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size){return 0;}
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute){return 0;}
    @Override public long getEnergyStored(GregTechTags.Tag type,Direction side){return type==GregTechTags.Energy.HU?heat:0;}
    @Override public long getEnergyCapacity(GregTechTags.Tag type,Direction side){return type==GregTechTags.Energy.HU?RATE*2:0;}
    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);tag.putLong("gt.hu",heat);tag.put("gt.hot",hot.writeToNBT(lookup,new CompoundTag()));tag.put("gt.cold",cold.writeToNBT(lookup,new CompoundTag()));}
    private static void restore(FluidTank tank,CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){com.gregtech.gregtech.api.fluid.NativeMachineTankData.restore(tank,tag,lookup);}
    @Override public void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);heat=Math.max(0,tag.getLong("gt.hu"));restore(hot,tag.getCompound("gt.hot"),lookup);restore(cold,tag.getCompound("gt.cold"),lookup);}
}
