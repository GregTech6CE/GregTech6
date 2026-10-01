package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.block.machine.*;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Dedicated GT6 fusion executor: input reservation, LU startup, timed EU output and retained products. */
public class FusionReactorControllerBlockEntity extends LargeRecipeMachineBlockEntity {
    private final PartBindings<BlockPos,MultiblockLayout.Role> fusionBindings=new PartBindings<>();
    private long chargeRemaining,chargeTotal,progress,duration,outputEU;
    private MachineWorkOutputs products;
    private Recipe continuousRecipe;
    private boolean running;

    public FusionReactorControllerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {
        super(type,pos,state);
        setSpec(((BasicMachineBlock)state.getBlock()).basicSpec());
    }
    @Override public boolean isStructureOk() {
        if(level==null||isRemoved())return false;
        var front=getBlockState().getValue(BasicMachineBlock.FACING);
        var candidates=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        int[] processors=new int[3];
        for(var cell:FusionStructure.CELLS) {
            var pos=cell.at(worldPosition,front);
            if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity))return broken();
            var actual=level.getBlockState(pos).getBlock();
            if(cell.part()>=18200&&cell.part()<=18202) {
                int kind=-1;
                for(int i=0;i<3;i++)if(actual==LargeMachineParts.block(18200+i))kind=i;
                if(kind<0)return broken();
                processors[kind]++;
            } else if(actual!=cell.block())return broken();
            candidates.put(pos,cell.role());
        }
        if(processors[0]!=3||processors[1]!=12||processors[2]!=12)return broken();
        return fusionBindings.update(candidates,p->((MultiblockPortBlockEntity)level.getBlockEntity(p)).canBind(worldPosition),
                (p,r)->((MultiblockPortBlockEntity)level.getBlockEntity(p)).bind(worldPosition,r),this::releaseFusionPart);
    }
    private boolean broken(){fusionBindings.clear(this::releaseFusionPart);return false;}
    private void releaseFusionPart(BlockPos pos){if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)part.release(worldPosition);}
    @Override public void setRemoved(){fusionBindings.clear(this::releaseFusionPart);super.setRemoved();}

    public static void serverTick(Level level,BlockPos pos,BlockState state,FusionReactorControllerBlockEntity be){be.tickFusion();}
    private void tickFusion() {
        running=false;
        if(!isStructureOk()||!switchesAllowRunning()){continuousRecipe=null;updateActivity();return;}
        if(products==null&&!startRecipe()){updateActivity();return;}
        if(chargeRemaining>0){updateActivity();return;}
        if(progress<duration) {
            running=true;
            if(outputEU>0)emitFusionEnergy();
            progress++;
        }
        if(progress>=duration&&products.flush(inventory(),inputSlots(),getTanksOutput())) {
            products=null;duration=0;progress=0;outputEU=0;
            // A continuously supplied, identical reaction retains its startup charge (GT6 mCurrentRecipe).
            startRecipe();
        }
        setChanged();updateActivity();
    }
    private boolean startRecipe() {
        var items=new ArrayList<ItemStack>();
        for(int i=0;i<inputSlots();i++)items.add(inventory().getStackInSlot(i));
        var fluids=Arrays.stream(getTanksInput()).map(t->t.getFluidInTank(0)).toList();
        var recipe=recipeMap().findRecipe(items,fluids,false,inputSlots(),0);
        if(recipe==null||recipe.mEUt>0||recipe.mEUt==Long.MIN_VALUE){continuousRecipe=null;return false;}
        var remaining=RecipeInputs.consume(recipe,items,fluids,1);
        if(remaining==null){continuousRecipe=null;return false;}
        products=MachineWorkOutputs.roll(recipe,1,level.random::nextInt);
        for(int i=0;i<items.size();i++)inventory().setStackInSlot(i,remaining.items().get(i));
        for(int i=0;i<fluids.size();i++)getTanksInput()[i].setFluid(remaining.fluids().get(i));
        chargeTotal=Math.max(0,recipe.mSpecialValue);
        chargeRemaining=continuousRecipe==recipe?0:chargeTotal;
        continuousRecipe=recipe;progress=0;duration=recipe.mDuration;outputEU=-recipe.mEUt;
        setChanged();return true;
    }
    private void emitFusionEnergy() {
        var center=worldPosition.relative(getBlockState().getValue(BasicMachineBlock.FACING).getOpposite(),2);
        for(var direction:Direction.Plane.HORIZONTAL) {
            var target=center.relative(direction,10);
            if(!level.hasChunkAt(target))continue;
            var receiver=level.getBlockEntity(target);
            if(receiver!=null&&EnergyTransfer.insertEnergyInto(GregTechTags.Energy.EU,direction.getOpposite(),outputEU,1,this,receiver)>0)break;
        }
    }
    private void updateActivity() {
        var state=getBlockState();boolean active=products!=null;
        if(state.getValue(BasicMachineBlock.RUNNING)!=active||state.getValue(BasicMachineBlock.LIT)!=running)
            level.setBlock(worldPosition,state.setValue(BasicMachineBlock.RUNNING,active).setValue(BasicMachineBlock.LIT,running),3);
    }
    @Override public boolean isRunning(){return running;}
    @Override public int getProgressPercent(){return duration>0?(int)Math.min(100,100.0*progress/duration):0;}
    public long chargeRemaining(){return chargeRemaining;}
    public long outputEU(){return outputEU;}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side){return List.of();}
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting){return false;}
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical){return false;}
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,Direction side,boolean theoretical){return false;}
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute){return 0;}
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role){return role==MultiblockLayout.Role.ENERGY_INPUT?List.of(GregTechTags.Energy.LU):role==MultiblockLayout.Role.ENERGY_OUTPUT?List.of(GregTechTags.Energy.EU):List.of();}
    @Override public boolean emitsPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type){return role==MultiblockLayout.Role.ENERGY_OUTPUT&&type==GregTechTags.Energy.EU;}
    @Override public long portEnergyOutputSize(MultiblockLayout.Role role,GregTechTags.Tag type){return emitsPortEnergy(role,type)?8192:0;}
    @Override public long portEnergyInputRecommended(MultiblockLayout.Role role,GregTechTags.Tag type){return 8192;}
    @Override public long portEnergyInputMin(MultiblockLayout.Role role,GregTechTags.Tag type){return 1;}
    @Override public long portEnergyInputMax(MultiblockLayout.Role role,GregTechTags.Tag type){return 16384;}
    @Override public long portEnergyStored(MultiblockLayout.Role role,GregTechTags.Tag type){return chargeTotal-chargeRemaining;}
    @Override public long portEnergyCapacity(MultiblockLayout.Role role,GregTechTags.Tag type){return chargeTotal;}
    @Override public long portEnergyDemanded(MultiblockLayout.Role role,GregTechTags.Tag type,long size) {
        return role==MultiblockLayout.Role.ENERGY_INPUT&&type==GregTechTags.Energy.LU&&size>0&&size<=16384&&chargeRemaining>0?1+(chargeRemaining-1)/size:0;
    }
    @Override public long injectPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type,long size,long amount,boolean execute) {
        if(role!=MultiblockLayout.Role.ENERGY_INPUT||type!=GregTechTags.Energy.LU||size<1||size>16384||amount<1||chargeRemaining<1||!isStructureOk())return 0;
        long accepted=Math.min(amount,1+(chargeRemaining-1)/size);
        if(execute){chargeRemaining=accepted>chargeRemaining/size?0:chargeRemaining-accepted*size;setChanged();}
        return accepted;
    }
    @Override public void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        var job=new CompoundTag();job.putLong("charge",chargeRemaining);job.putLong("totalCharge",chargeTotal);
        job.putLong("progress",progress);job.putLong("duration",duration);job.putLong("outputEU",outputEU);
        if(products!=null)job.put("products",products.save(lookup));
        tag.put("gt.fusion",job);
    }
    @Override public void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);var job=tag.getCompound("gt.fusion");
        chargeTotal=Math.max(0,job.getLong("totalCharge"));chargeRemaining=Math.max(0,Math.min(chargeTotal,job.getLong("charge")));
        duration=Math.max(0,job.getLong("duration"));progress=Math.max(0,Math.min(duration,job.getLong("progress")));
        outputEU=Math.max(0,job.getLong("outputEU"));products=job.contains("products")?MachineWorkOutputs.load(job.getCompound("products"),lookup):null;
        continuousRecipe=null;running=false;
    }
}
