package com.gregtech.gregtech.integration.client;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** EMI-only types stay in this lazily used helper so the JEI-only fixture can load. */
final class MaterialDataEmiBrowserSmoke {
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Material data EMI: "+message);}
    static boolean identities(ItemStack first,ItemStack reordered,ItemStack changed,ItemStack tierChanged,ItemStack blank,ItemStack named){
        var stack=EmiStack.of(first);
        return stack.isEqual(EmiStack.of(reordered))&&stack.isEqual(EmiStack.of(named))
                &&!stack.isEqual(EmiStack.of(changed))&&!stack.isEqual(EmiStack.of(tierChanged))&&!stack.isEqual(EmiStack.of(blank));
    }
    static long scanUses(ItemStack stack){return EmiApi.getRecipeManager().getRecipesByInput(EmiStack.of(stack)).stream().filter(r->r.getCategory().getId().getPath().equals(MachineRecipeMaps.ScannerMolecular.mNameInternal)).count();}
    private static EmiRecipe row(RecipeMap map,Recipe row){
        var id=ResourceLocation.fromNamespaceAndPath("gregtech","/machine/"+map.mNameInternal+"/"+new ArrayList<>(map.mRecipeList).indexOf(row));
        var found=EmiApi.getRecipeManager().getRecipe(id);require(found!=null,"native row "+id);return found;
    }
    static boolean fileFocus(Recipe print,ItemStack usb,ItemStack blank,ItemStack other,ItemStack named){
        var id=row(MachineRecipeMaps.Printer,print).getId();var manager=EmiApi.getRecipeManager();
        return manager.getRecipesByInput(EmiStack.of(usb)).stream().anyMatch(r->r.getId().equals(id))
                &&manager.getRecipesByInput(EmiStack.of(blank)).stream().noneMatch(r->r.getId().equals(id))
                &&manager.getRecipesByInput(EmiStack.of(other)).stream().noneMatch(r->r.getId().equals(id))
                &&manager.getRecipesByInput(EmiStack.of(named)).stream().anyMatch(r->r.getId().equals(id));
    }
    static void show(RecipeMap map,Recipe recipe,ItemStack pipe){
        if(!pipe.isEmpty())EmiApi.displayUses(EmiStack.of(pipe));
        EmiApi.displayRecipe(row(map,recipe));
    }
    static int[] slots(RecipeMap map,Recipe recipe,net.minecraft.client.gui.screens.Screen screen) throws ReflectiveOperationException {
        int slots=0,fluids=0,medium=0;
        var field=screen.getClass().getDeclaredField("currentPage");field.setAccessible(true);
        for(var group:(List<?>)field.get(screen)){
            if(group.getClass().getField("recipe").get(group)!=row(map,recipe))continue;
            for(var widget:(List<?>)group.getClass().getField("widgets").get(group))if(widget instanceof dev.emi.emi.api.widget.SlotWidget slot){
                var bounds=slot.getBounds();require(!slot.getTooltip(bounds.x()+bounds.width()/2,bounds.y()+bounds.height()/2).isEmpty(),"actual slot tooltip");slots++;
                for(var ingredient:slot.getStack().getEmiStacks()){
                    if(ingredient.getKey() instanceof net.minecraft.world.level.material.Fluid){require(ingredient.getClass().getSimpleName().equals("FluidDisplayEmiStack"),"fluid item retains native fluid key");fluids++;}
                    if(GTMaterialDataRecipes.scannedMaterial(ingredient.getItemStack())!=null)medium++;
                }
            }
        }
        return new int[]{slots,fluids,medium};
    }
}
