package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.energy.item.IItemEnergy;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

/** GT6 TileEntityBase10EnergyBatBox: item energy and the working buffer are distinct stores. */
public final class BatteryBoxEnergy {
    private final List<ItemStack> inventory;
    private final long voltage;
    private long buffer, receivablePower;
    private int mode;
    private boolean stopped, emitted;
    public BatteryBoxEnergy(List<ItemStack> inventory,long voltage) {
        this.inventory=inventory; this.voltage=voltage;
    }
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IItemEnergy energy
                && energy.isEnergyType(stack,GregTechTags.Energy.EU);
    }
    public boolean canReceive(){return receivablePower>0;}
    public long buffer(){return buffer;}
    public long bufferCapacity(){return voltage*320*inventory.size();}
    public int displayState(){return buffer<voltage?0:buffer>=voltage*300*inventory.size()?1:2;}
    public int mode(){return mode;}
    public void mode(int value){mode=value&15;}
    public boolean enabled(){return !stopped;}
    public void enabled(boolean value){stopped=!value;}
    public boolean running(){return buffer>=voltage;}
    public boolean emitted(){return emitted;}
    public int providers(){return count(false);}
    private int count(boolean charging) {
        int count=0;
        for(var stack:inventory) if(accepts(stack)) {
            var item=(IItemEnergy)stack.getItem();
            if(charging?item.canEnergyInjection(stack,GregTechTags.Energy.EU,voltage)
                    :item.canEnergyExtraction(stack,GregTechTags.Energy.EU,voltage)) count++;
        }
        return count;
    }
    public long stored(){return sum(false);}
    public long capacity(){return sum(true);}
    private long sum(boolean capacity) {
        long total=0;
        for(var stack:inventory) if(accepts(stack)) {
            var item=(IItemEnergy)stack.getItem();
            long amount=Math.max(0,capacity?item.getEnergyCapacity(stack,GregTechTags.Energy.EU)
                    :item.getEnergyStored(stack,GregTechTags.Energy.EU));
            total+=Math.min(Long.MAX_VALUE-total,amount);
        }
        return total;
    }
    /** Called once per server tick before network output. */
    public void tick(long time,Level level,BlockPos pos) {
        emitted=false;
        if(time%20==1) {
            int band=(int)Math.min(7,buffer/(voltage*40*inventory.size()));
            long packets=(band==0||band==7)?40:(band==1||band==6)?20:0;
            if(packets>0) for(var stack:inventory) if(accepts(stack)) {
                var item=(IItemEnergy)stack.getItem();
                if(band<2) {
                    long used=item.doEnergyExtraction(GregTechTags.Energy.EU,stack,voltage,packets,level,pos,true);
                    buffer+=voltage*Math.max(0,Math.min(packets,used));
                } else {
                    long offered=Math.min(packets,buffer/voltage);
                    long used=item.doEnergyInjection(GregTechTags.Energy.EU,stack,voltage,offered,level,pos,true);
                    buffer-=voltage*Math.max(0,Math.min(offered,used));
                }
            }
        }
        receivablePower=count(true)*voltage*2;
    }
    /** Caller handles overvoltage before this method, as GT6 does. */
    public long inject(long size,long amount,boolean execute) {
        if(size<=0||amount<=0||receivablePower<=0||buffer>=bufferCapacity())return 0;
        long space=bufferCapacity()-buffer;
        long fitting=space/size+(space%size==0?0:1);
        // Original loop stops once (consumed-1)*size <= remaining power.
        long used=Math.min(amount,Math.min(fitting,receivablePower/size+1));
        if(execute){buffer+=used*size;receivablePower-=used*size;}
        return used;
    }
    public long offered() {
        int providers=providers();
        return stopped?0:Math.min(buffer/voltage,mode==0?providers:Math.min(mode,providers));
    }
    public void emitted(long packets) {
        long used=Math.max(0,Math.min(offered(),packets));
        buffer-=used*voltage; emitted=used>0;
    }
    public void save(CompoundTag tag) {
        tag.putLong("gt.buffer",buffer);tag.putBoolean("gt.battery_stopped",stopped);
        tag.putInt("gt.battery_mode",mode);
    }
    public void load(CompoundTag tag) {
        buffer=Math.max(0,Math.min(bufferCapacity()+voltage*2,tag.getLong("gt.buffer")));
        stopped=tag.getBoolean("gt.battery_stopped");mode=tag.getInt("gt.battery_mode")&15;
        receivablePower=0;emitted=false;
    }
}
