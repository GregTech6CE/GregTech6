package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Exact original GT6_Main:145-150 / W:161-166 item-generation exclusions.
 * Recipe-local only: existing registry IDs and logical prefix validity remain unchanged.
 * This is NOT ORES-property evidence; oreRaw, rock and dust are never excluded here.
 */
public final class ReviewedCrushedFormGeneration {
    private ReviewedCrushedFormGeneration() {}

    public static boolean sourceExcluded(GTMaterial material, MaterialPrefix prefix) {
        if (material == null || !excludedPrefix(prefix)) return false;
        material = material.resolve();
        if (!material.isValid() || !GTMaterialRegistry.isCoreMaterial(material) || AddonMaterials.owns(material)) return false;
        String expected = switch (material.getId()) {
            case 2220 -> "Adamantium";
            case 260 -> "Iron";
            case 140 -> "Silicon";
            case 130 -> "Aluminium";
            case 220 -> "Titanium";
            case 740 -> "Tungsten";
            case 90 -> "Fluorine";
            case 730 -> "Tantalum";
            case 410 -> "Niobium";
            case 8317 -> "Dilithium";
            default -> null;
        };
        return expected != null && expected.equals(material.getName());
    }

    public static boolean excludedPrefix(MaterialPrefix prefix) {
        return prefix == MaterialPrefix.crushed || prefix == MaterialPrefix.crushedTiny
                || prefix == MaterialPrefix.crushedPurified || prefix == MaterialPrefix.crushedPurifiedTiny
                || prefix == MaterialPrefix.crushedCentrifuged || prefix == MaterialPrefix.crushedCentrifugedTiny;
    }

    /** Conservative builtin ownership check. External/accepted/changed metadata are UNKNOWN.
     * Called only for generated output selection; external mapping selection follows in the caller.
     */
    public static boolean excludedBuiltin(GTMaterial material, MaterialPrefix prefix, ItemStack canonical) {
        if (!sourceExcluded(material,prefix)) return false;
        material = material.resolve();
        final String name = material.getName();
        if (AddonMaterials.acceptedForms().stream().anyMatch(form -> form.materialName().equals(name)
                && form.prefixName().equals(prefix.getName()))) return false;
        if (canonical == null || canonical.isEmpty()) return true;
        var id = BuiltInRegistries.ITEM.getKey(canonical.getItem());
        if (!id.getNamespace().equals("gt6ce") || !id.getPath().equals(prefix.getItemId(material))) return false;
        if (!(canonical.getItem() instanceof MaterialFormItem form)
                || form.getMaterial().resolve() != material || form.getPrefix() != prefix) return false;
        var bound = GTItems.getStack(prefix,material,1);
        if (bound.isEmpty() || bound.getItem() != canonical.getItem()) return false;
        var explicit = ItemMaterialRegistry.explicit(canonical.getItem()).orElse(null);
        if (explicit != null && (explicit.prefix() != prefix || !explicit.recoverable()
                || explicit.components().size() != 1 || explicit.material().resolve() != material
                || explicit.amount() != prefix.getMaterialWeight())) return false;
        return true;
    }
}
