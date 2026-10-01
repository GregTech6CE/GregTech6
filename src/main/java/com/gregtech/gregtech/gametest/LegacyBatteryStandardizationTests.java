package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.energy.ChemicalBatteryBlock;
import com.gregtech.gregtech.blockentity.energy.*;
import com.gregtech.gregtech.content.energy.ChemicalBatterySpec;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.ChemicalBatteryItem;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class LegacyBatteryStandardizationTests {
    @GameTest(template="test_empty")
    public static void retainedIdsArePassiveLithiumCobaltWithRealItemEnergy(GameTestHelper h){
        h.assertTrue(GTChemicalBatteries.all().size()==25&&GTChemicalBatteries.legacy().size()==5&&GTChemicalBatteries.allRegistered().size()==30,"canonical catalogue excludes retained duplicate IDs");
        for(var entry:GTChemicalBatteries.legacy()){
            var block=entry.get();var spec=block.spec();var item=(ChemicalBatteryItem)block.asItem();var stack=new ItemStack(item);
            h.assertTrue(spec.chemistry()==ChemicalBatterySpec.Chemistry.LITHIUM_COBALT&&spec.capacity()==spec.voltage()*64000,"prototype capacity identifies lithium cobalt");
            h.assertTrue(GTBlockEntities.CHEMICAL_BATTERY.get().isValid(block.defaultBlockState()),"registered as a real chemical battery");
            h.assertTrue(GTEnergyNodes.all().stream().noneMatch(e->e.getId().equals(entry.getId())),"not simultaneously registered as an energy network node");
            h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,spec.voltage(),1,null,null,true)==1&&item.stored(stack)==spec.voltage(),"retained inventory item accepts rated energy");
            h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,spec.maximumPacket()+1,1,null,null,true)==0,"retained item rejects overvoltage");
            h.assertTrue(stack.is(net.minecraft.tags.ItemTags.create(GregTech.id("rechargeable_batteries/"+new String[]{"ulv","lv","mv","hv","ev"}[spec.tier()]))),"rechargeable tier tag includes retained ID");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void retainedPlacedBatteryDropsItsCharge(GameTestHelper h){
        var block=(ChemicalBatteryBlock)ForgeRegistries.BLOCKS.getValue(GregTech.id("battery_eu_32"));var item=(ChemicalBatteryItem)block.asItem();
        var stack=new ItemStack(item);item.setCharge(stack,98765);var p=new BlockPos(1,2,1);h.setBlock(p,block);
        var pos=h.absolutePos(p);block.setPlacedBy(h.getLevel(),pos,block.defaultBlockState(),null,stack);
        var be=(ChemicalBatteryBlockEntity)h.getBlockEntity(p);
        h.assertTrue(be.stored()==98765&&!((Object)be instanceof com.gregtech.gregtech.api.energy.IEnergyBlock),"placed retained battery is passive and charged");
        h.getLevel().destroyBlock(pos,true);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(.8));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(item)&&item.stored(e.getItem())==98765).count()==1,"exactly one real charged drop");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void oldEnergyNodeNbtConvertsToChemicalBatteryWithoutChargeLoss(GameTestHelper h){
        var p=new BlockPos(1,2,1);var pos=h.absolutePos(p);var block=ForgeRegistries.BLOCKS.getValue(GregTech.id("battery_eu_128"));h.setBlock(p,block);
        var state=block.defaultBlockState();var saved=new CompoundTag();saved.putString("id","gregtech:energy_node");saved.putInt("x",pos.getX());saved.putInt("y",pos.getY());saved.putInt("z",pos.getZ());saved.putLong("gt.buffer",123456);
        var loaded=BlockEntity.loadStatic(pos,state,saved);
        h.assertTrue(loaded instanceof EnergyNodeBlockEntity&&loaded.getType().isValid(state),"old block entity ID remains loadable for retained block");
        h.getLevel().setBlockEntity(loaded);loaded.onLoad();
        var replacement=h.getLevel().getBlockEntity(pos);
        h.assertTrue(replacement instanceof ChemicalBatteryBlockEntity&&((ChemicalBatteryBlockEntity)replacement).stored()==123456,"chunk load replaces old energy node and preserves its EU buffer as charge");
        h.assertTrue(replacement.saveWithFullMetadata().getString("id").equals("gregtech:chemical_battery"),"subsequent save uses canonical chemical block entity type");
        loaded.onLoad();h.assertTrue(h.getLevel().getBlockEntity(pos)==replacement,"removed old tile cannot repeat migration");h.succeed();
    }
}
