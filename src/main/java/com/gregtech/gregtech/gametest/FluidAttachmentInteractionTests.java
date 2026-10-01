package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.tool.FluidAttachmentBlock;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder("gregtech")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class FluidAttachmentInteractionTests {
    private static Block block(String id){return net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",id));}
    @GameTest(template="test_empty") public static void funnelTopPlacementAndWrenchPolicy(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
        var player=h.makeMockPlayer();
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p).add(0,.5,0),Direction.UP,p,false);
        var funnel=(FluidAttachmentBlock)block("fluid_funnel");
        var context=new BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(funnel),hit);
        var state=funnel.getStateForPlacement(context);
        h.assertTrue(state!=null&&state.getValue(FluidAttachmentBlock.FACING)==Direction.DOWN,"top placement points into tank below");
        var tool=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","tool_wrench")));
        var policy=funnel.toolInteraction(state,tool);
        h.assertTrue(policy!=null&&policy.allows(Direction.DOWN)&&!policy.allows(Direction.UP),"rendered wrench grid agrees with five allowed funnel faces");
        var tap=(FluidAttachmentBlock)block("tap");
        h.assertTrue(tap.getStateForPlacement(context)==null&&!tap.toolInteraction(tap.defaultBlockState(),tool).allows(Direction.DOWN),"tap stays horizontal");
        var bounds=state.getShape(h.getLevel(),p).bounds();
        h.assertTrue(bounds.minY==0&&bounds.maxY==3/16.0,"top funnel uses original shallow 3-pixel mesh bounds");h.succeed();
    }
    @GameTest(template="test_empty") public static void tapFillsCauldronsWithoutFreeWater(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(1,2,1));var below=p.below();var tank=new FluidTank(4000);
        h.getLevel().setBlockAndUpdate(below,Blocks.CAULDRON.defaultBlockState());tank.fill(new FluidStack(Fluids.WATER,333),FluidAction.EXECUTE);
        h.assertTrue(!FluidAttachmentBlock.pourBelow(h.getLevel(),p,tank)&&tank.getFluidAmount()==333,"less than one third cannot fill cauldron");
        tank.fill(new FluidStack(Fluids.WATER,1),FluidAction.EXECUTE);h.assertTrue(FluidAttachmentBlock.pourBelow(h.getLevel(),p,tank)&&tank.isEmpty(),"334 L buys exactly one level");
        h.assertTrue(h.getLevel().getBlockState(below).getValue(LayeredCauldronBlock.LEVEL)==1,"one cauldron level");
        tank.fill(new FluidStack(Fluids.WATER,667),FluidAction.EXECUTE);h.assertTrue(FluidAttachmentBlock.pourBelow(h.getLevel(),p,tank)&&tank.isEmpty(),"667 L buys remaining two levels");
        tank.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);h.assertTrue(!FluidAttachmentBlock.pourBelow(h.getLevel(),p,tank)&&tank.getFluidAmount()==1000,"full cauldron consumes no water");
        h.getLevel().setBlockAndUpdate(below,Blocks.CAULDRON.defaultBlockState());h.assertTrue(FluidAttachmentBlock.pourBelow(h.getLevel(),p,tank)&&tank.isEmpty(),"1000 L fills empty cauldron in one click");h.succeed();
    }
    @GameTest(template="test_empty") public static void tapPoursIntoPlacedCup(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(1,2,1));h.getLevel().setBlockAndUpdate(p.below(),block("fluid_cup").defaultBlockState());
        var source=new FluidTank(1000);source.fill(new FluidStack(Fluids.WATER,500),FluidAction.EXECUTE);
        var tap=(FluidAttachmentBlock)block("tap");
        h.assertTrue(FluidAttachmentBlock.pourBelow(h.getLevel(),p,tap.access(source))&&source.getFluidAmount()==250,"one click transfers 250 L into porcelain cup");
        h.assertTrue(!FluidAttachmentBlock.pourBelow(h.getLevel(),p,tap.access(source))&&source.getFluidAmount()==250,"full cup consumes nothing");
        var vessel=(com.gregtech.gregtech.blockentity.tool.PortableContainerBlockEntity)h.getLevel().getBlockEntity(p.below());
        var contents=vessel.contents().getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
        h.assertTrue(contents.getFluidInTank(0).getAmount()==250,"picked-up vessel item contains same transferred fluid");h.succeed();
    }
}
