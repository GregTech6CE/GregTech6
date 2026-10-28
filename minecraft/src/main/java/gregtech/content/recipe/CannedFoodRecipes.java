/* Derived from GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package gregtech.content.recipe;

import gregtech.content.data.*;
import gregtech.content.registry.*;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Explicit MultiItemCans and Loader_Recipes_Food:40-51 rows; no external ingredient guesses. */
public final class CannedFoodRecipes {
    private CannedFoodRecipes() {}
    private static ItemStack can(String id, int count) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",id)), count); }
    public static int register() {
        int registered=0;
        for(var entry:RegisteredFluids.all().entrySet()) {
            if(!entry.getValue().hasFlag(RegisteredFluids.FluidFlags.AIR)) continue;
            String field=entry.getKey(); var input=GTFluids.stack(field,16000); if(input==null || input.isEmpty()) continue;
            String id=field.equals("Air_End") ? "canned_space_air" : field.equals("Air_Nether") ? "canned_hot_air" : "canned_air";
            if(MachineRecipeMaps.Canner.addRecipe1(false,16,64,can("empty_food_can",1),input,FluidStack.EMPTY,can(id,1))!=null) registered++;
        }
        for(String[] row:new String[][]{{"Air","canned_air"},{"Air_Nether","canned_hot_air"},{"Air_End","canned_space_air"}}) {
            var output=GTFluids.stack(row[0],16000);
            if(output!=null&&!output.isEmpty()&&MachineRecipeMaps.Canner.addRecipe1(false,16,16,can(row[1],1),FluidStack.EMPTY,output,can("empty_food_can",1))!=null) registered++;
        }
        var plate=GTItems.getStack(PrefixRegistry.byName("plateCurved"),GTMaterialRegistry.get("TinAlloy"),1);
        if(!plate.isEmpty()&&MachineRecipeMaps.RollBender.addRecipe1(false,16,64,plate,can("empty_food_can",1))!=null) registered++;
        registered+=pack(new ItemStack(Items.ROTTEN_FLESH),"small_food_can_rotten","Canned Meat");
        registered+=pack(new ItemStack(Items.SPIDER_EYE),"tiny_food_can_rotten","Canned Meat");
        String[][] families={{"FishCooked","tiny_food_can_fish","Canned Fish"},{"MeatCooked","tiny_food_can_meat","Canned Meat"},{"Tofu","tiny_food_can_vegetables","Canned Tofu"},{"SoylentGreen","tiny_food_can_vegetables","Canned Emerald Green"}};
        String[] prefixes={"dustTiny","dustSmall","dust","nugget","chunkGt","billet","ingot"}; int[] counts={9,4,1,9,4,2,1};
        for(var family:families) for(int i=0;i<prefixes.length;i++) {
            var input=GTItems.getStack(PrefixRegistry.byName(prefixes[i]),GTMaterialRegistry.get(family[0]),counts[i]);
            if(!input.isEmpty()) registered+=pack(input,family[1],family[2]);
        }
        // Loader_Recipes_Food's meat listeners use food value to select the can size.
        String[] sizes = {"tiny", "small", "tall", "wide", "large"};
        for (var cut : gregtech.content.food.MeatCutCatalog.CUTS) {
            if (cut.cannedName().isEmpty()) continue;
            for (String id : new String[] {cut.raw(), cut.cooked()}) {
                var food = gregtech.content.food.GTFoodItems.entryFor(id);
                int size = Math.max(0, food.food().level() / 2 - 1);
                if (size >= sizes.length) throw new IllegalStateException("Unsupported meat can size: " + id);
                registered += pack(can(id, 1), sizes[size] + "_food_can_meat", cut.cannedName());
            }
        }
        return registered;
    }
    private static int pack(ItemStack input,String id,String name) {
//? if >=1.21.1 {
        /*var output=can(id,1); output.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal(name));
*///?} else {
        var output=can(id,1); output.setHoverName(Component.literal(name));
//?}
        return MachineRecipeMaps.Canner.addRecipe2(false,16,16,input,can("empty_food_can",1),output)!=null?1:0;
    }
}
