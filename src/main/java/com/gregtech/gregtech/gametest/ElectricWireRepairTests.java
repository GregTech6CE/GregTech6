package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.energy.GTVoltageTiers;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.misc.ChargingCraftingTableBlockEntity;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ElectricWireRepairTests {
    private static ElectricWireBlock block(boolean insulated,int size) {
        return (insulated?GTWires.allCables():GTWires.allWires()).stream().map(net.minecraftforge.registries.RegistryObject::get)
                .filter(b->b.spec().material()==Materials.Copper&&b.spec().size()==size).findFirst().orElseThrow();
    }
    private static ElectricWireBlockEntity place(GameTestHelper h,BlockPos pos,boolean insulated,int size) {
        h.setBlock(pos.below(),Blocks.STONE);
        h.setBlock(pos,block(insulated,size).defaultBlockState().setValue(ElectricWireBlock.WEST,true).setValue(ElectricWireBlock.EAST,true));
        return (ElectricWireBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    private static ChargingCraftingTableBlockEntity receiver(GameTestHelper h,BlockPos pos,int slots) {
        h.setBlock(pos,GTMiscBlocks.CHARGING_CRAFTING_TABLE.get());
        var table=(ChargingCraftingTableBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        for(int i=16;i<16+slots;i++)table.items().setStackInSlot(i,new ItemStack(GTElectricItems.BATTERY_LV.get()));
        return table;
    }
    private static long send(ElectricWireBlockEntity wire,long volts,long packets) {
        return wire.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,volts,packets,true);
    }
    @GameTest(template="test_empty")
    public static void rejectedPacketsNeverEnergizeWireAndAcceptedPowerAccumulates(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);var wire=place(h,pos,false,12);
        h.assertTrue(!wire.isConducting()&&send(wire,32,8)==0,"idle and unloaded open circuit are harmless");
        wire.serverTick();h.assertTrue(wire.lastWattage()==0&&wire.transferredAmperes()==0,"open circuit records no attempted wattage");
        var table=receiver(h,pos.east(),1);
        h.assertTrue(send(wire,32,8)==1&&send(wire,32,8)==1,"only receiver-accepted packets count across repeated calls");
        h.assertTrue(send(wire,-32,8)==1&&wire.transferredAmperes()==3,"GT6 battery accepts negative packet magnitude and wire counts actual transfer");
        h.assertTrue(wire.lastWattage()==0&&wire.transferredAmperes()==3,"power is published on tick, amperage accumulates across calls");
        wire.serverTick();h.assertTrue(wire.lastWattage()==3*(32-wire.lossPerBlock())&&wire.isConducting(),"previous-tick wattage is post-loss and cumulative");
        h.assertTrue(wire.transferredAmperes()==0,"tick clears amperage budget");
        var full=table.items().getStackInSlot(16).copy();full.getOrCreateTag().putLong("gt.charge",100000);table.items().setStackInSlot(16,full);
        h.assertTrue(send(wire,32,1)==0,"full receiver refuses input");wire.serverTick();
        h.assertTrue(!wire.isConducting()&&wire.lastWattage()==0,"no invented twenty-tick shock after current ceases");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void cumulativeOverloadBurnsThroughActualBlockTicker(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);var wire=place(h,pos,true,1);receiver(h,pos.east(),1);
        for(long i=0;i<wire.amperage();i++)h.assertTrue(send(wire,32,1)==1&&wire.burnCounter()==0,"cumulative current up to registered rating does not heat cable");
        for(int i=0;i<16;i++)h.assertTrue(send(wire,32,7)==7,"overloaded transfer consumes remaining offer as in GT6");
        h.assertTrue(wire.burnCounter()==16,"sixteen overload events saturate burn counter");
        h.assertTrue(h.getLevel().getBlockState(wire.getBlockPos()).getBlock()==block(true,1),"burn waits for block ticker");
        h.runAfterDelay(2,()->{h.assertTrue(h.getLevel().getBlockState(wire.getBlockPos()).getBlock()!=block(true,1)&&wire.isRemoved(),"registered block ticker destroys overloaded cable");h.succeed();});
    }
    @GameTest(template="test_empty")
    public static void voltageBoundaryCooldownAndExtremeCountersMatchOriginal(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);var wire=place(h,pos,true,12);
        var table=receiver(h,pos.east(),1);
        table.items().setStackInSlot(16,new ItemStack(GTElectricItems.BATTERY_MV.get()));
        table.items().setStackInSlot(17,new ItemStack(GTElectricItems.BATTERY_HV.get()));
        h.assertTrue(send(wire,wire.voltage()+wire.lossPerBlock(),1)==1&&wire.burnCounter()==0,"rating compares voltage after local loss");
        h.assertTrue(send(wire,wire.voltage()+wire.lossPerBlock()+1,7)==7&&wire.burnCounter()==1,"one volt over rating heats wire and consumes offer");
        wire.serverTick();h.assertTrue(wire.burnCounter()==1,"first tick does not cool");wire.serverTick();
        h.assertTrue(wire.burnCounter()==0,"GT6 timer modulo 512 equals 2 cools one step");
        send(wire,wire.voltage()+wire.lossPerBlock()+1,1);
        for(int i=0;i<511;i++)wire.serverTick();h.assertTrue(wire.burnCounter()==1,"cooldown is not a per-tick reset");
        wire.serverTick();h.assertTrue(wire.burnCounter()==0,"next 512-tick boundary cools again");
        wire.addToEnergyTransferred(Long.MAX_VALUE,Long.MAX_VALUE);wire.addToEnergyTransferred(Long.MAX_VALUE,Long.MAX_VALUE);
        h.assertTrue(wire.transferredAmperes()==Long.MAX_VALUE&&wire.burnCounter()==2,"extreme counters cannot overflow negative or bypass overload");
        wire.serverTick();h.assertTrue(wire.lastWattage()==Long.MAX_VALUE,"wattage saturates instead of becoming negative");
        var restored=new ElectricWireBlockEntity(wire.getBlockPos(),wire.getBlockState());restored.load(wire.saveWithoutMetadata());
        h.assertTrue(restored.burnCounter()==0&&restored.lastWattage()==0&&restored.transferredAmperes()==0,"GT6 runtime statistics are not persisted");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void bareWireContactUsesRealPreviousTickPower(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);var wire=place(h,pos,false,12);receiver(h,pos.east(),1);
        var cow=EntityType.COW.create(h.getLevel());h.assertTrue(cow!=null,"cow fixture");
        var block=block(false,12);float initial=cow.getHealth();
        block.entityInside(wire.getBlockState(),h.getLevel(),wire.getBlockPos(),cow);
        h.assertTrue(cow.getHealth()==initial,"idle bare wire is harmless");
        h.assertTrue(send(wire,32,1)==1,"real receiver takes packet");wire.serverTick();
        block.entityInside(wire.getBlockState(),h.getLevel(),wire.getBlockPos(),cow);
        long damage=GTVoltageTiers.tierMax(32-wire.lossPerBlock())*4L;
        h.assertTrue(cow.getHealth()==Math.max(0,initial-damage),"bare wire damage derives from actual previous-tick wattage");
        var creative=h.makeMockPlayer();block.entityInside(wire.getBlockState(),h.getLevel(),wire.getBlockPos(),creative);
        h.assertTrue(creative.getHealth()==creative.getMaxHealth(),"creative remains immune");
        var cable=place(h,new BlockPos(1,1,3),true,12);receiver(h,new BlockPos(2,1,3),1);send(cable,32,1);cable.serverTick();
        var safe=EntityType.COW.create(h.getLevel());block(true,12).entityInside(cable.getBlockState(),h.getLevel(),cable.getBlockPos(),safe);
        h.assertTrue(safe.getHealth()==safe.getMaxHealth(),"insulation protects even with real current");h.succeed();
    }
}
