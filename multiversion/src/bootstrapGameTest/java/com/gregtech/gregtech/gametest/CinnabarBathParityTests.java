package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeDocuments;
import com.gregtech.gregtech.content.recipe.ReviewedCinnabarFacts;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.recipe.MachineDatapackRecipe;
import com.gregtech.gregtech.recipe.MachineRecipeOverlay;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_ore_source_audit")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
import net.minecraftforge.fluids.FluidStack;
@net.minecraftforge.gametest.GameTestHolder("gregtech_ore_source_audit")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
/** Native map + parsed datapack overlay contracts. Does not claim disk/event reload coverage. */
public final class CinnabarBathParityTests {
    private record Control(RecipeMap map,ItemStack input,FluidStack fluid,Recipe expected) {}
    private static ItemStack input(String form,int n) { return OreSourceDifferentialTests.s(form,"Cinnabar",n); }
    private static FluidStack fluid(String name,int n) {
        var stack=GTFluids.stack(name,n);
        if(stack==null||stack.isEmpty())throw new AssertionError("Required fluid "+name);
        return stack;
    }
    private static Recipe lookup(RecipeMap map,ItemStack input,FluidStack fluid) {
        return map.findRecipe(List.of(input),List.of(fluid),false,map.mInputItemsCount,map.mOutputItemsCount);
    }
    private static void absent(GameTestHelper h) {
        h.assertTrue(ReviewedCinnabarFacts.supports(GTMaterialRegistry.get("Cinnabar")),"bounded source fixture, not an external-profile test");
        h.assertTrue(lookup(MachineRecipeMaps.Bath,input("crushed",1),fluid("Water",1000))==null,
                "Cinnabar has no automatic water-only Bath shortcut");
    }
    private static List<Control> controls(GameTestHelper h) {
        var result=new ArrayList<Control>();
        String[] in={"blockDust","crushed","crushedTiny"};int[] counts={1,1,9},mb={25,13,13};
        for(int i=0;i<3;i++) {
            var item=input(in[i],counts[i]);var liquid=fluid("Cryotheum",mb[i]);
            OreSourceDifferentialTests.row(h,"original explicit Cinnabar Cryotheum",MachineRecipeMaps.Bath,item,
                    new FluidStack[]{liquid},new ItemStack[]{input(i==0?"gemExquisite":"gemFlawless",1)},
                    new long[]{10000},new FluidStack[0],256,0);
            result.add(new Control(MachineRecipeMaps.Bath,item,liquid,lookup(MachineRecipeMaps.Bath,item,liquid)));
        }
        for(String water:List.of("Water","MnWtr","DistW","SpDew")) {
            var item=input("crushed",1);var liquid=fluid(water,900);
            Recipe row=lookup(MachineRecipeMaps.Sluice,item,liquid);
            h.assertTrue(row!=null&&row.mOutputs.length==9&&row.mDuration==144&&row.mEUt==16,
                    "four native water Sluice routes retain nine slots and work");
            h.assertTrue(row.getOutputChance(0)==10000,"Sluice main output certain");
            for(int j=1;j<9;j++)h.assertTrue(row.getOutputChance(j)==300,"eight Sluice probabilistic outputs retained");
            result.add(new Control(MachineRecipeMaps.Sluice,item,liquid,row));
        }
        return List.copyOf(result);
    }
    private static void retained(GameTestHelper h,List<Control> controls) {
        for(var c:controls) h.assertTrue(lookup(c.map,c.input,c.fluid)==c.expected,
                "independent source row identity retained during Bath override/restoration");
    }
    @GameTest(template="test_empty",batch="ore_overlay_lifecycle",timeoutTicks=200)
    public static void cinnabarWaterShortcutAbsentWhileSourceBathAndSluiceRemain(GameTestHelper h) {
        absent(h);controls(h);h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_overlay_lifecycle",timeoutTicks=200)
    public static void explicitCinnabarWaterOverrideWinsAndRemovalRestoresAbsence(GameTestHelper h) {
        absent(h);var preserved=controls(h);
        var server=h.getLevel().getServer();
        try {
            var markers=overrideManager(server);
            for(int round=0;round<2;round++) {
                MachineRecipeOverlay.apply(markers);
                var row=lookup(MachineRecipeMaps.Bath,input("crushed",1),fluid("Water",1000));
                h.assertTrue(row!=null&&row.mOutputs.length==1&&row.mOutputs[0].is(Items.NETHER_STAR)
                        &&row.mOutputs[0].getCount()==1&&row.mDuration==37&&row.mEUt==16,
                        "public explicit datapack Bath wins despite automatic source suppression");
                h.assertTrue(lookup(MachineRecipeMaps.Bath,input("crushed",1),fluid("Water",1000))==row,
                        "warm lookup selects explicit override");
                retained(h,preserved);
                MachineRecipeOverlay.apply(server.getRecipeManager());absent(h);retained(h,preserved);
            }
        } finally { MachineRecipeOverlay.apply(server.getRecipeManager()); }
        absent(h);retained(h,preserved);h.succeed();
    }
    private static net.minecraft.world.item.crafting.RecipeManager overrideManager(net.minecraft.server.MinecraftServer server) {
        String item=BuiltInRegistries.ITEM.getKey(input("crushed",1).getItem()).toString();
        String body="{\"map\":\""+MachineRecipeMaps.Bath.mNameInternal+"\",\"item_inputs\":[{\"item\":\""+item
                +"\"}],\"fluid_inputs\":[{\"fluid\":\"minecraft:water\",\"amount\":1000}],"
                +"\"item_outputs\":[{\"item\":\"minecraft:nether_star\"}],\"eu_per_tick\":16,\"duration\":37}";
        var addition=MachineRecipeDocuments.addition(body);
        var rid=ResourceLocation.fromNamespaceAndPath("gregtech_ore_source_audit","cinnabar_water_override");
//? if >=1.21.1 {
/*        var manager=new net.minecraft.world.item.crafting.RecipeManager(server.registryAccess());
        var rows=new ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(server.getRecipeManager().getRecipes());
        rows.add(new net.minecraft.world.item.crafting.RecipeHolder<>(rid,MachineDatapackRecipe.addition(addition)));
*///?} else {
        var manager=new net.minecraft.world.item.crafting.RecipeManager();
        var rows=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(server.getRecipeManager().getRecipes());
        rows.add(MachineDatapackRecipe.addition(rid,addition));
//?}
        manager.replaceRecipes(rows);return manager;
    }
}
