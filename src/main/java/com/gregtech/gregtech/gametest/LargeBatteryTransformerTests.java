package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.content.energy.BatteryBoxDefinitions;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.ChemicalBatteryItem;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LargeBatteryTransformerTests {
    private static EnergyNodeBlockEntity place(GameTestHelper h,BlockPos p,String id,Direction face){
        var block=(EnergyNodeBlock)ForgeRegistries.BLOCKS.getValue(GregTech.id(id));
        h.setBlock(p,block.defaultBlockState().setValue(EnergyNodeBlock.FACING,face));
        return (EnergyNodeBlockEntity)h.getBlockEntity(p);
    }
    private static void tick(GameTestHelper h,EnergyNodeBlockEntity b){EnergyNodeBlockEntity.serverTick(h.getLevel(),b.getBlockPos(),b.getBlockState(),b);}
    @GameTest(template="test_empty")
    public static void twentyBoxesUseActualBatterySlotsAndEmptyCapacity(GameTestHelper h){
        var specs=BatteryBoxDefinitions.specifications();
        h.assertTrue(specs.size()==20,"ten grades in each box family");
        for(var spec:specs){
            var b=place(h,new BlockPos(1,1,1),spec.id(),Direction.NORTH);
            h.assertTrue(b.isBatteryBox()&&b.batteryInventory().getSlots()==spec.batterySlots(),spec.id()+" slot definition");
            h.assertTrue(b.getEnergyCapacity(GregTechTags.Energy.EU,null)==0,"empty box must not supply free capacity");
            h.assertTrue(b.batteryEnergy().bufferCapacity()==spec.outputRate()*320*spec.batterySlots(),"original working buffer");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void sixteenthSlotSurvivesSaveAndExactMenuLayout(GameTestHelper h){
        var b=place(h,new BlockPos(1,1,1),"energy_storage_lv",Direction.NORTH);
        var item=(ChemicalBatteryItem)ForgeRegistries.ITEMS.getValue(GregTech.id("battery_lead_acid_lv"));
        var cell=new ItemStack(item);item.setCharge(cell,12345);
        b.batteryInventory().setStackInSlot(15,cell);
        var copy=new EnergyNodeBlockEntity(b.getBlockPos(),b.getBlockState());copy.load(b.saveWithoutMetadata());
        h.assertTrue(copy.batteryInventory().getStackInSlot(0).isEmpty()&&item.stored(copy.batteryInventory().getStackInSlot(15))==12345,"last slot and charge preserved without compaction");
        var player=h.makeMockPlayer();var menu=b.batteryMenu().createMenu(1,player.getInventory(),player);
        h.assertTrue(menu.getType()==GTMenuTypes.forSlotCount(16)&&menu.slots.size()==52,"exact sixteen slot menu plus 36 player slots");
        h.assertTrue(menu.slots.get(0).x==53&&menu.slots.get(0).y==8&&menu.slots.get(15).x==107&&menu.slots.get(15).y==62,"four by four original GUI slot positions");
        h.assertTrue(menu.slots.get(16).y==84 && menu.slots.get(43).y==142,"player inventory and hotbar match original background pixels");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void electricStepDownAndUpConserveActualNetworkEnergy(GameTestHelper h){
        var a=place(h,new BlockPos(1,1,1),"transformer_lv_mv",Direction.NORTH);
        var b=place(h,new BlockPos(1,1,2),"transformer_lv_mv",Direction.SOUTH);
        var c=place(h,new BlockPos(1,1,3),"transformer_lv_mv",Direction.NORTH);
        b.toggleInverted();var eu=GregTechTags.Energy.EU;
        h.assertTrue(a.doEnergyInjection(eu,Direction.WEST,128,1,true)==0&&a.doEnergyInjection(eu,Direction.NORTH,128,1,true)==1,"input only on front");
        tick(h,a);h.assertTrue(a.stored()==0&&b.stored()==128,"one 128 EU packet becomes four 32 EU packets without loss");
        tick(h,b);h.assertTrue(b.stored()==0&&c.stored()==128,"reverse produces one 128 EU packet without multiplication");
        h.assertTrue(c.doEnergyExtraction(eu,Direction.SOUTH,32,4,true)==0,"push-only converter cannot be pulled");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void disconnectedTransformerKeepsEnergyAndRecipesHaveRealIngredients(GameTestHelper h){
        var b=place(h,new BlockPos(1,1,1),"transformer_ulv_lv",Direction.NORTH);var eu=GregTechTags.Energy.EU;
        b.doEnergyInjection(eu,Direction.NORTH,32,1,true);tick(h,b);
        h.assertTrue(b.stored()==32,"EU transformer retains disconnected energy unlike waste-energy RU converter");
        b.toggleInverted();h.assertTrue(b.stored()==0&&b.isEnergyEmittingTo(eu,Direction.NORTH,false)&&!b.isEnergyAcceptingFrom(eu,Direction.NORTH,false),"reversal clears buffer and swaps faces");
        int count=0;
        for(var recipe:h.getLevel().getRecipeManager().getRecipes()){
            if(!recipe.getId().getNamespace().equals("gregtech")||!recipe.getId().getPath().startsWith("energy_nodes/"))continue;
            count++;
            int tier=java.util.List.of("ulv","lv","mv","hv","ev","iv","luv","zpm","uv","xv").indexOf(recipe.getId().getPath().substring(recipe.getId().getPath().lastIndexOf('_')+1));
            for(int i=0;i<recipe.getIngredients().size();i++){
                var ingredient=recipe.getIngredients().get(i);
                if(!ingredient.isEmpty()&&!(tier>=7&&!recipe.getId().getPath().contains("transformer_")&&(i==6||i==8)))h.assertTrue(ingredient.getItems().length>0,"resolved ingredient in "+recipe.getId());
            }
            h.assertTrue(!recipe.getResultItem(h.getLevel().registryAccess()).isEmpty(),"registered recipe output");
        }
        h.assertTrue(count==28,"28 source-backed recipes loaded; high circuits require external providers");
        h.succeed();
    }
}
