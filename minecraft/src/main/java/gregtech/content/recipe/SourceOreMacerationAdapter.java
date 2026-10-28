package gregtech.content.recipe;

import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Producer-local correction of OPL's native maceration, not RM.pulverizing's external integrations.
 * Accepted startup recipes remain in the existing overlay baseline. The separate non-ore dormant
 * ledger and its later continuation retain their original definitions and ownership.
 */
final class SourceOreMacerationAdapter {
    private SourceOreMacerationAdapter() {}

    private static final List<MaterialPrefix> OUTPUTS =
            List.of(MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72);
    private static final List<MaterialPrefix> INPUTS = SourceOreShreddingRules.shapes().stream()
            .map(SourceOreShreddingRules.Shape::input)
            .toList();

    enum Action {
        SOURCE_ROW,
        SOURCE_NO_ROW,
        DEFERRED_NO_ROW,
        LEGACY_UNSUPPORTED
    }

    record Definition(ItemStack input, ItemStack[] outputs, long duration) {
        Recipe admit() {
            return MachineRecipeMaps.Shredder.addRecipe1(
                    SourceOreShreddingRules.OPTIMIZE, SourceOreShreddingRules.POWER, duration, input, outputs);
        }
    }

    record Outcome(Action action, Definition row, String reason) {}

    private static Outcome none(Action action, String reason) {
        return new Outcome(action, null, reason);
    }

    /** This is a correction boundary, not an ORES condition on the original prefix handler. */
    static boolean supports(GTMaterial material, Snapshot snapshot) {
        if (!snapshot.identity(material)
                || snapshot.legacyContinuation(material)
                || snapshot.mortar(material) == Decision.UNKNOWN
                || snapshot.overridden(material, INPUTS)
                || snapshot.overridden(material, List.of(MaterialPrefix.oreRaw))) return false;
        GTMaterial target = snapshot.target(material);
        return target == null
                || !target.isValid()
                || snapshot.identity(target) && !snapshot.overridden(target, OUTPUTS);
    }

    static Outcome shredder(GTMaterial material, MaterialPrefix prefix, ItemStack input, Snapshot snapshot) {
        var shape = SourceOreShreddingRules.shape(prefix);
        if (shape == null || !supports(material, snapshot))
            return none(Action.LEGACY_UNSUPPORTED, "unreviewed-or-independent-context");
        NativeForm actual = snapshot.direct(prefix, material);
        if (actual.availability() == Availability.UNKNOWN) return none(Action.LEGACY_UNSUPPORTED, "input-provenance");
        if (actual.availability() == Availability.ABSENT || input == null || input.isEmpty())
            return none(Action.DEFERRED_NO_ROW, "required-input-absent");
        if (!ItemStack.matches(input, actual.stack())) return none(Action.LEGACY_UNSUPPORTED, "foreign-selected-input");
        GTMaterial target = snapshot.target(material);
        if (snapshot.anti(material) || target == null || !target.isValid())
            return none(Action.SOURCE_NO_ROW, "source-condition");
        var outputs = new ItemStack[shape.outputs().size()];
        var availability = new ArrayList<Availability>();
        for (int index = 0; index < outputs.length; index++) {
            NativeForm output = snapshot.direct(shape.outputs().get(index), target);
            if (output.availability() == Availability.UNKNOWN)
                return none(Action.LEGACY_UNSUPPORTED, "output-provenance");
            outputs[index] = output.stack();
            availability.add(output.availability());
        }
        Decision mortar = snapshot.mortar(material);
        Availability gate = SourceOreShreddingRules.eligibility(
                shape,
                material.isValid(),
                snapshot.anti(material),
                target.isValid(),
                mortar,
                actual.availability(),
                availability);
        if (gate != Availability.PRESENT) return none(Action.DEFERRED_NO_ROW, "required-output-absent");
        // Only the target material is read. Its conversion AMOUNT and quality never scale these fixed slots.
        long duration = SourceOreShreddingRules.duration(shape, mortar == Decision.YES, snapshot.quality(material));
        return new Outcome(Action.SOURCE_ROW, new Definition(input.copy(), outputs, duration), "verified");
    }

