package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.inventory.BlockContents;
import com.gregtech.gregtech.content.nuclear.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.*;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Original one/four-rod cores. Heat is converted into hot coolant, never emitted as free HU. */
public class ReactorCoreBlockEntity extends BlockEntity implements BlockContents,com.gregtech.gregtech.api.machine.MachineControl.Provider {
    public final ItemStack[] rods;
    public final int[] neutrons;
    private final List<ItemStack> legacyOverflow=new ArrayList<>();
    public final FluidTankGT input=new FluidTankGT(64000).setGasProof(true).setAcidProof(true).setOnChanged(this::setChanged);
    public final FluidTankGT output=new FluidTankGT(64000) {
        @Override public long capacity(FluidStack stack){
            var steam=com.gregtech.gregtech.registry.GTFluids.still("Steam");
            return !stack.isEmpty()&&steam!=null&&stack.getFluid()==steam.get()?Math.max(getAmount(),10240000L):super.capacity(stack);
        }
    }.setGasProof(true).setAcidProof(true).setOnChanged(this::setChanged);
    public long storedHeat,lastHeat;
    public boolean stopped=true,failed;
    private int disabledSlots;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,
            ()->!this.stopped,value->setStopped(!value),()->this.lastHeat>0,()->this.lastHeat>0,
            this::disabledSlots,this::setDisabledSlots);
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    public ReactorCoreBlockEntity(BlockPos pos,BlockState state){this(com.gregtech.gregtech.registry.GTBlockEntities.REACTOR_CORE.get(),pos,state,1);}
    protected ReactorCoreBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state,int count){super(type,pos,state);rods=new ItemStack[count];Arrays.fill(rods,ItemStack.EMPTY);neutrons=new int[count];}
    public static boolean isFuelRod(ItemStack stack){return RodPhysics.definition(stack)!=null;}
    public int rodCount(){return (int)Arrays.stream(rods).filter(s->!s.isEmpty()).count();}
    public int overflowCount(){return legacyOverflow.size();}
    public boolean insertRod(ItemStack stack){for(int i=0;i<rods.length;i++)if(insertRod(i,stack))return true;return false;}
    public boolean insertRod(int slot,ItemStack stack){if(slot<0||slot>=rods.length||!isFuelRod(stack)||!rods[slot].isEmpty())return false;rods[slot]=stack.copyWithCount(1);changed();return true;}
    public boolean insertHandRod(int slot,ItemStack stack){if(!insertRod(slot,stack))return false;setStopped(true);return true;}
    public ItemStack removeRod(){
        if(!legacyOverflow.isEmpty()){var stack=legacyOverflow.remove(legacyOverflow.size()-1);changed();return stack;}
        for(int i=rods.length-1;i>=0;i--)if(!rods[i].isEmpty())return removeRod(i);return ItemStack.EMPTY;
    }
    public ItemStack removeRod(int slot){if(slot<0||slot>=rods.length)return ItemStack.EMPTY;var stack=rods[slot];rods[slot]=ItemStack.EMPTY;neutrons[slot]=0;changed();return stack;}
    public void setStopped(boolean value){stopped=value;if(value)Arrays.fill(neutrons,0);if(!value)failed=false;changed();}
    public int disabledSlots(){return disabledSlots;}
    public void setDisabledSlots(int mask){disabledSlots=mask&((1<<rods.length)-1);changed();}
    public boolean slotActive(int slot){return !stopped&&(rods.length==1||(disabledSlots&(1<<slot))==0);}
    public long neutronTotal(){long total=0;for(int n:neutrons)total+=n;return total;}
    public ReactorCoolants coolant(){return ReactorCoolants.of(input.getFluid());}
    public static void serverTick(Level level,BlockPos pos,BlockState state,ReactorCoreBlockEntity be){ReactorNetwork.enqueue(be);}
    public void react(){
        var coolant=coolant();lastHeat=0;
        for(int i=0;i<rods.length;i++)if(slotActive(i)){
            var reaction=RodPhysics.react(rods[i],neutrons[i],coolant);rods[i]=reaction.stack();lastHeat+=reaction.heat();
        }
        if(coolant!=null)lastHeat=RodPhysics.ceil(lastHeat,coolant.heatDivider);
        storedHeat=Math.min(Long.MAX_VALUE-lastHeat,storedHeat)+lastHeat;
        if(!coolHeat()&&rodCount()>0)fail();
        pushFluids();
        if(level.getGameTime()%20==10&&neutronTotal()>0)irradiate(1);
        setChanged();if(level.getGameTime()%20==0)changed();
    }
    /** Whole-batch simulation before mutation: blocked output never consumes coolant. */
    public boolean coolHeat(){
        if(storedHeat<=0)return true;
        var coolant=coolant();if(coolant==null)return lastHeat==0;
        long units=storedHeat/coolant.heat;if(units==0)return true;
        if(coolant.outputFluid()==null||units>input.getAmount()||units>Integer.MAX_VALUE/coolant.expansion)return false;
        var result=new FluidStack(coolant.outputFluid(),(int)(units*coolant.expansion));
        if(output.fill(result,IFluidHandler.FluidAction.SIMULATE)!=result.getAmount())return false;
        input.remove(units);output.fill(result,IFluidHandler.FluidAction.EXECUTE);storedHeat-=units*coolant.heat;return true;
    }
    private void fail(){
        failed=true;stopped=true;irradiate(2);Arrays.fill(rods,ItemStack.EMPTY);Arrays.fill(neutrons,0);
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        // GT6 source deliberately leaves terrain explosions disabled; only installed rods are destroyed.
        changed();
    }
    private void irradiate(int multiplier){
        long exposure=RodPhysics.ceil(neutronTotal(),256)*multiplier;
        if(exposure<=0)return;int range=multiplier==1?200:500;
        var bounds=new net.minecraft.world.phys.AABB(worldPosition.getX()-range,level.getMinBuildHeight(),worldPosition.getZ()-range,worldPosition.getX()+range+1,level.getMaxBuildHeight(),worldPosition.getZ()+range+1);
        for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,bounds)){
            int strength=RodPhysics.bound((long)(exposure-entity.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))));
            if(strength>0)ReactorRadiation.apply(entity,(int)RodPhysics.ceil(strength,10),strength);
        }
    }

    public Direction outputSide(){return getBlockState().getValue(ReactorPorts.HOT);}
    public Direction coldOutputSide(){return getBlockState().getValue(ReactorPorts.COLD);}
    public void pushFluids(){
        if(level==null||level.isClientSide)return;
        if(coldOutputSide()!=outputSide())pushTank(input,coldOutputSide(),Math.max(0,input.getAmount()-input.capacity()/2));
        pushTank(output,outputSide(),output.getAmount());
    }
    private void pushTank(FluidTankGT tank,Direction side,long limit){
        if(tank.isEmpty()||limit<=0)return;
        var pos=worldPosition.relative(side);if(!level.hasChunkAt(pos))return;
        var handler=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,side.getOpposite());if(handler==null)return;
        var available=tank.drain((int)Math.min(Integer.MAX_VALUE,limit),IFluidHandler.FluidAction.SIMULATE);
        int amount=Math.min(available.getAmount(),Math.max(0,handler.fill(available,IFluidHandler.FluidAction.SIMULATE)));
        if(amount>0){available.setAmount(amount);int accepted=handler.fill(available,IFluidHandler.FluidAction.EXECUTE);tank.remove(Math.min(amount,Math.max(0,accepted)));}
    }
    /** Automation may only touch stopped slots; direct player insertion stops the whole core. */
    public final net.neoforged.neoforge.items.IItemHandler items=new net.neoforged.neoforge.items.IItemHandler(){
        public int getSlots(){return rods.length;}
        public ItemStack getStackInSlot(int slot){return slot>=0&&slot<rods.length?rods[slot].copy():ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return 1;}
        public boolean isItemValid(int slot,ItemStack stack){return !isRemoved()&&slot>=0&&slot<rods.length&&!slotActive(slot)&&isFuelRod(stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
            if(!isItemValid(slot,stack)||!rods[slot].isEmpty())return stack;
            if(!simulate)insertRod(slot,stack);
            return stack.getCount()==1?ItemStack.EMPTY:stack.copyWithCount(stack.getCount()-1);
        }
        public ItemStack extractItem(int slot,int amount,boolean simulate){
            if(isRemoved()||slot<0||slot>=rods.length||amount<=0||slotActive(slot))return ItemStack.EMPTY;
            return simulate?rods[slot].copy():removeRod(slot);
        }
    };
    public final IFluidHandler fluids=new IFluidHandler(){
        public int getTanks(){return 2;}public FluidStack getFluidInTank(int n){return (n==0?input:output).getFluid().copy();}
        public int getTankCapacity(int n){return (int)(n==0?input:output).capacity();}
        public boolean isFluidValid(int n,FluidStack s){return n==0&&ReactorCoolants.of(s)!=null;}
        public int fill(FluidStack s,FluidAction a){return !isRemoved()&&isFluidValid(0,s)?input.fill(s,a):0;}
        public FluidStack drain(FluidStack s,FluidAction a){return output.drain(s,a);}
        public FluidStack drain(int n,FluidAction a){return output.drain(n,a);}
    };
    /** Hand containers can also recover unused coolant; automation drains output only. */
    public final IFluidHandler handFluids=new IFluidHandler(){
        public int getTanks(){return 2;}public FluidStack getFluidInTank(int n){return fluids.getFluidInTank(n);}public int getTankCapacity(int n){return fluids.getTankCapacity(n);}
        public boolean isFluidValid(int n,FluidStack s){return fluids.isFluidValid(n,s);}public int fill(FluidStack s,FluidAction a){return fluids.fill(s,a);}
        public FluidStack drain(int n,FluidAction a){return (output.isEmpty()?input:output).drain(n,a);}
        public FluidStack drain(FluidStack s,FluidAction a){return (output.isEmpty()?input:output).drain(s,a);}
    };
    public net.neoforged.neoforge.items.IItemHandler itemCapability(Direction side){return isRemoved()?null:items;}
    public IFluidHandler fluidCapability(Direction side){return isRemoved()?null:fluids;}
    public void restorePersistentState(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){loadAdditional(tag,lookup);}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);tag.putInt("gt.disabled_slots",disabledSlots);tag.putInt("gt.reactor_version",1);tag.putBoolean("gt.stopped",stopped);tag.putBoolean("gt.failed",failed);tag.putLong("gt.heat",storedHeat);tag.putLong("gt.last_heat",lastHeat);tag.putIntArray("gt.neutrons",neutrons);
        var list=new ListTag();for(var rod:rods)list.add(rod.saveOptional(lookup));tag.put("gt.rods",list);
        var excess=new ListTag();for(var rod:legacyOverflow)excess.add(rod.saveOptional(lookup));tag.put("gt.overflow",excess);
        var in=new CompoundTag();input.writeToNBT(in,lookup);tag.put("gt.input",in);var out=new CompoundTag();output.writeToNBT(out,lookup);tag.put("gt.output",out);
    }
    @Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);Arrays.fill(rods,ItemStack.EMPTY);Arrays.fill(neutrons,0);legacyOverflow.clear();input.setEmpty();output.setEmpty();
        disabledSlots=tag.getInt("gt.disabled_slots")&((1<<rods.length)-1);
        boolean current=tag.contains("gt.reactor_version");stopped=!current||tag.getBoolean("gt.stopped");failed=tag.getBoolean("gt.failed");storedHeat=Math.max(0,tag.getLong("gt.heat"));lastHeat=Math.max(0,tag.getLong("gt.last_heat"));
        if(current){var counts=tag.getIntArray("gt.neutrons");for(int i=0;i<Math.min(counts.length,neutrons.length);i++)neutrons[i]=Math.max(0,counts[i]);}
        var list=tag.getList("gt.rods",10);
        for(int i=0;i<list.size();i++){var entry=list.getCompound(i);var rod=ItemStack.parseOptional(lookup,entry.contains("rod")?entry.getCompound("rod"):entry);if(i<rods.length)rods[i]=rod;else if(!rod.isEmpty())legacyOverflow.add(rod);}
        for(var raw:tag.getList("gt.overflow",10)){var rod=ItemStack.parseOptional(lookup,(CompoundTag)raw);if(!rod.isEmpty())legacyOverflow.add(rod);}
        if(tag.contains("gt.input"))input.readFromNBT(tag.getCompound("gt.input"),lookup);
        else if(tag.contains("gt.coolant"))input.readFromNBT(tag.getCompound("gt.coolant"),lookup);
        else if(tag.contains("gt.water"))input.readFromNBT(tag.getCompound("gt.water"),lookup);
        if(tag.contains("gt.output"))output.readFromNBT(tag.getCompound("gt.output"),lookup);
        if(!current)input.setCapacity(Math.max(64000,input.getAmount()));
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){loadAdditional(tag,lookup);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,net.minecraft.core.HolderLookup.Provider lookup){if(packet.getTag()!=null)loadAdditional(packet.getTag(),lookup);}
    @Override public void dropContents(){if(level==null||level.isClientSide)return;for(int i=0;i<rods.length;i++){BlockContents.drop(this,rods[i]);rods[i]=ItemStack.EMPTY;}for(var rod:legacyOverflow)BlockContents.drop(this,rod);legacyOverflow.clear();setChanged();}
}
