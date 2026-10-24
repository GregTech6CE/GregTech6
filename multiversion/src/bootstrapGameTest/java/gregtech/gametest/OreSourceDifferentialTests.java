package gregtech.gametest;

import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.recipe.Recipe;
import gregtech.api.recipe.RecipeMap;
import gregtech.data.MaterialPrefix;
import gregtech.data.MachineRecipeMaps;
import gregtech.registry.GTItems;
import gregtech.registry.GTFluids;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Ignored native draft: actual accepted maps only; no desired recipe insertion.
 * Expectations are independent source-oracle literals. Cached Forge/Neo native javac passed; GameTests NOT run.
 * Dedicated ore profile: no external Cinnabar byproduct additions, source optional fluids enabled.
 * Raw-craft and thermal blanket-absence claims excluded; Cryotheum Bath supplement is separate.
 */
@GameTestHolder("gregtech_ore_source_audit")
@PrefixGameTestTemplate(false)
public final class OreSourceDifferentialTests {
    private static final FluidStack[] NO_FLUIDS = new FluidStack[0];
    static ItemStack s(String form,String material,int count) {
        try {
            if(form.equals("blockDust")) {
                var blockMaterial=GTMaterialRegistry.get(material);
                var blockPrefix=gregtech.api.prefix.BlockMaterialPrefix.blockDust;
                var id=net.minecraft.resources.ResourceLocation.parse("gt6ce:"+blockPrefix.getBlockId(blockMaterial.resolve()));
                var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
                if(block==net.minecraft.world.level.block.Blocks.AIR)throw new AssertionError("Missing required source block form "+id);
                return new ItemStack(block,count);
            }
            var prefix=(MaterialPrefix)MaterialPrefix.class.getField(form).get(null);
            var mat=GTMaterialRegistry.get(material);
            if(mat==null || !mat.isValid())throw new AssertionError("Missing material fixture "+material);
            var stack=GTItems.getStack(prefix,mat.resolve(),count);
            if(stack.isEmpty())throw new AssertionError("Missing required source form "+form+":"+material);
            return stack;
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static FluidStack f(String field,int mb) {
        var fluid=GTFluids.stack(field,mb);
        if(fluid==null||fluid.isEmpty())throw new AssertionError("Missing required fluid fixture "+field);
        // Explicit source identity -> native registry identity. Do not normalize arbitrary aliases.
        String[] binding=switch(field) {
            case "Water" -> new String[]{"water","minecraft:water"};
            case "MnWtr" -> new String[]{"potion.mineralwater","gt6ce:potion_mineralwater"};
            case "DistW" -> new String[]{"ic2distilledwater","gt6ce:ic2distilledwater"};
            case "SpDew" -> new String[]{"spectral_dew","gt6ce:spectral_dew"};
            case "Sluice" -> new String[]{"sluicejuice","gt6ce:sluicejuice"};
            case "Petrotheum" -> new String[]{"petrotheum","gt6ce:petrotheum"};
            case "Steam" -> new String[]{"steam","gt6ce:steam"};
            case "GenMolten_Wax" -> new String[]{"molten.wax","gt6ce:molten_wax"};
            case "GenMolten_WaxBee" -> new String[]{"molten.waxbee","gt6ce:molten_waxbee"};
            case "GenMolten_WaxRefractory" -> new String[]{"molten.waxrefractory","gt6ce:molten_waxrefractory"};
            case "GenMolten_WaxParaffin" -> new String[]{"molten.waxparaffin","gt6ce:molten_waxparaffin"};
            case "GenMolten_WaxPlant" -> new String[]{"molten.waxplant","gt6ce:molten_waxplant"};
            case "GenMolten_WaxMagic" -> new String[]{"molten.waxmagic","gt6ce:molten_waxmagic"};
            case "GenMolten_WaxAmnesic" -> new String[]{"molten.waxamnesic","gt6ce:molten_waxamnesic"};
            case "GenMolten_WaxSoulful" -> new String[]{"molten.waxsoulful","gt6ce:molten_waxsoulful"};
            default -> throw new AssertionError("Unreviewed fluid alias " + field);
        };
        var entry=gregtech.data.RegisteredFluids.get(field);
        if(entry==null||!entry.registryName().equals(binding[0]))
            throw new AssertionError("Source fluid catalog binding mismatch "+field+" expected "+binding[0]);
        var id=net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        if(!id.toString().equals(binding[1]))
            throw new AssertionError("Native fluid identity mismatch "+field+" -> "+id+" expected "+binding[1]);
        return fluid;
    }
    private static boolean same(ItemStack a,ItemStack b) {
//? if >=1.21.1 {
        /*return a.getCount()==b.getCount() && ItemStack.isSameItemSameComponents(a,b);
*///?} else {
        return a.getCount()==b.getCount() && ItemStack.isSameItemSameTags(a,b);
//?}
    }
    private static boolean items(ItemStack[] a,ItemStack[] b) {
        if(a.length!=b.length)return false;
        for(int i=0;i<a.length;i++)if(!same(a[i],b[i]))return false;
        return true;
    }
    private static boolean fluids(FluidStack[] a,FluidStack[] b) {
        if(a.length!=b.length)return false;
        for(int i=0;i<a.length;i++)if(a[i].getFluid()!=b[i].getFluid()||a[i].getAmount()!=b[i].getAmount())return false;
        return true;
    }
    static void row(GameTestHelper h,String evidence,RecipeMap map,ItemStack input,
                            FluidStack[] fin,ItemStack[] out,long[] chance,FluidStack[] fout,long ticks,long eut) {
        var matches=map.mRecipeList.stream().filter(r->items(r.mInputs,new ItemStack[]{input})&&fluids(r.mFluidInputs,fin)).toList();
        h.assertTrue(matches.size()==1,evidence+": exactly one accepted native row; found "+matches.size());
        var r=matches.get(0);
        h.assertTrue(map.findRecipe(List.of(input),Arrays.asList(fin),false,map.mInputItemsCount,map.mOutputItemsCount)==r,
                evidence+": actual runtime lookup selects the accepted expected row");
        h.assertTrue(items(r.mOutputs,out),evidence+": exact ordered item forms/counts; actual "+Arrays.toString(r.mOutputs));
        h.assertTrue(r.mOutputs.length==chance.length,evidence+": chance slot alignment");
        for(int i=0;i<chance.length;i++)h.assertTrue(r.getOutputChance(i)==chance[i],evidence+": chance slot "+i);
        h.assertTrue(fluids(r.mFluidOutputs,fout),evidence+": exact output fluids");
        h.assertTrue(r.mDuration==ticks&&r.mEUt==eut,evidence+": exact work/EUt, actual "+r.mDuration+"/"+r.mEUt);
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void crusherraw(GameTestHelper h) {
        row(h,"crusher_raw: RecipeMapHandlerCrushing.java:50-140",MachineRecipeMaps.Crusher,s("oreRaw","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("crushed","Cinnabar",1),s("crushed","Cinnabar",1),s("gem","Cinnabar",1)},
            new long[]{10000,10000,3000},NO_FLUIDS,128,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void sluicewater(GameTestHelper h) {
        for(String water:new String[]{"Water","MnWtr","DistW","SpDew"}) {
            row(h,"sluice_water: Loader_Recipes_Ores.java:379-420",MachineRecipeMaps.Sluice,s("crushed","Cinnabar",1),
                new FluidStack[]{f(water,900)},new ItemStack[]{s("crushedPurified","Cinnabar",1),s("crushedPurifiedTiny","Redstone",9),s("crushedPurifiedTiny","Sulfur",9),s("crushedPurifiedTiny","Glowstone",9),s("crushedPurifiedTiny","Selenium",9),s("crushedPurifiedTiny","Cinnabar",9),s("crushedPurifiedTiny","Cinnabar",9),s("crushedPurifiedTiny","Cinnabar",9),s("crushedPurifiedTiny","Cinnabar",9)},
                new long[]{10000,300,300,300,300,300,300,300,300},new FluidStack[]{f("Sluice",900)},144,16);
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void sifter(GameTestHelper h) {
        row(h,"sifter: Loader_Recipes_Ores.java:460-463",MachineRecipeMaps.Sifting,s("crushedPurified","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("gemLegendary","Cinnabar",1),s("gemExquisite","Cinnabar",1),s("gemFlawless","Cinnabar",1),s("gem","Cinnabar",1),s("gemFlawed","Cinnabar",2),s("gemChipped","Cinnabar",4),s("dust","Cinnabar",1)},
            new long[]{9,90,360,1350,1800,3600,4500},NO_FLUIDS,144,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void centrifugepurified(GameTestHelper h) {
        row(h,"centrifuge_purified: Loader_Recipes_Ores.java:465-470",MachineRecipeMaps.Centrifuge,s("crushedPurified","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("crushedCentrifugedTiny","Cinnabar",9),s("crushedCentrifugedTiny","Redstone",9),s("crushedCentrifugedTiny","Sulfur",9),s("crushedCentrifugedTiny","Glowstone",9)},
            new long[]{10000,1000,1000,1000},NO_FLUIDS,144,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void sluicepetrotheum(GameTestHelper h) {
        row(h,"sluice_petrotheum: Loader_Recipes_Handlers.java:672-673; Loader_Fluids.java:132",MachineRecipeMaps.Sluice,s("crushed","Cinnabar",1),
            new FluidStack[]{f("Petrotheum",45)},new ItemStack[]{s("crushedPurified","Cinnabar",2),s("crushedPurifiedTiny","Cinnabar",9),s("dust","SluiceSand",1)},
            new long[]{10000,5000,10000},NO_FLUIDS,144,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void steamcracking(GameTestHelper h) {
        for(String wax:new String[]{"GenMolten_Wax","GenMolten_WaxBee","GenMolten_WaxParaffin","GenMolten_WaxPlant","GenMolten_WaxRefractory","GenMolten_WaxMagic","GenMolten_WaxAmnesic","GenMolten_WaxSoulful"}) {
            row(h,"steam_cracking: Loader_Recipes_Ores.java:473-481; Loader_Fluids205-212",MachineRecipeMaps.SteamCracking,s("crushedCentrifuged","Cinnabar",1),
                new FluidStack[]{f("Steam",1440),f(wax,144)},new ItemStack[]{s("dustTiny","Cinnabar",9),s("dustTiny","Redstone",9),s("dustTiny","Sulfur",9)},
                new long[]{10000,4000,4000},new FluidStack[]{f("Water",9)},144,16);
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void sluicewaterTiny(GameTestHelper h) {
        for(String water:new String[]{"Water","MnWtr","DistW","SpDew"}) {
            row(h,"sluice_waterTiny: Loader_Recipes_Ores.java:379-420",MachineRecipeMaps.Sluice,s("crushedTiny","Cinnabar",1),
                new FluidStack[]{f(water,100)},new ItemStack[]{s("crushedPurifiedTiny","Cinnabar",1),s("crushedPurifiedTiny","Redstone",1),s("crushedPurifiedTiny","Sulfur",1),s("crushedPurifiedTiny","Glowstone",1),s("crushedPurifiedTiny","Selenium",1),s("crushedPurifiedTiny","Cinnabar",1),s("crushedPurifiedTiny","Cinnabar",1),s("crushedPurifiedTiny","Cinnabar",1),s("crushedPurifiedTiny","Cinnabar",1)},
                new long[]{10000,300,300,300,300,300,300,300,300},new FluidStack[]{f("Sluice",100)},16,16);
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void sifterTiny(GameTestHelper h) {
        row(h,"sifterTiny: Loader_Recipes_Ores.java:460-463",MachineRecipeMaps.Sifting,s("crushedPurifiedTiny","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("gemLegendary","Cinnabar",1),s("gemExquisite","Cinnabar",1),s("gemFlawless","Cinnabar",1),s("gem","Cinnabar",1),s("gemFlawed","Cinnabar",2),s("gemChipped","Cinnabar",4),s("dust","Cinnabar",1)},
            new long[]{1,10,40,150,200,400,500},NO_FLUIDS,16,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void centrifugepurifiedTiny(GameTestHelper h) {
        row(h,"centrifuge_purifiedTiny: Loader_Recipes_Ores.java:465-470",MachineRecipeMaps.Centrifuge,s("crushedPurifiedTiny","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("crushedCentrifugedTiny","Cinnabar",1),s("crushedCentrifugedTiny","Redstone",1),s("crushedCentrifugedTiny","Sulfur",1),s("crushedCentrifugedTiny","Glowstone",1)},
            new long[]{10000,1000,1000,1000},NO_FLUIDS,16,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void sluicepetrotheumTiny(GameTestHelper h) {
        row(h,"sluice_petrotheumTiny: Loader_Recipes_Handlers.java:672-673; Loader_Fluids.java:132",MachineRecipeMaps.Sluice,s("crushedTiny","Cinnabar",1),
            new FluidStack[]{f("Petrotheum",5)},new ItemStack[]{s("crushedPurifiedTiny","Cinnabar",2),s("crushedPurifiedTiny","Cinnabar",1),s("dustTiny","SluiceSand",1)},
            new long[]{10000,5000,10000},NO_FLUIDS,16,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void steamcrackingTiny(GameTestHelper h) {
        for(String wax:new String[]{"GenMolten_Wax","GenMolten_WaxBee","GenMolten_WaxParaffin","GenMolten_WaxPlant","GenMolten_WaxRefractory","GenMolten_WaxMagic","GenMolten_WaxAmnesic","GenMolten_WaxSoulful"}) {
            row(h,"steam_crackingTiny: Loader_Recipes_Ores.java:473-481; Loader_Fluids205-212",MachineRecipeMaps.SteamCracking,s("crushedCentrifugedTiny","Cinnabar",1),
                new FluidStack[]{f("Steam",160),f(wax,16)},new ItemStack[]{s("dustTiny","Cinnabar",1),s("dustTiny","Redstone",1),s("dustTiny","Sulfur",1)},
                new long[]{10000,4000,4000},new FluidStack[]{f("Water",1)},16,16);
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void shreddercrushed(GameTestHelper h) {
        row(h,"shredder_crushed: Loader_Recipes_Handlers145-150; RecipeMapHandlerPrefix203-228",MachineRecipeMaps.Shredder,s("crushed","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("dust","Cinnabar",1),s("dustTiny","Cinnabar",1),s("dustDiv72","Cinnabar",1)},
            new long[]{10000,10000,10000},NO_FLUIDS,18,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void shreddercrushedPurified(GameTestHelper h) {
        row(h,"shredder_crushedPurified: Loader_Recipes_Handlers145-150; RecipeMapHandlerPrefix203-228",MachineRecipeMaps.Shredder,s("crushedPurified","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("dust","Cinnabar",1),s("dustSmall","Cinnabar",1)},
            new long[]{10000,10000},NO_FLUIDS,20,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void shreddercrushedCentrifuged(GameTestHelper h) {
        row(h,"shredder_crushedCentrifuged: Loader_Recipes_Handlers145-150; RecipeMapHandlerPrefix203-228",MachineRecipeMaps.Shredder,s("crushedCentrifuged","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("dust","Cinnabar",1),s("dustSmall","Cinnabar",1),s("dustTiny","Cinnabar",1),s("dustDiv72","Cinnabar",1)},
            new long[]{10000,10000,10000,10000},NO_FLUIDS,22,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void shreddercrushedTiny(GameTestHelper h) {
        row(h,"shredder_crushedTiny: Loader_Recipes_Handlers145-150; RecipeMapHandlerPrefix203-228",MachineRecipeMaps.Shredder,s("crushedTiny","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("dustTiny","Cinnabar",1),s("dustDiv72","Cinnabar",1)},
            new long[]{10000,10000},NO_FLUIDS,2,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void shreddercrushedPurifiedTiny(GameTestHelper h) {
        row(h,"shredder_crushedPurifiedTiny: Loader_Recipes_Handlers145-150; RecipeMapHandlerPrefix203-228",MachineRecipeMaps.Shredder,s("crushedPurifiedTiny","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("dustTiny","Cinnabar",1),s("dustDiv72","Cinnabar",1),s("dustDiv72","Cinnabar",1)},
            new long[]{10000,10000,10000},NO_FLUIDS,3,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void shreddercrushedCentrifugedTiny(GameTestHelper h) {
        row(h,"shredder_crushedCentrifugedTiny: Loader_Recipes_Handlers145-150; RecipeMapHandlerPrefix203-228",MachineRecipeMaps.Shredder,s("crushedCentrifugedTiny","Cinnabar",1),
            NO_FLUIDS,new ItemStack[]{s("dustTiny","Cinnabar",1),s("dustDiv72","Cinnabar",1),s("dustDiv72","Cinnabar",1),s("dustDiv72","Cinnabar",1)},
            new long[]{10000,10000,10000,10000},NO_FLUIDS,3,16);
        h.succeed();
    }
}
