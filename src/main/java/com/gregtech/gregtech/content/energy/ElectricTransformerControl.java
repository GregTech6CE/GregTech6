package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.machine.MachineControl;
import net.minecraft.nbt.CompoundTag;

/** GT6 non-wasting converter controls and its 64-tick trinary activity window. */
public final class ElectricTransformerControl implements MachineControl {
    private final Runnable changed;
    private boolean enabled=true;
    private final boolean[] possible=new boolean[2],emitted=new boolean[2];
    private int direction;
    public void select(boolean reversed){direction=reversed?1:0;}
    private int mode;
    private long history;
    public ElectricTransformerControl(Runnable changed){this.changed=changed;}
    public boolean accepts(){return enabled&&possible[direction]==emitted[direction];}
    public long limit(long maximum){return mode==0?maximum:maximum*(16-mode)/16;}
    public void tick(boolean canEmit,boolean didEmit){possible[direction]=canEmit;emitted[direction]=didEmit;history=(history<<1)|(canEmit?1:0);changed.run();}
    public int visual(){return !enabled||history==0?0:history==-1L?1:2;}
    @Override public boolean supportsMode(){return true;}
    @Override public boolean supportsProgress(){return false;}
    @Override public int mode(){return mode;}
    @Override public int setMode(int value){mode=value&15;changed.run();return mode;}
    @Override public boolean enabled(){return enabled;}
    @Override public boolean setEnabled(boolean value){enabled=value;changed.run();return enabled;}
    @Override public boolean running(){return possible[direction];}
    @Override public boolean active(){return emitted[direction];}
    @Override public long progress(){return 0;}
    @Override public long progressMax(){return 0;}
    public void save(CompoundTag tag){tag.putBoolean("gt.transformer_stopped",!enabled);tag.putInt("gt.transformer_mode",mode);tag.putBoolean("gt.transformer_possible",possible[direction]);tag.putBoolean("gt.transformer_emitted",emitted[direction]);}
    public void load(CompoundTag tag){enabled=!tag.getBoolean("gt.transformer_stopped");mode=tag.getInt("gt.transformer_mode")&15;select(tag.getBoolean("gt.inverted"));java.util.Arrays.fill(possible,tag.getBoolean("gt.transformer_possible"));java.util.Arrays.fill(emitted,tag.getBoolean("gt.transformer_emitted"));history=0;}
}
