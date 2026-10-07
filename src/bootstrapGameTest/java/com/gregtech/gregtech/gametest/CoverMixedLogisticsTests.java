package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.content.logistics.*;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

/** Native mixed attachment stores must retain GT6's single stop flag until every cover is gone. */
@GameTestHolder("gregtech_cover_mixed") @PrefixGameTestTemplate(false)
public final class CoverMixedLogisticsTests {
    @GameTest(template="test_empty",timeoutTicks=40)
    public static void logisticsCoverKeepsStoppedAcrossControllerRemovalAndSave(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,4,4));
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","logistics_wire"));
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,block.defaultBlockState());
        var owner=level.getBlockEntity(pos);var host=(PanelCoverHost)owner;var logistics=(LogisticsCoverHost)owner;
        h.assertTrue(logistics.logisticsCovers().attach(Direction.EAST,new ItemStack(GTTechnological.get(LogisticsCoverType.CPU_LOGIC.id()))),"logistics attachment occupies its own face");
        h.assertTrue(host.attachCover(Direction.NORTH,new ItemStack(GTTechnological.get(PanelCover.CONTROLLER.id))),"ordinary cover controller coexists on different face");
        host.panels().beforeTick();h.assertTrue(host.panels().stopped(),"unpowered cover controller stops attachments");
        host.removeCover(Direction.NORTH);host.panels().beforeTick();
        h.assertTrue(host.panels().stopped()&&host.hasAttachedCovers(),"last ordinary cover removal preserves stopped while logistics remains");
        var saved=owner.saveWithoutMetadata();
        h.assertTrue(saved.getBoolean("gt.cover.stopped"),"native save records the mixed-store stop flag");
        host.panels().restoreStopped(false);owner.load(saved);
        host.panels().beforeTick();h.assertTrue(host.panels().stopped(),"native load restores latch with only logistics attachment");
        logistics.logisticsCovers().remove(Direction.EAST);host.panels().beforeTick();
        h.assertTrue(!host.panels().stopped()&&!host.hasAttachedCovers(),"last attachment removal clears stop flag");h.succeed();
    }
}
