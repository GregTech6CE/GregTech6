package com.gregtech.gregtech.api.machine;

import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.function.*;

/** Small adapters owned by each machine definition; no central instanceof registry. */
public final class MachineControls {
    private MachineControls(){}
    public static MachineControl switchable(BlockEntity owner,BooleanSupplier enabled,Consumer<Boolean> setEnabled,BooleanSupplier running,BooleanSupplier active){
        return switchable(owner,enabled,setEnabled,running,active,null,null);
    }
    public static MachineControl switchable(BlockEntity owner,BooleanSupplier enabled,Consumer<Boolean> setEnabled,BooleanSupplier running,BooleanSupplier active,IntSupplier mode,IntConsumer setMode){
        return new MachineControl(){
            private boolean writable(){return !owner.isRemoved()&&(owner.getLevel()==null||!owner.getLevel().isClientSide);}
            public boolean available(){return !owner.isRemoved();}
            public boolean enabled(){return available()&&enabled.getAsBoolean();}
            public boolean setEnabled(boolean value){if(writable())setEnabled.accept(value);return enabled();}
            public boolean running(){return available()&&running.getAsBoolean();}
            public boolean active(){return available()&&active.getAsBoolean();}
            public boolean supportsProgress(){return false;}
            public long progress(){return 0;}
            public long progressMax(){return 0;}
            public boolean supportsMode(){return mode!=null&&setMode!=null;}
            public int mode(){return available()&&supportsMode()?mode.getAsInt():0;}
            public int setMode(int value){if(writable()&&supportsMode())setMode.accept(Math.max(0,Math.min(15,value)));return mode();}
        };
    }
}
