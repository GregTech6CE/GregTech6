package gregtech.engine.machine;

import gregtech.content.data.UsbDataCable;
import gregtech.content.data.UsbDataMedia;
import gregtech.content.recipe.GTMaterialDataRecipes;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Recipe lookup for a basic machine, including the printer/replicator USB-cable fallback.
 *
 * <p>The 1.21 node resolves that fallback in the version-specific {@code MachineContextRecipes};
 * 1.20.1 reads the adjacent USB data here. Keeping the fork in one place lets the recipe state
 * machine call a single lookup.</p>
 */
public final class RecipeLookup {
    private RecipeLookup() {}

    /** A match and the fluid side it was found on; output-tank matches drain output tanks. */
    public record Match(Recipe recipe, boolean outputTanks) {}

    @Nullable
    public static Match find(BlockEntity owner, RecipeMap map, MachineInventory inventory,
                             ItemStack autocraftingProgram, boolean searchOutputTanks) {
        if (map == null || inventory == null) return null;
        var items = inventory.items().stacks();
        Recipe direct = map.findRecipe(items, MachineInventory.contents(inventory.tanksInput()), map.mNeedsOutputs,
                map.mInputItemsCount, map.mOutputItemsCount, owner.getLevel(), owner, autocraftingProgram);
        // Items stay on the input slots. Only the fluid list changes for the second search.
        if (direct == null && searchOutputTanks && inventory.tanksOutput().length > 0) {
            Recipe fromOutput = map.findRecipe(items, MachineInventory.contents(inventory.tanksOutput()), map.mNeedsOutputs,
                    map.mInputItemsCount, map.mOutputItemsCount, owner.getLevel(), owner, autocraftingProgram);
            if (fromOutput != null) return new Match(fromOutput, true);
        }
        if (direct != null) return new Match(direct, false);

//? if >=1.21.1 {
        /*return machineContext(owner, map, inventory);
*///?} else {
        return usbPort(owner, map, inventory);
//?}
    }

//? if >=1.21.1 {
    /*@Nullable
    private static Match machineContext(BlockEntity owner, RecipeMap map, MachineInventory inventory) {
        if (!(owner instanceof gregtech.content.blockentity.machine.BasicMachineBlockEntity machine)) return null;
        Recipe found = gregtech.content.recipe.MachineContextRecipes.find(
                machine, map, inventory.inputItems(), inventory.inputFluids());
        return found == null ? null : new Match(found, false);
    }
*///?} else {
    /** GT6's printer and replicator accept a USB cable in place of a retained stick. */
    @Nullable
    private static Match usbPort(BlockEntity owner, RecipeMap map, MachineInventory inventory) {
        if (map != MachineRecipeMaps.Printer && map != MachineRecipeMaps.Replicator) return null;
        int requiredTier = map == MachineRecipeMaps.Printer ? 1 : 3;
        for (int slot = 0; slot < map.mInputItemsCount; slot++) {
            ItemStack cable = inventory.items().getStackInSlot(slot);
            if (UsbDataMedia.cableTier(cable) < requiredTier) continue;
            CompoundTag data = UsbDataCable.readAdjacent(owner, cable, requiredTier);
            if (data == null) continue;
            Recipe fromPort = map == MachineRecipeMaps.Printer
                    ? GTMaterialDataRecipes.printerFromPort(inventory.inputItems(), cable, data)
                    : GTMaterialDataRecipes.replicatorFromPort(cable, data);
            if (fromPort != null) return new Match(fromPort, false);
        }
        return null;
    }
//?}
}
