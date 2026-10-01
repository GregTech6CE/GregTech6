package com.gregtech.gregtech.content.cover;

import net.minecraft.world.item.ItemStack;


/** Source controller/detector conditions; independent of a particular machine implementation. */
public enum MachineCoverSpec {
    REDSTONE("redstone_machine_switch","redstoneswitch",0),
    AUTO_REDSTONE("auto_redstone_machine_switch","autoredstoneswitch",0),
    AUTOMATIC("automatic_machine_switch","autoswitch",0),
    TIMER_1("auto_reboot_switch_1m","autotimerswitch/1200",1200),
    TIMER_5("auto_reboot_switch_5m","autotimerswitch/6000",6000),
    TIMER_10("auto_reboot_switch_10m","autotimerswitch/12000",12000),
    TIMER_20("auto_reboot_switch_20m","autotimerswitch/24000",24000),
    TIMER_30("auto_reboot_switch_30m","autotimerswitch/36000",36000),
    POSSIBLE("activity_detector_possible","detectorrunningpossible",-1),
    RUNNING("activity_detector_running","detectorrunningpassively",-1),
    PROCESSING("activity_detector_processing","detectorrunningactively",-1),
    SUCCESS("activity_detector_success","detectorrunningsuccessfully",-1);
    public final String id,texture;
    public final int interval;
    MachineCoverSpec(String id,String texture,int interval){this.id=id;this.texture=texture;this.interval=interval;}
    public record State(boolean possible,boolean running,boolean processing,boolean success){}
    public boolean detector(){return interval<0;}
    public boolean invertible(){return detector()||this==REDSTONE||this==AUTO_REDSTONE;}
    public static MachineCoverSpec of(ItemStack stack){
        if(stack.isEmpty())return null;var key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(key==null||!key.getNamespace().equals("gregtech"))return null;
        for(var spec:values())if(spec.id.equals(key.getPath()))return spec;return null;
    }
    public static boolean inverted(ItemStack stack){return CoverStackData.has(stack)&&CoverStackData.read(stack).getBoolean("gt.cover.inverted");}
    public static boolean strong(ItemStack stack){return CoverStackData.has(stack)&&CoverStackData.read(stack).getBoolean("gt.cover.strong");}
    public boolean allows(State state,int signal,boolean inverted,long timer){
        if(interval>0)return state.processing()||Math.floorMod(timer,interval)>=interval-10;
        boolean powered=(signal>0)!=inverted;
        return switch(this){
            case REDSTONE->powered;
            case AUTO_REDSTONE->state.processing()&&!state.success()||powered;
            case AUTOMATIC->state.possible()||state.processing();
            default->true;
        };
    }
    public int signal(State state,boolean inverted){
        boolean value=switch(this){case POSSIBLE->state.possible();case RUNNING->state.running();case PROCESSING->state.processing();case SUCCESS->state.success();default->false;};
        return detector()&&(value!=inverted)?15:0;
    }
}
