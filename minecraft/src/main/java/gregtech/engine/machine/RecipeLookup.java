package gregtech.engine.machine;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.UsbDataCable;
import gregtech.content.data.UsbDataMedia;
import gregtech.content.recipe.GTMaterialDataRecipes;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Recipe lookup for a basic machine, including the printer/replicator USB-cable fallback.
 */
public final class RecipeLookup {
    private RecipeLookup() {}

    /** A match and the fluid side it was found on; output-tank matches drain output tanks. */
    public record Match(Recipe recipe, boolean outputTanks) {}

    /**
     * Whether a null {@link #find} answer is final while the inputs and the map's lookup revision
     * stay unchanged. Dynamic, contextual, USB-reading and live-predicate maps are never final.
     */
    public static boolean staticAnswerFinal(RecipeMap map) {
        if (map == null || !map.hasOnlyStaticLookup() || map.hasLiveItemPredicates()) return false;
        if (map == MachineRecipeMaps.Printer || map == MachineRecipeMaps.Replicator) return false;
        return true;
    }

    @Nullable
    public static Match find(
            BlockEntity owner,
            RecipeMap map,
            MachineInventory inventory,
            ItemStack autocraftingProgram,
            boolean searchOutputTanks) {
        if (map == null || inventory == null) return null;
        var items = inventory.matchingSlots();
        int inputSlots = map.mInputItemsCount + map.mInputPotionCount;
        Recipe direct = map.findRecipe(
                items,
                MachineInventory.contents(inventory.tanksInput()),
                map.mNeedsOutputs,
                inputSlots,
                map.mOutputItemsCount,
                owner.getLevel(),
                owner,
                autocraftingProgram);
        // Items stay on the input slots. Only the fluid list changes for the second search.
        if (direct == null && searchOutputTanks && inventory.tanksOutput().length > 0) {
            Recipe fromOutput = map.findRecipe(
                    items,
                    MachineInventory.contents(inventory.tanksOutput()),
                    map.mNeedsOutputs,
                    inputSlots,
                    map.mOutputItemsCount,
                    owner.getLevel(),
                    owner,
                    autocraftingProgram);
            if (fromOutput != null) return new Match(fromOutput, true);
        }
        if (direct != null) return new Match(direct, false);
        return usbPort(owner, map, inventory);
    }

    /** GT6's printer and replicator accept a USB cable in place of a retained stick. */
    @Nullable
    private static Match usbPort(BlockEntity owner, RecipeMap map, MachineInventory inventory) {
        if (map != MachineRecipeMaps.Printer && map != MachineRecipeMaps.Replicator) return null;
        int requiredTier = map == MachineRecipeMaps.Printer ? 1 : gregtech.content.data.UsbDataRules.SCANNER_TIER;
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
}
