package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.tool.DynamiteBlock;
import com.gregtech.gregtech.blockentity.tool.DynamiteBlockEntity;
import com.gregtech.gregtech.content.tool.DynamiteExplosion;
import com.gregtech.gregtech.registry.GTToolBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DynamiteRepairTests {
    @GameTest(template="test_empty")
    public static void nearbyDropsSurviveAndLivingTargetsTakeOriginalDamage(GameTestHelper h) {
        var level=h.getLevel(); var center=h.absolutePos(new BlockPos(2,2,2));
        var item=new net.minecraft.world.entity.item.ItemEntity(level,center.getX()+.5,center.getY()+.5,center.getZ()+.5,
                new ItemStack(net.minecraft.world.item.Items.DIAMOND));
        item.setNoGravity(true); level.addFreshEntity(item);
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(2,2,2));
        level.setBlock(center.east(),Blocks.STONE.defaultBlockState(),3);
        h.runAfterDelay(1,()-> {
            DynamiteExplosion.detonate(level,center,10,5);
            h.assertTrue(item.isAlive() && item.getItem().getCount()==1,"pre-existing dropped item survives explosion");
            h.assertTrue(cow.getHealth()==0,"quality ten deals twenty damage without distance falloff");
            h.runAfterDelay(1,()-> {
                h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(center).inflate(2)).stream().anyMatch(e->e.getItem().is(net.minecraft.world.item.Items.COBBLESTONE)),
                        "mined stone has full-chance loot, not TNT decay");
                h.succeed();
            });
        });
    }

    public static final class CancelBlast {
        boolean seen;
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void cancel(net.minecraftforge.event.level.ExplosionEvent.Start event) {
            if(event.getExplosion() instanceof DynamiteExplosion) { seen=true; event.setCanceled(true); }
        }
    }
    @GameTest(template="test_empty")
    public static void forgeProtectionCanCancelMiningBlast(GameTestHelper h) {
        var level=h.getLevel(); var center=h.absolutePos(new BlockPos(2,2,2));
        level.setBlock(center.east(),Blocks.STONE.defaultBlockState(),3);
        var listener=new CancelBlast(); net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(listener);
        try { DynamiteExplosion.detonate(level,center,40,5); }
        finally { net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener); }
        h.assertTrue(listener.seen && level.getBlockState(center.east()).is(Blocks.STONE),"Forge cancellation preserves protected terrain");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void fuseIgnitionDefusingPersistenceAndRemote(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(2,2,2)); var block=GTToolBlocks.DYNAMITE.get();
        level.setBlock(pos,block.defaultBlockState(),3);
        var charge=(DynamiteBlockEntity)level.getBlockEntity(pos);
        h.assertTrue(charge.onIgnite(level,pos,Direction.UP,null,ItemStack.EMPTY,false,0,0,0)==10000,"igniter cost");
        h.assertTrue(charge.remainingTicks()==100 && level.getBlockState(pos).getValue(DynamiteBlock.ARMED),"100 tick fuse");
        h.assertTrue(level.getBlockState(pos).getSignal(level,pos,Direction.NORTH)==15,"armed charge emits redstone");
        for(int i=0;i<30;i++) charge.serverTick();
        CompoundTag saved=charge.saveWithoutMetadata();
        var loaded=new DynamiteBlockEntity(pos,level.getBlockState(pos)); loaded.load(saved);
        h.assertTrue(loaded.remainingTicks()==70,"fuse survives save/load");
        h.assertTrue(charge.onExtinguish(level,pos,Direction.UP,null,ItemStack.EMPTY,false,0,0,0)==10000,"extinguisher defuses");
        for(int i=0;i<110;i++) charge.serverTick();
        h.assertTrue(level.getBlockState(pos).is(block) && !level.getBlockState(pos).getValue(DynamiteBlock.ARMED),"defused charge remains");
        h.assertTrue(!block.remoteActivate(level,pos) && charge.remainingTicks()==20,"remote arms and forgets coordinate");
        for(int i=0;i<19;i++) charge.serverTick();
        block.remoteActivate(level,pos);
        h.assertTrue(charge.remainingTicks()==1 && level.getBlockState(pos).is(block),"repeated remote never extends fuse");
        charge.serverTick();
        h.assertTrue(level.getBlockState(pos).isAir(),"explodes on final tick");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void miningBlastIsBoundedAndProtectsBedrockAndSpawner(GameTestHelper h) {
        var level=h.getLevel(); var center=h.absolutePos(new BlockPos(2,2,2));
        level.setBlock(center.east(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(center.west(),Blocks.SPAWNER.defaultBlockState(),3);
        level.setBlock(center.above(),Blocks.BEDROCK.defaultBlockState(),3);
        level.setBlock(center.south(),Blocks.OBSIDIAN.defaultBlockState(),3);
        level.setBlock(center.east(2),Blocks.STONE.defaultBlockState(),3);
        DynamiteExplosion.detonate(level,center,40,5);
        h.assertTrue(level.getBlockState(center.east()).isAir(),"stone within 3x3x3 breaks");
        h.assertTrue(level.getBlockState(center.east(2)).is(Blocks.STONE),"strong quality does not enlarge blast");
        h.assertTrue(level.getBlockState(center.west()).is(Blocks.SPAWNER),"spawner protected");
        h.assertTrue(level.getBlockState(center.above()).is(Blocks.BEDROCK),"unbreakable blocks protected");
        h.assertTrue(level.getBlockState(center.south()).is(Blocks.OBSIDIAN),"resistance above forty protected");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void qualityIsAResistanceThresholdAndFortuneMatchesOriginal(GameTestHelper h) {
        var level=h.getLevel(); var center=h.absolutePos(new BlockPos(2,2,2));
        var safe=com.gregtech.gregtech.registry.GTStorage.safe(com.gregtech.gregtech.content.material.Materials.Bronze,false);
        var pos=center.east(); level.setBlock(pos,safe.defaultBlockState(),3);
        h.assertTrue(safe.getExplosionResistance()>10 && safe.getExplosionResistance()<=40,"fixture separates qualities");
        DynamiteExplosion.detonate(level,center,10,3);
        h.assertTrue(level.getBlockState(pos).is(safe),"quality ten preserves stronger material");
        DynamiteExplosion.detonate(level,center,40,5);
        h.assertTrue(level.getBlockState(pos).isAir(),"quality forty breaks it without expanding range");
        h.assertTrue(GTToolBlocks.BOOMSTICK.get().fortune()==3 && GTToolBlocks.DYNAMITE.get().fortune()==5
                && GTToolBlocks.DYNAMITE_STRONG.get().fortune()==5,"GT6 fortune values");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void embeddedShapeSupportAndExplosionCenter(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(2,3,2)); var block=GTToolBlocks.DYNAMITE.get();
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos.below(2),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos.above(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,block.defaultBlockState().setValue(DynamiteBlock.FACING,Direction.UP).setValue(DynamiteBlock.SUNK,true),3);
        block.refreshSupport(level,pos);
        h.assertTrue(level.getBlockState(pos).getValue(DynamiteBlock.SUNK),"stone supports embedding");
        var box=level.getBlockState(pos).getShape(level,pos).bounds();
        h.assertTrue(box.minY==0 && box.maxY==.125,"embedded charge exposes two pixels");
        block.detonate(level,pos);
        h.assertTrue(level.getBlockState(pos.below(2)).isAir(),"blast centered in backing stone");
        h.assertTrue(level.getBlockState(pos.above()).is(Blocks.STONE),"block above lies beyond shifted blast");
        level.setBlock(pos,block.defaultBlockState().setValue(DynamiteBlock.FACING,Direction.UP).setValue(DynamiteBlock.SUNK,true),3);
        block.refreshSupport(level,pos);
        h.assertTrue(!level.getBlockState(pos).getValue(DynamiteBlock.SUNK),"missing support restores full stick");
        h.succeed();
    }
}
