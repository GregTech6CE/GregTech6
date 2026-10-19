package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.util.OM;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Physical forms make crucible recipe inputs discoverable from ordinary material items. */
public final class CrucibleDisplayItems {
    private CrucibleDisplayItems() {}

    public static ItemStack units(GTMaterial material, int units) {
        ItemStack physical = OM.ingotOrDust(material, Math.multiplyExact(GTValues.U, units));
        // Gases and abstract materials have no physical ingot/dust form.
        return physical.isEmpty() ? GTItems.getStack(MaterialPrefix.unit, material, units) : physical;
    }

    /** Search-only links; never offer informational unit items as usable machine ingredients. */
    public static List<ItemStack> aliases(ItemStack physical) {
        var form = com.gregtech.gregtech.api.material.MaterialEquivalence.form(physical);
        if (form == null) return List.of();
        var unit = GTItems.getStack(MaterialPrefix.unit, form.material(), 1);
        if (unit.isEmpty()) return List.of();
        var result = new ArrayList<ItemStack>();
        result.add(unit);
        result.addAll(com.gregtech.gregtech.api.material.MaterialDisplayBinding.alternatives(unit));
        return List.copyOf(result);
    }

    public static boolean isCrucible(com.gregtech.gregtech.api.recipe.RecipeMap map) {
        return map == com.gregtech.gregtech.data.MachineRecipeMaps.CrucibleAlloying
                || map == com.gregtech.gregtech.data.MachineRecipeMaps.CrucibleSmelting;
    }
}
