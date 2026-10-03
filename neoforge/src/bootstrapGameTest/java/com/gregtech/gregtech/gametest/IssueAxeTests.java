package com.gregtech.gregtech.gametest;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.GameType;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.item.GTToolItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@GameTestHolder("gregtech_issues")
@PrefixGameTestTemplate(false)
public final class IssueAxeTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h, boolean sneak) {
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GTAxeIssue"));
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(sneak);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,GTToolItem.create(GTToolType.AXE,GTMaterialRegistry.get("Iron"),GTMaterialRegistry.get("Wood")));
        return p;
    }
    @GameTest(template="test_empty",timeoutTicks=30)
    public static void onlySameColumnAndIncreasingWear(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var level=h.getLevel();
        for(int i=0;i<3;i++) level.setBlockAndUpdate(pos.above(i),Blocks.OAK_LOG.defaultBlockState());
        level.setBlockAndUpdate(pos.above(3),Blocks.BIRCH_LOG.defaultBlockState());
        level.setBlockAndUpdate(pos.above(4),Blocks.OAK_LOG.defaultBlockState());
        level.setBlockAndUpdate(pos.above().east(),Blocks.OAK_LOG.defaultBlockState());
        var p=player(h,false);var tool=p.getMainHandItem();p.gameMode.destroyBlock(pos);
        h.assertTrue(level.isEmptyBlock(pos)&&level.isEmptyBlock(pos.above())&&level.isEmptyBlock(pos.above(2)),"same oak column harvested");
        h.assertTrue(level.getBlockState(pos.above(3)).is(Blocks.BIRCH_LOG)&&level.getBlockState(pos.above(4)).is(Blocks.OAK_LOG)&&level.getBlockState(pos.above().east()).is(Blocks.OAK_LOG),"different log and diagonal remain");
        h.assertTrue(tool.getDamageValue()==303,"base 100 plus 101 and 102 wear, no recursion; actual "+tool.getDamageValue());h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=30)
    public static void sneakAndDurabilityStop(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var level=h.getLevel();
        level.setBlockAndUpdate(pos,Blocks.OAK_LOG.defaultBlockState());level.setBlockAndUpdate(pos.above(),Blocks.OAK_LOG.defaultBlockState());
        var p=player(h,true);p.gameMode.destroyBlock(pos);
        h.assertTrue(level.getBlockState(pos.above()).is(Blocks.OAK_LOG),"sneak harvests only clicked block");
        level.setBlockAndUpdate(pos,Blocks.OAK_LOG.defaultBlockState());p.setShiftKeyDown(false);
        var tool=p.getMainHandItem();tool.setDamageValue(GTToolHelper.getMaxDurability(tool)-200);p.gameMode.destroyBlock(pos);
        h.assertTrue(level.getBlockState(pos.above()).is(Blocks.OAK_LOG),"cannot afford 101 additional wear after base 100");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=30)
    public static void tallerColumnSlowsMining(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var level=h.getLevel();var p=player(h,false);
        var state=Blocks.OAK_LOG.defaultBlockState();level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        float one=com.gregtech.gregtech.item.behavior.AxeColumnHarvest.speed(p,p.getMainHandItem(),state,pos,10);
        level.setBlockAndUpdate(pos.above(),state);level.setBlockAndUpdate(pos.above(2),state);
        float tall=com.gregtech.gregtech.item.behavior.AxeColumnHarvest.speed(p,p.getMainHandItem(),state,pos,10);
        h.assertTrue(one==20&&tall<10,"height reduces original doubled log speed");h.succeed();
    }
}
