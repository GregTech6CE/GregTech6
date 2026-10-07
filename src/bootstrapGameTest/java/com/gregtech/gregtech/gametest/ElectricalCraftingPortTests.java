package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.content.recipe.ElectricalCraftingRules;
import com.gregtech.gregtech.data.MachineRecipeIngredients;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.gametest.*;
import java.util.*;

/** External circuits are test-only tags; none of these vanilla items is a production circuit. */
@GameTestHolder("gregtech_electrical") @PrefixGameTestTemplate(false)
public final class ElectricalCraftingPortTests {
    private static final List<String> TIERS=List.of("ulv","lv","mv","hv","ev","iv","luv","zpm","uv","xv");
    private static final List<String> METALS=List.of("lead","tin","copper","gold","aluminium","platinum","graphene","graphene","graphene","graphene");
    private static final List<String> CASINGS=List.of("tinalloy","steelgalvanized","aluminium","stainlesssteel","chromium","titanium","iridium","osmiumelemental","trinitanium","trinaquadalloy");
    private static ItemStack item(String path) {
        var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",path)));
        if(stack.isEmpty())throw new IllegalStateException("Missing electrical fixture item "+path);return stack;
    }
    private static ItemStack circuit(int tier) {
        if(tier>=7)return new ItemStack(tier==7?Items.DIAMOND:tier==8?Items.EMERALD:Items.NETHER_STAR);
        return item("circuit_"+List.of("basic","basic","good","advanced","elite","master","ultimate").get(tier));
    }
    private static boolean accepts(int required,ItemStack stack) {
        return stack.is(TagKey.create(Registries.ITEM,ResourceLocation.parse(ElectricalCraftingRules.circuitTag(required))));
    }
    private static CraftingRecipe recipe(GameTestHelper h,String path) {
        return (CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech","energy_nodes/"+path)).orElseThrow();
    }
    private static net.minecraft.world.inventory.CraftingContainer grid(List<ItemStack> stacks) {
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
            public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        for(int i=0;i<stacks.size();i++)grid.setItem(i,stacks.get(i));return grid;
    }
    private static List<ItemStack> inputs(int tier,boolean large,ItemStack circuit) {
        String size=large?"04":"01",metal=METALS.get(tier);
        var wire=item("wire_"+size+"_"+metal);var cable=item((tier>=6?"wire_":"cable_")+size+"_"+metal);
        var middle=item(large?"transformer_"+TIERS.get(tier)+"_"+TIERS.get(tier+1):"casing_machine_"+CASINGS.get(tier));
        return new ArrayList<>(List.of(wire.copy(),cable.copy(),wire.copy(),wire.copy(),cable.copy(),wire.copy(),circuit.copy(),middle,circuit.copy()));
    }
    @GameTest(template="test_empty",timeoutTicks=50)
    public static void cumulativeCircuitTagsKeepExactSourceGrades(GameTestHelper h) {
        for(int required=0;required<10;required++) {
            var resolved=MachineRecipeIngredients.resolve("circuit:"+required,GTMaterialRegistry.get("Iron"),1);
            h.assertTrue(resolved.equals(Map.of("tag","gregtech:circuits_tier_"+required+"_plus")),"no T0 shift or high-tier clamp");
            for(int supplied=1;supplied<10;supplied++)h.assertTrue(accepts(required,circuit(supplied))==(supplied>=required),"one-way original circuit inheritance "+supplied+" -> "+required);
            h.assertTrue(accepts(required,new ItemStack(Items.AMETHYST_SHARD))==(required<=6),"existing optional Forge ultimate provider enters the same chain");
            h.assertTrue(!accepts(required,new ItemStack(Items.IRON_INGOT)),"untyped vanilla item refused");
        }
        for(int required=7;required<=9;required++)h.assertTrue(!accepts(required,item("crystal_circuit_diamond")),"crystal circuit has no original high-tier registration");
        for(int invalid:new int[]{-1,10}){
            boolean rejected=false;try{MachineRecipeIngredients.resolve("circuit:"+invalid,GTMaterialRegistry.get("Iron"),1);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"invalid source grade is not silently clamped");
        }h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=50)
    public static void batteryBoxesCraftWithNativeAndExternalDependencies(GameTestHelper h) {
        int count=0;
        for(int tier=0;tier<10;tier++)for(boolean large:new boolean[]{false,true}){
            if(large&&tier==9)continue;
            var id=(large?"energy_storage_":"battery_box_")+TIERS.get(tier);var row=recipe(h,id);
            var inputs=inputs(tier,large,circuit(tier));var grid=grid(inputs);
            h.assertTrue(row.matches(grid,h.getLevel()),"original materials craft "+id);
            var result=row.assemble(grid,h.getLevel().registryAccess());
            h.assertTrue(result.getCount()==1&&result.is(item(id).getItem()),"one actual box output "+id);
            if(tier>=7){
                h.assertTrue(!row.matches(grid(inputs(tier,large,circuit(6))),h.getLevel()),"T6 cannot replace T"+tier);
                h.assertTrue(row.matches(grid(inputs(tier,large,circuit(9))),h.getLevel()),"T9 substitutes lower grades");
                inputs.set(1,item("cable_"+(large?"04":"01")+"_platinum"));
                h.assertTrue(!row.matches(grid(inputs),h.getLevel()),"high-tier graphene is not an arbitrary insulated cable");
            }
            if(tier==2){inputs=inputs(tier,large,circuit(tier));String size=large?"04":"01";
                for(int i:new int[]{0,2,3,5})inputs.set(i,item("wire_"+size+"_annealed_copper"));
                for(int i:new int[]{1,4})inputs.set(i,item("cable_"+size+"_annealed_copper"));
                h.assertTrue(row.matches(grid(inputs),h.getLevel()),"original ANY.Cu accepts annealed copper");
            }count++;
        }
        h.assertTrue(count==19,"ten ordinary plus nine source-resolvable large recipes");
        for(int tier=0;tier<9;tier++)recipe(h,"transformer_"+TIERS.get(tier)+"_"+TIERS.get(tier+1));
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("gregtech:energy_nodes/energy_storage_xv")).isEmpty(),"source-missing transformer 10049 has no guessed recipe");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=50)
    public static void sourceWireArraysAndCraftedHighBoxesUseRegisteredForms(GameTestHelper h) {
        for(int tier=0;tier<16;tier++){
            String metal=tier<6?METALS.get(tier):tier<=10?"graphene":"superconductor";
            for(boolean cable:new boolean[]{false,true}){
                String kind=cable?"cabletier:":"wiretier:";
                var resolved=MachineRecipeIngredients.resolve(kind+tier,GTMaterialRegistry.get("Iron"),1);
                String expected=tier==2?"gregtech:"+(cable?"cable_01":"wire_04")+"/any_copper"
                    :"gregtech:"+(cable&&tier<6?"cable_":"wire_")+(cable?"01":"04")+"_"+metal;
                h.assertTrue(resolved.equals(Map.of(tier==2?"tag":"item",expected)),"source wire array and exact form "+kind+tier);
                if(tier!=2)h.assertTrue(!new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(expected))).isEmpty(),"actual registered source form");
            }
        }
        for(int tier=7;tier<=9;tier++)for(boolean large:new boolean[]{false,true}){
            if(large&&tier==9)continue;
            String id=(large?"energy_storage_":"battery_box_")+TIERS.get(tier);
            var row=recipe(h,id);var output=row.assemble(grid(inputs(tier,large,circuit(tier))),h.getLevel().registryAccess());
            var block=((BlockItem)output.getItem()).getBlock();var pos=new BlockPos(2,1,2);h.setBlock(pos,block);
            var node=(EnergyNodeBlockEntity)h.getBlockEntity(pos);
            h.assertTrue(node.isBatteryBox()&&node.batteryInventory().getSlots()==(large?16:4),"crafted high box has original slots");
            h.assertTrue(node.spec().inputRate()==(8L<<(2*tier))&&node.spec().outputRate()==(8L<<(2*tier)),"original high EU packet size");
            h.assertTrue(node.getEnergyCapacity(GregTechTags.Energy.EU,null)==0,"empty box has no free capacity");
        }h.succeed();
    }
}
