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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@GameTestHolder("gregtech_issues")
@PrefixGameTestTemplate(false)
public final class IssueVanillaTests {
    @GameTest(template="test_empty",timeoutTicks=30)
    public static void loadedVanillaOverrides(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        for(var row:com.gregtech.gregtech.content.recipe.VanillaCraftingReplacements.rows()) {
            var found=manager.byKey(ResourceLocation.parse(row.id()));
            h.assertTrue(found.isPresent(),"loaded original override "+row.id());
            var recipe=found.get().value();
            h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).getCount()==row.count(),"original output count "+row.id());
            if(row.pattern()!=null)h.assertTrue(recipe instanceof com.gregtech.gregtech.recipe.ToolShapedRecipe,"GT tool-aware replacement "+row.id());
        }h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=30)
    public static void furnaceRequiresIgniterAndSawReturnsWornTool(GameTestHelper h) {
        var furnace=(net.minecraft.world.item.crafting.CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("minecraft:furnace")).orElseThrow().value();
        var grid=new ArrayList<ItemStack>();for(int i=0;i<9;i++)grid.add(i==4?ItemStack.EMPTY:new ItemStack(Blocks.COBBLESTONE));
        var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid);
        h.assertTrue(!furnace.matches(input,h.getLevel()),"old hollow cobblestone recipe rejected");
        grid.set(4,new ItemStack(Items.FLINT_AND_STEEL));input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid);
        h.assertTrue(furnace.matches(input,h.getLevel()),"firestarter completes GT furnace recipe");
        var saw=(net.minecraft.world.item.crafting.CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("gregtech:vanilla/oak_planks_saw")).orElseThrow().value();
        var wood=net.minecraft.world.item.crafting.CraftingInput.of(1,2,List.of(GTToolItem.create(GTToolType.SAW,GTMaterialRegistry.get("Iron"),GTMaterialRegistry.get("Wood")),new ItemStack(Blocks.OAK_LOG)));
        h.assertTrue(saw.matches(wood,h.getLevel()),"real saw plus oak log");
        h.assertTrue(saw.assemble(wood,h.getLevel().registryAccess()).getCount()==4,"saw yields four planks");
        h.assertTrue(saw.getRemainingItems(wood).get(0).getDamageValue()==GTToolType.SAW.damagePerCraft(),"saw returned with crafting wear");h.succeed();
    }
}
