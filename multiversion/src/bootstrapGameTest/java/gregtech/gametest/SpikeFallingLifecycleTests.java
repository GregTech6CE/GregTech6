package gregtech.gametest;

import gregtech.content.block.misc.SpikeBlock;
import gregtech.content.registry.GTSpikes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class SpikeFallingLifecycleTests {
    public static final class SoundProbe {
        final net.minecraft.world.level.Level level; final BlockPos pos; int anvils; boolean exact = true;
        SoundProbe(GameTestHelper h, BlockPos pos) { level = h.getLevel(); this.pos = pos; }
        @SubscribeEvent public void heard(PlayLevelSoundEvent.AtPosition e) {
            if (e.getLevel() == level && e.getPosition().distanceToSqr(Vec3.atCenterOf(pos)) < .01
                    && e.getSound().value() == net.minecraft.sounds.SoundEvents.ANVIL_LAND) {
                anvils++; exact &= e.getOriginalVolume() == 1 && e.getOriginalPitch() == 2;
            }
        }
    }
    /** Mutate once, only after the dedicated template is entity ticking. */
    private static void whenReady(GameTestHelper h, Runnable start) {
        var template = new HashSet<BlockPos>();
        h.forEveryBlockInStructure(at -> template.add(h.absolutePos(at)));
        h.startSequence().thenWaitUntil(() -> {
            for (var local : BlockPos.betweenClosed(new BlockPos(0,12,0),new BlockPos(11,23,7))) {
                var at = h.absolutePos(local);
                h.assertTrue(template.contains(at), "Spike fixture must fit dedicated template: " + at);
                h.assertTrue(h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))
                        && h.getLevel().isPositionEntityTicking(at), "waiting for entity-ticking Spike fixture: " + at);
            }
        }).thenExecute(start);
    }
    private static ItemStack stock(GameTestHelper h, SpikeBlock b, SpikeBlock.Mode mode, boolean secondary) {
        var s=b.defaultBlockState().setValue(SpikeBlock.MODE,mode).setValue(SpikeBlock.SECONDARY,secondary);
        var p=h.absolutePos(new BlockPos(3,20,3));
//? if >=1.21.1 {
        /*return b.getCloneItemStack(s,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false),h.getLevel(),p,null);
*///?} else {
        return b.getCloneItemStack(h.getLevel(),p,s);
//?}
    }
    private static void place(GameTestHelper h, BlockPos target, ItemStack stock) {
//? if >=1.21.1 {
        /*var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"[gt-spike-fall]"));
        player.setGameMode(GameType.SURVIVAL); player.setPos(target.getX()-3,target.getY(),target.getZ()-3);
        player.setItemInHand(InteractionHand.MAIN_HAND,stock);
        var side=target.east();h.getLevel().setBlock(side,Blocks.STONE.defaultBlockState(),3);
        var hit=new BlockHitResult(Vec3.atCenterOf(side).add(-.5,0,0),Direction.WEST,side,false);
        try {
            var result=stock.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            h.assertTrue(result.consumesAction() && stock.isEmpty(),"real ItemStack.useOn consumes exactly one");
        } finally { player.getInventory().clearContent(); player.discard(); }
    }
    private static void cleanupOnCompletion(GameTestHelper h, Map<BlockPos,net.minecraft.world.level.block.state.BlockState> before) {
        Runnable restore=()->{
            for(var p:before.keySet()) {
                h.getLevel().getEntitiesOfClass(FallingBlockEntity.class,new AABB(p).inflate(.5),e->e.getBlockState().getBlock() instanceof SpikeBlock).forEach(FallingBlockEntity::discard);
                h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(.5),e->e.getItem().getItem() instanceof net.minecraft.world.item.BlockItem bi&&bi.getBlock() instanceof SpikeBlock).forEach(ItemEntity::discard);
            }
            h.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(h.absolutePos(new BlockPos(0,12,0))).expandTowards(11,11,7),
                    e->e.getItem().getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof SpikeBlock)
                    .forEach(ItemEntity::discard);
            before.forEach((p,state)->{
                h.getLevel().getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(p));
                h.getLevel().setBlock(p,state,3);
            });
        };
        try {
            net.minecraft.gametest.framework.GameTestInfo info=null;
            for(var f:GameTestHelper.class.getDeclaredFields())if(f.getType()==net.minecraft.gametest.framework.GameTestInfo.class){f.setAccessible(true);info=(net.minecraft.gametest.framework.GameTestInfo)f.get(h);break;}
            if(info==null)throw new IllegalStateException("GameTestInfo cleanup unavailable");
            info.addListener(new net.minecraft.gametest.framework.GameTestListener(){
                @Override public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo t){}
//? if >=1.21.1 {
                /*@Override public void testPassed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){restore.run();}
                @Override public void testFailed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){restore.run();}
                @Override public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo a,net.minecraft.gametest.framework.GameTestInfo b,net.minecraft.gametest.framework.GameTestRunner r){restore.run();}
*///?} else {
                @Override public void testPassed(net.minecraft.gametest.framework.GameTestInfo t){restore.run();}
                @Override public void testFailed(net.minecraft.gametest.framework.GameTestInfo t){restore.run();}
//?}
            });
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    @GameTest(template="spike_falling_empty",timeoutTicks=240)
    public static void itemPlacementSchedulesFallingAndEmitsOriginalSound(GameTestHelper h) {
        whenReady(h, () -> run_itemPlacementSchedulesFallingAndEmitsOriginalSound(h));
    }
    private static void run_itemPlacementSchedulesFallingAndEmitsOriginalSound(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(3,20,3));
        var before=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(var q:List.of(p,p.below(),p.east()))before.put(q,h.getLevel().getBlockState(q));
        cleanupOnCompletion(h,before);
        for(var holder:GTSpikes.all())for(boolean secondary:new boolean[]{false,true})for(var mode:SpikeBlock.Mode.values()) {
            var b=holder.get();var probe=new SoundProbe(h,p);
            h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(p.below(),Blocks.AIR.defaultBlockState(),3);
            h.getLevel().getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(p));
