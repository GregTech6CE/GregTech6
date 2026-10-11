package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.*;
import com.gregtech.gregtech.blockentity.energy.*;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTLasers;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class OpticalEnergyRegressionTests {
    @GameTest(template="test_blueprint_empty")
    public static void opticalMaterialsCraftingAndHarvestAreRegistered(GameTestHelper helper) {
        for(var entry:GTLasers.all()) if(entry.get() instanceof LaserConverterBlock block) {
            helper.assertTrue(block.spec().material().isValid(),"optical material resolves: "+entry.getId());
            helper.assertTrue(block.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE),"optical housing is harvestable: "+entry.getId());
        }
        for(var family:java.util.List.of("co2_laser","flux_laser","quantum_energizer")) {
            var tiers=family.equals("quantum_energizer")?java.util.List.of("ev","iv","luv","zpm","uv"):java.util.List.of("lv","mv","hv","ev","iv");
            for(var tier:tiers) {
                var id=com.gregtech.gregtech.GregTech.id("optical/"+family+"_"+tier);
                var recipe=helper.getLevel().getRecipeManager().byKey(id);
                helper.assertTrue(recipe.isPresent(),"optical crafting recipe actually loaded: "+id);
                helper.assertTrue(!recipe.orElseThrow().getResultItem(helper.getLevel().registryAccess()).isEmpty(),"optical recipe has registered output");
            }
        }
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void laserFiberTransfersOnceAndDisconnects(GameTestHelper helper) {
        var source=new BlockPos(1,2,2);var target=new BlockPos(4,2,2);
        helper.setBlock(source,GTLasers.CO2_LASER_LV.get().defaultBlockState().setValue(LaserConverterBlock.FACING,Direction.EAST));
        helper.setBlock(target,GTLasers.QUANTUM_ENERGIZER_EV.get().defaultBlockState().setValue(LaserConverterBlock.FACING,Direction.EAST));
        helper.setBlock(source.east(),GTLasers.LASER_FIBER_WIRE.get());
        helper.setBlock(source.east(2),GTLasers.LASER_FIBER_WIRE.get());
        var laser=(LaserConverterBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(source));
        var quantum=(LaserConverterBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(target));
        helper.assertTrue(laser.doEnergyInjection(GregTechTags.Energy.EU,Direction.EAST,32,1,true)==0,"front rejects input");
        helper.assertTrue(laser.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,false)==1 && laser.getEnergyStored(GregTechTags.Energy.EU,null)==0,"simulation does not charge laser");
        laser.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true);laser.tick();
        helper.assertTrue(quantum.getEnergyStored(GregTechTags.Energy.LU,null)==16,"32 EU reaches receiver as 16 LU without fiber loss");
        laser.tick();helper.assertTrue(quantum.getEnergyStored(GregTechTags.Energy.LU,null)==16,"buffer cannot emit twice");
        var saved=quantum.saveWithoutMetadata();quantum.load(saved);
        helper.assertTrue(quantum.getEnergyStored(GregTechTags.Energy.LU,null)==16,"quantum buffer survives save/load");
        helper.setBlock(source.east(2),Blocks.AIR);
        laser.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true);laser.tick();
        helper.assertTrue(quantum.getEnergyStored(GregTechTags.Energy.LU,null)==16 && laser.getEnergyStored(GregTechTags.Energy.EU,null)==0,"broken fiber stops transfer; unused laser energy dissipates");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void fluxLaserCapabilityAndStopPersist(GameTestHelper helper) {
        var pos=new BlockPos(1,2,1);helper.setBlock(pos,GTLasers.FLUX_LASER_LV.get());
        var laser=(LaserConverterBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(!laser.getCapability(ForgeCapabilities.ENERGY,Direction.NORTH).isPresent(),"front has no FE input");
        var cap=laser.getCapability(ForgeCapabilities.ENERGY,Direction.SOUTH).orElseThrow(IllegalStateException::new);
        helper.assertTrue(cap.receiveEnergy(128,true)==128 && cap.getEnergyStored()==0,"FE simulation is read-only");
        cap.receiveEnergy(128,false);helper.assertTrue(laser.getEnergyStored(GregTechTags.Energy.RF,null)==128,"FE and RF share one buffer");
        laser.toggleStopped();laser.load(laser.saveWithoutMetadata());
        helper.assertTrue(cap.receiveEnergy(1,false)==0,"stopped state persists");
        laser.invalidateCaps();laser.reviveCaps();
        helper.assertTrue(laser.getCapability(ForgeCapabilities.ENERGY,Direction.SOUTH).isPresent(),"FE capability revives");
        var drops=net.minecraft.world.level.block.Block.getDrops(laser.getBlockState(),helper.getLevel(),helper.absolutePos(pos),laser);
        helper.assertTrue(drops.size()==1&&drops.get(0).getTag().getCompound("BlockEntityTag").getBoolean("gt.stopped"),"dropped converter retains settings");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void fiberCycleCannotDuplicatePackets(GameTestHelper helper) {
        var positions=java.util.List.of(new BlockPos(2,2,2),new BlockPos(3,2,2),new BlockPos(3,2,3),new BlockPos(2,2,3));
        var receiverPos=new BlockPos(4,2,2);
        helper.setBlock(receiverPos,GTLasers.LASER_ABSORBER_LV.get().defaultBlockState().setValue(LaserConverterBlock.FACING,Direction.EAST));
        for(var pos:positions){
            var state=GTLasers.LASER_FIBER_WIRE.get().defaultBlockState();
            for(var side:Direction.values())state=state.setValue(ElectricWireBlock.propFor(side),true);
            helper.setBlock(pos,state);
        }
        var cable=(LaserFiberBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(positions.get(0)));
        var receiver=(LaserConverterBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(receiverPos));
        helper.assertTrue(cable.doEnergyInjection(GregTechTags.Energy.LU,Direction.WEST,32,1,false)==1 && receiver.getEnergyStored(GregTechTags.Energy.LU,null)==0,"cycle simulation reaches receiver without mutation");
        helper.assertTrue(cable.doEnergyInjection(GregTechTags.Energy.LU,Direction.WEST,32,1,true)==1 && receiver.getEnergyStored(GregTechTags.Energy.LU,null)==32,"cycle consumes a packet once");
        helper.succeed();
    }
}
