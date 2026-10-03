package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.block.machine.*;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

@GameTestHolder("gregtech_alignment") @PrefixGameTestTemplate(false)
public final class NewIssueMachineTests {
    private static net.minecraft.world.level.block.Block block(String path) {
        var id=ResourceLocation.fromNamespaceAndPath("gregtech",path);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalStateException("Missing fixture " + id);
        return BuiltInRegistries.BLOCK.get(id);
    }
    @GameTest(template="test_empty")
    public static void originalOreMachineTablesContainVisibleExecutableRows(GameTestHelper h) {
        var crusher=MachineRecipeMaps.Crusher;
        var raw=GTItems.getStack(MaterialPrefix.oreRaw,Materials.Iron,1);
        var crushed=GTItems.getStack(MaterialPrefix.crushed,Materials.Iron.getTargetCrushingMaterial(),1);
        h.assertTrue(!raw.isEmpty()&&!crushed.isEmpty(),"actual iron forms exist");
        for(var map:List.of(crusher,MachineRecipeMaps.Bath,MachineRecipeMaps.Centrifuge,MachineRecipeMaps.Shredder)) {
            long visible=map.mRecipeList.stream().filter(r->r.mEnabled&&!r.mHidden&&!r.mFakeRecipe).count();
            h.assertTrue(visible>0,"ore viewer category has executable rows: "+map.mNameInternal);
            com.mojang.logging.LogUtils.getLogger().info("ORE_VIEWER_ROWS {} {}",map.mNameInternal,visible);
        }
        var rawRow=crusher.mRecipeList.stream().filter(r->r.mEnabled&&!r.mHidden
            &&Arrays.stream(r.mInputs).anyMatch(s->s.getItem()==raw.getItem())).findFirst().orElseThrow();
        h.assertTrue(rawRow.mOutputs.length==2&&rawRow.mOutputs[0].getItem()==crushed.getItem(),"source crusher: raw ore -> two guaranteed crushed outputs: "+Arrays.toString(rawRow.mOutputs));
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void fluidPipeDeclaresGridAndAllSixConnectionMarkers(GameTestHelper h) {
        var pipe=(FluidPipeBlock)BuiltInRegistries.BLOCK.stream().filter(b->b instanceof FluidPipeBlock).findFirst().orElseThrow();
        var state=pipe.defaultBlockState();var wrench=GTToolHelper.displayTool(GTToolType.WRENCH);
        var interaction=pipe.toolInteraction(state,wrench);
        h.assertTrue(interaction!=null&&interaction.connection()==ToolInteractionSpec.ConnectionKind.FLUID,"native overlay selects fluid-pipe grid");
        for(var side:Direction.values()) {
            state=state.setValue(FluidPipeBlock.propFor(side),true);
            h.assertTrue(interaction.allows(state,side)&&(interaction.activeFaces(state)&1<<side.ordinal())!=0,"connected side is marked: "+side);
        }
        h.assertTrue(interaction.activeFaces(state)==63&&pipe.toolInteraction(state,new ItemStack(Items.STICK))==null,"six markers and non-tool rejection");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void cokeFrontInsertionBurnsAndLeavesSourceDarkAsh(GameTestHelper h) {
        int tested=0;
        for(var material:List.of(Materials.CoalCoke,Materials.LigniteCoke,Materials.PetroleumCoke)) {
            for(var prefix:List.of(MaterialPrefix.dust,MaterialPrefix.dustSmall,MaterialPrefix.dustTiny,MaterialPrefix.gem)) {
                var coke=GTItems.getStack(prefix,material,1);if(coke.isEmpty())continue;
                var burn=FurnaceFuelHelper.burnOne(coke,7500).orElseThrow(()->new IllegalStateException("Coke rejected: "+material.getName()+"/"+prefix.getName()+" burnTime="+material.getFurnaceBurnTime()+" composition="+com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(coke)));
                h.assertTrue(burn.heatUnits()>0&&burn.byproduct()!=null&&!burn.byproduct().isEmpty(),"source coke has HU and ash");tested++;
            }
        }
        var pos=h.absolutePos(new BlockPos(2,2,2));var state=block("burning_box_solid_bronze").defaultBlockState();
        h.getLevel().setBlockAndUpdate(pos,state);var box=(SolidBurningBoxBlockEntity)h.getLevel().getBlockEntity(pos);
        var player=h.makeMockSurvivalPlayer();var fuel=GTItems.getStack(MaterialPrefix.dust,Materials.CoalCoke,1);
        player.setItemInHand(InteractionHand.MAIN_HAND,fuel);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),box.getFrontFacing(),pos,false);
        h.assertTrue(box.handleUse(player,InteractionHand.MAIN_HAND,hit).consumesAction()&&player.getMainHandItem().isEmpty(),"real front click accepts coke");
        h.assertTrue(box.usePoweredIgniter(box.getFrontFacing(),Long.MAX_VALUE,0),"loaded coke ignites");
        SolidBurningBoxBlockEntity.serverTick(h.getLevel(),pos,state,box);
        h.assertTrue(box.getFuelStack().isEmpty()&&!box.getAshStack().isEmpty(),"burned coke leaves ash in actual box");
        com.mojang.logging.LogUtils.getLogger().info("COKE_BURN_SUCCESS forms={} ash={}",tested,box.getAshStack());h.succeed();
    }
    @GameTest(template="test_empty")
    public static void pressurizedBoilerExplodesOnSurvivalRemovalOnly(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,3,3));var block=(BoilerTankBlock)block("steam_boiler_bronze");
        var state=block.defaultBlockState();var player=h.makeMockSurvivalPlayer();
        h.getLevel().setBlockAndUpdate(pos,state);
        block.playerWillDestroy(h.getLevel(),pos,state,player);
        h.assertTrue(h.getLevel().getBlockState(pos).is(block),"cold empty boiler does not explode");
        var boiler=(BoilerTankBlockEntity)h.getLevel().getBlockEntity(pos);
        boiler.steamTank().fill(new FluidStack(GTFluids.still("Steam").get(),(int)(boiler.steamTank().getCapacity()/5)),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(boiler.barometerValue()>4,"20% pressure exceeds source safe-removal threshold");
        block.playerWillDestroy(h.getLevel(),pos,state,h.makeMockPlayer());
        h.assertTrue(h.getLevel().getBlockState(pos).is(block),"creative dismantling is exempt like GT6");
        block.playerWillDestroy(h.getLevel(),pos,state,player);
        h.assertTrue(!h.getLevel().getBlockState(pos).is(block),"survival removal executes boiler explosion");h.succeed();
    }
}
