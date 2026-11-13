package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialFactKeys;
import gregtech.content.registry.GTItems;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Exact original GT6_Main / W item-generation exclusions.
 * Recipe-local only: existing registry IDs and logical prefix validity remain unchanged.
 * This is NOT ORES-property evidence; oreRaw, rock and dust are never excluded here.
 */
public final class SourceCrushedFormAdapter {
    private SourceCrushedFormAdapter() {}

    public static boolean sourceExcluded(GTMaterial material, MaterialPrefix prefix) {
        if (material == null || prefix == null) return false;
        material = material.resolve();
        if (!material.isValid() || !GTMaterialRegistry.isCoreMaterial(material) || AddonMaterials.owns(material))
            return false;
        return MaterialFactKeys.value(material, MaterialFactKeys.SOURCE_CRUSHED_EXCLUSIONS)
                .map(rule -> rule.excludes(prefix.getName()))
                .orElse(false);
    }

    /** Conservative builtin ownership check. External/accepted/changed metadata are UNKNOWN.
     * Called only for generated output selection; external mapping selection follows in the caller.
     */
    public static boolean excludedBuiltin(GTMaterial material, MaterialPrefix prefix, ItemStack canonical) {
        if (!sourceExcluded(material, prefix)) return false;
        material = material.resolve();
        final String name = material.getName();
        if (AddonMaterials.acceptedForms().stream()
                .anyMatch(form ->
                        form.materialName().equals(name) && form.prefixName().equals(prefix.getName()))) return false;
        if (canonical == null || canonical.isEmpty()) return true;
        var id = BuiltInRegistries.ITEM.getKey(canonical.getItem());
        if (!id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)
                || !id.getPath().equals(prefix.getItemId(material))) return false;
        if (!(canonical.getItem() instanceof MaterialFormItem form)
                || form.getMaterial().resolve() != material
                || form.getPrefix() != prefix) return false;
        var bound = GTItems.getStack(prefix, material, 1);
        if (bound.isEmpty() || bound.getItem() != canonical.getItem()) return false;
        var explicit = ItemMaterialRegistry.base(canonical.getItem()).orElse(null);
        if (explicit != null
                && (explicit.prefix() != prefix
                        || explicit.components().size() != 1
                        || explicit.material().resolve() != material
                        || explicit.amount() != prefix.getMaterialWeight())) return false;
        return true;
    }
}
