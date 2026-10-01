package com.gregtech.gregtech.content.energy;




/** GT6 non-wasting converter controls and its 64-tick trinary activity window. */
public class TransformerControlState {
    private final Runnable changed;
    private boolean enabled=true;
    private final boolean[] possible=new boolean[2],emitted=new boolean[2];
    private int direction;
    public void select(boolean reversed){direction=reversed?1:0;}
    private int mode;
    private long history;
    public TransformerControlState(Runnable changed){this.changed=changed;}
    public boolean accepts(){return enabled&&possible[direction]==emitted[direction];}
    public long limit(long maximum){return mode==0?maximum:maximum*(16-mode)/16;}
    public void tick(boolean canEmit,boolean didEmit){possible[direction]=canEmit;emitted[direction]=didEmit;history=(history<<1)|(canEmit?1:0);changed.run();}
    public int visual(){return !enabled||history==0?0:history==-1L?1:2;}
    public boolean supportsMode(){return true;}
    public boolean supportsProgress(){return false;}
    public int mode(){return mode;}
    public int setMode(int value){mode=value&15;changed.run();return mode;}
    public boolean enabled(){return enabled;}
    public boolean setEnabled(boolean value){enabled=value;changed.run();return enabled;}
    public boolean running(){return possible[direction];}
    public boolean active(){return emitted[direction];}
    public long progress(){return 0;}
    public long progressMax(){return 0;}
    public record Snapshot(boolean enabled,int mode,boolean possible,boolean emitted){}
    public Snapshot snapshot(){return new Snapshot(enabled,mode,possible[direction],emitted[direction]);}
    public void restore(Snapshot saved,boolean reversed){enabled=saved.enabled();mode=saved.mode()&15;select(reversed);java.util.Arrays.fill(possible,saved.possible());java.util.Arrays.fill(emitted,saved.emitted());history=0;}
}
