package gregtech.gametest;

import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.tool.GTToolType;
import gregtech.item.GTToolItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.ArrayList;
import java.util.List;

/** Actual recipe-manager coverage of GT6's early furnace and ceramic preparation chain. */
//? if >=1.21.1 {
/*@net.neoforged.neoforge.gametest.GameTestHolder("gt6ce_early_survival")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gt6ce_early_survival")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class EarlyCeramicRecipeTests {
    private EarlyCeramicRecipeTests() {}

    @GameTest(template="test_empty",timeoutTicks=100)
    public static void furnaceAcceptsOriginalFirestartersAndMaterialTool(GameTestHelper h) {
        var recipe=crafting(h,"minecraft:furnace");
        var igniters=new ArrayList<ItemStack>();
        for(String id:List.of("fire_starter","fire_starter_2","match","match_box_full","lighter_full"))
            igniters.add(item(id));
        igniters.add(new ItemStack(Items.FLINT_AND_STEEL));
        igniters.add(new ItemStack(Items.FIRE_CHARGE));
        igniters.add(tool(GTToolType.FLINT_AND_TINDER,"Flint"));
        for(var igniter:igniters) {
            var inputs=empty();
            for(int slot=0;slot<9;slot++)inputs.set(slot,slot==4?igniter:new ItemStack(Items.COBBLESTONE));
            var grid=TransportMaterialPortTests.grid(inputs);
            h.assertTrue(recipe.getIngredients().get(4).test(igniter),"furnace ingredient accepts "+igniter);
            h.assertTrue(recipe.matches(grid,h.getLevel()),"original furnace grid accepts "+igniter);
            h.assertTrue(recipe.assemble(grid,h.getLevel().registryAccess()).is(Items.FURNACE),"native furnace result");
        }
        h.assertTrue(!recipe.getIngredients().get(4).test(item("lighter_empty")),"empty lighter is not an original firestarter");
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=100)
    public static void sourceClayFormsFireIntoVisibleCeramicPartsAndReclaimBeforeFiring(GameTestHelper h) {
        String[][] forms={
                {"fluid_tools/form_clay_funnel","clay_funnel","CRC","kC "},
                {"survival_repairs/clay_basin","clay_basin","CkC","CRC"," C "},
                {"survival_repairs/clay_faucet","clay_faucet","C C","kCR"},
                {"survival_repairs/clay_crossing","clay_crossing","kCR","CCC"," C "},
                {"ceramic_utilities/form_clay_crossing_flipped","clay_crossing"," C ","CCC","kCR"}};
        for(var form:forms) {
            var inputs=empty();
            for(int row=2;row<form.length;row++)for(int x=0;x<form[row].length();x++) {
                char key=form[row].charAt(x);
                ItemStack value=switch(key) {
                    case 'C' -> new ItemStack(Items.CLAY_BALL);
                    case 'k' -> tool(GTToolType.KNIFE,"Flint");
                    case 'R' -> tool(GTToolType.ROLLING_PIN,"Wood");
                    default -> ItemStack.EMPTY;
                };
                inputs.set(x+(row-2)*3,value);
            }
            var recipe=crafting(h,form[0]);
            var grid=TransportMaterialPortTests.grid(inputs);
            h.assertTrue(recipe.matches(grid,h.getLevel()),"source clay grid "+form[0]);
            h.assertTrue(recipe.assemble(grid,h.getLevel().registryAccess()).is(item(form[1]).getItem()),"source raw output "+form[1]);
        }
        String[][] fired={
                {"clay_funnel","fluid_funnel_ceramic","3"},
                {"clay_tap","tap_ceramic","3"},
                {"clay_basin","mold_basin_ceramic","5"},
                {"clay_faucet","crucible_faucet_ceramic","3"},
                {"clay_crossing","crucible_crossing_ceramic","5"},
                {"clay_juicer","juicer","4"},
                {"clay_bowl","mixing_bowl","5"}};
        for(var pair:fired) {
            var input=item(pair[0]);var output=item(pair[1]);
            h.assertTrue(fires(h,input,output),"actual vanilla firing "+pair[0]+" -> "+pair[1]);
            var oven=gregtech.data.MachineRecipeMaps.Furnace.findRecipe(List.of(input),List.of(),true,1,1);
            h.assertTrue(oven!=null && oven.mOutputs.length==1 && oven.mOutputs[0].is(output.getItem()),"actual GT oven firing "+pair[0]);
            String reclaim=pair[0].equals("clay_funnel")||pair[0].equals("clay_tap")
                    ?"fluid_tools/reclaim_"+pair[0]:"ceramic_utilities/reclaim_"+pair[0];
            var grid=empty();grid.set(0,input);
            var recipe=crafting(h,reclaim);var inventory=TransportMaterialPortTests.grid(grid);
            h.assertTrue(recipe.matches(inventory,h.getLevel()),"raw clay reclaim input "+pair[0]);
            var result=recipe.assemble(inventory,h.getLevel().registryAccess());
            h.assertTrue(result.is(Items.CLAY_BALL)&&result.getCount()==Integer.parseInt(pair[2]),"exact source clay count "+pair[0]);
        }
        h.succeed();
    }

    private static ItemStack tool(GTToolType type,String material) {
        return GTToolItem.create(type,GTMaterialRegistry.get(material),GTMaterialRegistry.get("Wood"));
    }
    private static ItemStack item(String path) {
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",path));
        if(item==Items.AIR)throw new IllegalStateException("Missing early survival item "+path);
        return new ItemStack(item);
    }
    private static ArrayList<ItemStack> empty() {
        return new ArrayList<>(java.util.Collections.nCopies(9,ItemStack.EMPTY));
    }
    @SuppressWarnings("unchecked")
//? if >=1.21.1 {
    /*private static Recipe<net.minecraft.world.item.crafting.CraftingInput> crafting(GameTestHelper h,String path) {
        return (Recipe<net.minecraft.world.item.crafting.CraftingInput>)h.getLevel().getRecipeManager()
                .byKey(ResourceLocation.parse(path.contains(":")?path:"gt6ce:"+path)).orElseThrow().value();
*///?} else {
    private static Recipe<net.minecraft.world.inventory.CraftingContainer> crafting(GameTestHelper h,String path) {
        return (Recipe<net.minecraft.world.inventory.CraftingContainer>)h.getLevel().getRecipeManager()
                .byKey(ResourceLocation.parse(path.contains(":")?path:"gt6ce:"+path)).orElseThrow();
//?}
    }
    private static boolean fires(GameTestHelper h,ItemStack raw,ItemStack output) {
//? if >=1.21.1 {
        /*var input=new net.minecraft.world.item.crafting.SingleRecipeInput(raw);
        return h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream().anyMatch(holder->
                holder.value().matches(input,h.getLevel())&&holder.value().getResultItem(h.getLevel().registryAccess()).is(output.getItem()));
*///?} else {
        var input=new net.minecraft.world.SimpleContainer(raw);
        return h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream().anyMatch(recipe->
                recipe.matches(input,h.getLevel())&&recipe.getResultItem(h.getLevel().registryAccess()).is(output.getItem()));
//?}
    }
}
