package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.registry.GTElectricItems;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class ElectricWrenchModeTests {
    private static UseOnContext context(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
    }
    @GameTest(template="test_empty")
    public static void modeSwitchPreservesMaterialsChargeWearAndSurvivesSave(GameTestHelper h) {
        var item=GTElectricItems.ELECTRIC_WRENCH.get();
        var stack=item.assembled(com.gregtech.gregtech.content.material.Materials.Steel,10000);
        stack.getOrCreateTag().putLong("gt.charge",500);
        stack.getOrCreateTag().getCompound("GT.ToolStats").putLong("k",27);
        var before=stack.getTag().copy();
        var player=h.makeMockSurvivalPlayer();player.setShiftKeyDown(true);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var p=new BlockPos(2,2,2);h.setBlock(p,Blocks.STONE);
        h.assertTrue(item.useOn(context(player,h.absolutePos(p))).consumesAction(),"sneak click switches mode");
        h.assertTrue(GTToolHelper.isMonkeyWrench(stack)&&!GTToolHelper.matchesTool(stack,GTToolType.WRENCH),"mode uses monkey identity exclusively");
        var after=stack.getTag().copy();after.remove("gt.monkey_wrench");
        h.assertTrue(before.equals(after),"switching preserves capacity, charge, materials, wear and all other NBT");
        var restored=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
        h.assertTrue(item.monkeyWrenchMode(restored),"mode persists in saved stack");
        stack.getOrCreateTag().putLong("gt.charge",0);
        item.useOn(context(player,h.absolutePos(p)));
        h.assertTrue(!item.monkeyWrenchMode(stack)&&!GTToolHelper.isMachineWrench(stack),"empty tool can switch but cannot operate");
        player.setShiftKeyDown(false);item.useOn(context(player,h.absolutePos(p)));
        h.assertTrue(!item.monkeyWrenchMode(stack),"ordinary click does not switch");
        player.setShiftKeyDown(true);h.setBlock(p,Blocks.CHEST);
        h.assertTrue(item.useOn(context(player,h.absolutePos(p)))==InteractionResult.PASS&&!item.monkeyWrenchMode(stack),"foreign block entity is not a switch target");
        h.setBlock(p,Blocks.STONE);player.getAbilities().mayBuild=false;
        item.useOn(context(player,h.absolutePos(p)));
        h.assertTrue(!item.monkeyWrenchMode(stack),"permission denial leaves mode intact");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void machineClickPrecedesModeSwitchAndConsumesPowerOnce(GameTestHelper h) {
        var item=GTElectricItems.ELECTRIC_WRENCH.get();var stack=new ItemStack(item);
        stack.getOrCreateTag().putLong("gt.charge",300);stack.getOrCreateTag().putBoolean("gt.monkey_wrench",true);
        var player=h.makeMockSurvivalPlayer();player.setShiftKeyDown(true);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var p=new BlockPos(2,2,2);h.setBlock(p,ForgeRegistries.BLOCKS.getValue(GregTech.id("transformer_lv_mv")));
        var pos=h.absolutePos(p);var state=h.getLevel().getBlockState(pos);
        h.assertTrue(ToolInteractions.describe(state,stack)==null,"monkey mode does not offer transformer facing overlay");
        h.assertTrue(item.useOn(context(player,pos)).consumesAction(),"sneaking reaches transformer monkey interaction");
        h.assertTrue(item.monkeyWrenchMode(stack)&&item.getEnergyStored(stack,GregTechTags.Energy.EU)==200,"successful machine use keeps mode and costs exactly 100 EU");
        h.assertTrue(item.isCorrectToolForDrops(stack,state),"monkey mode retains electric wrench harvest targets");h.succeed();
    }
}
