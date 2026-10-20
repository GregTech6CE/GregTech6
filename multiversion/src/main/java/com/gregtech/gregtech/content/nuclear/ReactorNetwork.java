package com.gregtech.gregtech.content.nuclear;

//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.GregTech;
//?}
import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
import java.util.*;
/** Two-phase world-end exchange: every emission reads last cycle, independent of BE tick order. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
@Mod.EventBusSubscriber(modid=GregTech.MODID)
//?}
public final class ReactorNetwork {
    /**
     * Cores enqueued during the current level tick, drained by {@link #end} at the end of that tick.
     *
     * <p>The normal handler removes the entry before exchange and reaction. Weak keys alone do not
     * guarantee cleanup if a level stops ticking before that drain: each value strongly holds cores,
     * whose level field can refer back to the key. This class currently has no unload/stop cleanup
     * hook, so an entry can remain if the same level never receives another end-of-tick event.</p>
     */
    private static final Map<Level,Set<ReactorCoreBlockEntity>> PENDING=new WeakHashMap<>();
    private ReactorNetwork(){}
    public static void enqueue(ReactorCoreBlockEntity core){if(core.getLevel()!=null&&!core.getLevel().isClientSide)PENDING.computeIfAbsent(core.getLevel(),l->new HashSet<>()).add(core);}
//? if >=1.21.1 {
    /*@SubscribeEvent public static void end(LevelTickEvent.Post event){
        if(event.getLevel().isClientSide)return;
        var pending=PENDING.remove(event.getLevel());if(pending==null)return;
        var cores=pending.stream().filter(c->!c.isRemoved()&&event.getLevel().hasChunkAt(c.getBlockPos())).sorted(Comparator.comparingLong(c->c.getBlockPos().asLong())).toList();
        if(event.getLevel().getGameTime()%20==19)exchange(cores);
*///?} else {
    @SubscribeEvent public static void end(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.level.isClientSide)return;
        var pending=PENDING.remove(event.level);if(pending==null)return;
        var cores=pending.stream().filter(c->!c.isRemoved()&&event.level.hasChunkAt(c.getBlockPos())).sorted(Comparator.comparingLong(c->c.getBlockPos().asLong())).toList();
        if(event.level.getGameTime()%20==19)exchange(cores);
//?}
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
