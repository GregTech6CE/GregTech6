package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.*;
import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.logistics.ExtenderSpec;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class ExtenderEnergyTests {
    private static final BlockPos CENTER=new BlockPos(4,3,4);
    private static ExtenderBlockEntity relay(GameTestHelper h,BlockPos pos,ExtenderSpec spec){
        h.setBlock(pos,Blocks.AIR);h.setBlock(pos,GTMiscBlocks.SOURCE_EXTENDERS.get(spec).get().defaultBlockState().setValue(SourceExtenderBlock.FACING,Direction.UP).setValue(SourceExtenderBlock.SECONDARY,Direction.EAST));
        return (ExtenderBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    private static BasicMachineBlockEntity sink(GameTestHelper h,BlockPos pos,GregTechTags.Tag type,long size){
        var block=MachineRegistry.basicMachines().get(0).get();h.setBlock(pos,Blocks.AIR);h.setBlock(pos,block);
        var machine=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        machine.setSpec(BasicMachineSpec.builder("relay_test",block.basicSpec().material()).machineType("test").energy(type,size).recipes(block.basicSpec().recipeMap()).faces(FaceConfig.ALL_SIDES).build());return machine;
    }
    @GameTest(template="test_blueprint_empty") public static void nativeTypesSidesAndSimulation(GameTestHelper h){
        for(var spec:new ExtenderSpec[]{ExtenderSpec.UNIVERSAL,ExtenderSpec.UNIVERSAL_BRIDGE})
        for(var type:new GregTechTags.Tag[]{GregTechTags.Energy.EU,GregTechTags.Energy.HU,GregTechTags.Energy.KU,GregTechTags.Energy.RU,GregTechTags.Energy.LU,GregTechTags.Energy.QU})
        for(var side:Direction.values()){
            var relay=relay(h,CENTER,spec);var exit=spec.bridge?side.getOpposite():side==Direction.UP?Direction.EAST:Direction.UP;
            var target=sink(h,CENTER.relative(exit),type,32);
            h.assertTrue(relay.getEnergyTypes(side).contains(type)&&relay.isEnergyAcceptingFrom(type,side,false),"energy metadata reaches live target");
            h.assertTrue(relay.getEnergySizeInputRecommended(type,side)==32,"voltage is not replaced with generic energy");
            long expected=target.doEnergyInjection(type,exit.getOpposite(),-32,1,false);
            h.assertTrue(expected==1&&relay.doEnergyInjection(type,side,-32,1,false)==expected&&target.getEnergyTick()==0,"simulation and negative packets preserved: "+type+" / "+side);
            h.assertTrue(relay.doEnergyInjection(type,side,-32,1,true)==1&&target.getEnergyTick()==32,"execute transfers once");
            h.assertTrue(relay.getEnergyDemanded(type,side,0)==0&&relay.getEnergyDemanded(type,side,Long.MIN_VALUE)==0,"invalid demand query cannot divide by zero");
            h.assertTrue(relay.getEnergyDemanded(type,side,-32)==relay.getEnergyDemanded(type,side,32),"signed size demand counts positive packets");
            var other=type==GregTechTags.Energy.EU?GregTechTags.Energy.HU:GregTechTags.Energy.EU;
            h.assertTrue(relay.doEnergyInjection(other,side,32,1,true)==0,"relay does not convert energy types");
            h.setBlock(CENTER.relative(exit),Blocks.AIR);
            h.assertTrue(relay.doEnergyInjection(type,side,32,1,true)==0&&relay.getEnergyTypes(side).isEmpty(),"removed target cannot swallow energy");
        }
        var ordinary=relay(h,CENTER,ExtenderSpec.COMBINED);sink(h,CENTER.above(),GregTechTags.Energy.EU,32);
        h.assertTrue(ordinary.doEnergyInjection(GregTechTags.Energy.EU,Direction.DOWN,32,1,true)==0&&!ordinary.getCapability(ForgeCapabilities.ENERGY,Direction.DOWN).isPresent(),"ordinary extender remains inventory/fluid only");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void realZpmBridgeConservesPackets(GameTestHelper h){
        var bridge=relay(h,CENTER,ExtenderSpec.UNIVERSAL_BRIDGE);
        var size=com.gregtech.gregtech.content.energy.ZpmEnergy.PACKET;
        var target=sink(h,CENTER.east(),GregTechTags.Energy.EU,size);
        h.setBlock(CENTER.west(),GTLasers.ZPM_DISCHARGER_ADVANCED.get().defaultBlockState().setValue(com.gregtech.gregtech.block.energy.ZpmDischargerBlock.FACING,Direction.EAST));
        var zpm=(com.gregtech.gregtech.blockentity.energy.ZpmDischargerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(CENTER.west()));
        zpm.inventory().insertItem(0,com.gregtech.gregtech.content.energy.ZpmEnergy.charged(),false);
        long before=zpm.totalEnergy();zpm.tick();
        h.assertTrue(target.getEnergyTick()==size&&zpm.totalEnergy()==before-size,"real emitter crosses bridge exactly once");
        h.assertTrue(bridge.doEnergyExtraction(GregTechTags.Energy.EU,Direction.EAST,size,1,true)==0,"push-only generator cannot be drained a second time");
        h.setBlock(CENTER.east(),Blocks.AIR);before=zpm.totalEnergy();zpm.tick();
        h.assertTrue(zpm.totalEnergy()==before,"disconnected bridge preserves module charge");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void fluxCapabilityTracksReplacementAndInvalidation(GameTestHelper h){
        var relay=relay(h,CENTER,ExtenderSpec.UNIVERSAL_BRIDGE);
        var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","engine_flux_lead"));
        h.setBlock(CENTER.east(),block.defaultBlockState().setValue(com.gregtech.gregtech.block.machine.EngineBlock.FACING,Direction.EAST));
        var optional=relay.getCapability(ForgeCapabilities.ENERGY,Direction.WEST);var flux=optional.orElseThrow(IllegalStateException::new);
        h.assertTrue(flux.canReceive()&&flux.receiveEnergy(128,true)==128&&flux.getEnergyStored()==0,"FE simulation reaches real flux engine without mutation");
        h.assertTrue(flux.receiveEnergy(128,false)==128&&flux.getEnergyStored()==128,"FE remains FE and reaches engine buffer");
        h.assertTrue(!flux.canExtract()&&flux.extractEnergy(64,false)==0,"target direction is respected");
        h.setBlock(CENTER.east(),Blocks.AIR);
        h.assertTrue(!flux.canReceive()&&flux.receiveEnergy(128,false)==0&&flux.getEnergyStored()==0,"cached handler rejects removed engine");
        h.setBlock(CENTER.east(),block.defaultBlockState().setValue(com.gregtech.gregtech.block.machine.EngineBlock.FACING,Direction.WEST));
        h.assertTrue(!flux.canReceive(),"replacement wrong-facing engine cannot be powered through output");
        h.setBlock(CENTER,Blocks.AIR);
        h.assertTrue(!optional.isPresent()&&flux.receiveEnergy(1,false)==0,"removing relay invalidates capability and retained wrapper");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void recursiveEnergyChainsTerminate(GameTestHelper h){
        var a=relay(h,CENTER,ExtenderSpec.UNIVERSAL);var b=relay(h,CENTER.east(),ExtenderSpec.UNIVERSAL);
        h.getLevel().setBlock(a.getBlockPos(),a.getBlockState().setValue(SourceExtenderBlock.FACING,Direction.EAST).setValue(SourceExtenderBlock.SECONDARY,Direction.EAST),3);
        h.getLevel().setBlock(b.getBlockPos(),b.getBlockState().setValue(SourceExtenderBlock.FACING,Direction.WEST).setValue(SourceExtenderBlock.SECONDARY,Direction.WEST),3);
        h.assertTrue(!a.isEnergyAcceptingFrom(GregTechTags.Energy.EU,Direction.WEST,false)&&a.getEnergyTypes(Direction.WEST).isEmpty()&&a.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true)==0,"native cycle returns unavailable");
        h.assertTrue(a.doEnergyExtraction(GregTechTags.Energy.EU,Direction.WEST,32,1,true)==0&&a.getEnergyCapacity(GregTechTags.Energy.EU,Direction.WEST)==0,"cycle cannot invent stored energy");
        var flux=a.getCapability(ForgeCapabilities.ENERGY,Direction.WEST).orElseThrow(IllegalStateException::new);
        h.assertTrue(!flux.canReceive()&&flux.receiveEnergy(100,false)==0&&flux.getEnergyStored()==0,"FE cycle terminates independently");h.succeed();
    }
}
