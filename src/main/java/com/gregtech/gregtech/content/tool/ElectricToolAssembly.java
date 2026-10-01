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
    DRILL("electric_drill", MaterialPrefix.toolHeadDrill, "fSY", "TXW", "dVZ"),
    CHAINSAW("electric_chainsaw", MaterialPrefix.toolHeadChainsaw, "dAT", "XWX", "XVX"),
    WRENCH("electric_wrench", MaterialPrefix.toolHeadWrench, "dAT", "XWX", "XVX"),
    SCREWDRIVER("electric_screwdriver", MaterialPrefix.toolHeadScrewdriver, "XdA", "TWY", "VYX");

    public final String id;
    private final MaterialPrefix head;
    private final String[] rows;
    ElectricToolAssembly(String id, MaterialPrefix head, String... rows) {
        this.id=id; this.head=head; this.rows=ElectricToolCatalog.get(id).rows().toArray(String[]::new);
    }
    public ElectricToolItem item() {
        return switch(this) {
            case DRILL -> GTElectricItems.ELECTRIC_DRILL.get();
            case CHAINSAW -> GTElectricItems.ELECTRIC_CHAINSAW.get();
            case WRENCH -> GTElectricItems.ELECTRIC_WRENCH.get();
            case SCREWDRIVER -> GTElectricItems.ELECTRIC_SCREWDRIVER.get();
        };
    }
    public static boolean validMaterial(GTMaterial material) {
        return ElectricToolCatalog.validMaterial(material);
    }
    public ToolShapedRecipe recipe(GTMaterial material) {
        return recipe(material,new ItemStack(GTElectricItems.BATTERY_LV.get()),"");
    }
    public ToolShapedRecipe recipe(GTMaterial material,ItemStack battery,String suffix) {
        if(!(battery.getItem() instanceof com.gregtech.gregtech.api.energy.item.IItemEnergy energy))return null;
        if(battery.getItem() instanceof com.gregtech.gregtech.item.ChemicalBatteryItem chemical) {
            if(chemical.spec().tier()!=1)return null;
        } else if(!(battery.getItem() instanceof com.gregtech.gregtech.item.BatteryItem generic)||generic.tier()!=1)return null;
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
            case 'f' -> Ingredient.of(GTToolHelper.displayTool(GTToolType.FILE));
            case 'A' -> form(head,material);
            case 'S' -> form(MaterialPrefix.stick,material);
            case 'T' -> form(MaterialPrefix.screw,material);
            case 'X' -> form(MaterialPrefix.plateCurved,Materials.SteelGalvanized);
            case 'Y' -> form(MaterialPrefix.ring,Materials.SteelGalvanized);
            case 'Z' -> form(MaterialPrefix.plate,Materials.SteelGalvanized);
            case 'V' -> Ingredient.of(GTElectricItems.BATTERY_LV.get());
            case 'W' -> Ingredient.of(Objects.requireNonNull(GTTechnological.get("compact_electric_motor_lv")));
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
            var recipe=spec.recipe(material);
            if(recipe!=null) result.add(recipe);
            for(var entry:GTChemicalBatteries.all())if(entry.get().spec().tier()==1) {
                var chemical=spec.recipe(material,new ItemStack(entry.get()),"/"+entry.getId().getPath());
                if(chemical!=null)result.add(chemical);
            }
        }
        return result;
    }
}
