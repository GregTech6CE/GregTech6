package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.recipe.*;import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;import com.gregtech.gregtech.content.data.*;import com.gregtech.gregtech.data.MachineRecipeMaps;import net.minecraft.world.item.ItemStack;import net.neoforged.neoforge.fluids.FluidStack;import java.util.List;
/** Complete source1 adjacent selected-file cable lookup; platform recipes retain exact catalyst components. */
public final class MachineContextRecipes {
 public interface Resolver {Recipe find(BasicMachineBlockEntity host,RecipeMap map,List<ItemStack> items,List<FluidStack> fluids);}
 private static Resolver resolver;private MachineContextRecipes(){}public static void bind(Resolver value){resolver=java.util.Objects.requireNonNull(value);}
 public static Recipe find(BasicMachineBlockEntity host,RecipeMap map,List<ItemStack> items,List<FluidStack> fluids){if(map==MachineRecipeMaps.Printer||map==MachineRecipeMaps.Replicator){int tier=map==MachineRecipeMaps.Printer?1:UsbDataRules.SCANNER_TIER;for(var cable:items){if(UsbDataMedia.cableTier(cable)<tier)continue;var data=UsbDataCable.readAdjacent(host,cable,tier);if(data==null)continue;var row=map==MachineRecipeMaps.Printer?GTMaterialDataRecipes.printerFromPort(items,cable,data):GTMaterialDataRecipes.replicatorFromPort(cable,data);if(row!=null)return row;}}return resolver==null?null:resolver.find(host,map,items,fluids);}
}
