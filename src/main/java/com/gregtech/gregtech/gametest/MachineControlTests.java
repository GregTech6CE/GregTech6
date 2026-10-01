package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.block.misc.*;
import com.gregtech.gregtech.blockentity.energy.*;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.logistics.ExtenderSpec;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class MachineControlTests {
    private static final BlockPos CENTER=new BlockPos(4,3,4);
    private static ExtenderBlockEntity relay(GameTestHelper h,BlockPos pos){
        h.setBlock(pos,Blocks.AIR);h.setBlock(pos,GTMiscBlocks.SOURCE_EXTENDERS.get(ExtenderSpec.UNIVERSAL).get().defaultBlockState().setValue(SourceExtenderBlock.FACING,Direction.EAST).setValue(SourceExtenderBlock.SECONDARY,Direction.EAST));
        return (ExtenderBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    @GameTest(template="test_blueprint_empty") public static void generatorFamiliesControlAndSave(GameTestHelper h){
        var blocks=new java.util.ArrayList<Block>();
        MachineRegistry.electricEngines().forEach(e->blocks.add(e.get()));MachineRegistry.fluxEngines().forEach(e->blocks.add(e.get()));
        MachineRegistry.steamEngines().forEach(e->blocks.add(e.get()));MachineRegistry.dieselEngines().forEach(e->blocks.add(e.get()));
        GTLasers.all().forEach(e->{if(e.get() instanceof com.gregtech.gregtech.block.energy.LaserConverterBlock||e.get() instanceof com.gregtech.gregtech.block.energy.ZpmDischargerBlock)blocks.add(e.get());});
        AxialGeneratorDefinitions.blocks(true).forEach(e->blocks.add(e.get()));AxialGeneratorDefinitions.blocks(false).forEach(e->blocks.add(e.get()));
        GasTurbineDefinitions.blocks().forEach(e->blocks.add(e.get()));blocks.add(GTEnergyNodes.REACTOR_CORE_BLOCK.get());blocks.add(GTEnergyNodes.REACTOR_CORE_2X2.get());
        var relay=relay(h,CENTER);var external=relay.machineControl(null);
        for(var block:blocks){
            h.setBlock(CENTER.east(),Blocks.AIR);h.setBlock(CENTER.east(),block);
            var target=h.getLevel().getBlockEntity(h.absolutePos(CENTER.east()));
            h.assertTrue(external.available(),"all material/tier variants provide controls: "+block);
            h.assertTrue(!external.setEnabled(false)&&!MachineControl.find(target,null).enabled(),"remote stop reaches original switch: "+block);
            var saved=target.saveWithoutMetadata();target.load(saved);
            h.assertTrue(!external.enabled(),"stopped state survives reload: "+block);
            h.assertTrue(external.setEnabled(true),"remote restart reaches original switch: "+block);
            h.assertTrue(!external.supportsProgress(),"generators do not fabricate a recipe progress bar: "+block);
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void pistonModesMapToOriginalOddPowerSteps(GameTestHelper h){
        var relay=relay(h,CENTER);var second=relay(h,CENTER.east());
        for(var block:java.util.List.of(MachineRegistry.electricEngines().get(0).get(),MachineRegistry.fluxEngines().get(0).get())){
            var pos=CENTER.east(2);h.setBlock(pos,Blocks.AIR);h.setBlock(pos,block);
            var engine=(PistonEngineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));var control=relay.machineControl(null);
            h.assertTrue(control.supportsMode(),"two-relay chain exposes mode capability");
            for(int mode=0;mode<16;mode++){
                h.assertTrue(control.setMode(mode)==mode,"mode echoes actual mapped value");
                var state=engine.saveWithoutMetadata();h.assertTrue(state.getInt("piston.mode")==mode*2+1,"external mode selects original odd internal step");
                engine.load(state);h.assertTrue(control.mode()==mode,"mode survives save/load");
            }
            h.assertTrue(control.setMode(999)==15&&control.setMode(-999)==0,"invalid external modes are bounded to four bits");
            control.setEnabled(false);h.assertTrue(!engine.isEnergyAcceptingFrom(block==MachineRegistry.electricEngines().get(0).get()?com.gregtech.gregtech.data.GregTechTags.Energy.EU:com.gregtech.gregtech.data.GregTechTags.Energy.RF,engine.getBlockState().getValue(com.gregtech.gregtech.block.machine.EngineBlock.FACING).getOpposite(),false),"stopped piston rejects new input");
        }
        h.setBlock(CENTER.east(2),Blocks.AIR);
        h.getLevel().setBlock(second.getBlockPos(),second.getBlockState().setValue(SourceExtenderBlock.FACING,Direction.WEST).setValue(SourceExtenderBlock.SECONDARY,Direction.WEST),3);
        h.assertTrue(!relay.machineControl(null).supportsMode()&&relay.machineControl(null).setMode(5)==0,"cyclic mode forwarding terminates");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void reactorModeMasksAndShutdownNeutrons(GameTestHelper h){
        var relay=relay(h,CENTER);h.setBlock(CENTER.east(),GTEnergyNodes.REACTOR_CORE_2X2.get());
        var core=(ReactorCoreBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(CENTER.east()));var control=relay.machineControl(null);
        control.setEnabled(true);
        for(int mode=0;mode<16;mode++){
            control.setMode(mode);
            for(int slot=0;slot<4;slot++)h.assertTrue(core.slotActive(slot)==((mode&(1<<slot))==0),"mode bit disables exactly its reactor slot");
            var saved=core.saveWithoutMetadata();core.load(saved);h.assertTrue(control.mode()==mode,"reactor mask persists");
        }
        java.util.Arrays.fill(core.neutrons,100);control.setEnabled(false);
        h.assertTrue(core.stopped&&core.neutronTotal()==0,"remote stop uses real neutron-isolation path");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void playerToolsOperateControlsAndUnsupportedTargetsDoNotWear(GameTestHelper h){
        var relay=relay(h,CENTER);h.setBlock(CENTER.east(),MachineRegistry.electricEngines().get(0).get());
        var player=h.makeMockPlayer();var material=com.gregtech.gregtech.content.material.Materials.Steel;
        var wood=com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood");
        var screwdriver=com.gregtech.gregtech.item.GTToolItem.create(GTToolType.SCREWDRIVER,material,wood);
        var hammer=com.gregtech.gregtech.item.GTToolItem.create(GTToolType.SOFT_HAMMER,material,wood);
        var hit=new BlockHitResult(Vec3.atCenterOf(relay.getBlockPos()),Direction.WEST,relay.getBlockPos(),false);var block=(SourceExtenderBlock)relay.getBlockState().getBlock();
        var control=relay.machineControl(null);int before=control.mode();player.setItemInHand(InteractionHand.MAIN_HAND,screwdriver);
        block.use(relay.getBlockState(),h.getLevel(),relay.getBlockPos(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(control.mode()==((before+1)&15)&&screwdriver.getDamageValue()>0,"screwdriver changes remote mode and wears once");
        player.setItemInHand(InteractionHand.MAIN_HAND,hammer);block.use(relay.getBlockState(),h.getLevel(),relay.getBlockPos(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(!control.enabled()&&hammer.getDamageValue()>0,"soft hammer changes real stop state");
        h.setBlock(CENTER.east(),Blocks.AIR);int wear=hammer.getDamageValue();block.use(relay.getBlockState(),h.getLevel(),relay.getBlockPos(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(hammer.getDamageValue()==wear,"absent target neither reports success nor consumes durability");h.succeed();
    }
    @GameTest(template="test_blueprint_empty") public static void remoteEnableCannotClearAxialOverload(GameTestHelper h){
        var relay=relay(h,CENTER);h.setBlock(CENTER.east(),AxialGeneratorDefinitions.blocks(false).get(0).get());
        var machine=(AxialGeneratorBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(CENTER.east()));
        var tag=machine.saveWithoutMetadata();tag.putBoolean("gt.overloaded",true);tag.putBoolean("gt.stopped",true);machine.load(tag);
        h.assertTrue(!relay.machineControl(null).setEnabled(true)&&machine.isOverloaded()&&machine.isStopped(),"remote enable does not replace mechanical fault reset");h.succeed();
    }
}
