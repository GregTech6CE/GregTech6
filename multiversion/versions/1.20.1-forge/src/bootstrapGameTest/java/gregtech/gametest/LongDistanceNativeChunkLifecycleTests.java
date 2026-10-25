package gregtech.gametest;

import gregtech.content.block.misc.LongDistEndpointBlock;
import gregtech.content.block.misc.LongDistEndpointBlockEntity;
import gregtech.content.registry.GTLongDistance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import java.util.*;

/** Actual UNKNOWN33 expiry and disk-backed fresh chunks; not manual ChunkEvent posting. */
@GameTestHolder("gt6ce_long_distance_reload")
@PrefixGameTestTemplate(false)
public final class LongDistanceNativeChunkLifecycleTests {
    @GameTest(template="test_blueprint_empty",timeoutTicks=1200)
    public static void nativeChunkReloadRevalidatesHintsWithoutBreakingAuthenticatedCache(GameTestHelper h){
        var f=new Lifecycle(h);
        try{
            GameTestInfo info=null;
            for(var field:GameTestHelper.class.getDeclaredFields())if(field.getType()==GameTestInfo.class){field.setAccessible(true);info=(GameTestInfo)field.get(h);break;}
            if(info==null)throw new AssertionError("native test cleanup listener unavailable");
            info.addListener(new GameTestListener(){
                public void testStructureLoaded(GameTestInfo i){}
                public void testPassed(GameTestInfo i){f.close();}
                public void testFailed(GameTestInfo i){f.close();}
            });
            f.start();
            h.onEachTick(()->{if(!f.closed)try{f.poll();}catch(RuntimeException|Error e){f.close();throw e;}});
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        catch(RuntimeException|Error e){f.close();throw e;}
    }

    public static final class Lifecycle {
        private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
        // FULL33 tickets still retain non-FULL generation holders through maxDistance.
        // The selected middle must lie outside BOTH endpoint retention neighborhoods.
        private static final int RETENTION_RADIUS=net.minecraft.world.level.chunk.ChunkStatus.maxDistance();
        private static final int MIDDLE_CHUNKS=RETENTION_RADIUS+1;
        private static final int SPAN_CHUNKS=2*MIDDLE_CHUNKS;
        private static final int MIDDLE_OFFSET=16*MIDDLE_CHUNKS,RECEIVER_OFFSET=16*SPAN_CHUNKS;
        private final GameTestHelper h;
        private final ServerLevel level;
        // Exactly on a north chunk boundary: successful BFS encounters unavailable northern neighbors.
        private final BlockPos sourcePos=new BlockPos(18_100_001,80,18_100_000);
        private final BlockPos receiverPos=sourcePos.east(RECEIVER_OFFSET),inputPos=sourcePos.west(),outputPos=receiverPos.east();
        private final ChunkPos sourceChunk=new ChunkPos(sourcePos),middleChunk=new ChunkPos(sourcePos.east(MIDDLE_OFFSET)),receiverChunk=new ChunkPos(receiverPos);
        private final ChunkPos branchChunk=new ChunkPos(sourceChunk.x,sourceChunk.z-1);
        private final Set<ChunkPos> unloaded=new HashSet<>(),loaded=new HashSet<>();
        private final Map<BlockPos,BlockState> originals=new LinkedHashMap<>();
        private final Set<BlockPos> ownedChests=new HashSet<>();
        private LongDistEndpointBlockEntity oldSource,oldReceiver,freshSource;
        private net.minecraft.world.level.chunk.LevelChunk oldMiddle,oldSourceChunk,oldReceiverChunk;
        private boolean registered,closed,loadCallbackQuery;
        private int phase;
        private long transitionTick=-1;
        private int savedInput,savedOutput;
        private RuntimeException callbackFailure;
        Lifecycle(GameTestHelper h){this.h=h;level=h.getLevel();}

        @SubscribeEvent public void unloaded(ChunkEvent.Unload e){
            if(e.getLevel()==level)unloaded.add(e.getChunk().getPos());
        }
        @SubscribeEvent(priority=EventPriority.LOWEST)
        public void loaded(ChunkEvent.Load e){
            if(e.getLevel()!=level)return;
            loaded.add(e.getChunk().getPos());
            if(phase==5&&e.getChunk().getPos().equals(middleChunk)&&freshSource!=null){
                // Forge getChunkNow exposes currentlyLoading here. Exercise the actual
                // reachable event window, not an invented absent-FULL-future condition.
                try{
                    var remainder=items(sourcePos).insertItem(0,new ItemStack(Items.DIAMOND),true);
                    if(remainder.getCount()!=0&&remainder.getCount()!=1)throw new IllegalStateException("invalid event-window remainder");
                    loadCallbackQuery=true;
                }catch(RuntimeException failure){callbackFailure=failure;}
            }
        }
        private static long epoch(Level level){
            try{var m=Class.forName("gregtech.content.block.misc.LongDistanceTopology").getDeclaredMethod("version",Level.class);m.setAccessible(true);return (long)m.invoke(null,level);}
            catch(ReflectiveOperationException e){throw new AssertionError(e);}
        }
        private IItemHandler items(BlockPos pos){return level.getBlockEntity(pos).getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);}
        private int count(BlockPos pos){int n=0;var handler=items(pos);for(int i=0;i<handler.getSlots();i++)n+=handler.getStackInSlot(i).getCount();return n;}
        private int move(){var input=items(inputPos);var offered=input.extractItem(0,1,true);var remainder=items(sourcePos).insertItem(0,offered,false);int accepted=offered.getCount()-remainder.getCount();if(accepted>0)input.extractItem(0,accepted,false);return accepted;}
        private void positive(String reason){int a=count(inputPos),b=count(outputPos);h.assertTrue(move()==1&&count(inputPos)==a-1&&count(outputPos)==b+1,reason);}
        private void negative(String reason){int a=count(inputPos),b=count(outputPos);h.assertTrue(move()==0&&count(inputPos)==a&&count(outputPos)==b,reason);}
        private void set(BlockPos pos,BlockState state){
            if(!originals.containsKey(pos)){h.assertTrue(level.getBlockEntity(pos)==null,"remote fixture refuses to overwrite existing BE");originals.put(pos,level.getBlockState(pos));}
            if(!level.setBlock(pos,state,FLAGS))throw new AssertionError("fixture placement failed: "+pos);
            if(state.is(Blocks.CHEST))ownedChests.add(pos.immutable());
        }
        private void keepEndpoints(){
            level.getChunkSource().addRegionTicket(TicketType.UNKNOWN,sourceChunk,0,sourceChunk);
            level.getChunkSource().addRegionTicket(TicketType.UNKNOWN,receiverChunk,0,receiverChunk);
        }
        void start(){
            h.assertTrue(!level.hasChunkAt(sourcePos)&&!level.hasChunkAt(sourcePos.east(MIDDLE_OFFSET))&&!level.hasChunkAt(receiverPos)
                    &&!level.hasChunk(branchChunk.x,branchChunk.z),"isolated fixture chunks start unavailable");
            MinecraftForge.EVENT_BUS.register(this);registered=true;
            h.assertTrue(middleChunk.x-sourceChunk.x>RETENTION_RADIUS&&receiverChunk.x-middleChunk.x>RETENTION_RADIUS,
                    "selected middle lies beyond both FULL-ticket generation retention radii");
            // Load only the finite one-chunk-wide route, not a rectangular world region.
            for(int i=0;i<=SPAN_CHUNKS;i++){
                var chunk=level.getChunk(sourceChunk.x+i,sourceChunk.z);
                if(i==0)oldSourceChunk=chunk;
                if(i==MIDDLE_CHUNKS)oldMiddle=chunk;
                if(i==SPAN_CHUNKS)oldReceiverChunk=chunk;
            }
            System.out.println("[long-distance-native-lifecycle] retention-radius="+RETENTION_RADIUS
                    +" span-chunks="+SPAN_CHUNKS+" middle="+middleChunk);
            var endpoint=GTLongDistance.block("long_dist_endpoint_item").defaultBlockState().setValue(LongDistEndpointBlock.FACING,Direction.WEST);
            set(sourcePos,endpoint);set(receiverPos,endpoint);
            for(int i=1;i<RECEIVER_OFFSET;i++)set(sourcePos.east(i),GTLongDistance.block("long_dist_pipe_item").defaultBlockState());
            set(inputPos,Blocks.CHEST.defaultBlockState());set(outputPos,Blocks.CHEST.defaultBlockState());
            h.assertTrue(items(inputPos).insertItem(0,new ItemStack(Items.DIAMOND,32),false).isEmpty(),"real source inventory seeded once");
            oldSource=(LongDistEndpointBlockEntity)level.getBlockEntity(sourcePos);oldReceiver=(LongDistEndpointBlockEntity)level.getBlockEntity(receiverPos);
            positive("initial actual route establishes authenticated cache");
            h.assertTrue(oldSource.saveWithoutMetadata().contains("gt.target"),"target hint persisted before natural unload");
            h.assertTrue(!level.hasChunk(branchChunk.x,branchChunk.z),"initial loaded-only scan leaves north frontier unavailable");
            keepEndpoints();phase=1;
        }
        void poll(){
            if(h.getTick()>1100)h.fail("native long-distance lifecycle deadline phase="+phase+" unloads="+unloaded
                    +" middle-full="+level.hasChunkAt(sourcePos.east(MIDDLE_OFFSET))
                    +" middle-holder="+level.getChunkSource().getChunkDebugData(middleChunk));
            if(phase==1){
                keepEndpoints();
                if(!unloaded.contains(middleChunk)||level.hasChunkAt(sourcePos.east(MIDDLE_OFFSET)))return;
                if(transitionTick<0){transitionTick=h.getTick();return;}
                if(h.getTick()<=transitionTick)return;
                h.assertTrue(level.getBlockEntity(sourcePos)==oldSource&&level.getBlockEntity(receiverPos)==oldReceiver,"endpoints remain original while middle naturally unloads");
                positive("authenticated route retains original unloaded-middle transfer semantics");
                var other=level.getServer().getLevel(Level.NETHER);long otherBefore=other==null?0:epoch(other),before=epoch(level);
                level.getChunk(branchChunk.x,branchChunk.z);
                h.assertTrue(loaded.contains(branchChunk)&&epoch(level)>before,"real branch ChunkEvent.Load reaches routing epoch");
                h.assertTrue(other==null||epoch(other)==otherBefore,"routing epoch remains isolated from another ServerLevel");
                positive("unrelated pending north branch load does not destroy authenticated route across unloaded middle");
                h.assertTrue(!level.hasChunkAt(sourcePos.east(MIDDLE_OFFSET)),"cached positive controls did not load middle");
                savedInput=count(inputPos);savedOutput=count(outputPos);transitionTick=-1;phase=3;
                level.getChunkSource().removeRegionTicket(TicketType.UNKNOWN,sourceChunk,0,sourceChunk);
                level.getChunkSource().removeRegionTicket(TicketType.UNKNOWN,receiverChunk,0,receiverChunk);
                return;
            }
            if(phase==3){
                if(!unloaded.contains(sourceChunk)||!unloaded.contains(receiverChunk)||!oldSource.isRemoved()||!oldReceiver.isRemoved()
                        ||level.hasChunkAt(sourcePos)||level.hasChunkAt(receiverPos))return;
                if(transitionTick<0){transitionTick=h.getTick();return;}
                if(h.getTick()<=transitionTick)return; // let ServerChunkCache discard its prior-tick cache naturally
                var newSourceChunk=level.getChunk(sourceChunk.x,sourceChunk.z);
                var newReceiverChunk=level.getChunk(receiverChunk.x,receiverChunk.z);
                freshSource=(LongDistEndpointBlockEntity)level.getBlockEntity(sourcePos);
                var freshReceiver=(LongDistEndpointBlockEntity)level.getBlockEntity(receiverPos);
                h.assertTrue(newSourceChunk!=oldSourceChunk&&newReceiverChunk!=oldReceiverChunk&&freshSource!=null&&freshReceiver!=null
                        &&freshSource!=oldSource&&freshReceiver!=oldReceiver&&!freshSource.isRemoved()&&!freshReceiver.isRemoved(),"actual native chunk I/O creates fresh world-bound endpoints");
                h.assertTrue(freshSource.saveWithoutMetadata().getLong("gt.target")==receiverPos.asLong(),"native saved target survives without manually loading captured NBT");
                h.assertTrue(count(inputPos)==savedInput&&count(outputPos)==savedOutput,"native chest persistence retains exact counts without reseeding");
                h.assertTrue(!level.hasChunkAt(sourcePos.east(MIDDLE_OFFSET)),"endpoint-only reload leaves middle unavailable");
                for(int i=0;i<32;i++)negative("unvalidated hint waits without consuming source or crediting target");
                h.assertTrue(!level.hasChunkAt(sourcePos.east(MIDDLE_OFFSET)),"repeated rejected route operations do not load missing middle");
                // Restore the remaining route explicitly, but leave the selected middle last
                // so the witnessed Load callback completes a previously unavailable path.
                for(int i=1;i<SPAN_CHUNKS;i++)if(i!=MIDDLE_CHUNKS)level.getChunk(sourceChunk.x+i,sourceChunk.z);
                phase=5;loaded.remove(middleChunk);long before=epoch(level);
                var newMiddle=level.getChunk(middleChunk.x,middleChunk.z);
                h.assertTrue(newMiddle!=oldMiddle&&loaded.contains(middleChunk)&&epoch(level)>before,"actual fresh middle reload dispatches routing event");
                if(callbackFailure!=null)throw callbackFailure;
                h.assertTrue(loadCallbackQuery,"witness actually queried during the real load notification");
                h.assertTrue(count(inputPos)==savedInput&&count(outputPos)==savedOutput,"load-window simulation has no source or destination effects");
                positive("pending hint recovers after resident middle reload including real event-window query");
                level.setBlock(sourcePos.east(MIDDLE_OFFSET),Blocks.AIR.defaultBlockState(),FLAGS);
                negative("real break after chunk recovery immediately refuses transport");
                level.setBlock(sourcePos.east(MIDDLE_OFFSET),GTLongDistance.block("long_dist_pipe_item").defaultBlockState(),FLAGS);
                positive("repair after actual chunk recovery restores transport without tools");
                close();h.succeed();
            }
        }
        void close(){
            if(closed)return;closed=true;
            if(registered)MinecraftForge.EVENT_BUS.unregister(this);
            level.getChunkSource().removeRegionTicket(TicketType.UNKNOWN,sourceChunk,0,sourceChunk);
            level.getChunkSource().removeRegionTicket(TicketType.UNKNOWN,receiverChunk,0,receiverChunk);
            // Explicit fixture cleanup is outside the measured no-force-load operation window.
            RuntimeException error=null;
            for(var entry:originals.entrySet())try{
                level.getChunk(entry.getKey().getX()>>4,entry.getKey().getZ()>>4);
                if(ownedChests.contains(entry.getKey())){
                    var entity=level.getBlockEntity(entry.getKey());
                    if(!level.getBlockState(entry.getKey()).is(Blocks.CHEST)
                            ||!(entity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest))
                        throw new IllegalStateException("owned fixture chest identity changed before cleanup: "+entry.getKey());
                    // End-of-test only: suppress Containers.dropContents from the owned chest.
                    chest.clearContent();chest.setChanged();
                }
                level.setBlock(entry.getKey(),entry.getValue(),FLAGS);
            }catch(RuntimeException e){if(error==null)error=e;else error.addSuppressed(e);}
            if(error!=null)throw error;
        }
    }
}
