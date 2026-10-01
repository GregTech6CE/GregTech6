package com.gregtech.gregtech.content.transport.fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import com.gregtech.gregtech.api.fluid.FluidHazards;
/** Pending Neo Fermenter recipe/selector-tag(0) binding, explicitly unbound until that module is ported. */
public final class FermentationAccess {
    public record Recipe(FluidStack input,FluidStack output,long power,long duration) {}
    @FunctionalInterface public interface Provider {
        /** Match enabled/nonfake Fermenter entries with exactly one selector-tag(0) input and one fluid input. */
        Recipe findForSelectorTagZero(FluidStack input);
    }
    private static volatile Provider provider;
    private FermentationAccess() {}
    public static void bind(Provider value){provider=java.util.Objects.requireNonNull(value);}
    public static boolean isBound(){return provider!=null;}
    /** Call after the real platform recipe maps and selector items have initialized. */
    public static void bindOriginalRecipeMaps(){
        bind(fluid->{
            var selector=com.gregtech.gregtech.registry.GTTechnological.selectorTag(0);
            if(selector==null)return null;
            var item=new net.minecraft.world.item.ItemStack(selector);
            for(var recipe:com.gregtech.gregtech.data.MachineRecipeMaps.Fermenter.mRecipeList){
                if(!recipe.mEnabled||recipe.mFakeRecipe)continue;
                if(recipe.mInputs.length!=1||!com.gregtech.gregtech.api.recipe.RecipeInputs.matches(recipe.mInputs[0],item))continue;
                if(recipe.mFluidInputs.length!=1||recipe.mFluidOutputs.length==0)continue;
                var input=recipe.mFluidInputs[0];var output=recipe.mFluidOutputs[0];
                if(input==null||output==null||input.getFluid()!=fluid.getFluid())continue;
                if(FluidHazards.isGas(input.getFluid())||FluidHazards.isGas(output.getFluid()))continue;
                return new Recipe(input,output,recipe.mEUt,recipe.mDuration);
            }
            return null;
        });
    }
    public static Recipe find(FluidStack input){
        if(input.isEmpty()||provider==null)return null;
        var recipe=provider.findForSelectorTagZero(input);
        if(recipe==null||recipe.input()==null||recipe.output()==null||recipe.input().isEmpty()||recipe.output().isEmpty()
                ||recipe.input().getFluid()!=input.getFluid()||FluidHazards.isGas(recipe.input().getFluid())||FluidHazards.isGas(recipe.output().getFluid()))return null;
        return recipe;
    }
}
