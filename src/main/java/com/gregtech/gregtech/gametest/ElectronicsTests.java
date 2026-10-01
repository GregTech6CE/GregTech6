package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.content.recipe.ElectronicsRecipes;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.data.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class ElectronicsTests {
    @GameTest(template="test_blueprint_empty")
    public static void componentRecipesLoadAndCraft(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        var recipes=manager.getRecipes().stream().filter(r->r.getId().getNamespace().equals("gregtech")&&r.getId().getPath().startsWith("components/")).toList();
        h.assertTrue(recipes.size()==84,"84 component crafting recipes actually loaded; found "+recipes.size());
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        for(var untyped:recipes) {
            var recipe=(net.minecraft.world.item.crafting.ShapedRecipe)untyped;
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
            for(int slot=0;slot<9;slot++) {
                var ingredient=recipe.getIngredients().get(slot);
                if(ingredient.isEmpty())continue;
                h.assertTrue(ingredient.getItems().length>0,"resolved ingredient: "+recipe.getId());
                var stack=ingredient.getItems()[0].copy();
                if(stack.getItem() instanceof com.gregtech.gregtech.item.GTToolItem) {
                    var type=com.gregtech.gregtech.api.tool.GTToolHelper.getType(stack);
                    stack=com.gregtech.gregtech.item.GTToolItem.create(type,com.gregtech.gregtech.content.material.Materials.Steel,
                            com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
                }
                grid.setItem(slot,stack);
            }
            h.assertTrue(recipe.matches(grid,h.getLevel()),"original pattern crafts: "+recipe.getId());
            h.assertTrue(!recipe.assemble(grid,h.getLevel().registryAccess()).isEmpty(),"nonempty component result");
            var remainders=recipe.getRemainingItems(grid);
            for(int slot=0;slot<9;slot++) if(grid.getItem(slot).getItem() instanceof com.gregtech.gregtech.item.GTToolItem) {
                var tool=grid.getItem(slot);
                h.assertTrue(remainders.get(slot).getDamageValue()==com.gregtech.gregtech.api.tool.GTToolHelper.getType(tool).damagePerCraft(),"each tool receives its own wear");
                h.assertTrue(tool.getDamageValue()==0,"craft preview does not damage input");
                tool.setDamageValue(tool.getMaxDamage());
                h.assertTrue(!recipe.matches(grid,h.getLevel()),"broken tool rejected");
                tool.setDamageValue(0);
            }
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void electronicsInputsExecuteAndSolderLimitsTier(GameTestHelper h) {
        h.assertTrue(ElectronicsRecipes.recipes().size()==173,"173 source-backed electronics recipes");
        for(var recipe:ElectronicsRecipes.recipes()) {
            var items=Arrays.stream(recipe.mInputs).map(ItemStack::copy).toList();
            var fluids=Arrays.stream(recipe.mFluidInputs).map(net.minecraftforge.fluids.FluidStack::copy).toList();
            var consumed=RecipeInputs.consume(recipe,items,fluids,1);
            h.assertTrue(consumed!=null,"registered recipe inputs are executable");
            h.assertTrue(consumed.fluids().stream().allMatch(net.minecraftforge.fluids.FluidStack::isEmpty),"fluid quantities consumed exactly");
            for(int i=0;i<items.size();i++) {
                h.assertTrue(items.get(i).getCount()==recipe.mInputs[i].getCount(),"simulation leaves input untouched");
                h.assertTrue(recipe.isCatalystInput(i)?consumed.items().get(i).getCount()==items.get(i).getCount():consumed.items().get(i).isEmpty(),"only catalysts survive");
            }
        }
        int[] expected={3,4,5};String[] solders={"Lead","Tin","SolderingAlloy"};
        for(int i=0;i<3;i++) {
            var fluid=GTFluids.still("GenMolten_"+solders[i]).get();
            var recipe=MachineRecipeMaps.Bath.mRecipeList.stream().filter(r->r.mInputs.length==1&&r.mInputs[0].is(GTTechnological.get("circuit_board_ultimate"))
                    &&r.mFluidInputs.length==1&&r.mFluidInputs[0].getFluid()==fluid).findFirst().orElseThrow();
            h.assertTrue(recipe.mOutputs[0].is(GTTechnological.get("circuit_"+ElectronicsRecipes.TIERS[expected[i]])),"solder limits final circuit tier");
            h.assertTrue(recipe.mEUt==0&&recipe.mDuration==64&&recipe.mFluidInputs[0].getAmount()==72,"original bath costs");
        }
        h.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void lensesAreReservedAndParallelSafe(GameTestHelper h) {
        var recipe=ElectronicsRecipes.recipes().stream().filter(r->r.mInputs.length==2&&r.isCatalystInput(1)).findFirst().orElseThrow();
        var foil=recipe.mInputs[0].copy();foil.setCount(12);var lens=recipe.mInputs[1].copy();
        var remainder=RecipeInputs.consume(recipe,List.of(foil,lens),List.of(),3);
        h.assertTrue(remainder!=null&&remainder.items().get(0).isEmpty()&&remainder.items().get(1).getCount()==1,"one lens supports three operations");
        h.assertTrue(RecipeInputs.consume(recipe,List.of(foil),List.of(),1)==null,"lens required");
        var shared=new Recipe(new ItemStack[]{lens.copy(),lens.copy()},new ItemStack[]{foil.copy()},null,null,null,null,1,1,0).withCatalystInputs(0);
        h.assertTrue(RecipeInputs.consume(shared,List.of(lens),List.of(),1)==null,"same lens cannot be retained and consumed simultaneously");
        var two=lens.copy();two.setCount(2);
        h.assertTrue(RecipeInputs.consume(shared,List.of(two),List.of(),1).items().get(0).getCount()==1,"reserve one and consume one");
        h.succeed();
    }
}
