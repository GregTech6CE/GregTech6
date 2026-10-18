package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineControl;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.content.energy.BatteryBoxEnergy;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.ChemicalBatteryItem;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BatteryBoxEnergyTests {
    private static ItemStack battery(String name,long charge) {
        var item=(ChemicalBatteryItem)ForgeRegistries.ITEMS.getValue(new ResourceLocation("gregtech","battery_"+name));
        var stack=new ItemStack(item);item.setCharge(stack,charge);return stack;
    }
    private static long charge(ItemStack stack){return ((ChemicalBatteryItem)stack.getItem()).stored(stack);}
    private static EnergyNodeBlockEntity place(GameTestHelper h,BlockPos pos,String id) {
        var block=GTEnergyNodes.all().stream().map(net.minecraftforge.registries.RegistryObject::get)
                .filter(b->b.spec().id().equals(id)).findFirst().orElseThrow();
        h.setBlock(pos,block.defaultBlockState().setValue(EnergyNodeBlock.FACING,Direction.EAST));
        return (EnergyNodeBlockEntity)h.getBlockEntity(pos);
    }
    private static void buffer(BatteryBoxEnergy state,long amount){var tag=new CompoundTag();state.save(tag);tag.putLong("gt.buffer",amount);state.load(tag);}

    @GameTest(template="test_empty")
    public static void originalBandsMoveEnergyBetweenRealItemsAndBuffer(GameTestHelper h) {
        var list=NonNullList.withSize(4,ItemStack.EMPTY);var stack=battery("lithium_cobalt_lv",100000);list.set(3,stack);
        var state=new BatteryBoxEnergy(list,32);
        state.tick(1,h.getLevel(),h.absolutePos(BlockPos.ZERO));
        h.assertTrue(state.buffer()==1024&&charge(stack)==98976,"band 0 requests 40 packets, LV battery caps at 32");
        long before=charge(stack);state.tick(2,null,null);
        h.assertTrue(charge(stack)==before,"no item callbacks between one-second boundaries");
        buffer(state,5120);state.tick(21,null,null);
        h.assertTrue(state.buffer()==5760&&charge(stack)==before-640,"band 1 extracts 20 packets");
        before=charge(stack);buffer(state,30720);state.tick(41,null,null);
        h.assertTrue(state.buffer()==30080&&charge(stack)==before+640,"band 6 injects 20 packets");
        before=charge(stack);buffer(state,35840);state.tick(61,null,null);
        h.assertTrue(state.buffer()==34816&&charge(stack)==before+1024,"band 7 injects up to item packet limit");
        before=charge(stack);buffer(state,16384);state.tick(81,null,null);
        h.assertTrue(state.buffer()==16384&&charge(stack)==before,"middle bands do not exchange energy");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void inputBudgetDirectionsVoltageAndTheoryAreDistinct(GameTestHelper h) {
        var be=place(h,new BlockPos(1,2,1),"battery_box_lv");var state=be.batteryEnergy();
        var raw=ForgeRegistries.ITEMS.getValue(new ResourceLocation("gregtech","lead_acid_cell_filled"));
        h.assertTrue(!be.installBattery(new ItemStack(raw)),"filled cells are not rechargeable battery items");
        var full=battery("lead_acid_lv",64000);be.installBattery(full);state.tick(2,null,null);
        h.assertTrue(be.doEnergyInjection(GregTechTags.Energy.EU,Direction.EAST,32,1,true)==0,"front cannot receive");
        h.assertTrue(be.doEnergyInjection(GregTechTags.Energy.HU,Direction.WEST,32,1,true)==0,"wrong energy rejected");
        h.assertTrue(be.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,Long.MAX_VALUE,false)==3&&state.buffer()==0,"GT6 budget allows last packet; simulation pure even for huge count");
        h.assertTrue(be.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,Long.MAX_VALUE,true)==3&&state.buffer()==96,"full battery still has theoretical charging capability");
        h.assertTrue(be.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true)==0,"same tick budget exhausted");
        state.tick(3,null,null);h.assertTrue(be.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,65,1,false)==1&&state.buffer()==96,"overvoltage simulation consumes packet without mutation");
        be.batteryInventory().setStackInSlot(0,battery("lead_acid_mv",0));state.tick(4,null,null);
        h.assertTrue(be.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true)==0,"wrong voltage battery cannot enable LV input");
        var tool=new ItemStack(GTElectricItems.ELECTRIC_DRILL.get());tool.getOrCreateTag().putLong("gt.charge",2000);
        be.batteryInventory().setStackInSlot(0,tool);state.tick(21,null,null);
        h.assertTrue(state.providers()==0&&state.buffer()==96&&tool.getTag().getLong("gt.charge")==2000,"tool may charge but cannot act as a generator");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void realNetworkOutputHonorsProviderCountModeAndStop(GameTestHelper h) {
        var be=place(h,new BlockPos(1,2,1),"battery_box_lv");var target=place(h,new BlockPos(2,2,1),"energy_storage_lv");
        for(int i=0;i<4;i++)be.batteryInventory().setStackInSlot(i,battery("lead_acid_lv",5000));
        target.installBattery(battery("lead_acid_lv",0));target.batteryEnergy().tick(2,null,null);
        target.batteryInventory().setStackInSlot(1,battery("lead_acid_lv",0));
        target.batteryInventory().setStackInSlot(2,battery("lead_acid_lv",0));
        target.batteryEnergy().tick(2,null,null);
        buffer(be.batteryEnergy(),16384);
        EnergyNodeBlockEntity.serverTick(h.getLevel(),be.getBlockPos(),be.getBlockState(),be);
        h.assertTrue(target.stored()==128,"four batteries emit four 32 EU packets through front");
        var control=MachineControl.find(be,Direction.NORTH);h.assertTrue(control!=null&&control.supportsMode(),"machine control available to relays");
        control.setMode(2);EnergyNodeBlockEntity.serverTick(h.getLevel(),be.getBlockPos(),be.getBlockState(),be);
        h.assertTrue(target.stored()==192,"mode limits output to two amps");
        control.setEnabled(false);EnergyNodeBlockEntity.serverTick(h.getLevel(),be.getBlockPos(),be.getBlockState(),be);
        h.assertTrue(target.stored()==192&&!be.isEnergyEmittingTo(GregTechTags.Energy.EU,Direction.EAST,false),"stop blocks output");
        h.assertTrue(be.isEnergyEmittingTo(GregTechTags.Energy.EU,Direction.EAST,true),"theoretical output unchanged");
        h.assertTrue(be.doEnergyExtraction(GregTechTags.Energy.EU,Direction.EAST,32,1,true)==0,"original box is push driven, not arbitrary pull");
        var copy=new EnergyNodeBlockEntity(be.getBlockPos(),be.getBlockState());copy.load(be.saveWithoutMetadata());
        h.assertTrue(!copy.batteryEnergy().enabled()&&copy.batteryEnergy().mode()==2&&copy.stored()==be.stored(),"mode stop and buffer persist");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void removedAndDroppedBatteriesKeepTheirOwnCharge(GameTestHelper h) {
        var be=place(h,new BlockPos(1,2,1),"battery_box_lv");
        be.batteryInventory().setStackInSlot(3,battery("lead_acid_lv",12345));buffer(be.batteryEnergy(),1234);
        h.assertTrue(be.getEnergyStored(GregTechTags.Energy.EU,null)==12345&&be.getEnergyCapacity(GregTechTags.Energy.EU,null)==64000,"Jade reports item energy and capacity, excluding working buffer");
        var out=be.removeBattery();h.assertTrue(charge(out)==12345&&be.stored()==1234,"removal keeps item charge and does not truncate buffer");
        be.batteryInventory().setStackInSlot(2,out);
        var saved=be.saveWithoutMetadata();var copy=new EnergyNodeBlockEntity(be.getBlockPos(),be.getBlockState());copy.load(saved);
        h.assertTrue(charge(copy.batteryInventory().getStackInSlot(2))==12345&&copy.stored()==1234,"item and working buffer separately saved");
        var pos=be.getBlockPos();h.getLevel().destroyBlock(pos,true);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(0.8));
        long count=drops.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(s->s.is(out.getItem())&&charge(s)==12345).count();
        h.assertTrue(count==1,"actual destruction drops charged battery exactly once");h.succeed();
    }
}
