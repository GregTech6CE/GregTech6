package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.ElectricToolItem;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class ElectricToolInteractionTests {
    private static ItemStack charged(ElectricToolItem item, long charge) {
        var stack = new ItemStack(item); stack.getOrCreateTag().putLong("gt.charge", charge); return stack;
    }
    private static long energy(ItemStack stack) {
        return ((ElectricToolItem)stack.getItem()).getEnergyStored(stack, GregTechTags.Energy.EU);
    }
    private static BlockHitResult hit(BlockPos pos, Direction side) {
        return new BlockHitResult(Vec3.atCenterOf(pos).add(side.getStepX()*.5,side.getStepY()*.5,side.getStepZ()*.5),side,pos,false);
    }
    @GameTest(template="test_empty")
    public static void electricWrenchUsesSharedFacesAndDepletesWithoutFreeExtraClick(GameTestHelper h) {
        var p = new BlockPos(2,2,2); var level=h.getLevel();
        var block=ForgeRegistries.BLOCKS.getValue(GregTech.id("transformer_lv_mv"));
        h.setBlock(p,block.defaultBlockState().setValue(EnergyNodeBlock.FACING,Direction.NORTH));
        var pos=h.absolutePos(p);var player=h.makeMockSurvivalPlayer();
        var wrench=charged(GTElectricItems.ELECTRIC_WRENCH.get(),150);player.setItemInHand(InteractionHand.MAIN_HAND,wrench);
        var spec=ToolInteractions.describe(level.getBlockState(pos),wrench);
        h.assertTrue(spec!=null&&!GTToolHelper.isMonkeyWrench(wrench),"electric wrench has facing overlay, not monkey mode");
        block.use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos,Direction.EAST));
        h.assertTrue(level.getBlockState(pos).getValue(EnergyNodeBlock.FACING)==Direction.EAST&&energy(wrench)==50,"successful rotation costs 100 EU");
        block.use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos,Direction.EAST));
        h.assertTrue(energy(wrench)==0,"original same-facing click spends the last partial charge");
        h.assertTrue(ToolInteractions.describe(level.getBlockState(pos),wrench)==null,"empty tool has no active overlay");
        ToolInteractions.use(spec,level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos,Direction.WEST));
        h.assertTrue(level.getBlockState(pos).getValue(EnergyNodeBlock.FACING)==Direction.EAST,"cached spec cannot bypass empty battery");
        wrench.getOrCreateTag().putLong("gt.charge",200);player.getAbilities().mayBuild=false;
        ToolInteractions.use(spec,level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos,Direction.WEST));
        h.assertTrue(energy(wrench)==200&&level.getBlockState(pos).getValue(EnergyNodeBlock.FACING)==Direction.EAST,"denied interaction neither rotates nor spends power");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void electricScrewdriverConfiguresPanelAndEmptyToolNeverBecomesFingerClick(GameTestHelper h) {
        var p=new BlockPos(2,2,2);var level=h.getLevel();
        h.setBlock(p,ForgeRegistries.BLOCKS.getValue(GregTech.id("transformer_lv_mv")));
        var node=(EnergyNodeBlockEntity)h.getBlockEntity(p);var pos=node.getBlockPos();
        h.assertTrue(node.attachCover(Direction.NORTH,new ItemStack(GTTechnological.get(PanelCover.STATUS.id))),"status panel attaches");
        h.assertTrue(node.attachCover(Direction.SOUTH,new ItemStack(GTTechnological.get(PanelCover.BUTTONS.id))),"button panel attaches");
        var screwdriver=charged(GTElectricItems.ELECTRIC_SCREWDRIVER.get(),100);var player=h.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND,screwdriver);
        boolean reset=node.getCover(Direction.SOUTH).getOrCreateTag().getBoolean(PanelCoverRuntime.RESET);
        PanelCoverInteraction.use(node,player,InteractionHand.MAIN_HAND,hit(pos,Direction.SOUTH),true);
        h.assertTrue(node.getCover(Direction.SOUTH).getOrCreateTag().getBoolean(PanelCoverRuntime.RESET)!=reset&&energy(screwdriver)==0,"screwdriver configures button reset mode and pays 100 EU");
        boolean enabled=node.machineControl(Direction.NORTH).enabled();
        var toggle=new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.2,.1,0),Direction.NORTH,pos,false);
        PanelCoverInteraction.use(node,player,InteractionHand.MAIN_HAND,toggle,true);
        h.assertTrue(node.machineControl(Direction.NORTH).enabled()==enabled,"empty electric screwdriver never presses status panel");
        PanelCoverInteraction.use(node,player,InteractionHand.MAIN_HAND,hit(pos,Direction.SOUTH),true);
        h.assertTrue(node.getCover(Direction.SOUTH).getOrCreateTag().getBoolean(PanelCoverRuntime.RESET)!=reset,"empty screwdriver cannot configure button panel again");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void originalReturnCostRoundingAndCreativeExemption(GameTestHelper h) {
        var tool=charged(GTElectricItems.ELECTRIC_SCREWDRIVER.get(),500);var player=h.makeMockSurvivalPlayer();
        GTToolHelper.damageForToolClickReturn(tool,101,player);
        h.assertTrue(energy(tool)==498,"GT6 click return is rounded up per 100 units");
        player.getAbilities().instabuild=true;GTToolHelper.damageForUse(tool,1,player);
        h.assertTrue(energy(tool)==498,"creative interaction preserves charge");
        player.getAbilities().instabuild=false;GTToolHelper.damageForToolClickReturn(tool,Long.MAX_VALUE,player);
        h.assertTrue((tool.isEmpty()||energy(tool)==0)&&!GTToolHelper.isScrewdriver(tool),"extreme return drains safely or breaks the tool; neither permits another click");
        h.assertTrue(!GTToolHelper.matchesTool(charged(GTElectricItems.ELECTRIC_DRILL.get(),1000),GTToolType.PICKAXE),"LV drill is not a pickaxe");
        h.succeed();
    }
}
