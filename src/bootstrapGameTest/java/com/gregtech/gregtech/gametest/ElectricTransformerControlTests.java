package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.energy.*;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.content.energy.ElectricTransformerControl;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class ElectricTransformerControlTests {
    private static EnergyNodeBlockEntity place(GameTestHelper h,BlockPos p,Direction facing){
        h.setBlock(p,ForgeRegistries.BLOCKS.getValue(GregTech.id("transformer_lv_mv")).defaultBlockState().setValue(EnergyNodeBlock.FACING,facing));
        return (EnergyNodeBlockEntity)h.getBlockEntity(p);
    }
    private static void tick(GameTestHelper h,EnergyNodeBlockEntity b){EnergyNodeBlockEntity.serverTick(h.getLevel(),b.getBlockPos(),b.getBlockState(),b);}
    private static ItemStack panel(PanelCover p){return new ItemStack(GTTechnological.get(p.id));}
    @GameTest(template="test_empty")
    public static void activityUsesSixtyFourTicksAndSeparateConversionDirections(GameTestHelper h){
        var state=new ElectricTransformerControl(()->{});
        h.assertTrue(state.visual()==0&&state.accepts(),"new converter starts idle and accepts input");
        state.tick(true,false);h.assertTrue(state.visual()==2&&state.running()&&!state.active()&&!state.accepts(),"blocked but possible converter blinks and backpressures input");
        state.select(true);h.assertTrue(state.accepts(),"fresh reverse converter has separate possible/emitted flags");state.select(false);h.assertTrue(!state.accepts(),"returning retains original direction flags");
        for(int i=1;i<64;i++)state.tick(true,true);
        h.assertTrue(state.visual()==1,"64 consecutive possible ticks become steady active");
        state.tick(false,false);h.assertTrue(state.visual()==2,"one inactive sample returns to blinking");
        for(int i=1;i<64;i++)state.tick(false,false);
        h.assertTrue(state.visual()==0,"64 inactive ticks become idle");
        state.tick(true,true);state.setEnabled(false);h.assertTrue(state.visual()==0&&!state.accepts(),"stopped control forces idle appearance and rejects input");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void realWorldActivityAndBlockedInputFollowOriginalConverter(GameTestHelper h){
        var b=place(h,new BlockPos(1,1,1),Direction.NORTH);var eu=GregTechTags.Energy.EU;
        b.doEnergyInjection(eu,Direction.NORTH,128,1,true);tick(h,b);
        h.assertTrue(b.getBlockState().getValue(ElectricTransformerBlock.ACTIVITY)==2,"world block state uses blinking overlay");
        h.assertTrue(b.doEnergyInjection(eu,Direction.NORTH,128,1,true)==0&&b.stored()==128,"blocked output prevents accumulating input");
        h.assertTrue(b.isEnergyAcceptingFrom(eu,Direction.NORTH,true),"theoretical front remains an input");
        for(int i=1;i<64;i++)tick(h,b);
        h.assertTrue(b.getBlockState().getValue(ElectricTransformerBlock.ACTIVITY)==1,"sustained threshold reaches original active overlay even with no receiver");
        b.machineControl(Direction.NORTH).setEnabled(false);tick(h,b);
        h.assertTrue(b.getBlockState().getValue(ElectricTransformerBlock.ACTIVITY)==0,"stop is reflected by block state");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void modeLimitsPacketSizeAndStopDoesNotDeleteBufferedOutput(GameTestHelper h){
        var a=place(h,new BlockPos(1,1,1),Direction.NORTH);var b=place(h,new BlockPos(1,1,2),Direction.SOUTH);b.toggleInverted();
        var eu=GregTechTags.Energy.EU;var control=a.machineControl(Direction.NORTH);control.setMode(12);
        a.doEnergyInjection(eu,Direction.NORTH,128,1,true);tick(h,a);
        h.assertTrue(a.stored()==64&&b.stored()==64,"mode 12 caps normal output at 16 EU x four packets");
        control.setEnabled(false);tick(h,a);
        h.assertTrue(a.stored()==0&&b.stored()==128,"stopping refuses further input but sends the already buffered 64 EU");
        h.assertTrue(a.doEnergyInjection(eu,Direction.NORTH,128,1,true)==0,"stopped converter refuses input");
        var copy=new EnergyNodeBlockEntity(a.getBlockPos(),a.getBlockState());copy.load(a.saveWithoutMetadata());
        h.assertTrue(!copy.machineControl(Direction.NORTH).enabled()&&copy.machineControl(Direction.NORTH).mode()==12,"mode and stop persist");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void directSelectorAndStatusPanelsPersistAndDrop(GameTestHelper h){
        var b=place(h,new BlockPos(2,2,2),Direction.NORTH);
        h.assertTrue(b.attachCover(Direction.NORTH,panel(PanelCover.BUTTONS))&&b.attachCover(Direction.SOUTH,panel(PanelCover.STATUS)),"original control panels attach to transformer");
        h.assertTrue(b.panels().click(Direction.NORTH,.3,.3)&&b.machineControl(Direction.NORTH).mode()==5,"selector controls converter mode");
        h.assertTrue(b.panels().click(Direction.SOUTH,.8,.9)&&!b.machineControl(Direction.NORTH).enabled(),"status toggle controls input stop");
        var copy=new EnergyNodeBlockEntity(b.getBlockPos(),b.getBlockState());copy.load(b.getUpdateTag());
        h.assertTrue(PanelCover.of(copy.getCover(Direction.NORTH))==PanelCover.BUTTONS&&copy.machineControl(Direction.NORTH).mode()==5,"update NBT contains panel and mode");
        b.removeCover(Direction.NORTH);h.assertTrue(b.machineControl(Direction.NORTH).mode()==0,"removing selector resets mode");
        var pos=b.getBlockPos();h.getLevel().destroyBlock(pos,true);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(.8));
        h.assertTrue(drops.stream().filter(e->PanelCover.of(e.getItem())==PanelCover.STATUS).count()==1,"remaining transformer panel drops once");h.succeed();
    }
}
