package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeDocuments;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.recipe.MachineDatapackRecipe;
import com.gregtech.gregtech.recipe.MachineRecipeOverlay;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Tests existing APIs only. No proposed owner ledger, no material mutation, no injected source rows.
 * Run with the existing addonSample=true fixture. Marker managers model supported parsed datapacks;
 * this does not claim disk reload/event delivery coverage (covered by MachineRecipeOverlayTests).
 */
@GameTestHolder("gregtech_ore_source_audit")
@PrefixGameTestTemplate(false)
public final class OreOverlayLifecycleControls {
    @GameTest(template="test_empty", batch="ore_overlay_lifecycle", timeoutTicks=200)
    public static void warmedSifterAndStartupAddonRowsSurviveRepeatedOverlay(GameTestHelper h) {
        ItemStack input = purified();
        Recipe original = lookup(MachineRecipeMaps.Sifting, input);
        h.assertTrue(original != null, "registered Cinnabar sifter exists");
        var sample = RecipeMap.RECIPE_MAPS.get("addon_sample.recipe.press");
        h.assertTrue(sample != null && sample.hasBaseline(), "supported onRecipesReady sample baseline exists");
        Recipe startup = sampleStartup(h,sample);
        for (int round=0;round<2;round++) {
            Recipe warmed=lookup(MachineRecipeMaps.Sifting,input);
            h.assertTrue(lookup(MachineRecipeMaps.Sifting,input)==warmed,"warm lookup selects current source row");
            MachineRecipeOverlay.apply(h.getLevel().getRecipeManager());
            Recipe current = lookup(MachineRecipeMaps.Sifting,input);
            sameOutputs(h,original,current);
            h.assertTrue(lookup(sample,new ItemStack(Items.IRON_INGOT))==startup,
                    "supported startup addon row identity preserved, not re-created or claimed");
            sampleDatapack(h,sample);
        }
        h.succeed();
    }

    @GameTest(template="test_empty", batch="ore_overlay_lifecycle", timeoutTicks=200)
    public static void sifterDatapackReplacementWinsWarmLookupAndRemovalRestoresSource(GameTestHelper h) {
        var server=h.getLevel().getServer();
        ItemStack input=purified();
        Recipe original=lookup(MachineRecipeMaps.Sifting,input);
        h.assertTrue(original!=null,"actual Cinnabar source row exists");
        var sample=RecipeMap.RECIPE_MAPS.get("addon_sample.recipe.press");
        h.assertTrue(sample!=null,"actual startup addon fixture present");
        Recipe startup=sampleStartup(h,sample);
        try {
            var markers=replacementManager(server,input);
            for(int round=0;round<2;round++) {
                lookup(MachineRecipeMaps.Sifting,input); // warm cache before every change
                MachineRecipeOverlay.apply(markers);
                Recipe replacement=lookup(MachineRecipeMaps.Sifting,input);
                h.assertTrue(replacement!=null&&replacement!=original&&replacement.mOutputs.length==1
                        &&replacement.mOutputs[0].is(Items.NETHER_STAR)&&replacement.mOutputs[0].getCount()==1,
                        "datapack removal/addition wins; old warm source row cannot leak");
                h.assertTrue(lookup(MachineRecipeMaps.Sifting,input)==replacement,"warm replacement selected");
                h.assertTrue(lookup(sample,new ItemStack(Items.IRON_INGOT))==startup,
                        "unrelated startup addon preserved under supported datapack override");
                sampleDatapack(h,sample);
            }
        } finally { MachineRecipeOverlay.apply(server.getRecipeManager()); }
        sameOutputs(h,original,lookup(MachineRecipeMaps.Sifting,input));
        h.succeed();
    }

