package gregtech.engine.recipe;

import gregtech.engine.stack.StackCompat;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.List;

/** Pure, bounded planning of fluid fuel charges before the input or any exhaust tank changes. */
public final class FluidFuelBatch {
    public record Plan(FluidStack input,FluidStack output,long energy,int charges) {}
    private FluidFuelBatch() {}
    public record MultiPlan(FluidStack input,List<FluidStack> outputs,long energy,int charges) {}
    public static Plan plan(Recipe recipe,FluidStack input,FluidStack output,int capacity,long needed,int efficiency) {
        var plan=plan(recipe,input,List.of(output),new int[]{capacity},needed,efficiency);
        return plan==null?null:new Plan(plan.input(),plan.outputs().get(0),plan.energy(),plan.charges());
    }
    public static MultiPlan plan(Recipe recipe,FluidStack input,List<FluidStack> outputs,int[] capacities,long needed,int efficiency) {
        if (!recipe.enabled||recipe.fakeRecipe||recipe.energyPerTick>=0||recipe.itemInputs.length!=0||recipe.itemOutputs.length!=0||recipe.fluidInputs.length!=1||recipe.fluidOutputs.length>outputs.size()||capacities.length!=outputs.size()||needed<=0||efficiency<=0)return null;
        long energy=FluidFuelChargeMath.energy(recipe.energyPerTick,recipe.durationTicks,efficiency);
        if (energy<=0)return null;
        int target=(int)Math.min(Integer.MAX_VALUE,needed/energy+(needed%energy==0?0:1));
        for (int i=0;i<recipe.fluidOutputs.length;i++) {
            var exhaust=recipe.fluidOutputs[i];var output=outputs.get(i);
            if (exhaust.isEmpty())continue;
            if (!output.isEmpty()&&!StackCompat.sameFluid(output,exhaust))return null;
            target=Math.min(target,Math.max(0,capacities[i]-output.getAmount())/exhaust.getAmount());
        }
        target=(int)Math.min(target,Long.MAX_VALUE/energy);
        int lo=0,hi=target;
        while (lo<hi){int mid=(int)(lo+((long)hi-lo+1)/2);if (RecipeInputs.consume(recipe,List.of(),List.of(input),mid)!=null)lo=mid;else hi=mid-1;}
        if (lo==0)return null;
        var remaining=RecipeInputs.consume(recipe,List.of(),List.of(input),lo);
        var products=new java.util.ArrayList<FluidStack>();
        for (int i=0;i<outputs.size();i++) {
            var product=outputs.get(i).copy();
            if (i<recipe.fluidOutputs.length&&!recipe.fluidOutputs[i].isEmpty()) {
                var exhaust=recipe.fluidOutputs[i];
                if (product.isEmpty()){product=exhaust.copy();product.setAmount(exhaust.getAmount()*lo);}else product.grow(exhaust.getAmount()*lo);
            }
            products.add(product);
        }
        return new MultiPlan(remaining.fluids().get(0),List.copyOf(products),energy*lo,lo);
    }
}
