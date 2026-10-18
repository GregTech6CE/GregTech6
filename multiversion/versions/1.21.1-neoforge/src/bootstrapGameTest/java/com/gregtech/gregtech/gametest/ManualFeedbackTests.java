package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.tool.CrankBlock;
import com.gregtech.gregtech.item.behavior.BehaviorChunkEraser;
import com.gregtech.gregtech.item.behavior.BehaviorWorldgenDebugger;
import com.gregtech.gregtech.registry.GTManualStations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;

@GameTestHolder("gt6ce_feedback_manual")
@PrefixGameTestTemplate(false)
public final class ManualFeedbackTests {
    @GameTest(template="crank_stage",timeoutTicks=100)
    public static void crankPowersThroughItsMountAndNotifiesWhenStopped(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var block=GTManualStations.CRANK.get();
        var lamps=new ArrayList<BlockPos>();
        var cranks=new ArrayList<BlockPos>();
        for(var handle:Direction.values()) {
            var mount=helper.absolutePos(new BlockPos(3+handle.ordinal()*5,4,3));
            var crank=mount.relative(handle);
            var lamp=mount.relative(handle.getOpposite());
            level.setBlock(mount,Blocks.STONE.defaultBlockState(),3);
            level.setBlock(lamp,Blocks.REDSTONE_LAMP.defaultBlockState(),3);
            var state=block.defaultBlockState().setValue(CrankBlock.FACING,handle);
            level.setBlock(crank,state,3);
            block.interact(state,level,crank,player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(crank),handle,crank,false));
            helper.assertTrue(level.getBestNeighborSignal(mount)==15,"crank strongly powers its mount: "+handle);
            helper.assertTrue(level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT),
                    "real lamp behind the mount receives the neighbor update: "+handle);
            helper.assertTrue(level.getSignal(crank,handle.getOpposite())==0,"no signal toward the handle: "+handle);
            lamps.add(lamp);cranks.add(crank);
        }
        helper.runAfterDelay(CrankBlock.ACTIVE_TICKS+5,()->{
            for(int i=0;i<lamps.size();i++) {
                helper.assertTrue(!level.getBlockState(cranks.get(i)).getValue(CrankBlock.ACTIVE),"crank stopped: "+Direction.values()[i]);
                helper.assertTrue(!level.getBlockState(lamps.get(i)).getValue(RedstoneLampBlock.LIT),"lamp behind mount switched off");
                var pos=cranks.get(i);var state=level.getBlockState(pos);
                block.interact(state,level,pos,player,InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos),state.getValue(CrankBlock.FACING),pos,false));
                level.removeBlock(pos,false);
            }
            helper.runAfterDelay(6,()->{
                for(var lamp:lamps)helper.assertTrue(!level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT),"removing an active crank clears conducted signal");
                helper.succeed();
            });
        });
    }

    @GameTest(template="test_empty",timeoutTicks=200)
    public static void debugToolClicksUseModernDimensionHeights(GameTestHelper helper) {
        var level=helper.getLevel();
        var center=helper.absolutePos(new BlockPos(48,2,48));
        level.getChunk(center);
        var bottom=new BlockPos(center.getX(),level.getMinBuildHeight(),center.getZ());
        var low=bottom.above();
        var top=new BlockPos(center.getX(),level.getMaxBuildHeight()-1,center.getZ());
        var ore=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof OreBlock).findFirst().orElseThrow();
        level.setBlock(bottom,Blocks.BEDROCK.defaultBlockState(),2);
        level.setBlock(low,Blocks.STONE.defaultBlockState(),2);
        level.setBlock(top,Blocks.STONE.defaultBlockState(),2);
        var orePos=low.east();
        level.setBlock(orePos,ore.defaultBlockState(),2);
        BehaviorWorldgenDebugger.useOn(level,center,Direction.UP,null,ItemStack.EMPTY,.5f,.5f,.5f);
        helper.assertTrue(level.getBlockState(low).isAir()&&level.getBlockState(top).isAir(),"debugger clears negative Y and expanded ceiling");
        helper.assertTrue(level.getBlockState(orePos).is(ore),"debugger retains GT ore below zero");
        helper.assertTrue(level.getBlockState(bottom).is(Blocks.BEDROCK),"original bottom-layer exception follows dimension floor");
        level.setBlock(low,Blocks.STONE.defaultBlockState(),2);
        level.setBlock(top,Blocks.STONE.defaultBlockState(),2);
        BehaviorChunkEraser.useOn(level,center,Direction.UP,null,ItemStack.EMPTY,.5f,.5f,.5f);
        helper.assertTrue(level.getBlockState(low).isAir()&&level.getBlockState(top).isAir()&&level.getBlockState(orePos).isAir(),"eraser clears the same extended range, including GT ore");
        helper.assertTrue(level.getBlockState(bottom).is(Blocks.BEDROCK),"eraser preserves the dimension floor");
        helper.succeed();
    }
}
