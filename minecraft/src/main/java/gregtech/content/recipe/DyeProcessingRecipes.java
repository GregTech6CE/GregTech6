package gregtech.content.recipe;

import gregtech.engine.recipe.*;
import gregtech.engine.recipe.*;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.data.*;
import gregtech.content.registry.*;
import gregtech.content.tool.PaintingRules;
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
            var waters=RecipeInputGroups.fluids("water","Water","MnWtr","DistW","SpDew");
            add(MachineRecipeMaps.Mixer,waters.apply(new Recipe(new ItemStack[]{dye.copy()},null,null,null,
                    new FluidStack[]{waters.stack(144)},new FluidStack[]{fluid("Dye_Water_"+color,216)},16,16,0),0));
            var naturalDyes=RecipeInputGroups.fluids("dyes/"+vanilla+"/natural","Dye_Water_"+color,"Dye_Flower_"+color);
            var oils=RecipeInputGroups.fluids("vegetable_oils","Oil_Sunflower","Oil_Olive","Oil_Nut","Oil_Seed","Oil_Lin","Oil_Hemp");
            var chemical=new Recipe(null,null,null,null,new FluidStack[]{naturalDyes.stack(216),oils.stack(20)},
                    new FluidStack[]{fluid("Dye_Chemical_"+color,288)},16,16,0);
            naturalDyes.apply(chemical,0); oils.apply(chemical,1); add(MachineRecipeMaps.Mixer,chemical);
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
                var molten=gregtech.content.recipe.generated.GeneratedRecipeResolver.materialFluid("Glass",1296*count);
                if(molten==null||molten.isEmpty())throw new IllegalStateException("Missing molten glass");
                add(MachineRecipeMaps.Mixer,new Recipe(new ItemStack[]{powder},new ItemStack[]{colored("glass_clear",dyeColor,count)},null,null,
                        new FluidStack[]{molten,fluid("Dye_Chemical_"+color,36*count)},null,16*count,16,0));
            }
            add(MachineRecipeMaps.Injector,new Recipe(new ItemStack[]{new ItemStack(Items.GLOWSTONE_DUST),colored("glass_clear",dyeColor,1)},
                    new ItemStack[]{colored("glass_glow",dyeColor,1)},null,null,null,null,32,16,0));
            for(String kind:new String[]{"glass_clear","glass_glow","cfoam"})
                for(var coolant:RecipeInputGroups.coolants(false))
                    add(MachineRecipeMaps.Cutter,coolant.apply(new Recipe(new ItemStack[]{colored(kind,dyeColor,1)},
                            new ItemStack[]{colored(kind+"_slab",dyeColor,2)},null,null,
                            new FluidStack[]{fluid(coolant.field(),5*coolant.multiplier())},null,
                            16*coolant.multiplier(),16,0).withEmptyContainerInputs()));

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
        bathDyeing();
        add(MachineRecipeMaps.Drying,new Recipe(new ItemStack[]{GTItems.getStack(MaterialPrefix.stick,GTMaterialRegistry.get("Iron"),1)},
                new ItemStack[]{colored("concrete_reinforced",net.minecraft.world.item.DyeColor.LIGHT_GRAY,1)},null,null,
                new FluidStack[]{fluid("Concrete",144)},new FluidStack[]{fluid("DistW",8)},16,16,0));
        add(MachineRecipeMaps.Drying,new Recipe(new ItemStack[]{new ItemStack(GTTechnological.selectorTag(0))},
                new ItemStack[]{colored("concrete",net.minecraft.world.item.DyeColor.LIGHT_GRAY,1)},null,null,
                new FluidStack[]{fluid("Concrete",144)},new FluidStack[]{fluid("DistW",8)},16,16,0).withCatalystInputs(0));
        // The complete plastic-canister prefix matrix belongs to SourceExtruderRecipes.
        var co2=gregtech.content.recipe.generated.GeneratedRecipeResolver.materialFluid("CarbonDioxide",16000);
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
        foamFamilies();
    }

    /**
     * {@code Loader_Recipes_Other.java:252-330}: one tagged row per colour and batch size.
     * A rock's group decides the foam colour, and the first
     * group leaves it undyed. GT6's {@code OM.dust(clay, U4)} is one small pile while the large
     * recipe takes one full dust.
     */
    private static void foamFamilies(){
        for(String[] group:FOAM_ROCKS_BY_DYE){
            String dye=group[0];
            String output=dye.isEmpty()?"CFoam":"CFoam_Dyed_"+dye;
            for(boolean large:new boolean[]{false,true}) {
                int multiplier=large?4:1;
                var recipe=new Recipe(new ItemStack[]{dust(group[1],6*multiplier),dust(FOAM_SANDS[0],2*multiplier),
                        GTItems.getStack(large?MaterialPrefix.dust:MaterialPrefix.dustSmall,GTMaterialRegistry.get(FOAM_CLAYS[0]),1)},
                        null,null,null,new FluidStack[]{fluid("Water",1000*multiplier)},
                        new FluidStack[]{fluid(output,1000*multiplier)},128*multiplier,16,0)
                        .withItemTagInput(0,materialTag("stone_dusts/"+(dye.isEmpty()?"neutral":dye.toLowerCase(Locale.ROOT))))
                        .withItemTagInput(1,materialTag("silica_dusts"))
                        .withItemTagInput(2,materialTag(large?"clay_dusts":"small_clay_dusts"))
                        .withFluidTagInput(0,materialTag("water"));
                add(MachineRecipeMaps.Mixer,recipe);
            }
        }
    }

    private static ResourceLocation materialTag(String path){return ResourceLocation.fromNamespaceAndPath("gt6ce",path);}

    /** Feed the built-in datapack from the same material groups used by the mixer recipes. */
    public static void foamTags(java.util.function.BiConsumer<String,ItemStack> items,
            java.util.function.BiConsumer<String,FluidStack> fluids){
        for(String[] group:FOAM_ROCKS_BY_DYE)
            for(int rock=1;rock<group.length;rock++) items.accept(materialTag("stone_dusts/"+
                    (group[0].isEmpty()?"neutral":group[0].toLowerCase(Locale.ROOT))).getPath(),dust(group[rock],1));
        for(String sand:FOAM_SANDS) items.accept(materialTag("silica_dusts").getPath(),dust(sand,1));
        for(String clay:FOAM_CLAYS){
            items.accept(materialTag("clay_dusts").getPath(),dust(clay,1));
            items.accept(materialTag("small_clay_dusts").getPath(),GTItems.getStack(MaterialPrefix.dustSmall,GTMaterialRegistry.get(clay),1));
        }
        for(String water:new String[]{"Water","MnWtr","DistW","SpDew"}) fluids.accept(materialTag("water").getPath(),fluid(water,1));
    }

    /**
     * Loader_Recipes_Other:449-462: Bath recolouring. A white construction block takes 1/24 of a dye
     * unit, a slab 1/48, and any panel 1/144; each colour's dye fluids are its water-mixed, flower
     * and chemical dyes plus the source list's extra entries (squid ink for black, indigo and
     * blueberry juice for blue, cranberry juice for red). EU 0, 16 ticks.
     */
    private static void bathDyeing(){
        for(PaintingRules.Dye dye:PaintingRules.DYES){
            var target=net.minecraft.world.item.DyeColor.byName(dye.id(),net.minecraft.world.item.DyeColor.WHITE);
            for(String family:new String[]{"cfoam","asphalt","concrete","concrete_reinforced"}){
                var white=colored(family,net.minecraft.world.item.DyeColor.WHITE,1);
                var result=colored(family,target,1);
                for(var d:dyeGroups(dye.index())) bath(white,null,d,result,24);
            }
            // The port has only the C-Foam slab; asphalt/concrete/reinforced slabs have no identity yet.
            var whiteSlab=colored("cfoam_slab",net.minecraft.world.item.DyeColor.WHITE,1);
            var colouredSlab=colored("cfoam_slab",target,1);
            for(var d:dyeGroups(dye.index())) bath(whiteSlab,null,d,colouredSlab,48);
            for(String kind:new String[]{"concrete","cfoam","asphalt"}){
                var out=item("gt6ce:panel_"+kind+"_"+dye.id(),1);
                var panels=RecipeInputGroups.exactItems("panels/"+kind,PaintingRules.DYES.stream()
                        .map(source->item("gt6ce:panel_"+kind+"_"+source.id(),1)).toList());
                for(var d:dyeGroups(dye.index())) bath(panels.stack(1),panels,d,out,144);
            }
        }
    }
    /** One colour's dye fluids with their unit amounts in mB. */
    private static List<String[]> dyeFluids(int index){
        StringBuilder camel=new StringBuilder();
        for(String part:PaintingRules.DYES.get(index).id().split("_"))
            camel.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        List<String[]> fluids=new ArrayList<>();
        for(String kind:new String[]{"Water","Flower","Chemical"})
            fluids.add(new String[]{"Dye_"+kind+"_"+camel,"144"});
        switch(index){
            case 0 -> fluids.add(new String[]{"InkSquid","144"});
            case 1 -> fluids.add(new String[]{"Juice_Cranberry","250"});
            case 4 -> {fluids.add(new String[]{"Indigo","144"});fluids.add(new String[]{"Juice_Blueberry","250"});}
            default -> {}
        }
        return fluids;
    }
    private record DyeGroup(int unit,RecipeInputGroups.Fluids fluids) {}
    private static final Map<Integer,List<DyeGroup>> DYE_GROUPS=new HashMap<>();
    private static List<DyeGroup> dyeGroups(int index){
        return DYE_GROUPS.computeIfAbsent(index,key->{
            Map<Integer,List<String>> groups=new LinkedHashMap<>();
            for(String[] dye:dyeFluids(key))groups.computeIfAbsent(Integer.parseInt(dye[1]),ignored->new ArrayList<>()).add(dye[0]);
            return groups.entrySet().stream().map(e->new DyeGroup(e.getKey(),RecipeInputGroups.fluids(
                    "dyes/"+PaintingRules.DYES.get(key).id()+"/"+(e.getKey()==144?"standard":"juices"),
                    e.getValue().toArray(String[]::new)))).toList();
        });
    }
    private static void bath(ItemStack input,RecipeInputGroups.Items items,DyeGroup dye,ItemStack output,int divisor){
        int mb=Math.max(1,(dye.unit()+divisor-1)/divisor);
        var recipe=new Recipe(new ItemStack[]{input.copy()},new ItemStack[]{output.copy()},null,null,
                new FluidStack[]{dye.fluids().stack(mb)},null,16,0,0);
        dye.fluids().apply(recipe,0); if(items!=null)items.apply(recipe,0);
        add(MachineRecipeMaps.Bath,recipe);
    }

    /** GT6's rock-to-foam-colour partition; an empty first entry means the foam stays undyed. */
    private static final String[][] FOAM_ROCKS_BY_DYE={
            {"","Stone","Concrete","Asbestos","Dolomite","Talc","Gneiss","Rhyolite","Chert","Shale","OilShale"},
            {"White","Diorite","Marble","Chalk","Calcite","Endstone","Livingrock","Holystone","Castlerock"},
            {"Black","Basalt","Gabbro","GraniteBlack","Deepslate"},
            {"Gray","Migmatite","Eclogite","SpaceStone","Slate","Cragrock"},
            {"LightGray","Andesite","Dacite","Deadrock","Greywacke","MoonStone","MoonTurf"},
            {"LightBlue","Blueschist"},
            {"Lime","Greenschist","Betweenstone","Prismarine"},
            {"Green","Pitstone","PrismarineDark","Mazestone"},
            {"Red","Redrock","MarsStone","MarsSand","Netherrack","GraniteRed","Granite"},
            {"Yellow","Komatiite","Templerock"},
            {"Orange","Limestone"},
            {"Pink","Quartzite","Siltstone"},
            {"Brown","Umber","Kimberlite"}};

    /** GT6 {@code ANY.SiO2.mToThis}, in the order the source group registers them. */
    private static final String[] FOAM_SANDS={
            "SiliconDioxide","Glass","Flint","Sand","RedSand","WhiteEndSand","BlackEndSand",
            "NetherQuartz","CertusQuartz","ChargedCertusQuartz","QuartzBlack","Fluix","SunnyQuartz",
            "LavenderQuartz","RedQuartz","BlazeQuartz","SmokeyQuartz","ManaQuartz","ElvenQuartz",
            "MilkyQuartz","VoidQuartz","Quartzite"};

    /** GT6 {@code ANY.Clay.mToThis}. */
    private static final String[] FOAM_CLAYS={"Clay","ClayBrown","ClayRed","Bentonite","Palygorskite","Kaolinite"};
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
        var stack=gregtech.content.block.misc.ConcreteBlock.coloredItem(block,color);stack.setCount(count);return stack;
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
