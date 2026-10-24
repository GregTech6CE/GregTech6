package gregtech.gametest;

import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.material.GTValues;
import gregtech.api.material.ItemMaterialRegistry;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.tool.GTToolHelper;
import gregtech.api.tool.GTToolType;
import gregtech.content.material.Materials;
import gregtech.content.material.generated.WoodMaterials;
import gregtech.data.MachineRecipeMaps;
import gregtech.data.MaterialPrefix;
import gregtech.item.GTToolItem;
import gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;

/** Actual recipe lookup and crafting over native wood items; no generated-row-only assertions. */
@GameTestHolder("gt6ce_early_survival")
@PrefixGameTestTemplate(false)
public final class EarlyWoodRecipeTests {
    private EarlyWoodRecipeTests() {}

    @GameTest(template="test_empty",timeoutTicks=100)
    public static void woodPulpKeepsSpeciesAndOriginalAmounts(GameTestHelper helper) {
        for(var map:List.of(MachineRecipeMaps.Crusher,MachineRecipeMaps.Shredder)) {
            for(String[] wood:new String[][]{{"minecraft:oak","Oak"},{"minecraft:jungle","Junglewood"},
                    {"gt6ce:pine","Pine"},{"gt6ce:cinnamon","Cinnamonwood"}}) {
                String[] id=wood[0].split(":");boolean nativeWood=id[0].equals("minecraft");
                var log=item(id[0]+":"+(nativeWood?id[1]+"_log":"log_"+id[1]));
                var plank=item(id[0]+":"+(nativeWood?id[1]+"_planks":"planks_"+id[1]));
                var slab=item(id[0]+":"+(nativeWood?id[1]+"_slab":"slab_"+id[1]));
                helper.assertTrue(ItemMaterialRegistry.get(plank).orElseThrow().material().resolve()
                                ==GTMaterialRegistry.get(wood[1]).resolve(),
                        "Late generic vanilla binding replaced the wood species of "+plank);
                pulp(helper,map,log,wood[1],6*GTValues.U);
                pulp(helper,map,plank,wood[1],GTValues.U);
                pulp(helper,map,slab,wood[1],GTValues.U/2);
                pulp(helper,MachineRecipeMaps.Mortar,plank,wood[1],GTValues.U);
                pulp(helper,MachineRecipeMaps.Mortar,GTItems.getStack(MaterialPrefix.stick,
                        GTMaterialRegistry.get(wood[1]),1),wood[1],GTValues.U/2);
                if(nativeWood)pulp(helper,map,item(id[0]+":"+id[1]+"_stairs"),wood[1],3*GTValues.U/4);
                else pulp(helper,map,item(id[0]+":beam_"+id[1]),wood[1],7*GTValues.U);
                var logRecipe=map.findRecipe(List.of(log),List.of(),false,1,8);
                helper.assertTrue(logRecipe.mOutputs.length>1&&logRecipe.getOutputChance(1)==5000,
                        "log bark must retain original 50% chance: "+wood[0]);
            }
            pulp(helper,map,item("gt6ce:log_dry"),"WoodDead",3*GTValues.U);
            pulp(helper,map,item("gt6ce:slab_dry"),"WoodDead",GTValues.U/2);
            // MT.WoodCompressed.setPulver(Wood): this source-defined target is intentionally generic.
            pulp(helper,map,item("gt6ce:planks_compressed"),"Wood",GTValues.U);
        }
        pulp(helper,MachineRecipeMaps.Mortar,item("gt6ce:planks_dry"),"WoodDead",GTValues.U);
        pulp(helper,MachineRecipeMaps.Mortar,item("gt6ce:planks_compressed"),"Wood",GTValues.U);
        helper.succeed();
    }

    private static void pulp(GameTestHelper helper,RecipeMap map,ItemStack input,String name,long units) {
        var recipe=map.findRecipe(List.of(input),List.of(),false,1,8);
        helper.assertTrue(recipe!=null,"Missing native wood pulp recipe "+map+" "+input);
        var output=recipe.mOutputs[0];
        var data=ItemMaterialRegistry.get(output).orElseThrow();
        helper.assertTrue(data.material().resolve()==GTMaterialRegistry.get(name).resolve()
                        &&data.amount()*output.getCount()==units,
                "Wrong wood species/amount in "+map.mNameInternal+" from "+input+": "+output
                        +" actual="+data.material().getName()+"/"+(data.amount()*output.getCount())
                        +" (expected "+name+"/"+units+")");
    }

    @GameTest(template="test_empty",timeoutTicks=100)
    public static void woodenRollingPinsUsePlanksAndMixedWoodSticks(GameTestHelper helper) {
        var level=helper.getLevel();
        for(int index=1;index<=2;index++) {
            var holder=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gt6ce","tools/rolling_pin_"+index)).orElseThrow();
            var recipe=(CraftingRecipe)holder.value();
            for(int slot:new int[]{2,4,6,8})helper.assertTrue(!recipe.getIngredients().get(slot).isEmpty(),
                    "Wooden rolling pin viewer ingredient missing at "+slot);
            var toolType=index==1?GTToolType.FILE:GTToolType.KNIFE;
            for(String[] wood:new String[][]{{"minecraft:oak_planks","Oak"},{"gt6ce:planks_cinnamon","Cinnamonwood"},
                    {"gt6ce:planks_wood","Wood"}}) {
                var tool=GTToolItem.create(toolType,Materials.Bronze,WoodMaterials.Wood);
                var grid=new ArrayList<>(List.of(ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.STICK),
                        ItemStack.EMPTY,item(wood[0]),ItemStack.EMPTY,
                        GTItems.getStack(MaterialPrefix.stick,WoodMaterials.Maple,1),ItemStack.EMPTY,tool));
                var input=CraftingInput.of(3,3,grid);
                helper.assertTrue(recipe.matches(input,level),"Original plank + mixed wood sticks + "+toolType+" did not match");
                var output=recipe.assemble(input,level.registryAccess());
                helper.assertTrue(GTToolHelper.getType(output)==GTToolType.ROLLING_PIN
                                &&GTToolHelper.getHead(output)==GTMaterialRegistry.get(wood[1])
                                &&GTToolHelper.getHandle(output)==WoodMaterials.Wood,
                        "Rolling pin must retain plank species and generic Wood handle");
                var remains=recipe.getRemainingItems(input);
                helper.assertTrue(!remains.get(8).isEmpty()&&remains.get(8).getDamageValue()>tool.getDamageValue(),
                        "Rolling pin file/knife must return with crafting wear");
                var mirror=new ArrayList<ItemStack>();
                for(int y=0;y<3;y++)for(int x=0;x<3;x++)mirror.add(grid.get(y*3+2-x));
                helper.assertTrue(recipe.matches(CraftingInput.of(3,3,mirror),level),"Original rolling pin must allow mirroring");
                grid.set(2,GTItems.getStack(MaterialPrefix.stick,Materials.Bronze,1));
                helper.assertTrue(!recipe.matches(CraftingInput.of(3,3,grid),level),"Wooden rolling pin must reject metal sticks");
            }
        }
        helper.succeed();
    }

    private static ItemStack item(String id) {
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if(item==Items.AIR)throw new IllegalStateException("Missing native wood test item "+id);
        return new ItemStack(item);
    }
}
