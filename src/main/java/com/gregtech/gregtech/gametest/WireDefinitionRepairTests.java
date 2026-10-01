package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.misc.ChargingCraftingTableBlockEntity;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Set;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class WireDefinitionRepairTests {
    private static ElectricWireBlock wire(String name,int size,boolean insulated) {
        return GTWires.all().stream().map(net.minecraftforge.registries.RegistryObject::get).filter(b->b.spec().id().equals(name)&&b.spec().size()==size&&b.spec().insulated()==insulated).findFirst().orElseThrow();
    }
    @GameTest(template="test_empty")
    public static void actualRegistryAndCollisionBoundsUseOriginalSpecs(GameTestHelper h) {
        h.assertTrue(GTWires.allWires().size()==480&&GTWires.allCables().size()==140,"30 bare families, 28 insulated families and original size sets");
        int[] diameters={2,3,4,6,7,7,8,8,9,10,11,12,13,14,15,16};
        for(var entry:GTWires.all()) {
            var b=entry.get();var s=b.spec();
            double width=s.insulated()?switch(s.size()){case 1->4;case 2->6;case 4->8;case 8->12;case 12->16;default->throw new AssertionError("Invalid cable size");}:diameters[s.size()-1];
            var bounds=b.getCollisionShape(b.defaultBlockState(),h.getLevel(),h.absolutePos(BlockPos.ZERO),CollisionContext.empty()).bounds();
            h.assertTrue(bounds.getXsize()==width/16&&bounds.getYsize()==width/16&&bounds.getZsize()==width/16,"actual collision bounds match original diameter: "+entry.getId());
            h.assertTrue(s.contactDamage()==(!s.insulated()&&!Set.of("graphene","superconductor").contains(s.id())),"contact flags are independent of insulation");
            h.assertTrue(b.getExplosionResistance()==2.0F&&b.defaultBlockState().getDestroySpeed(h.getLevel(),h.absolutePos(BlockPos.ZERO))==1.0F,"GT6 hardness and blast resistance");
        }
        h.assertTrue(wire("copper",1,false).spec().amperage()==1&&wire("copper",1,false).spec().lossPerBlock()==2&&wire("copper",1,true).spec().lossPerBlock()==1,"copper 1 A, bare loss 2 and cable loss 1");
        h.assertTrue(wire("gold",4,true).spec().amperage()==12&&wire("gold",4,true).spec().lossPerBlock()==1,"gold current is not confused with cable loss");
        h.assertTrue(wire("tungsten",1,false).spec().amperage()==8&&wire("tungsten",1,true).spec().lossPerBlock()==2,"tungsten parameters retain their distinct positions");
        h.assertTrue(wire("superconductor",1,false).spec().voltage()==8589934592L&&wire("superconductor",1,false).spec().lossPerBlock()==1,"GT6 V[15] superconductor still has one unit loss");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void nonHazardousOriginalWiresRemainHarmlessWithRealCurrent(GameTestHelper h) {
        for(int i=0;i<2;i++) {
            String name=i==0?"graphene":"superconductor";var b=wire(name,1,false);var pos=new BlockPos(1,1,1+i*2);
            h.setBlock(pos,b.defaultBlockState().setValue(ElectricWireBlock.WEST,true).setValue(ElectricWireBlock.EAST,true));
            h.setBlock(pos.east(),GTMiscBlocks.CHARGING_CRAFTING_TABLE.get());
            var table=(ChargingCraftingTableBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos.east()));table.items().setStackInSlot(16,new ItemStack(GTElectricItems.BATTERY_LV.get()));
            var be=(ElectricWireBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
            h.assertTrue(be.doEnergyInjection(GregTechTags.Energy.EU,Direction.WEST,32,1,true)==1,"real current through "+name);be.serverTick();
            h.assertTrue(be.lastWattage()==32-b.spec().lossPerBlock(),"nonzero carried power");
            var cow=EntityType.COW.create(h.getLevel());b.entityInside(be.getBlockState(),h.getLevel(),be.getBlockPos(),cow);
            h.assertTrue(cow.getHealth()==cow.getMaxHealth(),"GT6 non-contact-damage material stays harmless: "+name);
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void recipesNeverRequireNonexistentInsulatedVariants(GameTestHelper h) {
        for(String material:new String[]{"graphene","superconductor"})for(int size:new int[]{1,2,4,8,12,16})
            h.assertTrue(!ForgeRegistries.ITEMS.containsKey(GregTech.id("cable_%02d_%s".formatted(size,material))),"no invented insulated "+material);
        for(var entry:GTWires.allWires())h.assertTrue(!ForgeRegistries.ITEMS.containsKey(GregTech.id("cable_16_"+entry.get().spec().id())),"GT6 has no 16x insulated cable");
        var recipe=h.getLevel().getRecipeManager().byKey(GregTech.id("hand_components/usb4_cable")).orElseThrow();
        var graphene=wire("graphene",1,false).asItem();
        h.assertTrue(recipe.getIngredients().get(4).test(new ItemStack(graphene))&&recipe.getIngredients().get(7).test(new ItemStack(graphene)),"USB4 C slots use MT.DATA.CABLES_01[6], which is bare graphene in GT6");
        for(var recipeEntry:h.getLevel().getRecipeManager().getRecipes())if(recipeEntry.getId().getNamespace().equals("gregtech")&&recipeEntry.getId().getPath().startsWith("wire_working/"))for(var ingredient:recipeEntry.getIngredients())
            h.assertTrue(ingredient==net.minecraft.world.item.crafting.Ingredient.EMPTY||ingredient.getItems().length>0,"loaded recipe has resolvable ingredients: "+recipeEntry.getId());
        h.succeed();
    }
}
