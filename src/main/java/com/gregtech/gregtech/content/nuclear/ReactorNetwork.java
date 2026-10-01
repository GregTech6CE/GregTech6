package com.gregtech.gregtech.content.nuclear;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** Two-phase world-end exchange: every emission reads last cycle, independent of BE tick order. */
@Mod.EventBusSubscriber(modid=GregTech.MOD_ID)
public final class ReactorNetwork {
    /**
     * Cores enqueued during the current level tick, drained by {@link #end} at the end of that tick.
     *
     * <p>Weakly keyed: the entry normally lasts less than one tick, but a level that stops ticking
     * between an {@link #enqueue} and its end-of-tick drain (an aborted tick, a removed dimension, a
     * shutdown) would otherwise stay reachable from this static map for the rest of the JVM's life,
     * keeping the whole dimension with it. {@code Level} does not override equals/hashCode, so weak
     * keys are the same identity keys as before.</p>
     */
    private static final Map<Level,Set<ReactorCoreBlockEntity>> PENDING=new WeakHashMap<>();
    private ReactorNetwork(){}
    public static void enqueue(ReactorCoreBlockEntity core){if(core.getLevel()!=null&&!core.getLevel().isClientSide)PENDING.computeIfAbsent(core.getLevel(),l->new HashSet<>()).add(core);}
    @SubscribeEvent public static void end(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.level.isClientSide)return;
        var pending=PENDING.remove(event.level);if(pending==null)return;
        var cores=pending.stream().filter(c->!c.isRemoved()&&event.level.hasChunkAt(c.getBlockPos())).sorted(Comparator.comparingLong(c->c.getBlockPos().asLong())).toList();
        if(event.level.getGameTime()%20==19)exchange(cores);
        for(var core:cores)core.react();
    }
    /** Only the supplied, ticking cores participate; never loads a neighboring chunk. */
    public static void exchange(List<ReactorCoreBlockEntity> cores){
        Map<BlockPos,ReactorCoreBlockEntity> positions=new HashMap<>();
        Map<ReactorCoreBlockEntity,int[]> incoming=new IdentityHashMap<>();
        Map<ReactorCoreBlockEntity,int[]> emissions=new IdentityHashMap<>();
        Map<ReactorCoreBlockEntity,boolean[]> moderated=new IdentityHashMap<>();
        for(var c:cores){
            positions.put(c.getBlockPos(),c);int[] counts=new int[c.rods.length],emit=new int[c.rods.length];boolean[] mods=new boolean[c.rods.length];
            for(int i=0;i<counts.length;i++)if(c.slotActive(i)){counts[i]=RodPhysics.self(c.rods[i],c.coolant());emit[i]=RodPhysics.emission(c.rods[i],c.neutrons[i],c.coolant());mods[i]=RodPhysics.moderated(c.rods[i]);}
            incoming.put(c,counts);emissions.put(c,emit);moderated.put(c,mods);
        }
        for(var c:cores)for(int i=0;i<c.rods.length;i++)if(c.slotActive(i)){
            int count=emissions.get(c)[i];boolean mod=moderated.get(c)[i];
            if(count==0&&!mod)continue;
            if(c.rods.length==1){
                int half=(int)RodPhysics.ceil(count,2);
                for(var side:Direction.Plane.HORIZONTAL){var next=positions.get(c.getBlockPos().relative(side));if(next==null)continue;
                    for(int edge=0;edge<2;edge++)send(c,i,next,edgeSlot(side.getOpposite(),edge,next.rods.length),half,mod,incoming);
                }
            }else{
                int x=i/2,z=i%2;
                for(var side:Direction.Plane.HORIZONTAL){int nx=x+side.getStepX(),nz=z+side.getStepZ();
                    if(nx>=0&&nx<2&&nz>=0&&nz<2)send(c,i,c,nx*2+nz,count,mod,incoming);
                    else{var next=positions.get(c.getBlockPos().relative(side));if(next!=null)send(c,i,next,edgeSlot(side.getOpposite(),side.getAxis()==Direction.Axis.X?z:x,next.rods.length),count,mod,incoming);}
                }
            }
        }
        for(var c:cores){System.arraycopy(incoming.get(c),0,c.neutrons,0,c.neutrons.length);for(var rod:c.rods)RodPhysics.finishCycle(rod);c.setChanged();}
    }
    private static int edgeSlot(Direction side,int edge,int slots){if(slots==1)return 0;return switch(side){case NORTH->edge*2;case SOUTH->edge*2+1;case WEST->edge;case EAST->2+edge;default->throw new IllegalArgumentException();};}
    private static void send(ReactorCoreBlockEntity source,int slot,ReactorCoreBlockEntity target,int targetSlot,int amount,boolean moderated,Map<ReactorCoreBlockEntity,int[]> incoming){
        if(!target.slotActive(targetSlot))return;var result=RodPhysics.receive(target.rods[targetSlot],amount,moderated);
        incoming.get(target)[targetSlot]=RodPhysics.bound((long)incoming.get(target)[targetSlot]+result.absorbed());
        incoming.get(source)[slot]=RodPhysics.bound((long)incoming.get(source)[slot]+result.reflected());
        if(result.moderates())RodPhysics.moderate(source.rods[slot]);
    }
}
