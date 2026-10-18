package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.machine.FurnaceFuelValue;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
@net.neoforged.neoforge.gametest.GameTestHolder("gt6ce_survival_feedback")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
import net.minecraftforge.fluids.FluidStack;
@net.minecraftforge.gametest.GameTestHolder("gt6ce_survival_feedback")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class SingleItemSurvivalFeedbackTests {
    private SingleItemSurvivalFeedbackTests() {}
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void originalFuelsAndAllFiveCompoundsReachNativeFurnace(GameTestHelper h) {
        for (String[] fuel : new String[][]{{"dry_grass","100"},{"dry_bark","50"},{"bale_grass_dry","900"},
                {"bale_grass","450"},{"fruit_remains","50"},{"nut_remains","100"}}) {
            var stack = item(fuel[0]); int expected = Integer.parseInt(fuel[1]);
            h.assertTrue(FurnaceFuelValue.getBurnValue(stack) == expected, "GT fuel " + fuel[0]);
//? if >=1.21.1 {
            /*int actual=stack.getBurnTime(RecipeType.SMELTING);
*///?} else {
            int actual=net.minecraftforge.common.ForgeHooks.getBurnTime(stack,RecipeType.SMELTING);
//?}
            h.assertTrue(actual >= expected, "native furnace fuel hook " + fuel[0] + ": " + actual);
        }
        h.assertTrue(FurnaceFuelValue.getBurnValue(new ItemStack(Items.HAY_BLOCK)) == 450, "original hay-bale value");
        var bark = GTItems.getStack(MaterialPrefix.dust, GTMaterialRegistry.get("Bark"), 1);
        h.assertTrue(FurnaceFuelValue.getBurnValue(bark) == 100, "one U bark burns for 100 ticks");
        for (var compound : OriginalSingleItemCatalog.COMPOUNDS) {
            var input = item(compound.item());
            var output = GTItems.getStack(MaterialPrefix.nugget, GTMaterialRegistry.get(compound.material()), 1);
            var row = MachineRecipeMaps.Furnace.findRecipe(List.of(input),List.of(),true,1,1);
            h.assertTrue(row != null && row.mOutputs[0].is(output.getItem()) && row.mOutputs[0].getCount() == 1,
                    "one compound fires to one nugget " + compound.item());
            h.assertTrue(fires(h,input,output), "actual vanilla furnace bridge " + compound.item());
            var data = ItemMaterialRegistry.get(input).orElseThrow();
            h.assertTrue(data.amount() == GTValues.U/9, "source compound material count");
        }
        audit(h);
        h.succeed();
    }
    @GameTest(template="test_empty", timeoutTicks=100)
    public static void animalRemainsMeatAndSixteenPaintCansHaveExecutableSourceRoutes(GameTestHelper h) {
        for (String part : OriginalSingleItemCatalog.ANIMAL_PARTS) {
            var input=item(part);
            h.assertTrue(fires(h,input,item("sticky_goo")), "animal-part native cooking " + part);
            for(var map:List.of(MachineRecipeMaps.Mortar,MachineRecipeMaps.Shredder)) {
                var row=map.findRecipe(List.of(input),List.of(),true,1,1);
                h.assertTrue(row!=null && row.mOutputs[0].is(Items.BONE_MEAL), "bone-meal route " + part);
            }
        }
        for(String remains:OriginalSingleItemCatalog.PLANT_REMAINS) {
            var row=MachineRecipeMaps.Fermenter.findRecipe(List.of(item(remains)),
                    List.of(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,135)),true,1,0);
            h.assertTrue(row!=null && row.mFluidOutputs.length==1 && row.mFluidOutputs[0].getAmount()==135,
                    "source biomass route " + remains);
        }
        var grind=MachineRecipeMaps.Mortar.findRecipe(List.of(item("scrap_meat")),List.of(),true,1,1);
        h.assertTrue(grind!=null && grind.mOutputs[0].getCount()==2, "scrap meat retains 2U");
        h.assertTrue(fires(h,item("raw_meat_bar"),item("cooked_meat_bar")), "meat bar cooking");
        for(var dye:com.gregtech.gregtech.content.tool.PaintingRules.DYES) {
            String color=Arrays.stream(RegisteredFluids.DYE_OREDICTS_POST)
                    .filter(value->value.replace("LightBlue","light_blue").replace("LightGray","light_gray")
                            .toLowerCase(Locale.ROOT).equals(dye.id())).findFirst().orElseThrow();
            var row=MachineRecipeMaps.Canner.findRecipe(List.of(item("empty_spray_can")),
                    List.of(DyeProcessingRecipes.fluid("Dye_Chemical_"+color,2304)),true,1,1);
            h.assertTrue(row!=null && row.mOutputs[0].is(item(dye.fullId()).getItem())
                    && row.mDuration==256 && row.mEUt==16 && row.mFluidInputs[0].getAmount()==2304,
                    "source spray paint route " + dye.id());
        }
        var chum=SingleItemSourceRecipes.INSTANCE.entries().stream().filter(row->row.id().equals("chum/MeatRotten/Oil_Fish"))
                .findFirst().orElseThrow().recipe();
        h.assertTrue(chum.mInputs.length==6 && chum.mOutputs[0].is(item("chum").getItem())
                && chum.mOutputs[0].getCount()==8 && chum.mFluidOutputs[0].getAmount()==1000,
                "original six-ingredient chum recipe");
        h.assertTrue(RecipeInputs.consume(chum, Arrays.asList(chum.mInputs), Arrays.asList(chum.mFluidInputs), 1)!=null,
                "chum recipe can consume its exact ingredients");
        h.succeed();
    }
    private static ItemStack item(String id) {
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id.contains(":")?id:"gt6ce:"+id));
        if(item==Items.AIR)throw new IllegalStateException("Missing survival feedback item "+id);
        return new ItemStack(item);
    }
    private static boolean fires(GameTestHelper h,ItemStack input,ItemStack output) {
//? if >=1.21.1 {
        /*var inventory=new net.minecraft.world.item.crafting.SingleRecipeInput(input);
        return h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream().anyMatch(holder->
                holder.value().matches(inventory,h.getLevel())&&holder.value().getResultItem(h.getLevel().registryAccess()).is(output.getItem()));
*///?} else {
        var inventory=new net.minecraft.world.SimpleContainer(input);
        return h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream().anyMatch(recipe->
                recipe.matches(inventory,h.getLevel())&&recipe.getResultItem(h.getLevel().registryAccess()).is(output.getItem()));
//?}
    }
    /** Batch evidence over every independently registered multi-item, not a claim of complete recipe parity. */
    private static void audit(GameTestHelper h) {
        var inputMaps=new IdentityHashMap<Item,Set<String>>();var outputMaps=new IdentityHashMap<Item,Set<String>>();
        for(var map:RecipeMap.RECIPE_MAP_LIST)for(var row:map.mRecipeList) {
            if(!row.mEnabled||row.mFakeRecipe)continue;
            for(var input:row.mInputs)inputMaps.computeIfAbsent(input.getItem(),ignored->new TreeSet<>()).add(map.mNameInternal);
            for(var output:row.mOutputs)if(!output.isEmpty())outputMaps.computeIfAbsent(output.getItem(),ignored->new TreeSet<>()).add(map.mNameInternal);
        }
        var rows=new ArrayList<Map<String,Object>>();
        var definitions=com.gregtech.gregtech.registry.GTMultiItemsGen.ENTRIES;
        for(int i=0;i+3<definitions.length;i+=4) {
            var stack=item(definitions[i]);
            var row=new LinkedHashMap<String,Object>();row.put("id","gt6ce:"+definitions[i]);row.put("category",definitions[i+2]);
            row.put("machine_inputs",inputMaps.getOrDefault(stack.getItem(),Set.of()));
            row.put("machine_outputs",outputMaps.getOrDefault(stack.getItem(),Set.of()));
            row.put("fuel_ticks",FurnaceFuelValue.originalBurnTime(stack));
            rows.add(row);
        }
        var report=new LinkedHashMap<String,Object>();report.put("scope","all independently registered GTMultiItemsGen identities; static executable machine maps plus explicit fuel only");
        report.put("not_covered",List.of("crafting-grid recipes","loot and harvest acquisition","dynamic recipes","right-click and equipment uses","complete survival playthrough"));
        report.put("items",rows);report.put("single_item_source_rows",SingleItemSourceRecipes.INSTANCE.entries().size());
        report.put("single_item_smelting_rows",SingleItemSourceRecipes.smeltingRows().size());
        try {
            var path=java.nio.file.Path.of("build/survival-feedback-single-item-audit.json");java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report),java.nio.charset.StandardCharsets.UTF_8);
        } catch(java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
}
