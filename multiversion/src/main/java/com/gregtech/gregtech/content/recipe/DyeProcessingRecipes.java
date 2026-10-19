package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {

/*import net.neoforged.neoforge.fluids.FluidStack;

import net.minecraft.core.registries.BuiltInRegistries;

*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
//?}
import java.util.*;

/** Original Loader_Recipes_Other dye/oil/foam chain and Loader_Recipes_Vanilla flowers.
 * Unit amounts are GT6 L=144 and liquid oil U/50=20 mB; no invented replacement ingredients.
 */

/** Original Loader_Recipes_Other dye/oil/foam chain and Loader_Recipes_Vanilla flowers.

 * Unit amounts are GT6 L=144 and liquid oil U/50=20 mB; no invented replacement ingredients.

 */
public final class DyeProcessingRecipes {
    public record Entry(RecipeMap map,Recipe recipe) {}
    private static final List<Entry> ENTRIES=new ArrayList<>();
    public static List<Entry> entries(){return Collections.unmodifiableList(ENTRIES);}
    private DyeProcessingRecipes(){}
    public static int register(){
        if(!ENTRIES.isEmpty())throw new IllegalStateException("Dye recipes registered twice");
        for(String color:RegisteredFluids.DYE_OREDICTS_POST){
            String vanilla=color.replace("LightBlue","light_blue").replace("LightGray","light_gray").toLowerCase(Locale.ROOT);
            var dye=item("minecraft:"+vanilla+"_dye",1);
            for(String water:new String[]{"Water","MnWtr","DistW","SpDew"})
                mixer(16,new ItemStack[]{dye.copy()},new FluidStack[]{fluid(water,144)},fluid("Dye_Water_"+color,216));
            for(String kind:new String[]{"Water","Flower"})for(String oil:new String[]{"Oil_Sunflower","Oil_Olive","Oil_Nut","Oil_Seed","Oil_Lin","Oil_Hemp"})
                mixer(16,null,new FluidStack[]{fluid("Dye_"+kind+"_"+color,216),fluid(oil,20)},fluid("Dye_Chemical_"+color,288));
            mixer(16,null,new FluidStack[]{fluid("Dye_Chemical_"+color,16),fluid("CFoam",100)},fluid("CFoam_Dyed_"+color,100));
            // MultiItemRandomTools:246: 16 dye units (16 * 144 mB), 256 ticks at 16 EU/t.
            add(MachineRecipeMaps.Canner,new Recipe(new ItemStack[]{item("gt6ce:empty_spray_can",1)},
                    new ItemStack[]{item("gt6ce:spray_paint_"+vanilla,1)},null,null,
                    new FluidStack[]{fluid("Dye_Chemical_"+color,2304)},null,256,16,0));
            var dyeColor = net.minecraft.world.item.DyeColor.byName(vanilla, net.minecraft.world.item.DyeColor.WHITE);
            var wet = colored("cfoam_fresh", dyeColor, 1);
            add(MachineRecipeMaps.Injector,new Recipe(null,new ItemStack[]{wet},null,null,
                    new FluidStack[]{fluid("CFoam_Dyed_"+color,100),fluid("Air",1000)},null,16,16,0));
            add(MachineRecipeMaps.Drying,new Recipe(new ItemStack[]{wet},new ItemStack[]{colored("cfoam",dyeColor,1)},null,null,null,null,16,16,0));
            for(String sulfate:new String[]{"SodiumSulfate","PotassiumSulfate"}) for(int count:new int[]{1,4}) {
                var powder=GTItems.getStack(count==1?MaterialPrefix.dustSmall:MaterialPrefix.dust,GTMaterialRegistry.get(sulfate),1);
                var molten=com.gregtech.gregtech.content.recipe.generated.GTGeneratedChem.materialFluid("Glass",1296*count);
                if(molten==null||molten.isEmpty())throw new IllegalStateException("Missing molten glass");
                add(MachineRecipeMaps.Mixer,new Recipe(new ItemStack[]{powder},new ItemStack[]{colored("glass_clear",dyeColor,count)},null,null,
                        new FluidStack[]{molten,fluid("Dye_Chemical_"+color,36*count)},null,16*count,16,0));
            }
            add(MachineRecipeMaps.Injector,new Recipe(new ItemStack[]{new ItemStack(Items.GLOWSTONE_DUST),colored("glass_clear",dyeColor,1)},
                    new ItemStack[]{colored("glass_glow",dyeColor,1)},null,null,null,null,32,16,0));
            for(String kind:new String[]{"glass_clear","glass_glow","cfoam"})
                for(var coolant:SawingCoolants.variants(false))
                    add(MachineRecipeMaps.Cutter,new Recipe(new ItemStack[]{colored(kind,dyeColor,1)},
                            new ItemStack[]{colored(kind+"_slab",dyeColor,2)},null,null,
                            new FluidStack[]{fluid(coolant.field(),5*coolant.multiplier())},null,
                            16*coolant.multiplier(),16,0).withEmptyContainerInputs());

            mixer(16,new ItemStack[]{GTItems.getStack(MaterialPrefix.dustSmall,GTMaterialRegistry.get("Palladium"),1)},
                    new FluidStack[]{fluid("CFoam_Dyed_"+color,100)},fluid("CFoam_DyedOwned_"+color,100));
            mixer(64,new ItemStack[]{GTItems.getStack(MaterialPrefix.dust,GTMaterialRegistry.get("Palladium"),1)},
                    new FluidStack[]{fluid("CFoam_Dyed_"+color,400)},fluid("CFoam_DyedOwned_"+color,400));
        }
        flower("poppy","Red",false);flower("blue_orchid","LightBlue",false);flower("allium","Magenta",false);
        flower("azure_bluet","LightGray",false);flower("red_tulip","Red",false);flower("orange_tulip","Orange",false);
        flower("white_tulip","LightGray",false);flower("pink_tulip","Pink",false);flower("oxeye_daisy","LightGray",false);
        flower("dandelion","Yellow",false);flower("lilac","Magenta",true);flower("rose_bush","Red",true);flower("peony","Pink",true);
        precursors();
        add(MachineRecipeMaps.Drying,new Recipe(new ItemStack[]{GTItems.getStack(MaterialPrefix.stick,GTMaterialRegistry.get("Iron"),1)},
                new ItemStack[]{colored("concrete_reinforced",net.minecraft.world.item.DyeColor.LIGHT_GRAY,1)},null,null,
                new FluidStack[]{fluid("Concrete",144)},new FluidStack[]{fluid("DistW",8)},16,16,0));
        add(MachineRecipeMaps.Drying,new Recipe(new ItemStack[]{new ItemStack(GTTechnological.selectorTag(0))},
                new ItemStack[]{colored("concrete",net.minecraft.world.item.DyeColor.LIGHT_GRAY,1)},null,null,
                new FluidStack[]{fluid("Concrete",144)},new FluidStack[]{fluid("DistW",8)},16,16,0).withCatalystInputs(0));
        for(var prefix:new MaterialPrefix[]{MaterialPrefix.ingot,MaterialPrefix.plate,MaterialPrefix.dust})
            for(String mold:new String[]{"extruder_shape_cell","low_heat_extruder_shape_cell"}) {
                var feed=GTItems.getStack(prefix,GTMaterialRegistry.get("Plastic"),3);
                if(!feed.isEmpty())add(MachineRecipeMaps.Extruder,new Recipe(new ItemStack[]{feed,item("gt6ce:"+mold,1)},
                        new ItemStack[]{item("gt6ce:plastic_canister",1)},null,null,null,null,192,16,0).withCatalystInputs(1));
            }
        var co2=com.gregtech.gregtech.content.recipe.generated.GTGeneratedChem.materialFluid("CarbonDioxide",16000);
        if(co2==null||co2.isEmpty())throw new IllegalStateException("Missing CO2");
        add(MachineRecipeMaps.Canner,new Recipe(new ItemStack[]{item("gt6ce:empty_spray_can",1)},new ItemStack[]{item("gt6ce:fire_extinguisher_co2",1)},null,null,
                new FluidStack[]{co2},null,256,16,0));
        UtilityProcessingRecipes.register();
        return ENTRIES.size();
    }
    /** GT6 Loader_Recipes_Crops seed oils and Loader_Recipes_Other foam hydration. */
    private static void precursors(){
        String[] seeds={"wheat_seeds","melon_seeds","pumpkin_seeds","beetroot_seeds"};
        int[] amounts={50,30,60,30};
        for(int i=0;i<seeds.length;i++)for(var map:new RecipeMap[]{MachineRecipeMaps.Squeezer,MachineRecipeMaps.Juicer})
            add(map,new Recipe(new ItemStack[]{item("minecraft:"+seeds[i],1)},null,null,null,null,
                    new FluidStack[]{fluid("Oil_Seed",amounts[i])},16,16,0));
        for(var map:new RecipeMap[]{MachineRecipeMaps.Squeezer,MachineRecipeMaps.Juicer})
            add(map,new Recipe(new ItemStack[]{new ItemStack(Items.SUNFLOWER)},new ItemStack[]{new ItemStack(Items.YELLOW_DYE,2)},null,null,null,
                    new FluidStack[]{fluid("Oil_Sunflower",map==MachineRecipeMaps.Squeezer?100:75)},16,16,0));
        for(String water:new String[]{"Water","MnWtr","DistW","SpDew"}){
            mixer(128,new ItemStack[]{dust("Stone",6),dust("SiO2",2),GTItems.getStack(MaterialPrefix.dustSmall,GTMaterialRegistry.get("Clay"),1)},
                    new FluidStack[]{fluid(water,1000)},fluid("CFoam",1000));
            mixer(512,new ItemStack[]{dust("Stone",24),dust("SiO2",8),dust("Clay",1)},
                    new FluidStack[]{fluid(water,4000)},fluid("CFoam",4000));
        }
    }
    private static ItemStack dust(String material,int count){
        return GTItems.getStack(MaterialPrefix.dust,GTMaterialRegistry.get(material),count);
    }
    private static void flower(String name,String color,boolean large){
        add(MachineRecipeMaps.Squeezer,new Recipe(new ItemStack[]{item("minecraft:"+name,1)},new ItemStack[]{item("gt6ce:plant_remains",1)},
                null,new long[]{large?4000:2000},null,new FluidStack[]{fluid("Dye_Flower_"+color,large?432:288)},16,16,0));
        add(MachineRecipeMaps.Juicer,new Recipe(new ItemStack[]{item("minecraft:"+name,1)},new ItemStack[]{item("gt6ce:plant_remains",1)},
                null,new long[]{large?6000:3000},null,new FluidStack[]{fluid("Dye_Flower_"+color,large?288:144)},16,16,0));
    }
    private static ItemStack colored(String id,net.minecraft.world.item.DyeColor color,int count){
        var block=((net.minecraft.world.item.BlockItem)item("gt6ce:"+id,1).getItem()).getBlock();
        var stack=com.gregtech.gregtech.block.misc.ConcreteBlock.coloredItem(block,color);stack.setCount(count);return stack;
    }
    private static ItemStack item(String id,int count){
//? if >=1.21.1 {

        /*var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));

*///?} else {
        var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
//?}
        if(item==null||item==Items.AIR)throw new IllegalStateException("Missing dye ingredient "+id);
        return new ItemStack(item,count);
    }
    public static FluidStack fluid(String field,int amount){
        if(field.equals("Water"))return new FluidStack(Fluids.WATER,amount);
        var fluid=GTFluids.still(field);
//? if >=1.21.1 {

        /*if(fluid==null||!fluid.isBound())throw new IllegalStateException("Missing dye fluid "+field);

*///?} else {
        if(fluid==null||!fluid.isPresent())throw new IllegalStateException("Missing dye fluid "+field);
//?}
        return new FluidStack(fluid.get(),amount);
    }
    private static void mixer(int ticks,ItemStack[] items,FluidStack[] inputs,FluidStack output){
        add(MachineRecipeMaps.Mixer,new Recipe(items,null,null,null,inputs,new FluidStack[]{output},ticks,16,0));
    }
    private static void add(RecipeMap map,Recipe recipe){
        for(var input:recipe.mInputs)if(input.isEmpty())throw new IllegalStateException("Empty dye recipe input");
        recipe.mExactItemInputs=true;
        if(map.addRecipe(recipe)==null)throw new IllegalStateException("Conflicting dye recipe in "+map.mNameInternal);
        ENTRIES.add(new Entry(map,recipe));
    }
}
