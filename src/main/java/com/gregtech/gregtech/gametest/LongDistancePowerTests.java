package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class LongDistancePowerTests {
    private static final BlockPos SOURCE=new BlockPos(2,2,2), RECEIVER=new BlockPos(5,2,2);
    private static final class Sink extends GTEnergyBlockEntity {
        long size, calls;
        Sink(BlockPos pos,BlockState state){super(GTBlockEntities.LONG_DIST_TRANSFORMER.get(),pos,state);}
        @Override public boolean isEnergyType(GregTechTags.Tag t,Direction s,boolean e){return t==GregTechTags.Energy.EU;}
        @Override public long doEnergyInjection(GregTechTags.Tag t,Direction side,long size,long amount,boolean execute){if(execute){this.size=size;calls++;}return Math.min(1,amount);}
        @Override public long getEnergyDemanded(GregTechTags.Tag t,Direction s,long size){return 1;}
        @Override public long getEnergyOffered(GregTechTags.Tag t,Direction s,long size){return 0;}
        @Override public long getEnergySizeInputRecommended(GregTechTags.Tag t,Direction s){return 524288;}
        @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag t,Direction s){return 0;}
    }
    private static LongDistanceTransformerBlockEntity setup(GameTestHelper h,LongDistanceTransformerBlock source,LongDistanceTransformerBlock receiver,Block wire){
        h.setBlock(SOURCE,source.defaultBlockState().setValue(LongDistanceTransformerBlock.FACING,Direction.WEST));
        h.setBlock(SOURCE.east(),wire);h.setBlock(SOURCE.east(2),wire);
        h.setBlock(RECEIVER,receiver.defaultBlockState().setValue(LongDistanceTransformerBlock.FACING,Direction.WEST));
        return (LongDistanceTransformerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(SOURCE));
    }
    private static Sink sink(GameTestHelper h){
        var pos=RECEIVER.east();var state=GTMiscBlocks.LONG_DIST_TRANSFORMER_ULV.get().defaultBlockState();h.setBlock(pos,state);
        var sink=new Sink(h.absolutePos(pos),state);h.getLevel().setBlockEntity(sink);return sink;
    }
    @GameTest(template="test_blueprint_empty")
    public static void everyVoltageAndSignedLoss(GameTestHelper h){
        var grades=new LongDistanceTransformerBlock[]{GTMiscBlocks.LONG_DIST_TRANSFORMER_ULV.get(),GTMiscBlocks.LONG_DIST_TRANSFORMER_LV.get(),GTMiscBlocks.LONG_DIST_TRANSFORMER_MV.get(),GTMiscBlocks.LONG_DIST_TRANSFORMER_ZPM.get(),GTMiscBlocks.LONG_DIST_TRANSFORMER_UV.get()};
        var names=new String[]{"ev","iv","luv","zpm","uv"};
        for(int i=0;i<grades.length;i++){
            var wire=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","long_dist_wire_"+names[i]));
            var source=setup(h,grades[i],grades[i],wire);var sink=sink(h);long voltage=2048L<<(i*2);
            h.assertTrue(source.voltage()==voltage,"original endpoint tier");
            h.assertTrue(source.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,voltage,2,false)==2&&sink.calls==0,"simulation does not transfer");
            h.assertTrue(source.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,-voltage,2,true)==1&&sink.size==64-voltage,"negative packet keeps sign and loses 64 EU");
            h.assertTrue(source.getEnergyOffered(GregTechTags.Energy.EU,null,voltage)==0,"no duplicate pull output");
        }
        h.assertTrue(LongDistanceTransformerBlockEntity.loss(1024)==128,"original eighth-EU distance loss with minimum 64");h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void breakStopAndDirection(GameTestHelper h){
        var grade=GTMiscBlocks.LONG_DIST_TRANSFORMER_ULV.get();var source=setup(h,grade,grade,GTMiscBlocks.LONG_DIST_WIRE.get());var sink=sink(h);
        h.assertTrue(source.doEnergyInjection(GregTechTags.Energy.HU,Direction.WEST,2048,1,true)==0&&source.doEnergyInjection(GregTechTags.Energy.EU,Direction.EAST,2048,1,true)==0,"wrong energy or side rejected");
        source.setStopped(true);source.load(source.saveWithoutMetadata());h.assertTrue(source.isStopped()&&source.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,2048,1,true)==0,"stop persists");source.setStopped(false);
        h.setBlock(SOURCE.east(),Blocks.AIR);h.assertTrue(source.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,2048,1,true)==0&&sink.calls==0,"broken route cannot transfer");
        h.setBlock(SOURCE.east(),GTMiscBlocks.LONG_DIST_WIRE.get());h.assertTrue(source.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,2048,1,true)==1,"repair immediately restores route");
        var branch=SOURCE.east().north();h.setBlock(branch,grade.defaultBlockState().setValue(LongDistanceTransformerBlock.FACING,Direction.SOUTH));
        h.assertTrue(source.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,2048,1,true)==0,"new endpoint invalidates cached route and rejects ambiguous fork");
        h.setBlock(branch,Blocks.AIR);h.assertTrue(source.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,2048,1,true)==1,"removing fork restores unique destination");h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void overvoltageBurnsOnlyOnExecution(GameTestHelper h){
        var grade=GTMiscBlocks.LONG_DIST_TRANSFORMER_LV.get();var source=setup(h,grade,grade,GTMiscBlocks.LONG_DIST_WIRE.get());
        source.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,8192,1,false);
        h.assertTrue(h.getBlockState(SOURCE.east()).is(GTMiscBlocks.LONG_DIST_WIRE.get()),"simulation cannot burn wire");
        source.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,8192,1,true);
        h.assertTrue(!h.getBlockState(SOURCE.east()).is(GTMiscBlocks.LONG_DIST_WIRE.get())&&!h.getBlockState(SOURCE.east(2)).is(GTMiscBlocks.LONG_DIST_WIRE.get()),"EV line burns under IV voltage");h.succeed();
    }
}
