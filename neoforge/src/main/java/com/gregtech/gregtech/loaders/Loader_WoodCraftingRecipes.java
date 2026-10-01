package com.gregtech.gregtech.loaders;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.ItemStack;
/** Original material/tool/wood spec resolver for the native reloadable recipe pack. */
public final class Loader_WoodCraftingRecipes {
 private Loader_WoodCraftingRecipes(){}
    public static ItemStack block(String kind, String species) {
        WoodSpecies resolved = species(species);
        if (resolved == null) return ItemStack.EMPTY;
        return switch (kind) {
            case "log" -> new ItemStack(GTWoods.log(resolved));
            case "planks" -> new ItemStack(GTWoods.planks(resolved));
            case "beam" -> new ItemStack(GTWoods.beam(resolved));
            default -> ItemStack.EMPTY;
        };
    }

    private static WoodSpecies species(String id) {
        for (WoodSpecies candidate : WoodSpecies.values()) {
            if (candidate.id().equals(id)) return candidate;
        }
        return null;
    }

    public static ItemStack resolve(String spec) {
        int colon = spec.indexOf(':');
        if (colon < 0) return ItemStack.EMPTY;
        String kind = spec.substring(0, colon);
        String argument = spec.substring(colon + 1);
        switch (kind) {
            case "log", "planks", "beam":
                return block(kind, argument);
            case "rod":
            case "rodlong": {
                GTMaterial material = GTMaterialRegistry.get(argument);
                if (material == null || !material.isValid()) return ItemStack.EMPTY;
                return GTItems.getStack(kind.equals("rod") ? MaterialPrefix.stick : MaterialPrefix.stickLong,
                        material, 1);
            }
            case "tool": {
                for (GTToolType type : GTToolType.values()) {
                    if (type.id().equals(argument)) {
                        var item = GTToolItems.get(type);
                        return item == null ? ItemStack.EMPTY : new ItemStack(item);
                    }
                }
                return ItemStack.EMPTY;
            }
            default:
                return ItemStack.EMPTY;
        }
    }
}
