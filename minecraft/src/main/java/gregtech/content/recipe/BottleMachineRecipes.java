package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.fluid.VanillaPotionBottling;
import gregtech.content.item.BottleItem;
import gregtech.content.data.generated.GTBottlesGen;
import gregtech.engine.fluid.PotionBottleHandler;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** GT6 RecipeMapFluidCanner, materialized for both runtime machines and recipe viewers.
 * Every existing bottle consumes/returns its actual source portion and empty container.
 */
public final class BottleMachineRecipes {
    private BottleMachineRecipes() {}
    private static final java.util.List<String> MISSING = new java.util.ArrayList<>();
    public static java.util.List<String> missing() { return java.util.List.copyOf(MISSING); }
    public static int register() {
        MISSING.clear();
        int count=0;
        for (int i=0;i<GTBottlesGen.ENTRIES.length;i+=3){
            String id=GTBottlesGen.ENTRIES[i];
            var item=gregtech.content.food.GTFoodItems.itemOrNull(id);
            if (item==null){MISSING.add(id);continue;}
            var filled=new ItemStack(item);
            var liquid=gregtech.engine.fluid.PotionBottleHandler.contents(filled);
            if (liquid.isEmpty()){MISSING.add(id);continue;}
            var empty=item==Items.HONEY_BOTTLE?new ItemStack(Items.GLASS_BOTTLE):BottleItem.emptyBottle();
            if (MachineRecipeMaps.Canner.register(RecipeBuilder.of(16, 16)
                    .itemInputs(empty)
                    .fluidInputs(liquid)
                    .itemOutputs(filled))!=null)count++;
            if (MachineRecipeMaps.Canner.register(RecipeBuilder.of(16, 16)
                    .itemInputs(filled)
                    .itemOutputs(empty)
                    .fluidOutputs(liquid))!=null)count++;
            var gtEmpty=BottleItem.emptyBottle();
            if (item==Items.HONEY_BOTTLE&&!gtEmpty.is(empty.getItem())
                    &&MachineRecipeMaps.Canner.register(RecipeBuilder.of(16, 16)
                            .itemInputs(gtEmpty)
                            .fluidInputs(liquid)
                            .itemOutputs(filled))!=null)count++;
        }
        count += registerWaterBottle();
        com.mojang.logging.LogUtils.getLogger().info("GT6 bottle filling/draining: {} rows, unresolved {}",count,MISSING);
        return count;
    }

    /** The vanilla water bottle keeps its canner rows after the potion removal. */
    private static int registerWaterBottle() {
        var water = new FluidStack(Fluids.WATER, VanillaPotionBottling.AMOUNT);
        var filled = PotionBottleHandler.filled(water);
        if (filled.isEmpty()) return 0;
        int count = 0;
        var empty = new ItemStack(Items.GLASS_BOTTLE);
        if (MachineRecipeMaps.Canner.register(RecipeBuilder.of(16, 16)
                .itemInputs(empty)
                .fluidInputs(water)
                .itemOutputs(filled)) != null) count++;
        var gtEmpty = BottleItem.emptyBottle();
        if (!gtEmpty.is(empty.getItem())
                && MachineRecipeMaps.Canner.register(RecipeBuilder.of(16, 16)
                        .itemInputs(gtEmpty)
                        .fluidInputs(water)
                        .itemOutputs(filled)) != null) count++;
        var drain = new Recipe(new ItemStack[] {filled}, new ItemStack[] {empty},
                null, null, new FluidStack[] {water}, 16, 16, 0);
        drain.exactItemInputs = true;
        if (MachineRecipeMaps.Canner.addRecipe(drain) != null) count++;
        return count;
    }
}
