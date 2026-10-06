package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.api.transport.PipeConnections;
import com.gregtech.gregtech.content.logistics.*;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

/** Extra narrow regression for the actual Neo connector property identity used by tool overlays. */
@GameTestHolder("gregtech_cover_tools") @PrefixGameTestTemplate(false)
public final class NeoCoverToolBoundaryTests {
    @GameTest(template="test_empty",timeoutTicks=40)
    public static void logisticsToolPreviewAndOppositeCoverGate(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,4,4));var neighbor=pos.east();
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","logistics_wire"));
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(neighbor,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos,block.defaultBlockState());level.setBlockAndUpdate(neighbor,block.defaultBlockState());
        var host=(LogisticsCoverHost)level.getBlockEntity(pos);
        var cutter=com.gregtech.gregtech.item.GTToolItem.create(GTToolType.WIRE_CUTTER,
                com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,cutter);
        var spec=ToolInteractions.describe(level.getBlockState(pos),cutter);
        h.assertTrue(spec!=null&&spec.connection().property(Direction.EAST)==PipeConnections.propFor(Direction.EAST),"overlay spec uses actual logistics property object");
        spec.activeFaces(level.getBlockState(pos));
        h.assertTrue(host.logisticsCovers().attach(Direction.EAST,new ItemStack(GTTechnological.get(LogisticsCoverType.CPU_LOGIC.id()))),"CPU cover attaches and disconnects native connectors");
        h.assertTrue(!ToolInteractions.canApply(spec,level.getBlockState(pos),level,pos,player,cutter,Direction.EAST),"tool preview rejects covered local face");
        var otherSpec=ToolInteractions.describe(level.getBlockState(neighbor),cutter);
        h.assertTrue(!ToolInteractions.canApply(otherSpec,level.getBlockState(neighbor),level,neighbor,player,cutter,Direction.WEST),"tool preview rejects opposite host cover");
        h.succeed();
    }
}
