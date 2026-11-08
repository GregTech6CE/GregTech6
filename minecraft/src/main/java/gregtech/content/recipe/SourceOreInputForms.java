package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.material.MaterialFormItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Recipe-local guard for reviewed source NO; does not unregister legacy items or alter target links.
 * Evidence must come from a separately reviewed ID+canonical-name profile, not missing MaterialForms.
 * External ore adapters and explicit recipes stay independent. Not wired into frozen stage1.
 */
public final class SourceOreInputForms {
    public enum Evidence {
        YES,
        NO,
        UNKNOWN
    }

    private SourceOreInputForms() {}

    private record ExternalForm(MaterialPrefix prefix, GTMaterial material) {}

    /** Non-GT items exposing one material form, rebuilt when item compositions change. */
    private static final Map<ExternalForm, List<Item>> EXTERNAL_FORMS = new HashMap<>();

    private static long externalFormsRevision = -1;

    private static synchronized List<Item> externalForms(MaterialPrefix prefix, GTMaterial material) {
        long revision = ItemMaterialRegistry.compositionRevision();
        if (revision != externalFormsRevision) {
            EXTERNAL_FORMS.clear();
            for (Item item : ItemMaterialRegistry.entries().keySet()) {
                if (BuiltInRegistries.ITEM
                        .getKey(item)
                        .getNamespace()
                        .equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) continue;
                var form = MaterialEquivalence.form(new ItemStack(item, 1));
                if (form == null) continue;
                EXTERNAL_FORMS
                        .computeIfAbsent(new ExternalForm(form.prefix(), form.material()), ignored -> new ArrayList<>())
                        .add(item);
            }
            externalFormsRevision = revision;
        }
        return EXTERNAL_FORMS.getOrDefault(new ExternalForm(prefix, material), List.of());
    }

    public static boolean allowGeneratedInput(
            GTMaterial material, MaterialPrefix prefix, ItemStack input, Evidence evidence) {
        if (input == null || input.isEmpty()) return false;
        if (evidence != Evidence.NO || !oreOnly(prefix)) return true;
        material = material.resolve();
        if (AddonMaterials.owns(material) || !GTMaterialRegistry.isCoreMaterial(material)) return true;
        // A real externally registered ore input remains admissible despite builtin ORES_NO.
        if (!BuiltInRegistries.ITEM
                .getKey(input.getItem())
                .getNamespace()
                .equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) return true;
        if (!(input.getItem() instanceof MaterialFormItem form)
                || form.getPrefix() != prefix
                || form.getMaterial().resolve() != material) return true; // uncertain provenance retains legacy
        String name = material.getName();
        if (AddonMaterials.acceptedForms().stream()
                .anyMatch(f -> f.materialName().equals(name) && f.prefixName().equals(prefix.getName()))) return true;
        return false; // only a manufactured builtin legacy ore input lacking source generation evidence
    }
    /** Source MT Stone8500 -> stone factory G_STONE, without ORES. Missing profile rows are UNKNOWN.
     * Applies only to generated recipe outputs; legitimate explicit external forms remain authoritative. */
    public static ItemStack reviewedOreOutput(MaterialPrefix prefix, GTMaterial material, int count) {
        material = material.resolve();
        ItemStack canonical = gregtech.content.registry.GTItems.getStack(prefix, material, count);
        boolean excluded = SourceOreOutputRows.resolving()
                && ReviewedCrushedFormGeneration.excludedBuiltin(material, prefix, canonical);
        Evidence evidence = (material.getId() == 8500 && material.getName().equals("Stone")) || excluded
                ? Evidence.NO
                : Evidence.UNKNOWN;
        if (evidence != Evidence.NO || AddonMaterials.owns(material) || !GTMaterialRegistry.isCoreMaterial(material))
            return canonical;
        String name = material.getName();
        if (AddonMaterials.acceptedForms().stream()
                .anyMatch(f -> f.materialName().equals(name) && f.prefixName().equals(prefix.getName())))
            return canonical;
        if (!canonical.isEmpty()
                && !BuiltInRegistries.ITEM
                        .getKey(canonical.getItem())
                        .getNamespace()
                        .equals("gt6ce")) return canonical;
        ItemStack external = ItemStack.EMPTY;
        boolean unsupportedFine = false;
        for (var item : externalForms(prefix, material)) {
            ItemStack stack = new ItemStack(item, count);
            if (excluded && !usableFine(canonical, stack)) {
                unsupportedFine = true;
                continue;
            }
            // Ambiguous externally owned targets are UNKNOWN; do not replace their existing selection.
            if (!external.isEmpty() && external.getItem() != item) return canonical;
            external = stack;
        }
        if (excluded) {
            // This branch runs only inside the after-tag ledger admission, never during initial OPL generation.
            for (var entry : gregtech.content.recipe.CraftingMaterialForms.nativeAliases()
                    .entrySet()) {
                if (!entry.getKey().prefix().equals(prefix.getName())
                        || !entry.getKey().material().equals(name)) continue;
                for (var item : entry.getValue()) {
                    if (!canonical.isEmpty() && item == canonical.getItem()) continue;
                    var stack = new ItemStack(item, count);
                    // A display/tag alias is not necessarily a machine-usable form. Ignore unsupported
                    // aliases before ambiguity checks so they cannot overrule a usable explicit mapping.
                    if (!usableFine(canonical, stack)) {
                        unsupportedFine = true;
                        continue;
                    }
                    var explicit = gregtech.engine.material.ItemMaterialRegistry.explicit(item)
                            .orElse(null);
                    if (explicit != null
                            && (explicit.prefix() != prefix
                                    || explicit.components().size() != 1
                                    || explicit.material().resolve() != material
                                    || explicit.amount() != prefix.getMaterialWeight())) return canonical;
                    if (explicit == null && !(item instanceof MaterialFormItem)) {
                        var tagged = gregtech.content.recipe.CraftingMaterialForms.taggedForms(stack);
                        if (tagged.size() != 1
                                || !tagged.get(0).prefix().equals(prefix.getName())
                                || !tagged.get(0).material().equals(name)) return canonical;
                    }
                    if (!external.isEmpty() && external.getItem() != item)
                        return canonical; // ambiguous binding stays UNKNOWN
                    external = stack;
                }
            }
        }
        if (!external.isEmpty()) return external;
        if (excluded && unsupportedFine)
            return canonical; // configured but unusable alias is UNKNOWN, not an emitted foreign item
        return allowGeneratedInput(material, prefix, canonical, evidence) ? canonical : ItemStack.EMPTY;
    }
    /** New source-fine policy only: do not emit an external form native machines cannot consume. */
    private static boolean usableFine(ItemStack canonical, ItemStack candidate) {
        return !canonical.isEmpty() && gregtech.engine.recipe.RecipeInputs.matches(canonical, candidate);
    }

    private static boolean oreOnly(MaterialPrefix prefix) {
        return prefix == MaterialPrefix.oreRaw
                || prefix == MaterialPrefix.crushed
                || prefix == MaterialPrefix.crushedTiny
                || prefix == MaterialPrefix.crushedPurified
                || prefix == MaterialPrefix.crushedPurifiedTiny
                || prefix == MaterialPrefix.crushedCentrifuged
                || prefix == MaterialPrefix.crushedCentrifugedTiny;
    }
}
