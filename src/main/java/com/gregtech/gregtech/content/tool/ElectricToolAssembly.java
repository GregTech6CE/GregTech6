package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.ElectricToolItem;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import java.util.*;

/** GT6 Loader_Tools:354-359, concrete material/battery rows, like OreProcessing_Tool.
 * LV Drill deliberately uses a rod, unlike the separate Mining Drill's tool head. */
public enum ElectricToolAssembly {
    DRILL("electric_drill"),
    CHAINSAW("electric_chainsaw"),
    WRENCH("electric_wrench"),
    SCREWDRIVER("electric_screwdriver"),
    MINING_DRILL("electric_mining_drill"),
    MIXER("electric_mixer"),
    BUZZSAW("electric_buzzsaw"),
    TRIMMER("electric_trimmer"),
    WRENCH_MV("electric_wrench_mv"),
    MINING_DRILL_MV("electric_mining_drill_mv"),
    CHAINSAW_MV("electric_chainsaw_mv"),
    WRENCH_HV("electric_wrench_hv"),
    MINING_DRILL_HV("electric_mining_drill_hv"),
    CHAINSAW_HV("electric_chainsaw_hv");

    public final String id;
    private final MaterialPrefix head;
    private final String[] rows;
    ElectricToolAssembly(String id) {
        this.id=id;var spec=ElectricToolCatalog.get(id);
        this.head=com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(spec.headPrefix());
        this.rows=spec.rows().toArray(String[]::new);
    }
    public ElectricToolCatalog.Definition definition(){return ElectricToolCatalog.get(id);}
    public ElectricToolItem item(){return GTElectricItems.get(id);}
    public static boolean validMaterial(GTMaterial material) {
        return ElectricToolCatalog.validMaterial(material);
    }
    public ToolShapedRecipe recipe(GTMaterial material) {
        var battery=GTChemicalBatteries.item(com.gregtech.gregtech.content.energy.ChemicalBatterySpec.Chemistry.NICKEL_CADMIUM,definition().tier());
        return recipe(material,new ItemStack(battery),"/"+battery.spec().id());
    }
    public ToolShapedRecipe recipe(GTMaterial material,ItemStack battery,String suffix) {
        if(!(battery.getItem() instanceof com.gregtech.gregtech.item.ChemicalBatteryItem energy)
                ||energy.spec().tier()!=definition().tier())return null;
        if (!validMaterial(material) || GTItems.getStack(head,material,1).isEmpty()) return null;
        var ingredients=NonNullList.withSize(9,Ingredient.EMPTY);
        for(int i=0;i<9;i++) {
            char symbol=rows[i/3].charAt(i%3);
            var ingredient=symbol=='V'?Ingredient.of(battery):ingredient(symbol,material);
            if(ingredient.isEmpty()) return null; // Never turn a missing component into an empty slot.
            ingredients.set(i,ingredient);
        }
        long capacity=energy.getEnergyCapacity(battery,
                com.gregtech.gregtech.data.GregTechTags.Energy.EU);
        var result=item().assembled(material,capacity);
        var key=GregTech.id("electric_tools/"+id+"/"+material.getName().toLowerCase(Locale.ROOT)+suffix);
        return new ToolShapedRecipe(new ShapedRecipe(key,"gregtech.electric_tools",
                CraftingBookCategory.EQUIPMENT,3,3,ingredients,result),false);
    }
    private Ingredient ingredient(char symbol,GTMaterial material) {
        return switch(symbol) {
            case 'd' -> Ingredient.of(GTToolHelper.displayTool(GTToolType.SCREWDRIVER));
            case 'h' -> Ingredient.of(GTToolHelper.displayTool(GTToolType.HARD_HAMMER));
            case 'f' -> Ingredient.of(GTToolHelper.displayTool(GTToolType.FILE));
            case 'A' -> form(head,material);
            case 'S' -> form(MaterialPrefix.stick,material);
            case 'T' -> form(MaterialPrefix.screw,material);
            case 'X' -> form(MaterialPrefix.plateCurved,GTMaterialRegistry.get(definition().chassisMaterial()));
            case 'Y' -> form(id.equals("electric_buzzsaw")?MaterialPrefix.plate:MaterialPrefix.ring,GTMaterialRegistry.get(definition().chassisMaterial()));
            case 'Z' -> form(id.equals("electric_trimmer")?MaterialPrefix.stickLong:MaterialPrefix.plate,GTMaterialRegistry.get(definition().chassisMaterial()));
            case 'W' -> Ingredient.of(Objects.requireNonNull(GTTechnological.get("compact_electric_"+(id.equals("electric_trimmer")?"piston_":"motor_")+definition().tierName())));
            default -> Ingredient.EMPTY;
        };
    }
    private static Ingredient form(MaterialPrefix prefix,GTMaterial material) {
        var stack=GTItems.getStack(prefix,material,1);
        if(stack.isEmpty()) return Ingredient.EMPTY;
        // Include the concrete item and common Forge material forms (rods/screws/rings/plates).
        String common=MaterialEquivalence.tagPath(new MaterialEquivalence.Form(prefix,material));
        return common==null ? Ingredient.of(stack) : Ingredient.merge(List.of(Ingredient.of(stack),
                Ingredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge",common)))));
    }
    public static List<ToolShapedRecipe> build() {
        List<ToolShapedRecipe> result=new ArrayList<>();
        for(var material:GTMaterialRegistry.allMaterials()) for(var spec:values()) {
            for(var entry:GTChemicalBatteries.all())if(entry.get().spec().tier()==spec.definition().tier()) {
                var chemical=spec.recipe(material,new ItemStack(entry.get()),"/"+entry.getId().getPath());
                if(chemical!=null)result.add(chemical);
            }
        }
        return result;
    }
}
