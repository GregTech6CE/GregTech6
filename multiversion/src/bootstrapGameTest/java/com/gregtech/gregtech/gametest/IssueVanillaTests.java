package com.gregtech.gregtech.gametest;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.GameType;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.item.GTToolItem;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
import java.util.*;

@GameTestHolder("gregtech_issues")
@PrefixGameTestTemplate(false)
public final class IssueVanillaTests {
    @GameTest(template="test_empty",timeoutTicks=30)
    public static void loadedVanillaOverrides(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        for(var row:com.gregtech.gregtech.content.recipe.VanillaCraftingReplacements.rows()) {
//? if >=1.21.1 {
            /*var found=manager.byKey(ResourceLocation.parse(row.id()));
*///?} else {
            var found=manager.byKey(new ResourceLocation(row.id()));
//?}
            h.assertTrue(found.isPresent(),"loaded original override "+row.id());
//? if >=1.21.1 {
            /*var recipe=found.get().value();
*///?} else {
            var recipe=found.get();
//?}
            h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).getCount()==row.count(),"original output count "+row.id());
            if(row.pattern()!=null)h.assertTrue(recipe instanceof com.gregtech.gregtech.recipe.ToolShapedRecipe,"GT tool-aware replacement "+row.id());
        }h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=30)
    public static void furnaceRequiresIgniterAndSawReturnsWornTool(GameTestHelper h) {
//? if >=1.21.1 {
        /*var furnace=(net.minecraft.world.item.crafting.CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("minecraft:furnace")).orElseThrow().value();
        var grid=new ArrayList<ItemStack>();for(int i=0;i<9;i++)grid.add(i==4?ItemStack.EMPTY:new ItemStack(Blocks.COBBLESTONE));
        var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid);
*///?} else {
        var furnace=(net.minecraft.world.item.crafting.CraftingRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation("minecraft:furnace")).orElseThrow();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}};
        var input=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);for(int i=0;i<9;i++)if(i!=4)input.setItem(i,new ItemStack(Blocks.COBBLESTONE));
//?}
        h.assertTrue(!furnace.matches(input,h.getLevel()),"old hollow cobblestone recipe rejected");
//? if >=1.21.1 {
        /*grid.set(4,new ItemStack(Items.FLINT_AND_STEEL));input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid);
*///?} else {
        input.setItem(4,new ItemStack(Items.FLINT_AND_STEEL));
//?}
        h.assertTrue(furnace.matches(input,h.getLevel()),"firestarter completes GT furnace recipe");
//? if >=1.21.1 {
        /*var saw=(net.minecraft.world.item.crafting.CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("gregtech:vanilla/oak_planks_saw")).orElseThrow().value();
        var wood=net.minecraft.world.item.crafting.CraftingInput.of(1,2,List.of(GTToolItem.create(GTToolType.SAW,GTMaterialRegistry.get("Iron"),GTMaterialRegistry.get("Wood")),new ItemStack(Blocks.OAK_LOG)));
*///?} else {
        var saw=(net.minecraft.world.item.crafting.CraftingRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation("gregtech:vanilla/oak_planks_saw")).orElseThrow();
        var wood=new net.minecraft.world.inventory.TransientCraftingContainer(menu,1,2);wood.setItem(0,GTToolItem.create(GTToolType.SAW,GTMaterialRegistry.get("Iron"),GTMaterialRegistry.get("Wood")));wood.setItem(1,new ItemStack(Blocks.OAK_LOG));
//?}
        h.assertTrue(saw.matches(wood,h.getLevel()),"real saw plus oak log");
        h.assertTrue(saw.assemble(wood,h.getLevel().registryAccess()).getCount()==4,"saw yields four planks");
        h.assertTrue(saw.getRemainingItems(wood).get(0).getDamageValue()==GTToolType.SAW.damagePerCraft(),"saw returned with crafting wear");h.succeed();
    }
}
