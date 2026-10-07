package com.gregtech.gregtech.content.cover;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.machine.MachineControl;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** Server behavior for the original interactive panels, scales and cover gate. No client dependency. */
public final class PanelCoverRuntime {
    public static final String VALUE="gt.panel.value",STYLE="gt.panel.style",RESET="gt.panel.reset",COUNTDOWN="gt.panel.countdown";
    private final PanelCoverHost host;
    private final int[] signals=new int[6];
    private final boolean[] strong=new boolean[6];
    private boolean stopped,restore=true;
    private long utilityTick=Long.MIN_VALUE;
    public PanelCoverRuntime(PanelCoverHost host){this.host=host;}
    public boolean stopped(){return stopped;}
    public static int value(ItemStack stack){return stack.hasTag()?stack.getTag().getInt(VALUE):0;}
    public static int style(ItemStack stack){return stack.hasTag()?stack.getTag().getInt(STYLE):0;}
    public void loaded(){restore=true;}
    public void restoreStopped(boolean value){stopped=value;restore=true;}
    private boolean server(){return host.coverOwner().getLevel()!=null&&!host.coverOwner().getLevel().isClientSide&&!host.coverOwner().isRemoved();}
    public int incoming(Direction side){
        var owner=host.coverOwner();var level=owner.getLevel();var pos=owner.getBlockPos().relative(side);
        return level!=null&&level.hasChunkAt(pos)?level.getSignal(pos,side):0;
    }
    private MachineControl control(Direction side){var c=host.coverControl(side);return c!=null&&c.available()?c:null;}
    private long[] energy(Direction side){
        if(host.coverOwner() instanceof IEnergyBlock energy){
            var types=energy.getEnergyCapacitorTypes(side);
            if(!types.isEmpty()){var type=types.iterator().next();return new long[]{energy.getEnergyStored(type,side),energy.getEnergyCapacity(type,side)};}
        }
        return null;
    }
    public boolean canAttach(Direction side,ItemStack stack){
        if(!ComponentCoverRuntime.canAttach(host,side,stack))return false;
        if(host.coverOwner() instanceof com.gregtech.gregtech.content.logistics.LogisticsCoverHost logistics&&!logistics.logisticsCovers().get(side).isEmpty())return false;
        // GT6 logistics covers only attach to ITileEntityLogistics; ordinary machines and pipes
        // must not accept them merely because they are registered cover items.
        if(com.gregtech.gregtech.content.logistics.LogisticsCoverType.of(stack)!=null)
            return host.coverOwner() instanceof com.gregtech.gregtech.content.logistics.LogisticsHost logistics
                    && logistics.canLogistics(null);
        var id=CoverItems.behavior(stack);
        var candidateControl=control(side);
        if(CoverItems.TAG_SELECTOR.equals(id))return candidateControl!=null&&candidateControl.supportsMode();
        if(CoverItems.REDSTONE_TORCH.equals(id)||CoverItems.REDSTONE_REPEATER.equals(id))return host.coverOwner() instanceof com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity&&host.coverOwner().getBlockState().getBlock() instanceof com.gregtech.gregtech.block.energy.SignalWireBlock wire&&wire.insulated();
        if(CoverAttachmentBehaviors.DRAIN.equals(id)||CoverAttachmentBehaviors.AIR_VENT.equals(id))return host.componentTicks()&&host.componentFluids(side)!=null&&host.componentFluids(side).getTanks()>0;
        if(CoverUtilityBehaviors.FILTER_ITEM.equals(id)){
            var owner=host.coverOwner();var level=owner.getLevel();
            return host.componentItems(side)!=null&&!(owner instanceof com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity&&level!=null&&level.getBlockEntity(owner.getBlockPos().relative(side)) instanceof com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity);
        }
        if(CoverUtilityBehaviors.FILTER_FLUID.equals(id))return host.componentFluids(side)!=null&&host.componentFluids(side).getTanks()>0;
        if(CoverUtilityBehaviors.PRESSURE_VALVE.equals(id))return host.coverOwner() instanceof com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
        if(MachineCoverSpec.of(stack)!=null)return candidateControl!=null&&candidateControl.supportsSwitch();
        var panel=PanelCover.of(stack);if(panel==null)return CoverItems.isCover(stack);
        var c=control(side);
        if(panel.selector())return c!=null&&c.supportsMode();
        if(panel==PanelCover.PROGRESS)return c!=null&&c.supportsProgress();
        if(panel==PanelCover.STATUS)return c!=null&&c.supportsSwitch();
        if(panel.energy())return energy(side)!=null;
        return true;
    }
    public void attached(Direction side){
        ComponentCoverRuntime.attached(host,side);
        var stack=host.getCover(side);var panel=PanelCover.of(stack);var c=control(side);
        if(panel!=null&&panel.selector()&&c!=null)stack.getOrCreateTag().putInt(VALUE,c.mode()&15);
        refreshStopped();CoverConnections.attached(host,side);changed();decorativeSound(stack);
    }
    public void changed(){
        var owner=host.coverOwner();owner.setChanged();
        if(server())owner.getLevel().sendBlockUpdated(owner.getBlockPos(),owner.getBlockState(),owner.getBlockState(),2);
    }
    private boolean setValue(ItemStack stack,int value){
        if(value(stack)==value)return false;stack.getOrCreateTag().putInt(VALUE,value);return true;
    }
    /** Exact source quantisation without overflowing long at large capacities. */
    public static int scale(long amount,long capacity,int maximum){
        if(amount<=0||capacity<=0)return 0;if(amount>=capacity)return maximum;
        // floor((capacity-amount)*(maximum-1)/capacity), using exact integer arithmetic.
        long remaining=capacity-amount;
        int quotient=remaining<=Long.MAX_VALUE/(maximum-1)
            ?(int)(remaining*(maximum-1)/capacity)
            :java.math.BigInteger.valueOf(remaining).multiply(java.math.BigInteger.valueOf(maximum-1)).divide(java.math.BigInteger.valueOf(capacity)).intValue();
        return maximum-1-Math.min(maximum-2,quotient);
    }
    private void refreshStopped(){
        if(!host.hasAttachedCovers())stopped=false;
        // Removing CoverControllerCovers leaves its stopped flag latched until all covers are gone.
        // GT6 evaluates cover controllers in side order; the final one owns the shared stopped flag.
        for(var side:Direction.values())if(PanelCover.of(host.getCover(side))==PanelCover.CONTROLLER)
            stopped=(incoming(side)>0)==MachineCoverSpec.inverted(host.getCover(side));
    }
    /** Caller has already freed the face; detached items carry the default cover configuration. */
    public ItemStack removed(Direction side,ItemStack stack){
        var c=control(side);var panel=PanelCover.of(stack);var spec=MachineCoverSpec.of(stack);
        if(c!=null){
            if(spec!=null&&!spec.detector()||panel==PanelCover.STATUS)c.setEnabled(true);
            if(panel!=null&&panel.selector()||CoverItems.TAG_SELECTOR.equals(CoverItems.behavior(stack)))c.setMode(0);
        }
        refreshStopped();changed();afterTick();decorativeSound(stack);
        return stack.isEmpty()?ItemStack.EMPTY:new ItemStack(stack.getItem());
    }
    /** Original CoverTextureSimple uses its wood/stone dig sound for both actions. */
    private void decorativeSound(ItemStack stack) {
        if(server()&&stack.getItem() instanceof com.gregtech.gregtech.item.PanelItemView panel) {
            var owner=host.coverOwner();owner.getLevel().playSound(null,owner.getBlockPos(),
                panel.panelSpec().kind().equals("wood")?net.minecraft.sounds.SoundEvents.WOOD_BREAK:net.minecraft.sounds.SoundEvents.STONE_BREAK,
                net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);
        }
    }
    public void beforeTick(){
        if(!server())return;boolean wasStopped=stopped;refreshStopped();if(wasStopped!=stopped)for(var face:Direction.values())if(PanelCover.of(host.getCover(face))==PanelCover.SHUTTER)CoverConnections.update(host,face,!shuttered(face));boolean changed=false;
        for(var side:Direction.values()){
            var stack=host.getCover(side);var c=control(side);if(c==null)continue;
            if(CoverItems.TAG_SELECTOR.equals(CoverItems.behavior(stack))&&c.supportsMode()&&!stopped)c.setMode(CoverItems.selectorTag(stack));
            var spec=MachineCoverSpec.of(stack);
            if(spec!=null&&!spec.detector()&&!(host instanceof com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity))
                c.setEnabled(spec.allows(new MachineCoverSpec.State(host.coverPossible(side),c.running(),c.active(),false),incoming(side),MachineCoverSpec.inverted(stack),host.coverOwner().getLevel().getGameTime()));
        }
        for(var side:Direction.values()){
            var stack=host.getCover(side);var panel=PanelCover.of(stack);if(panel==null||!panel.selector())continue;
            var c=control(side);if(c==null||!c.supportsMode()||stopped)continue;
            if(panel==PanelCover.REDSTONE)c.setMode(incoming(side));
            else if(restore)c.setMode(value(stack)&15);
            if(panel==PanelCover.BUTTONS&&stack.hasTag()){
                int ticks=stack.getTag().getInt(COUNTDOWN);
                if(ticks>1){stack.getTag().putInt(COUNTDOWN,--ticks);host.coverOwner().setChanged();if(ticks==1)c.setMode(0);}
            }
            changed|=setValue(stack,c.mode()&15);
        }
        restore=false;if(changed)changed();
    }
    public void afterTick(){
        if(!server())return;
        long now=host.coverOwner().getLevel().getGameTime();
        if(utilityTick!=now){utilityTick=now;tickAttachments(now);}
        boolean visualChanged=false,signalChanged=false;
        int conducted=0;
        for(var side:Direction.values())if(PanelCover.of(host.getCover(side))==PanelCover.CONDUCTOR_IN)conducted=Math.max(conducted,incoming(side));
        for(var side:Direction.values()){
            var stack=host.getCover(side);var panel=PanelCover.of(stack);int signal=0;boolean power=false;
            if(panel!=null){
                var c=control(side);int value=value(stack);
                switch(panel){
                    case PROGRESS -> value=c!=null&&c.supportsProgress()?scale(c.progress(),c.progressMax(),15):0;
                    case ENERGY,ENERGY_DISPLAY -> {var energy=energy(side);value=energy==null?0:scale(energy[0],energy[1],panel==PanelCover.ENERGY?15:10);}
                    case STATUS -> {
                        value=0;
                        if(host.coverSupportsPossible())value|=32|(host.coverPossible(side)?1:0);
                        if(c!=null)value|=64|128|256|(c.running()?2:0)|(c.active()?4:0)|(c.enabled()?8:0);
                    }
                    case CONDUCTOR_OUT -> value=conducted;
                    default -> {}
                }
                visualChanged|=setValue(stack,value);
                if(panel.output())signal=Math.max(0,Math.min(15,value));
                if(panel==PanelCover.PROGRESS||panel==PanelCover.ENERGY)if(MachineCoverSpec.inverted(stack))signal=15-signal;
                power=panel.strongConfig()&&MachineCoverSpec.strong(stack);
            }
            var spec=MachineCoverSpec.of(stack);
            if(spec!=null&&spec.detector()){
                var c=control(side);
                signal=c==null?0:spec.signal(new MachineCoverSpec.State(host.coverPossible(side),c.running(),c.active(),false),MachineCoverSpec.inverted(stack));
                power=MachineCoverSpec.strong(stack);
            }
            if(host.coverOwner() instanceof com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity wire){
                var id=CoverItems.behavior(stack);
                if(CoverItems.REDSTONE_TORCH.equals(id))signal=wire.signal()>0?0:15;
                else if(CoverItems.REDSTONE_REPEATER.equals(id))signal=wire.signal()>0?15:0;
                if(CoverItems.REDSTONE_TORCH.equals(id)||CoverItems.REDSTONE_REPEATER.equals(id))power=true;
            }
            int i=side.ordinal();
            if(signals[i]!=signal||strong[i]!=power){
                boolean old=strong[i];signals[i]=signal;strong[i]=power;signalChanged=true;
                var owner=host.coverOwner();var neighbor=owner.getBlockPos().relative(side);
                if((old||power)&&owner.getLevel().hasChunkAt(neighbor))owner.getLevel().updateNeighborsAt(neighbor,owner.getBlockState().getBlock());
            }
        }
        if(visualChanged)changed();
        if(signalChanged){var owner=host.coverOwner();owner.getLevel().updateNeighborsAt(owner.getBlockPos(),owner.getBlockState().getBlock());}
    }
    private void tickAttachments(long now){
        if(stopped||!host.componentTicks()
                ||host.coverOwner() instanceof com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity
                ||host.coverOwner() instanceof com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity
                ||host.coverOwner() instanceof com.gregtech.gregtech.blockentity.machine.ItemPipeBlockEntity)return;
        for(var side:Direction.values()){
            var id=CoverItems.behavior(host.getCover(side));
            if(!CoverAttachmentBehaviors.DRAIN.equals(id)&&!CoverAttachmentBehaviors.AIR_VENT.equals(id))continue;
            var sink=host.componentFluids(side);if(sink==null||sink.getTanks()==0)continue;
            var owner=host.coverOwner();
            if(CoverAttachmentBehaviors.DRAIN.equals(id))CoverAttachmentBehaviors.tickDrain(owner.getLevel(),owner.getBlockPos(),side,sink,now);
            else CoverAttachmentBehaviors.tickVent(owner.getLevel(),owner.getBlockPos(),side,sink,now);
        }
    }
    public int signal(Direction side){return signals[side.ordinal()];}
    public int strongSignal(Direction side){return strong[side.ordinal()]?signal(side):0;}
    public boolean shuttered(Direction side){
        var stack=host.getCover(side);
        return PanelCover.of(stack)==PanelCover.SHUTTER&&(!MachineCoverSpec.inverted(stack)==stopped);
    }
    public boolean configure(Direction side,boolean cutter,boolean chisel){
        var stack=host.getCover(side);var panel=PanelCover.of(stack);
        var spec=MachineCoverSpec.of(stack);
        if(panel==null){
            if(spec==null||chisel||(cutter?!spec.detector():!spec.invertible()))return false;
            if(server()){CoverStackData.putBoolean(stack,cutter?"gt.cover.strong":"gt.cover.inverted",!(cutter?MachineCoverSpec.strong(stack):MachineCoverSpec.inverted(stack)));changed();afterTick();}
            return true;
        }
        if(chisel&&panel!=PanelCover.BUTTONS&&panel!=PanelCover.STATUS)return false;
        if(!chisel&&(cutter?!panel.strongConfig():!panel.invertible()&&panel!=PanelCover.BUTTONS))return false;
        if(!server())return true;
        var tag=stack.getOrCreateTag();
        if(chisel)tag.putInt(STYLE,Math.floorMod(style(stack)+1,panel==PanelCover.BUTTONS?8:2));
        else if(cutter)tag.putBoolean("gt.cover.strong",!MachineCoverSpec.strong(stack));
        else if(panel==PanelCover.BUTTONS){tag.putBoolean(RESET,!tag.getBoolean(RESET));tag.putInt(COUNTDOWN,0);}
        else tag.putBoolean("gt.cover.inverted",!MachineCoverSpec.inverted(stack));
        refreshStopped();if(panel==PanelCover.SHUTTER)CoverConnections.update(host,side,!shuttered(side));changed();afterTick();return true;
    }
    public boolean click(Direction side,double u,double v){
        var stack=host.getCover(side);var panel=PanelCover.of(stack);if(panel==null)return false;
        if(stopped&&panel.selector())return false;
        var c=control(side);
        if(panel==PanelCover.STATUS){
            if(c==null||u<10/16.0||(Math.floorMod(style(stack),2)==0?v<12/16.0:v>4/16.0))return false;
            if(server()){c.setEnabled(!c.enabled());afterTick();}return true;
        }
        if(panel!=PanelCover.MANUAL&&panel!=PanelCover.BUTTONS&&panel!=PanelCover.EMITTER)return false;
        if(panel.selector()&&(c==null||!c.supportsMode()))return false;
        int next=panel==PanelCover.BUTTONS?Math.min(3,(int)(u*4))+4*Math.min(3,(int)(v*4)):CoverFaceCoordinates.select(value(stack)&15,u,v);
        if(next<0)return false;
        if(server()){
            if(panel.selector())next=c.setMode(next)&15;
            setValue(stack,next);
            if(panel==PanelCover.BUTTONS&&stack.getOrCreateTag().getBoolean(RESET))stack.getOrCreateTag().putInt(COUNTDOWN,10);
            changed();afterTick();
        }
        return true;
    }
}
