package gregtech.content.recipe;

import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.data.*;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.recipe.*;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Adjacent selected-file lookup for printer and replicator cable recipes; keeps exact catalyst components. */
public final class MachineContextRecipes {
    public interface Resolver {
        Recipe find(BasicMachineBlockEntity host, RecipeMap map, List<ItemStack> items, List<FluidStack> fluids);
    }

    private static Resolver resolver;

    private MachineContextRecipes() {}

    public static void bind(Resolver value) {
        resolver = java.util.Objects.requireNonNull(value);
    }

    /** Whether a bound resolver can answer maps beyond the USB printer/replicator branches. */
    public static boolean hasBoundResolver() {
        return resolver != null;
    }

    public static Recipe find(
            BasicMachineBlockEntity host, RecipeMap map, List<ItemStack> items, List<FluidStack> fluids) {
        if (map == MachineRecipeMaps.Printer || map == MachineRecipeMaps.Replicator) {
            int tier = map == MachineRecipeMaps.Printer ? 1 : UsbDataRules.SCANNER_TIER;
            for (var cable : items) {
                if (UsbDataMedia.cableTier(cable) < tier) continue;
                var data = UsbDataCable.readAdjacent(host, cable, tier);
                if (data == null) continue;
                var row = map == MachineRecipeMaps.Printer
                        ? GTMaterialDataRecipes.printerFromPort(items, cable, data)
                        : GTMaterialDataRecipes.replicatorFromPort(cable, data);
                if (row != null) return row;
            }
        }
        return resolver == null ? null : resolver.find(host, map, items, fluids);
    }
}
