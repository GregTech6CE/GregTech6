/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Source LargeHeatExchanger settings, fluid storage and eight-outlet cycle. */
package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.fluid.HeatExchangerData;
import com.gregtech.gregtech.content.multiblock.HeatExchangerRules;
import com.gregtech.gregtech.api.fluid.FluidPort;
import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Original two-layer heat exchanger, hot-fluid recipes and eight upward heat outlets. */
public class LargeHeatExchangerControllerBlockEntity extends GTEnergyBlockEntity implements MultiblockPortOwner,MultiblockToolTarget,IFluidHandler,com.gregtech.gregtech.api.fluid.FluidToolTarget,com.gregtech.gregtech.api.machine.MachineControl.Provider {
    public static final int RATE=com.gregtech.gregtech.content.multiblock.OriginalGeneratorParameters.HEAT_EXCHANGER_RATE;
    private long heat,activityHistory;
    private boolean active,structureOkay;
    private HeatExchangerRules.Settings settings=HeatExchangerRules.DEFAULTS;
    private Recipe lastRecipe;
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    private final FluidTankGT hot=new FluidTankGT(settings.inputCapacity()).setOnChanged(this::setChanged);
    private final FluidTankGT cold=new FluidTankGT().setOnChanged(this::setChanged);
    private final IFluidHandler input=new FluidPort(this,true,false);
    private FluidTankGT tappedTank(){return HeatExchangerRules.tapTank(cold.getAmount())==1?cold:hot;}
    private final IFluidHandler tap=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){return tank==0?tappedTank().getFluid().copy():FluidStack.EMPTY;}
        public int getTankCapacity(int tank){return tank==0?tappedTank().getCapacity():0;}
        public boolean isFluidValid(int tank,FluidStack fluid){return false;}
        public int fill(FluidStack fluid,FluidAction action){return 0;}
        public FluidStack drain(FluidStack fluid,FluidAction action){return tappedTank().drain(fluid,action);}
        public FluidStack drain(int amount,FluidAction action){return tappedTank().drain(amount,action);}
    };
    @Override public IFluidHandler fluidToolHandler(Direction side,boolean filling){return isRemoved()?null:filling?input:tap;}
    @Override public void onLoad(){
        super.onLoad();
        // Older port saves had horizontal fronts. Original GT6 accepts only SIDE_BOTTOM.
        var facing=com.gregtech.gregtech.block.machine.LargeHeatExchangerControllerBlock.FACING;
        if(level!=null&&!level.isClientSide&&getBlockState().getValue(facing)!=Direction.DOWN)
            level.setBlock(worldPosition,getBlockState().setValue(facing,Direction.DOWN),3);
    }
    public HeatExchangerRules.Settings settings(){return settings;}
    public long tankAmount(int index){return index==0?hot.getAmount():index==1?cold.getAmount():0;}
    public long tankCapacity(int index){return index==0?hot.capacity():index==1?cold.capacity():0;}
    public boolean isActive(){return active;}
    private RecipeMap recipes(){return RecipeMap.RECIPE_MAPS.get(settings.fuelMap());}
    private final com.gregtech.gregtech.api.machine.MachineControl control=new com.gregtech.gregtech.api.machine.MachineControl(){
        public boolean available(){return !isRemoved();}
        public boolean supportsSwitch(){return false;}
        public boolean supportsProgress(){return false;}
        public boolean enabled(){return true;}
        public boolean setEnabled(boolean value){return true;}
        public boolean running(){return active;}
        public boolean active(){return active;}
        public long progress(){return 0;}
        public long progressMax(){return 0;}
    };
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    private LazyOptional<IFluidHandler> fluids=LazyOptional.of(()->this);
    public LargeHeatExchangerControllerBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.LARGE_HEAT_EXCHANGER.get(),pos,state);}
    @Override public boolean isStructureOk(){
        if(level==null||isRemoved())return false;
        if(level.isClientSide)return structureOkay;
        var parts=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        boolean complete=true;
        for(var cell:com.gregtech.gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS){
            var pos=worldPosition.offset(cell.right(),cell.up(),cell.back());var expected=LargeMachineParts.block(cell.part());
            if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(expected)
                    ||!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)||!part.canBind(worldPosition)) {
                complete=false;
                continue;
            }
            parts.put(pos,MultiblockLayout.Role.valueOf(cell.role().name()));
        }
        // Source checkAndSetTarget binds each valid cell even when another cell is missing.
        boolean claimed=bindings.update(parts,p->level.getBlockEntity(p) instanceof MultiblockPortBlockEntity part&&part.canBind(worldPosition),
                (p,r)->((MultiblockPortBlockEntity)level.getBlockEntity(p)).bind(worldPosition,r),this::release);
        boolean formed=complete&&claimed;
        if(structureOkay!=formed){structureOkay=formed;setChanged();}
        return formed;
    }
    @Override public boolean containsToolPosition(BlockPos pos){
        return com.gregtech.gregtech.content.multiblock.SharedHeatExchangerStructure.contains(
                pos.getX()-worldPosition.getX(),pos.getY()-worldPosition.getY(),pos.getZ()-worldPosition.getZ());
    }
    @Override public long useMultiblockTool(net.minecraft.world.item.context.UseOnContext context,
                                           List<net.minecraft.network.chat.Component> messages){
        if(level==null||level.isClientSide||isRemoved()||!containsToolPosition(context.getClickedPos()))return 0;
        if(com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(context.getItemInHand(),com.gregtech.gregtech.api.tool.GTToolType.BUILDER_WAND)){
            for(var cell:com.gregtech.gregtech.content.multiblock.SharedHeatExchangerStructure.CELLS)
                MultiblockTools.build(context,worldPosition.offset(cell.right(),cell.up(),cell.back()),LargeMachineParts.block(cell.part()));
            isStructureOk();
            return MultiblockToolRules.BUILDER_COST;
        }
        if(com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(context.getItemInHand(),com.gregtech.gregtech.api.tool.GTToolType.MAGNIFYING_GLASS)){
            messages.addAll(magnifyingGlassMessages());
            return MultiblockToolRules.MAGNIFIER_COST;
        }
        return 0;
    }
    public List<net.minecraft.network.chat.Component> magnifyingGlassMessages(){
        boolean previous=structureOkay,formed=isStructureOk();
        var messages=new ArrayList<net.minecraft.network.chat.Component>();
        messages.add(net.minecraft.network.chat.Component.literal(MultiblockToolRules.structureMessage(previous,formed)));
        if(previous&&formed){
            messages.add(tankMessage("Input: ",hot));
            messages.add(tankMessage("Output: ",cold));
        }
        return List.copyOf(messages);
    }
    private static net.minecraft.network.chat.Component tankMessage(String prefix,FluidTankGT tank){
        var message=net.minecraft.network.chat.Component.literal(prefix);
        if(tank.isEmpty())return message.append("Empty");
        var fluid=tank.getFluidLong();
        return message.append(MultiblockToolRules.amount(tank.getAmount())+" L of ").append(fluid.getDisplayName())
                .append(GTFluids.isGas(fluid)?" (Gaseous)":" (Liquid)");
    }
    private void release(BlockPos pos){if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)part.release(worldPosition);}
    @Override public void setRemoved(){bindings.clear(this::release);super.setRemoved();}
    public static void serverTick(Level level,BlockPos pos,BlockState state,LargeHeatExchangerControllerBlockEntity machine){machine.tick();}
    public void tick(){
        if(level==null||level.isClientSide)return;
        long previousHeat=heat,previousHistory=activityHistory;boolean previousActive=active;
        // The source checks/binds its shell but does not gate buffered heat or fuel on the result.
        isStructureOk();
        long perOutlet=HeatExchangerRules.perOutlet(settings.rate(),heat);
        if(perOutlet>0){
            heat-=perOutlet*8;
            var emitted=settings.energyType();
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0){
                var target=worldPosition.offset(x,2,z);
                if(emitted!=null&&level.hasChunkAt(target))
                    EnergyTransfer.insertEnergyInto(emitted,Direction.DOWN,1,perOutlet,this,level.getBlockEntity(target));
            }
            setChanged();
        }
        if(heat<settings.bufferTarget()) {
            active=false;
            if(cold.getAmount()<settings.outputBackpressure())burnFuel();
        }
        if(heat<8)heat=0;
        activityHistory=(activityHistory<<1)|(active?1:0);
        if(previousHeat!=heat||previousHistory!=activityHistory||previousActive!=active)setChanged();
        var below=worldPosition.below();
        if(!cold.isEmpty()&&level.hasChunkAt(below)&&level.getBlockEntity(below)!=null){
            level.getBlockEntity(below).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).ifPresent(target->{
                int accepted=target.fill(cold.getFluid().copy(),FluidAction.EXECUTE);if(accepted>0)cold.drain(accepted,FluidAction.EXECUTE);
            });
        }
    }
    private boolean matches(Recipe recipe){
        return recipe!=null&&recipe.mEnabled&&!recipe.mFakeRecipe&&recipe.mInputs.length==0
                &&recipe.mFluidInputs.length==1&&!recipe.mFluidInputs[0].isEmpty()
                &&hot.getFluidLong().isFluidEqual(recipe.mFluidInputs[0]);
    }
    private void burnFuel(){
        if(hot.isEmpty())return;
        var map=recipes();
        if(map==null)return; // Keep an unknown saved map identity; never silently burn another fuel type.
        Recipe recipe=map.mRecipeList.contains(lastRecipe)&&matches(lastRecipe)?lastRecipe:null;
        if(recipe==null)for(var candidate:map.mRecipeList)if(matches(candidate)){recipe=candidate;break;}
        if(recipe==null){hot.setEmpty();return;}
        var output=recipe.mFluidOutputs.length==0?FluidStack.EMPTY:recipe.mFluidOutputs[0];
        if(!output.isEmpty()&&!cold.isEmpty()&&!cold.getFluidLong().isFluidEqual(output))return;
        if(!output.isEmpty()&&Long.MAX_VALUE-cold.getAmount()<output.getAmount())return;
        var plan=HeatExchangerRules.charge(hot.getAmount(),recipe.mFluidInputs[0].getAmount(),cold.getAmount(),
                output.getAmount(),heat,settings.bufferTarget(),recipe.mEUt,recipe.mDuration,settings.efficiency());
        if(plan==null){if(hot.getAmount()<recipe.mFluidInputs[0].getAmount()&&activityHistory==0)hot.setEmpty();return;}
        hot.remove(plan.inputUsed());
        if(plan.outputMade()>0)cold.setFluid(output,cold.getAmount()+plan.outputMade());
        heat+=plan.energyAdded();active=true;lastRecipe=recipe;setChanged();
    }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role){return role==MultiblockLayout.Role.FLUID_INPUT?input:null;}
    @Override public int getTanks(){return 2;}
    @Override public FluidStack getFluidInTank(int tank){return tank==0?hot.getFluid().copy():tank==1?cold.getFluid().copy():FluidStack.EMPTY;}
    @Override public int getTankCapacity(int tank){return tank==0?hot.getCapacity():tank==1?cold.getCapacity():0;}
    @Override public boolean isFluidValid(int tank,FluidStack stack){return tank==0&&recipes()!=null&&recipes().containsInput(stack);}
    @Override public int fill(FluidStack stack,FluidAction action){return isFluidValid(0,stack)?hot.fill(stack,action):0;}
    @Override public FluidStack drain(FluidStack stack,FluidAction action){return cold.drain(stack,action);}
    @Override public FluidStack drain(int amount,FluidAction action){return cold.drain(amount,action);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.FLUID_HANDLER?fluids.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();fluids.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();fluids=LazyOptional.of(()->this);}
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting){return emitting&&type!=null&&type==settings.energyType();}
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,Direction side,boolean theoretical){return (side==null||side==Direction.UP)&&super.isEnergyEmittingTo(type,side,theoretical);}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side){return settings.energyType()==null?List.of():List.of(settings.energyType());}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side){return 0;}
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side){return settings.rate();}
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag type,Direction side){return settings.rate();}
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type,Direction side){return settings.rate();}
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size){return Math.min(settings.rate(),heat);}
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size){return 0;}
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute){return 0;}
    @Override public long getEnergyStored(GregTechTags.Tag type,Direction side){return type!=null&&type==settings.energyType()?heat:0;}
    @Override public long getEnergyCapacity(GregTechTags.Tag type,Direction side){return type!=null&&type==settings.energyType()?settings.bufferTarget():0;}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);
        HeatExchangerData.save(tag,settings);tag.putLong("gt.energy",heat);
        tag.putBoolean("gt.active",active);tag.putLong("gt.active.data",activityHistory);
        tag.putBoolean("gt.state.str",structureOkay);
        var inputTag=new CompoundTag();var outputTag=new CompoundTag();
        hot.writeToNBT(inputTag);cold.writeToNBT(outputTag);
        tag.put("gt.hot",inputTag);tag.put("gt.cold",outputTag);
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);
        settings=HeatExchangerData.settings(tag);lastRecipe=null;
        heat=Math.max(0,tag.getLong(tag.contains("gt.energy")?"gt.energy":"gt.hu"));
        active=tag.getBoolean("gt.active");activityHistory=tag.getLong("gt.active.data");
        structureOkay=tag.getBoolean("gt.state.str");
        HeatExchangerData.restore(hot,tag.getCompound(tag.contains("gt.hot")?"gt.hot":"gt.tank.0"),settings.inputCapacity());
        HeatExchangerData.restore(cold,tag.getCompound(tag.contains("gt.cold")?"gt.cold":"gt.tank.1"),Long.MAX_VALUE);
    }
}
