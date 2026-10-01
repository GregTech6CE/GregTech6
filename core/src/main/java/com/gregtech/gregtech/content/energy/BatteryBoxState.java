package com.gregtech.gregtech.content.energy;







import java.util.List;

/** GT6 TileEntityBase10EnergyBatBox: item energy and the working buffer are distinct stores. */
public final class BatteryBoxState {
    private final List<Battery> inventory;
    private final long voltage;
    private long buffer, receivablePower;
    private int mode;
    private boolean stopped, emitted;
    public BatteryBoxState(List<Battery> inventory,long voltage) {
        this.inventory=inventory; this.voltage=voltage;
    }
    public interface Battery {
        boolean accepts();boolean canInject(long voltage);boolean canExtract(long voltage);
        long stored();long capacity();long extract(long voltage,long packets);long inject(long voltage,long packets);
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
        for(var stack:inventory) if(stack.accepts()) {
            var item=stack;
            if(charging?item.canInject(voltage)
                    :item.canExtract(voltage)) count++;
        }
        return count;
    }
    public long stored(){return sum(false);}
    public long capacity(){return sum(true);}
    private long sum(boolean capacity) {
        long total=0;
        for(var stack:inventory) if(stack.accepts()) {
            var item=stack;
            long amount=Math.max(0,capacity?item.capacity()
                    :item.stored());
            total+=Math.min(Long.MAX_VALUE-total,amount);
        }
        return total;
    }
    /** Called once per server tick before network output. */
    public void tick(long time) {
        emitted=false;
        if(time%20==1) {
            int band=(int)Math.min(7,buffer/(voltage*40*inventory.size()));
            long packets=(band==0||band==7)?40:(band==1||band==6)?20:0;
            if(packets>0) for(var stack:inventory) if(stack.accepts()) {
                var item=stack;
                if(band<2) {
                    long used=item.extract(voltage,packets);
                    buffer+=voltage*Math.max(0,Math.min(packets,used));
                } else {
                    long offered=Math.min(packets,buffer/voltage);
                    long used=item.inject(voltage,offered);
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
    public record Snapshot(long buffer,boolean stopped,int mode){}
    public Snapshot snapshot(){return new Snapshot(buffer,stopped,mode);}
    public void restore(Snapshot saved){buffer=Math.max(0,Math.min(bufferCapacity()+voltage*2,saved.buffer()));stopped=saved.stopped();mode=saved.mode()&15;receivablePower=0;emitted=false;}
}
