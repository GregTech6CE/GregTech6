package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class EnergySurvivalGateTests {
    private static ItemStack item(String id){return new ItemStack(ForgeRegistries.ITEMS.getValue(GregTech.id(id)));}
    private static ItemStack panel(PanelCover p){return new ItemStack(GTTechnological.get(p.id));}
    @GameTest(template="test_empty")
    public static void transformerMaterialsAcceptOriginalFamiliesWithoutMixingForms(GameTestHelper h){
        var recipe=h.getLevel().getRecipeManager().byKey(GregTech.id("energy_nodes/transformer_ulv_lv")).orElseThrow();
        var in=recipe.getIngredients();
        h.assertTrue(in.get(1).test(item("plate_double_iron"))&&in.get(1).test(item("plate_double_steel"))&&in.get(1).test(item("plate_double_wroughtiron")),"iron family includes steel and wrought iron double plates");
        h.assertTrue(!in.get(1).test(item("plate_iron"))&&!in.get(1).test(item("plate_double_copper")),"single plates and unrelated metals are not substitutes");
        h.assertTrue(in.get(0).test(item("wire_01_copper"))&&in.get(0).test(item("wire_01_annealed_copper"))&&in.get(3).test(item("wire_04_annealed_copper")),"ANY.Cu applies to both conductor counts");
        h.assertTrue(!in.get(0).test(item("wire_04_copper"))&&!in.get(0).test(item("cable_01_copper")),"wire count and insulation cannot be interchanged");
        var high=h.getLevel().getRecipeManager().byKey(GregTech.id("energy_nodes/transformer_hv_ev")).orElseThrow();
        h.assertTrue(high.getIngredients().get(0).test(item("wire_01_annealed_copper"))&&!high.getIngredients().get(0).test(item("wire_01_copper")),"higher transformer requires specifically annealed copper");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void mvBatteryBoxAcceptsBothOriginalCopperTypesInExactSizes(GameTestHelper h){
        for(boolean large:new boolean[]{false,true}){
            var recipe=h.getLevel().getRecipeManager().byKey(GregTech.id("energy_nodes/"+(large?"energy_storage_mv":"battery_box_mv"))).orElseThrow();
            String size=large?"04":"01";var in=recipe.getIngredients();
            h.assertTrue(in.get(0).test(item("wire_"+size+"_annealed_copper"))&&in.get(1).test(item("cable_"+size+"_annealed_copper")),"ANY copper wire and insulated cable");
            h.assertTrue(!in.get(1).test(item("wire_"+size+"_copper")),"insulated cable slot rejects bare wire");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void cachedBatteryAutomationHonorsShutterControllerAndRemoval(GameTestHelper h){
        var p=new BlockPos(2,2,2);h.setBlock(p,ForgeRegistries.BLOCKS.getValue(GregTech.id("energy_storage_lv")));
        var b=(EnergyNodeBlockEntity)h.getBlockEntity(p);
        var optional=b.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH);var north=optional.orElseThrow(AssertionError::new);
        var south=b.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.SOUTH).orElseThrow(AssertionError::new);
        var battery=item("battery_lead_acid_lv");
        h.assertTrue(b.attachCover(Direction.NORTH,panel(PanelCover.SHUTTER)),"shutter attaches");
        h.assertTrue(north.insertItem(15,battery,true).isEmpty()&&b.batteryInventory().getStackInSlot(15).isEmpty(),"normal shutter starts open; simulation is pure");
        b.panels().configure(Direction.NORTH,false,false);
        h.assertTrue(!north.insertItem(15,battery,true).isEmpty()&&!north.insertItem(15,battery,false).isEmpty()&&!north.isItemValid(15,battery),"inverted shutter blocks cached capability in simulate and execute modes");
        h.assertTrue(south.insertItem(15,battery,false).isEmpty(),"uncovered side still inserts into shared final slot");
        h.assertTrue(north.extractItem(15,1,true).isEmpty()&&north.extractItem(15,1,false).isEmpty()&&!b.batteryInventory().getStackInSlot(15).isEmpty(),"closed face cannot extract or mutate inventory");
        var saved=b.saveWithoutMetadata();b.load(saved);
        h.assertTrue(north.extractItem(15,1,false).isEmpty(),"saved inverted shutter still blocks cached handler");
        b.panels().configure(Direction.NORTH,false,false);
        h.assertTrue(b.attachCover(Direction.WEST,panel(PanelCover.CONTROLLER)),"redstone controller attaches");b.panels().beforeTick();
        h.assertTrue(north.extractItem(15,1,false).isEmpty(),"normal shutter closes while controller stops covers");
        h.setBlock(p.west(),Blocks.REDSTONE_BLOCK);b.panels().beforeTick();
        h.assertTrue(!north.extractItem(15,1,true).isEmpty()&&!b.batteryInventory().getStackInSlot(15).isEmpty(),"redstone opens shutter without simulation consuming battery");
        b.panels().configure(Direction.NORTH,false,false);h.assertTrue(north.extractItem(15,1,false).isEmpty(),"inversion reverses powered state");
        b.removeCover(Direction.NORTH);h.assertTrue(!north.extractItem(15,1,false).isEmpty()&&b.batteryInventory().getStackInSlot(15).isEmpty(),"removing cover immediately opens the same cached handler");
        b.invalidateCaps();h.assertTrue(!optional.isPresent(),"sided capability invalidated with block entity");h.succeed();
    }
}
