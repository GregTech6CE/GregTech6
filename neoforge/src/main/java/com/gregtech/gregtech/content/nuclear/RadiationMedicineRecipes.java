package com.gregtech.gregtech.content.nuclear;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

/** MultiItemFood 31001 and Loader_Recipes_Extruder food-grade wax pill routes. */
public final class RadiationMedicineRecipes {
    private RadiationMedicineRecipes() {}
    public static void register() {
        for(String gas:new String[]{"Helium","Neon","Argon"}) {
            var fluid=GTFluids.still(gas);
            if(fluid==null)throw new IllegalStateException("Missing Geiger counter gas: "+gas);
            MachineRecipeMaps.Canner.addRecipe(new Recipe(new ItemStack[]{item("geiger_counter_empty")},
                    new ItemStack[]{item("geiger_counter")},null,null,
                    new net.neoforged.neoforge.fluids.FluidStack[]{new net.neoforged.neoforge.fluids.FluidStack(fluid.get(),1000)},null,64,16,0));
        }
        var empty = item("empty_wax_pill");
        add(MachineRecipeMaps.Boxinator, new ItemStack[]{GTItems.getStack(MaterialPrefix.dust,
                GTMaterialRegistry.get("Iodine"), 1), empty}, item("radaway"), 16);
        for (String wax : new String[]{"WaxPlant", "WaxParaffin", "WaxBee"}) {
            for (var prefix : new MaterialPrefix[]{MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny}) {
                int amount = prefix == MaterialPrefix.dust ? 1 : prefix == MaterialPrefix.dustSmall ? 4 : 9;
                var input = GTItems.getStack(prefix, GTMaterialRegistry.get(wax), amount);
                if (input.isEmpty()) continue;
                for (String shape : new String[]{"extruder_shape_bottle", "low_heat_extruder_shape_bottle"}) {
                    var mold = GTTechnological.get(shape);
                    if (mold != null) add(MachineRecipeMaps.Extruder, new ItemStack[]{input, new ItemStack(mold)}, empty, 64);
                }
            }
        }
    }
    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", id)));
    }
    private static void add(RecipeMap map, ItemStack[] inputs, ItemStack output, int ticks) {
        if (output.isEmpty() || java.util.Arrays.stream(inputs).anyMatch(ItemStack::isEmpty))
            throw new IllegalStateException("Missing radiation medicine ingredient: " + java.util.Arrays.toString(inputs) + " -> " + output);
        // Food processing owns the same Radaway row. Retain one identical executable recipe.
        for(var existing:map.mRecipeList){
            if(!existing.mEnabled||existing.mFakeRecipe||existing.mDuration!=ticks||existing.mEUt!=16||existing.mInputs.length!=inputs.length||existing.mOutputs.length!=1||existing.mFluidInputs.length!=0||existing.mFluidOutputs.length!=0)continue;
            if(existing.mOutputs[0].getCount()!=output.getCount()||!ItemStack.isSameItemSameComponents(existing.mOutputs[0],output))continue;
            boolean identical=true;for(int i=0;i<inputs.length;i++)if(existing.mInputs[i].getCount()!=inputs[i].getCount()||!ItemStack.isSameItemSameComponents(existing.mInputs[i],inputs[i])){identical=false;break;}
            if(identical)return;
        }
        if(map.addRecipe(new Recipe(inputs, new ItemStack[]{output}, null, null, null, null, ticks, 16, 0))==null)
            throw new IllegalStateException("Rejected radiation medicine recipe: " + java.util.Arrays.toString(inputs));
    }
}
