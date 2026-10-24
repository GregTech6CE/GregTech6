package gregtech.content.recipe;

import gregtech.data.MachineRecipeMaps;
import gregtech.data.MaterialPrefix;
import gregtech.api.material.*;
import gregtech.content.plant.WoodSlabCatalog;
import net.minecraft.world.item.ItemStack;

/** Loader_Recipes_Woods / WoodEntry / BeamEntry defaults. Stripped logs are modern beams. */
public final class VanillaWoodProcessingRecipes extends OriginalRecipeBatch {
    public static final VanillaWoodProcessingRecipes INSTANCE = new VanillaWoodProcessingRecipes();
    private static final String[] WOODS={"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry"};
    /** WoodDictionary replaces the generic vanilla item table before dependent recipes/panels. */
    public static void registerMaterials(){
        for(String wood:WOODS) registerMaterials(wood,"log","wood");
        for(String wood:new String[]{"crimson","warped"}) registerMaterials(wood,"stem","hyphae");
    }
    private static void registerMaterials(String wood,String... types){
        var material=RegisteredWoodSurvivalRecipes.material(wood);
        var bark=GTMaterialRegistry.get("Bark");
        for(String type:types){
            ItemMaterialRegistry.register(mc(wood+"_"+type,1).getItem(),new ItemComposition(null,
                    java.util.List.of(MaterialComponent.of(material,8*GTValues.U),MaterialComponent.of(bark,GTValues.U)),
                    "GT6 WoodEntry (modern wood binding)",true));
            ItemMaterialRegistry.register(mc("stripped_"+wood+"_"+type,1).getItem(),null,material,8*GTValues.U);
        }
        ItemMaterialRegistry.register(mc(wood+"_planks",1).getItem(),MaterialPrefix.plate,material,GTValues.U);
        ItemMaterialRegistry.register(mc(wood+"_stairs",1).getItem(),null,material,3*GTValues.U/4);
        ItemMaterialRegistry.register(mc(wood+"_slab",1).getItem(),null,material,GTValues.U/2);
    }
    public static int register() {
        for(String wood:WOODS) INSTANCE.wood(wood);
        TreatedWoodRecipes.register();
        return INSTANCE.entries().size();
    }
    private static ItemStack mc(String id,int count) { return item("minecraft:"+id,count); }
    private void saw(String input,int ticks,int coolant,ItemStack... outputs) {
        for(var v:SawingCoolants.variants(false))
            add("saw/"+input+"/"+v.field(),MachineRecipeMaps.Cutter,ticks*v.multiplier(),16,items(mc(input,1)),outputs,
                    fluids(fluid(v.field(),coolant*v.multiplier())),null);
    }
    private void wood(String wood) {
        String name=WoodSlabCatalog.material(wood);
        var bark=mat(MaterialPrefix.dust,"Bark",1);
        for(String suffix:new String[]{"log","wood"}) {
            String raw=wood+"_"+suffix, stripped="stripped_"+raw;
            saw(raw,128,4,mc(wood+"_planks",6),bark);
            saw(stripped,128,4,mc(wood+"_planks",7),mat(MaterialPrefix.dust,name,1));
            MachineRecipeMaps.pulverizing(mc(raw,1),RegisteredWoodSurvivalRecipes.pulp(wood,6*GTValues.U),bark,50);
            RegisteredWoodSurvivalRecipes.pulverize(mc(stripped,1),wood,7*GTValues.U);
            for(String water:new String[]{"Water","DistW","SpDew","MnWtr"})
                add("debark/"+raw+"/"+water,MachineRecipeMaps.PressureWasher,64,16,items(mc(raw,1)),
                        items(mc(stripped,1),mat(MaterialPrefix.dust,"Bark",1)),fluids(fluid(water,200)),null);
            for(String input:new String[]{raw,stripped}) {
                add("lathe/"+input,MachineRecipeMaps.Lathe,80,16,items(mc(input,1)),
                        items(mat(MaterialPrefix.stickLong,name,input.equals(raw)?4:5),mat(MaterialPrefix.dust,name,1)),null,null);
                add("coke/"+input,MachineRecipeMaps.CokeOven,3600,0,items(mc(input,1)),items(mc("charcoal",1)),
                        null,fluids(fluid("Oil_Creosote",input.equals(raw)?250:200)));
            }
        }
        saw(wood+"_planks",72,3,mc(wood+"_slab",2));
        saw(wood+"_stairs",72,3,mc(wood+"_slab",1),mat(MaterialPrefix.dustSmall,name,1));
        add("lathe/"+wood+"_planks",MachineRecipeMaps.Lathe,16,16,items(mc(wood+"_planks",1)),items(mat(MaterialPrefix.stick,name,2)),null,null);
        RegisteredWoodSurvivalRecipes.pulverize(mc(wood+"_planks",1),wood,GTValues.U);
        RegisteredWoodSurvivalRecipes.pulverize(mc(wood+"_stairs",1),wood,3*GTValues.U/4);
        RegisteredWoodSurvivalRecipes.pulverize(mc(wood+"_slab",1),wood,GTValues.U/2);
    }
}
