package gregtech.gametest;

import gregtech.GregTech;
import gregtech.block.energy.ChemicalBatteryBlock;
import gregtech.blockentity.energy.*;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.data.GregTechTags;
import gregtech.item.ChemicalBatteryItem;
import gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class ChemicalBatteryIdentityTests {
    @GameTest(template="test_empty")
    public static void canonicalBatteriesHaveRealItemEnergy(GameTestHelper h){
        h.assertTrue(GTChemicalBatteries.all().size()==25&&GTChemicalBatteries.allRegistered().size()==37,"25 chemical batteries and 12 canonical energy crystals");
        for(var entry:GTChemicalBatteries.all()){
            var block=entry.get();var spec=block.spec();var item=(ChemicalBatteryItem)block.asItem();var stack=new ItemStack(item);
            h.assertTrue(ForgeRegistries.BLOCKS.getKey(block).equals(GregTech.id(spec.id())),"block uses its chemistry and tier identity");
            h.assertTrue(GTBlockEntities.CHEMICAL_BATTERY.get().isValid(block.defaultBlockState()),"registered as a real chemical battery");
            h.assertTrue(GTEnergyNodes.all().stream().noneMatch(e->e.getId().equals(entry.getId())),"not simultaneously registered as an energy network node");
            h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,spec.voltage(),1,null,null,true)==1&&item.stored(stack)==spec.voltage(),"inventory item accepts rated energy");
            h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU,stack,spec.maximumPacket()+1,1,null,null,true)==0,"item rejects overvoltage");
            h.assertTrue(stack.is(net.minecraft.tags.ItemTags.create(GregTech.id("rechargeable_batteries/"+new String[]{"ulv","lv","mv","hv","ev"}[spec.tier()]))),"rechargeable tier tag includes canonical ID");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void placedBatteryDropsItsCharge(GameTestHelper h){
        var block=(ChemicalBatteryBlock)ForgeRegistries.BLOCKS.getValue(GregTech.id("battery_lithium_cobalt_lv"));var item=(ChemicalBatteryItem)block.asItem();
        var stack=new ItemStack(item);item.setCharge(stack,98765);var p=new BlockPos(1,2,1);h.setBlock(p,block);
        var pos=h.absolutePos(p);block.setPlacedBy(h.getLevel(),pos,block.defaultBlockState(),null,stack);
        var be=(ChemicalBatteryBlockEntity)h.getBlockEntity(p);
        h.assertTrue(be.stored()==98765&&!((Object)be instanceof gregtech.internal.energy.IEnergyBlock),"placed battery is passive and charged");
        h.getLevel().destroyBlock(pos,true);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(.8));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(item)&&item.stored(e.getItem())==98765).count()==1,"exactly one real charged drop");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void canonicalBatteryNbtPreservesCharge(GameTestHelper h){
        var p=new BlockPos(1,2,1);var pos=h.absolutePos(p);var block=ForgeRegistries.BLOCKS.getValue(GregTech.id("battery_lithium_cobalt_mv"));h.setBlock(p,block);
        var state=block.defaultBlockState();var saved=new CompoundTag();saved.putString("id","gt6ce:chemical_battery");saved.putInt("x",pos.getX());saved.putInt("y",pos.getY());saved.putInt("z",pos.getZ());saved.putLong(ChemicalBatteryItem.CHARGE,123456);
        var loaded=BlockEntity.loadStatic(pos,state,saved);
        h.assertTrue(loaded instanceof ChemicalBatteryBlockEntity&&loaded.getType().isValid(state),"canonical battery block entity loads");
        h.getLevel().setBlockEntity(loaded);loaded.onLoad();
        h.assertTrue(((ChemicalBatteryBlockEntity)loaded).stored()==123456,"canonical load preserves charge");
        h.assertTrue(loaded.saveWithFullMetadata().getString("id").equals("gt6ce:chemical_battery"),"save keeps canonical chemical block entity type");h.succeed();
    }
}
