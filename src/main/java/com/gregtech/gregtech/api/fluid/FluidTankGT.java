package com.gregtech.gregtech.api.fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.Map;
/** Original FluidTankGT API, backed by the single shared long storage implementation. */
public class FluidTankGT implements IFluidHandler {
    private FluidStack fluid=FluidStack.EMPTY;
    private final LongFluidStorage storage;
    private boolean gasProof,acidProof,plasmaProof,magicProof;
    private long maxTemperature=Long.MAX_VALUE;
    private Runnable onChanged=()->{};
    public FluidTankGT(long capacity){storage=new LongFluidStorage(capacity);}
    public FluidTankGT(){this(Long.MAX_VALUE);}
    public FluidTankGT setOnChanged(Runnable v){onChanged=v;return this;}
    public FluidTankGT setPreventDraining(boolean v){storage.preventDraining(v);return this;}
    public FluidTankGT setKeepFilterOnEmpty(boolean v){storage.keepFilterOnEmpty(v);return this;}
    public FluidTankGT setVoidExcess(boolean v){storage.voidExcess(v);return this;}
    public FluidTankGT setAdjustableCapacity(Map<String,Long> v,long m){storage.adjustableCapacity(v,m);return this;}
    public FluidTankGT setGasProof(boolean v){gasProof=v;return this;}
    public FluidTankGT setAcidProof(boolean v){acidProof=v;return this;}
    public FluidTankGT setPlasmaProof(boolean v){plasmaProof=v;return this;}
    public FluidTankGT setMagicProof(boolean v){magicProof=v;return this;}
    public FluidTankGT setMaxTemperature(long v){maxTemperature=v;return this;}
    public boolean isGasProof(){return gasProof;}
    public boolean isAcidProof(){return acidProof;}
    public boolean isPlasmaProof(){return plasmaProof;}
    public boolean isMagicProof(){return magicProof;}
    public long maxTemperature(){return maxTemperature;}
    private String key(){return fluid.isEmpty()?null:fluid.getFluid().getFluidType().toString();}
    public long add(long offered){
        if(offered<=0||fluid.isEmpty())return 0;
        long space=capacity()-getAmount();
        long accepted=storage.add(offered,key());
        if(space>0)onChanged.run();
        return accepted;
    }
    public long add(long offered,FluidStack match){return match==null||match.isEmpty()||fluid.isEmpty()
            ||match.getFluid()!=fluid.getFluid()?0:add(offered);}
    private void clearSpentIdentity(){if(getAmount()<=0&&!storage.keepsFilterOnEmpty())fluid=FluidStack.EMPTY;}
    public long remove(long requested){
        if(storage.preventsDraining()||requested<=0||fluid.isEmpty())return 0;
        long removed=storage.remove(requested);clearSpentIdentity();onChanged.run();return removed;
    }
    public boolean drainAll(long requested){
        if(!storage.drainAll(requested))return false;
        clearSpentIdentity();onChanged.run();return true;
    }
    public void setFluid(FluidStack value,long amount){fluid=value.copy();storage.setAmount(amount);onChanged.run();}
    public void setFluid(FluidStack value){setFluid(value,value.getAmount());}
    public void setEmpty(){fluid=FluidStack.EMPTY;storage.setAmount(0);onChanged.run();}
    public long capacity(){return storage.capacity(key());}
    public long capacity(FluidStack value){return storage.capacity(value==null||value.isEmpty()?null:value.getFluid().getFluidType().toString());}
    public long capacity(String key){return storage.capacity(key);}
    public long baseCapacity(){return storage.baseCapacity();}
    public void setCapacity(long value){storage.setCapacity(value);}
    public long getAmount(){return storage.amount();}
    public FluidStack getFluid(){if(fluid.isEmpty())return FluidStack.EMPTY;var result=fluid.copy();result.setAmount(bindInt(getAmount()));return result;}
    public FluidStack getFluidLong(){return fluid.isEmpty()?FluidStack.EMPTY:fluid.copy();}
    public boolean isEmpty(){return fluid.isEmpty()||getAmount()<=0;}
    public int getCapacity(){return bindInt(capacity());}
    /** Hazard admission is the owning BE's responsibility, as in the original. */
    public boolean isFluidValid(FluidStack stack){return true;}
    @Override public int getTanks(){return 1;}
    @Override public FluidStack getFluidInTank(int tank){return getFluid();}
    @Override public int getTankCapacity(int tank){return getCapacity();}
    @Override public boolean isFluidValid(int tank,FluidStack stack){return isFluidValid(stack);}
    @Override public int fill(FluidStack resource,FluidAction action){
        if(resource.isEmpty())return 0;
        if(fluid.isEmpty()){
            int accepted=bindInt(Math.min(resource.getAmount(),capacity()));
            if(accepted<=0)return 0;
            if(action.execute()){fluid=resource.copy();fluid.setAmount(1);storage.setAmount(accepted);onChanged.run();}
            return accepted;
        }
        if(!fluid.isFluidEqual(resource))return 0;
        int accepted=bindInt(Math.min(resource.getAmount(),capacity()-getAmount()));
        if(accepted<=0)return 0;
        if(action.execute()){storage.setAmount(getAmount()+accepted);onChanged.run();}
        return accepted;
    }
    @Override public FluidStack drain(FluidStack resource,FluidAction action){
        if(resource.isEmpty()||fluid.isEmpty()||storage.preventsDraining()||!fluid.isFluidEqual(resource))return FluidStack.EMPTY;
        return drain(resource.getAmount(),action);
    }
    @Override public FluidStack drain(int requested,FluidAction action){
        if(requested<=0||fluid.isEmpty()||storage.preventsDraining())return FluidStack.EMPTY;
        int removed=bindInt(Math.min(requested,getAmount()));if(removed<=0)return FluidStack.EMPTY;
        var result=fluid.copy();result.setAmount(removed);
        if(action.execute()){storage.remove(removed);clearSpentIdentity();onChanged.run();}
        return result;
    }
    public static int bindInt(long value){return LongFluidStorage.bindInt(value);}
    public void writeToNBT(net.minecraft.nbt.CompoundTag tag){
        tag.putLong("Amount",getAmount());tag.putLong("Capacity",baseCapacity());
        if(!fluid.isEmpty())tag.put("Fluid",fluid.writeToNBT(new net.minecraft.nbt.CompoundTag()));
    }
    public void readFromNBT(net.minecraft.nbt.CompoundTag tag){
        storage.restore(tag.getLong("Amount"),tag.contains("Capacity")?tag.getLong("Capacity"):null);
        if(tag.contains("Fluid"))fluid=FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid"));
        else{fluid=FluidStack.EMPTY;storage.setAmount(0);}
    }
}