//? if >=1.21.1 {
            /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(probe);
*///?} else {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(probe);
//?}
            try {place(h,p,stock(h,b,mode,secondary));}
            finally {
//? if >=1.21.1 {
                /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(probe);
*///?} else {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(probe);
//?}
            }
            var actual=h.getLevel().getBlockState(p);
            h.assertTrue(actual.is(b) && actual.getValue(SpikeBlock.MODE)==mode && actual.getValue(SpikeBlock.SECONDARY)==secondary,"item overlay retains native identity");
            h.assertTrue(h.getLevel().getBlockTicks().hasScheduledTick(p,b)==(mode==SpikeBlock.Mode.FALLING),"only FALLING schedules native tick");
            h.assertTrue(probe.anvils==(mode==SpikeBlock.Mode.FALLING?1:0)&&probe.exact,"source anvil placement sound exactly once only falling");
        }
        h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(p.east(),Blocks.AIR.defaultBlockState(),3);h.succeed();
    }
    @GameTest(template="spike_falling_empty",timeoutTicks=240)
    public static void realFallingItemLandsOrDropsWithoutLosingVariant(GameTestHelper h) {
        whenReady(h, () -> run_realFallingItemLandsOrDropsWithoutLosingVariant(h));
    }
    private static void run_realFallingItemLandsOrDropsWithoutLosingVariant(GameTestHelper h) {
        var b=GTSpikes.get(SpikeBlock.Family.STEEL);var landing=h.absolutePos(new BlockPos(2,20,2));var failure=landing.offset(4,0,0);
        var before=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(var local:BlockPos.betweenClosed(new BlockPos(0,12,0),new BlockPos(11,23,7))) {
            var at=h.absolutePos(local);
            h.assertTrue(h.getLevel().getBlockEntity(at)==null,"sealed upper fixture must not overwrite any block entity or controller: "+at);
            before.put(at,h.getLevel().getBlockState(at));
        }
        cleanupOnCompletion(h,before);
        int quiet=net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                |net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
        // Seal only the owned upper chamber, never the template control block at local zero.
        // The old floor-only fixture admitted falling gravel from well above the template.
        for(var local:BlockPos.betweenClosed(new BlockPos(0,12,0),new BlockPos(11,23,7))) {
            if(local.getX()==0||local.getX()==11||local.getY()==12||local.getY()==23
                    ||local.getZ()==0||local.getZ()==7)
                h.getLevel().setBlock(h.absolutePos(local),Blocks.STONE.defaultBlockState(),quiet);
        }
        for(var local:BlockPos.betweenClosed(new BlockPos(1,14,1),new BlockPos(10,22,6)))
            h.getLevel().setBlock(h.absolutePos(local),Blocks.AIR.defaultBlockState(),quiet);
        var ownedArea=new AABB(h.absolutePos(new BlockPos(0,12,0))).expandTowards(11,11,7);
        // Setup-only removal of ambient natural falling blocks/items, never test Spike entities.
        // No entity is filtered/removed between real placement and the final exact-one assertion.
        h.getLevel().getEntitiesOfClass(FallingBlockEntity.class,ownedArea,
                e->!(e.getBlockState().getBlock() instanceof SpikeBlock)).forEach(FallingBlockEntity::discard);
        h.getLevel().getEntitiesOfClass(ItemEntity.class,ownedArea,
                e->e.getItem().getItem() instanceof net.minecraft.world.item.BlockItem bi
                        &&!(bi.getBlock() instanceof SpikeBlock)).forEach(ItemEntity::discard);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,ownedArea).isEmpty(),"sealed falling fixture starts with exactly zero items");
        h.assertTrue(h.getLevel().getEntitiesOfClass(FallingBlockEntity.class,ownedArea).isEmpty(),"sealed falling fixture starts with exactly zero falling entities");
        for(var local:BlockPos.betweenClosed(new BlockPos(0,13,0),new BlockPos(11,13,7)))
            h.getLevel().setBlock(h.absolutePos(local),Blocks.STONE.defaultBlockState(),3);
        for(var top:List.of(landing,failure))for(int dy=-6;dy<=0;dy++)h.getLevel().setBlock(top.offset(0,dy,0),Blocks.AIR.defaultBlockState(),3);
        h.getLevel().setBlock(landing.below(5),Blocks.STONE.defaultBlockState(),3);
        h.getLevel().setBlock(failure.below(5),Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,SlabType.BOTTOM),3);
        h.assertTrue(!h.getLevel().getBlockState(failure.below(5)).canBeReplaced(
                new net.minecraft.world.item.context.DirectionalPlaceContext(h.getLevel(),failure.below(5),Direction.DOWN,ItemStack.EMPTY,Direction.UP)),
                "bottom slab is genuinely nonreplaceable for native falling landing");
        place(h,landing,stock(h,b,SpikeBlock.Mode.FALLING,true));place(h,failure,stock(h,b,SpikeBlock.Mode.FALLING,true));
        h.runAfterDelay(5,()->{
            h.assertTrue(h.getLevel().getBlockState(landing).isAir()&&h.getLevel().getBlockState(failure).isAir(),"scheduled ticks actually remove placed blocks");
            h.assertTrue(h.getLevel().getEntitiesOfClass(FallingBlockEntity.class,new AABB(landing.below(6)).expandTowards(5,8,1)).size()==2,"native falling entities spawned");
        });
        h.runAfterDelay(70,()->{
            var landed=h.getLevel().getBlockState(landing.below(4));
            h.assertTrue(landed.is(b)&&landed.getValue(SpikeBlock.MODE)==SpikeBlock.Mode.FALLING&&landed.getValue(SpikeBlock.SECONDARY),"normal landing preserves red steel falling state");
            var area=new AABB(h.absolutePos(new BlockPos(0,12,0))).expandTowards(11,11,7);
            var items=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
            var allItems=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
            var falling=h.getLevel().getEntitiesOfClass(FallingBlockEntity.class,area);
            String diagnostic="; queriedCount="+items.size()+"; templateItems="+allItems.stream().map(e->e.getItem()+"@"+e.position()).toList()+"; queriedItems="+items.stream().map(e->e.getItem()+"@"+e.position()).toList()
                    +"; falling="+falling.stream().map(e->e.getBlockState()+"@"+e.position()+" age="+e.time+" drop="+e.dropItem).toList()
                    +"; slab="+h.getLevel().getBlockState(failure.below(5))
                    +"; aboveSlab="+h.getLevel().getBlockState(failure.below(4))
                    +"; entityDrops="+h.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS);
            h.assertTrue(items.size()==1,"nonreplaceable bottom slab causes exactly one item drop"+diagnostic);
            var drop=items.get(0).getItem();
            h.assertTrue(drop.is(b.asItem())&&drop.getCount()==1&&SpikeBlock.itemMode(drop)==SpikeBlock.Mode.FALLING&&SpikeBlock.itemSecondary(drop),"failed landing preserves falling and secondary item identity");
            h.succeed();
        });
    }
}
