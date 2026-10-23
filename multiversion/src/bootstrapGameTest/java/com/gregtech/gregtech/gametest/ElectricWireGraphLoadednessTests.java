package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.EnergyNet;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Explicit native graph callback at remote FULL chunks, not a naturally ticking or restart test. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class ElectricWireGraphLoadednessTests {
    private static CompoundTag save(GameTestHelper h,BlockEntity entity){
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }
    private static void load(GameTestHelper h,BlockEntity entity,CompoundTag data){
//? if >=1.21.1 {
/*        entity.loadWithComponents(data,h.getLevel().registryAccess());
*///?} else {
        entity.load(data);
//?}
    }
    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state,CompoundTag data){}
        final GameTestHelper h;
        final java.util.Map<BlockPos,Saved> saved=new java.util.LinkedHashMap<>();
        Fixture(GameTestHelper h){this.h=h;}
        void set(BlockPos p,BlockState state){
            // Do not snapshot an unavailable target before its negative assertion: that read would load it.
            if(!saved.containsKey(p)){var be=h.getLevel().getBlockEntity(p);saved.put(p.immutable(),new Saved(h.getLevel().getBlockState(p),be==null?null:save(h,be)));}
            h.getLevel().removeBlockEntity(p);
            h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            h.getLevel().setBlock(p,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        }
        @Override public void close(){
            for(var p:saved.keySet())h.getLevel().removeBlockEntity(p);
            for(var e:saved.entrySet()){
                h.getLevel().setBlock(e.getKey(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                h.getLevel().setBlock(e.getKey(),e.getValue().state(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                var be=h.getLevel().getBlockEntity(e.getKey());if(be!=null&&e.getValue().data()!=null)load(h,be,e.getValue().data());
            }
        }
    }
    @GameTest(template="test_empty",timeoutTicks=200)
    public static void wireConnectionCallbackSkipsAbsentSavedMaskNeighbors(GameTestHelper h) {
        var wire=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof ElectricWireBlock)
                .sorted(java.util.Comparator.comparing(b->BuiltInRegistries.BLOCK.getKey(b).toString())).findFirst().orElseThrow();
        try(var f=new Fixture(h)) {
            for(int sign:new int[]{1,-1}) {
                var pos=new BlockPos(29_100_008,200,sign*29_100_000);
                var target=pos.north();
                h.assertTrue(!h.getLevel().hasChunkAt(target),"neighbor absent before any target state/BE read sign="+sign);
                f.set(pos,wire.defaultBlockState());
                var entity=(ElectricWireBlockEntity)h.getLevel().getBlockEntity(pos);
                h.assertTrue(entity!=null,"registered native wire exists");
                // Guarantee graph registration without relying on deferred loader onLoad timing.
                EnergyNet.onPlace(h.getLevel(),pos);
                var mask=wire.defaultBlockState().setValue(ElectricWireBlock.propFor(Direction.NORTH),true);
                h.getLevel().setBlock(pos,mask,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                h.assertTrue(!h.getLevel().hasChunkAt(target),"known-shape open-mask setup leaves neighbor absent");
                entity.onConnectionChanged();
                h.assertTrue(!h.getLevel().hasChunkAt(target),"actual native graph rebuild skips absent neighbor");
                h.assertTrue(entity.getBlockState().getValue(ElectricWireBlock.propFor(Direction.NORTH)),"unavailable neighbor does not erase saved connection intention");
                h.assertTrue(EnergyNet.getConnectedWires(h.getLevel(),pos).equals(java.util.Set.of(pos)),"absent receiver contributes no graph edge");
                // Explicit target load occurs only after the negative proof.
                f.set(target,wire.defaultBlockState().setValue(ElectricWireBlock.propFor(Direction.SOUTH),true));
                entity.onConnectionChanged();
                h.assertTrue(EnergyNet.getConnectedWires(h.getLevel(),pos).contains(target),"explicitly loaded reciprocal wire remains discoverable");
                // Vertical same-chunk control exercises the same callback, not a modeled graph.
                f.set(pos.above(),wire.defaultBlockState().setValue(ElectricWireBlock.propFor(Direction.DOWN),true));
                h.getLevel().setBlock(pos,entity.getBlockState().setValue(ElectricWireBlock.propFor(Direction.UP),true),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                entity.onConnectionChanged();
                h.assertTrue(EnergyNet.getConnectedWires(h.getLevel(),pos).contains(pos.above()),"same-chunk vertical edge preserved");
            }
        }
        h.succeed();
    }
}