    /** Relinquish only OPL's early rock rows. CrusherFamilyRecipes owns the actual amount-aware row. */
    static boolean retireRock(GTMaterial material, ItemStack input, Snapshot snapshot) {
        if (input == null
                || input.isEmpty()
                || !snapshot.identity(material)
                || snapshot.overridden(material, List.of(MaterialPrefix.rockGt))) return false;
        NativeForm actual = snapshot.direct(MaterialPrefix.rockGt, material);
        if (actual.availability() != Availability.PRESENT || !ItemStack.matches(input, actual.stack())) return false;
        GTMaterial target = snapshot.target(material);
        if (target == null || !target.isValid()) return true;
        if (!snapshot.identity(target) || snapshot.overridden(target, OUTPUTS)) return false;
        for (MaterialPrefix prefix : OUTPUTS)
            if (snapshot.direct(prefix, target).availability() == Availability.UNKNOWN) return false;
        return true;
    }

    /** One startup observation: never reinterpret generation exclusions as missing registered inputs. */
    static class Snapshot {
        private final Map<GTMaterial, Set<MaterialPrefix>> overrides = new IdentityHashMap<>();

        Snapshot() {
            for (var accepted : AddonMaterials.acceptedForms()) {
                GTMaterial material =
                        GTMaterialRegistry.get(accepted.materialName()).resolve();
                var prefix = gregtech.engine.prefix.PrefixRegistry.byName(accepted.prefixName());
                if (prefix != null) mark(material, prefix);
            }
            for (var entry : ItemMaterialRegistry.entries().entrySet()) {
                var data = entry.getValue();
                if (data.prefix() == null) continue;
                String namespace = BuiltInRegistries.ITEM.getKey(entry.getKey()).getNamespace();
                if (namespace.equals("minecraft")) continue; // Established vanilla aliases are not new integrations.
                for (var part : data.components()) {
                    GTMaterial material = part.material().resolve();
                    if (!namespace.equals("gt6ce")
                            || !data.recoverable()
                            || data.components().size() != 1
                            || data.amount() != data.prefix().getMaterialWeight()
                            || !SourceFurnaceNativeAdapter.nativeForm(
                                    data.prefix(), material, new ItemStack(entry.getKey())))
                        mark(material, data.prefix());
                }
            }
        }

        private void mark(GTMaterial material, MaterialPrefix prefix) {
            overrides
                    .computeIfAbsent(material, unused -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(prefix);
        }

        boolean overridden(GTMaterial material, Collection<MaterialPrefix> prefixes) {
            return prefixes.stream().anyMatch(overrides.getOrDefault(material.resolve(), Set.of())::contains);
        }

        boolean identity(GTMaterial material) {
            if (material == null || !material.isValid()) return false;
            var resolved = material.resolve();
            return GTMaterialRegistry.isCoreMaterial(resolved)
                    && !AddonMaterials.owns(resolved)
                    && SourceRecyclingFacts.find(resolved.getId(), resolved.getName()) != null;
        }

        boolean legacyContinuation(GTMaterial material) {
            // Preserve all existing dormant/startup/late-activation choices, not just today's inactive subset.
            return ReviewedOreInputProfile.decision(material) == ReviewedOreInputProfile.Evidence.NO;
        }

        Decision mortar(GTMaterial material) {
            return SourceOreMacerationFacts.mortar(material.getId(), material.getName());
        }

        GTMaterial target(GTMaterial material) {
            var target = material.getTargetPulverMaterial();
            return target == null ? null : target.resolve();
        }

        int quality(GTMaterial material) {
            return material.getToolQuality();
        }

        boolean anti(GTMaterial material) {
            return material.has(MaterialProperty.ANTIMATTER);
        }

        NativeForm direct(MaterialPrefix prefix, GTMaterial material) {
            if (material == null || !material.isValid()) return NativeForm.absent();
            ItemStack selected = GTItems.getStack(prefix, material.resolve(), 1);
            if (selected.isEmpty()) return NativeForm.absent();
            if (!SourceFurnaceNativeAdapter.nativeForm(prefix, material, selected)) return NativeForm.unknown();
            var data = ItemMaterialRegistry.explicit(selected.getItem()).orElse(null);
            if (data == null
                    || !data.recoverable()
                    || data.prefix() != prefix
                    || data.components().size() != 1
                    || data.material().resolve() != material.resolve()
                    || data.amount() != prefix.getMaterialWeight()) return NativeForm.unknown();
            return NativeForm.present(selected);
        }
    }
}