    @GameTest(template="test_empty", batch="ore_overlay_lifecycle", timeoutTicks=200)
    public static void exactUnlinkInvalidatesWarmLookupWithoutRemovingIndependentOrDynamicRows(GameTestHelper h) {
        var map=new RecipeMap(new ArrayList<>(),"gt.recipe.test.ore_refresh_cache","Ore refresh cache","test",
                1,1,1,0,0,0,1,false,false,false,false);
        try {
            Recipe independent=map.addRecipe1(true,16,16,new ItemStack(Items.STICK),new ItemStack(Items.DIAMOND));
            Recipe old=map.addRecipe1(true,16,16,new ItemStack(Items.APPLE),new ItemStack(Items.COAL));
            Recipe dynamic=map.addRecipe1(true,16,16,new ItemStack(Items.PAPER),new ItemStack(Items.EMERALD));
            h.assertTrue(independent!=null&&old!=null&&dynamic!=null,"isolated lifecycle fixtures accepted");
            map.unlink(dynamic);
            map.addDynamicRecipes((items,fluids)->items.stream().anyMatch(s->s.is(Items.PAPER))?dynamic:null);
            h.assertTrue(lookup(map,new ItemStack(Items.APPLE))==old,"old cache warmed");
            h.assertTrue(map.unlink(old),"exact old instance unlinked");
            Recipe fresh=map.addRecipe1(true,16,16,new ItemStack(Items.APPLE),new ItemStack(Items.GOLD_NUGGET));
            h.assertTrue(fresh!=null&&lookup(map,new ItemStack(Items.APPLE))==fresh,"same-size mutation invalidates cache");
            h.assertTrue(!map.unlink(old),"stale identity cannot unlink replacement");
            Recipe rejected=map.addRecipe1(true,16,16,new ItemStack(Items.STICK),new ItemStack(Items.COAL));
            h.assertTrue(rejected==null,"collision does not grant a new ownership handle");
            h.assertTrue(lookup(map,new ItemStack(Items.STICK))==independent,"independent row identity preserved");
            h.assertTrue(lookup(map,new ItemStack(Items.PAPER))==dynamic,"dynamic provider preserved");
            map.captureBaseline(java.util.Set.of(fresh));
            map.restoreBaseline();
            h.assertTrue(lookup(map,new ItemStack(Items.APPLE))==null,"excluded identity not resurrected by baseline");
            h.assertTrue(lookup(map,new ItemStack(Items.STICK))==independent
                    &&lookup(map,new ItemStack(Items.PAPER))==dynamic,"baseline leaves independent and provider intact");
        } finally { RecipeMap.unregister(map); }
        h.succeed();
    }
    private static Recipe sampleStartup(GameTestHelper h,RecipeMap map) {
        // Concrete onRecipesReady sentinel, NOT every live addon row: press_iron.json is recreated.
        Recipe row=lookup(map,new ItemStack(Items.IRON_INGOT));
        h.assertTrue(row!=null&&row.mInputs.length==1&&row.mInputs[0].getCount()==1
                &&row.mOutputs.length==1&&row.mOutputs[0].is(Items.IRON_NUGGET)&&row.mOutputs[0].getCount()==1
                &&row.mChances.length==1&&row.mChances[0]==5000&&row.mDuration==20&&row.mEUt==16,
                "exact onRecipesReady iron-ingot chance sentinel present");
        return row;
    }
    private static void sampleDatapack(GameTestHelper h,RecipeMap map) {
        Recipe row=lookup(map,new ItemStack(Items.IRON_BLOCK));
        h.assertTrue(row!=null&&row.mOutputs.length==1&&row.mOutputs[0].is(Items.GOLD_NUGGET)
                &&row.mOutputs[0].getCount()==1&&row.mDuration==20&&row.mEUt==32,
                "sample press_iron datapack semantics preserved; no identity guarantee for rebuilt marker rows");
    }
    private static ItemStack purified() {
        return GTItems.getStack(MaterialPrefix.crushedPurified,GTMaterialRegistry.get("Cinnabar"),1);
    }
    private static Recipe lookup(RecipeMap map,ItemStack input) {
        return map.findRecipe(List.of(input),List.of(),false,1,12);
    }
    private static void sameOutputs(GameTestHelper h,Recipe expected,Recipe actual) {
        h.assertTrue(actual!=null&&actual.mOutputs.length==expected.mOutputs.length,"source output slots restored");
        for(int i=0;i<expected.mOutputs.length;i++) h.assertTrue(actual.mOutputs[i].is(expected.mOutputs[i].getItem())
                &&actual.mOutputs[i].getCount()==expected.mOutputs[i].getCount(),"source output identity/count restored at "+i);
        h.assertTrue(java.util.Arrays.equals(actual.mChances,expected.mChances)
                &&actual.mDuration==expected.mDuration&&actual.mEUt==expected.mEUt,"source chances/work restored");
    }
    private static net.minecraft.world.item.crafting.RecipeManager replacementManager(
            net.minecraft.server.MinecraftServer server,ItemStack input) {
        String id=BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        String map=MachineRecipeMaps.Sifting.mNameInternal;
        var removal=MachineRecipeDocuments.removal("{\"map\":\""+map+"\",\"item_inputs\":[{\"item\":\""+id+"\",\"count\":1}]}");
        var addition=MachineRecipeDocuments.addition("{\"map\":\""+map+"\",\"item_inputs\":[{\"item\":\""+id
                +"\"}],\"item_outputs\":[{\"item\":\"minecraft:nether_star\"}],\"eu_per_tick\":16,\"duration\":16}");
        var rid=ResourceLocation.fromNamespaceAndPath("gregtech_ore_source_audit","sifter_override");
//? if >=1.21.1 {
/*        var manager=new net.minecraft.world.item.crafting.RecipeManager(server.registryAccess());
        var rows=new ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(server.getRecipeManager().getRecipes());
        rows.add(new net.minecraft.world.item.crafting.RecipeHolder<>(rid.withSuffix("_remove"),MachineDatapackRecipe.removal(removal)));
        rows.add(new net.minecraft.world.item.crafting.RecipeHolder<>(rid.withSuffix("_add"),MachineDatapackRecipe.addition(addition)));
*///?} else {
        var manager=new net.minecraft.world.item.crafting.RecipeManager();
        var rows=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(server.getRecipeManager().getRecipes());
        rows.add(MachineDatapackRecipe.removal(rid.withSuffix("_remove"),removal));
        rows.add(MachineDatapackRecipe.addition(rid.withSuffix("_add"),addition));
//?}
        manager.replaceRecipes(rows);return manager;
    }
}
