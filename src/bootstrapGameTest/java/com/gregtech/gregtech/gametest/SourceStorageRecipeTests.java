package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import static com.gregtech.gregtech.gametest.TransportMaterialPortTests.*;

/** One finite native crafting/recovery scenario for the pooled storage/source batches. */
@net.minecraftforge.gametest.GameTestHolder("gregtech_block_properties")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class SourceStorageRecipeTests {
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void nativeStorageCraftingSynchronizationToolsAndRecoveryProtectStoredItems(GameTestHelper h) {
        var cases = Map.of(
                "hand/storage/advanced_crafting_table_steel","advanced_crafting_table",
                "hand/storage/charging_crafting_table_gold","charging_crafting_table_gold",
                "hand/storage/logistics_mass_storage_aluminium","logistics_mass_storage_aluminium",
                "hand/storage/logistics_mass_storage_platinum","logistics_mass_storage_platinum",
                "bookshelves/bookshelf_metal_stainless_steel","bookshelf_metal_stainless_steel",
                "scaffolds/scaffold","scaffold");
        for (var entry : cases.entrySet()) {
            var recipe = lookup(h,entry.getKey());
            var stacks = new ArrayList<ItemStack>();for(int i=0;i<9;i++)stacks.add(ItemStack.EMPTY);
            var ingredients=recipe.getIngredients();
            for(int i=0;i<ingredients.size();i++) {
                var values=ingredients.get(i).getItems();if(values.length==0)continue;
                var value=values[0].copyWithCount(1);
                if(value.getItem() instanceof GTToolItem tool)value=GTToolItem.create(tool.toolType(),
                        GTMaterialRegistry.get("Steel"),GTMaterialRegistry.get("Wood"));
                stacks.set(i,value);
            }
            h.assertTrue(recipe.matches(grid(stacks),h.getLevel()),"native source grid "+entry.getKey());
            var result=recipe.assemble(grid(stacks),h.getLevel().registryAccess());
            h.assertTrue(result.is(item("gregtech:"+entry.getValue()).getItem()) && result.getCount()==1,"native source result "+entry.getKey());
            h.assertTrue(wire(h,recipe).matches(grid(stacks),h.getLevel()),"real storage recipe synchronization "+entry.getKey());
            var remains=recipe.getRemainingItems(grid(stacks));
            for(int i=0;i<stacks.size();i++)if(stacks.get(i).getItem() instanceof GTToolItem tool)
                h.assertTrue(remains.get(i).is(tool) && remains.get(i).getDamageValue()==tool.toolType().damagePerCraft(),"actual storage tool wear");
            var data=ItemMaterialRegistry.get(result).orElseThrow();
            h.assertTrue(data.components().equals(com.gregtech.gregtech.content.machine.MachineConstructionMaterials
                    .block(entry.getValue()).orElseThrow().components()),"native exact REV registration "+entry.getValue());
            checkCrucible(h,result);
            var recovery=VanillaRecoveryRecipes.recipes().stream().filter(r->r.mInputs[0].is(result.getItem()))
                    .findFirst().orElseThrow(()->new IllegalStateException("No exact storage recovery "+entry.getValue()));
            h.assertTrue(RecipeInputs.consume(recovery,List.of(result),List.of(),1)!=null,"actual clean storage recovery input");
            var totals=new HashMap<GTMaterial,Long>();var quanta=new HashMap<GTMaterial,Long>();
            for(var c:data.components())totals.merge(c.material().getTargetPulverMaterial().resolve(),
                    MaterialRecoveryRules.pulverizedAmount(c.material(),c.amount()),Math::addExact);
            for(var output:recovery.mOutputs) {
                var form=MaterialEquivalence.form(output);h.assertTrue(form!=null,"native output material/pile");
                totals.merge(form.material(),-form.prefix().getMaterialWeight()*output.getCount(),Long::sum);
                quanta.put(form.material(),form.prefix().getMaterialWeight());
            }
            for(var row:totals.entrySet())h.assertTrue(row.getValue()>=0 && row.getValue()<quanta.getOrDefault(row.getKey(),0L),
                    "actual selected-pile rounding conserves storage material "+entry.getValue()+" "+row.getKey());
            var unsafe=result.copy();stored(unsafe);
            h.assertTrue(!ItemMaterialRegistry.canRecover(unsafe) && RecipeInputs.consume(recovery,List.of(unsafe),List.of(),1)==null
                    && com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(unsafe).isEmpty(),"stored/configured storage blocks cannot be recycled");
        }
        h.assertTrue(cases.size()==6,"five native families plus platinum merge case");h.succeed();
    }
    @SuppressWarnings("unchecked")
    private static net.minecraft.world.item.crafting.Recipe<net.minecraft.world.inventory.CraftingContainer> lookup(GameTestHelper h,String path) {
        return (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.inventory.CraftingContainer>)h.getLevel().getRecipeManager()
                .byKey(ResourceLocation.parse("gregtech:"+path)).orElseThrow();
    }
}
