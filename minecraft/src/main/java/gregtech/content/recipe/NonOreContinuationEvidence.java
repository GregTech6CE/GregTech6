package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/** evaluated only after ExternalOreProcessing.rebuild, not during machine lookup.
 * Source property and supported override are distinct facts. No recipe signature inference.
 */
public final class NonOreContinuationEvidence {
    private NonOreContinuationEvidence() {}

    private static final Set<String> PROTECTED = Set.of(
            "oreRaw",
            "crushed",
            "crushedTiny",
            "crushedPurified",
            "crushedPurifiedTiny",
            "crushedCentrifuged",
            "crushedCentrifugedTiny");

    public record Snapshot(Set<GTMaterial> defer) {
        public Snapshot {
            Set<GTMaterial> copy = Collections.newSetFromMap(new IdentityHashMap<>());
            copy.addAll(defer);
            defer = Collections.unmodifiableSet(copy);
        }

        public boolean active(GTMaterial material) {
            return defer.contains(material);
        }
    }

    public static Snapshot snapshot(Collection<GTMaterial> reviewed) {
        Set<GTMaterial> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
        candidates.addAll(reviewed);
        Set<GTMaterial> defer = Collections.newSetFromMap(new IdentityHashMap<>());
        defer.addAll(startupSnapshot(reviewed).defer());
        Set<String> triggers = triggers();
        for (var entry : CraftingMaterialForms.nativeAliases().entrySet()) {
            if (!triggers.contains(entry.getKey().prefix())) continue;
            GTMaterial material =
                    GTMaterialRegistry.get(entry.getKey().material()).resolve();
            if (!candidates.contains(material)) continue;
            MaterialPrefix prefix =
                    gregtech.engine.prefix.PrefixRegistry.byName(entry.getKey().prefix());
            for (Item item : entry.getValue()) if (!canonicalSelf(item, prefix, material)) defer.add(material);
        }
        return new Snapshot(defer);
    }
    /** No tag indexing or mutation: preserve pre-existing explicit/startup UNKNOWN in original loader order. */
    public static Snapshot startupSnapshot(Collection<GTMaterial> reviewed) {
        Set<GTMaterial> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
        candidates.addAll(reviewed);
        Set<GTMaterial> defer = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> triggers = triggers();
        for (var material : candidates) {
            if (ReviewedOreInputProfile.decision(material) != ReviewedOreInputProfile.Evidence.NO) defer.add(material);
            // Explicit startup request is material-wide defer, even when only one intermediate requested.
            if (AddonMaterials.acceptedForms().stream()
                    .anyMatch(f -> f.materialName().equals(material.getName()) && triggers.contains(f.prefixName())))
                defer.add(material);
        }
        for (var material : candidates)
            for (String name : PROTECTED) {
                MaterialPrefix prefix = gregtech.engine.prefix.PrefixRegistry.byName(name);
                var stack = GTItems.getStack(prefix, material, 1);
                if (stack.isEmpty()) continue;
                if (!canonicalSelf(stack.getItem(), prefix, material)) {
                    defer.add(material);
                    continue;
                }
                var data = ItemMaterialRegistry.explicit(stack.getItem()).orElse(null);
                if (data != null
                        && (data.prefix() != prefix
                                || !data.recoverable()
                                || data.components().size() != 1
                                || data.material().resolve() != material
                                || data.amount() != prefix.getMaterialWeight())) defer.add(material);
            }
        // Explicit mixed/ambiguous compositions are conservative UNKNOWN, never source prohibition.
        for (var entry : ItemMaterialRegistry.entries().entrySet()) {
            var data = entry.getValue();
            if (data.prefix() == null || !triggers.contains(data.prefix().getName())) continue;
            for (var part : data.components()) {
                GTMaterial material = part.material().resolve();
                if (!candidates.contains(material)) continue;
                if (!canonicalSelf(entry.getKey(), data.prefix(), material)
                        || !data.recoverable()
                        || data.components().size() != 1
                        || data.amount() != data.prefix().getMaterialWeight()) defer.add(material);
            }
        }
        return new Snapshot(defer);
    }

    private static Set<String> triggers() {
        Set<String> names = new HashSet<>(PROTECTED);
        for (var route : ExternalOreProcessingRules.ROUTES)
            if (PROTECTED.contains(route.output().getName()))
                names.add(route.input().getName());
        return names;
    }

    private static boolean canonicalSelf(Item item, MaterialPrefix prefix, GTMaterial material) {
        if (prefix == null) return false;
        var id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null
                || !id.getNamespace().equals("gt6ce")
                || !(item instanceof MaterialFormItem form)
                || form.getPrefix() != prefix
                || form.getMaterial().resolve() != material) return false;
        var stack = GTItems.getStack(prefix, material, 1);
        return !stack.isEmpty() && stack.getItem() == item;
    }
}
