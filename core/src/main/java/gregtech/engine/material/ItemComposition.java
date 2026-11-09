package gregtech.engine.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialSentinels;
import gregtech.content.data.MaterialPrefix;

import java.util.*;
import java.util.stream.Collectors;

/** One immutable effective item composition; stack inspection stays in platform registries. */
public record ItemComposition(
        MaterialPrefix prefix,
        List<MaterialComponent> components,
        GTMaterial primaryMaterial,
        List<ItemCompositionSource> sources,
        boolean bulkRecoveryAllowed) {
    public ItemComposition {
        var merged = new LinkedHashMap<GTMaterial, Long>();
        for (var c : components) {
            if (!c.material().isValid() || c.amount() <= 0)
                throw new IllegalArgumentException("Invalid item material component: " + c);
            merged.merge(c.material().resolve(), c.amount(), Math::addExact);
        }
        sources = List.copyOf(new LinkedHashSet<>(Objects.requireNonNull(sources, "sources")));
        if (sources.isEmpty()) throw new IllegalArgumentException("Item composition needs a source");
        if (primaryMaterial == null) primaryMaterial = MaterialSentinels.INVALID;
        primaryMaterial = primaryMaterial.resolve();
        var normalizedComponents = merged.entrySet().stream()
                .map(e -> MaterialComponent.of(e.getKey(), e.getValue()))
                .toList();
        GTMaterial requiredPrimary = primaryMaterial;
        if (requiredPrimary.isValid()
                && normalizedComponents.stream().noneMatch(c -> c.material().resolve() == requiredPrimary))
            throw new IllegalArgumentException("Primary material is not a composition component: " + primaryMaterial);
        if (!primaryMaterial.isValid() && normalizedComponents.size() == 1)
            primaryMaterial = normalizedComponents.get(0).material().resolve();
        GTMaterial orderingPrimary = primaryMaterial;
        components = normalizedComponents.stream()
                .sorted(Comparator.comparing((MaterialComponent component) ->
                                component.material().resolve() != orderingPrimary)
                        .thenComparing(
                                component -> component.material().resolve().getId())
                        .thenComparing(
                                component -> component.material().resolve().getName()))
                .toList();
    }

    public ItemComposition(MaterialPrefix prefix, GTMaterial material, long amount) {
        this(
                prefix,
                List.of(MaterialComponent.of(material, amount)),
                material,
                List.of(new ItemCompositionSource(
                        ItemCompositionSource.Kind.EXPLICIT,
                        "gt6:item-form/" + material.resolve().getName() + "/"
                                + (prefix == null ? "unprefixed" : prefix.getName()),
                        "explicit item form",
                        "",
                        "")),
                true);
    }

    public ItemComposition(
            MaterialPrefix prefix, List<MaterialComponent> components, String source, boolean bulkRecoveryAllowed) {
        this(
                prefix,
                components,
                MaterialSentinels.INVALID,
                List.of(ItemCompositionSource.fromLegacy(source)),
                bulkRecoveryAllowed);
    }

    public ItemComposition(
            MaterialPrefix prefix,
            List<MaterialComponent> components,
            GTMaterial primaryMaterial,
            String source,
            boolean bulkRecoveryAllowed) {
        this(
                prefix,
                components,
                primaryMaterial,
                List.of(ItemCompositionSource.fromLegacy(source)),
                bulkRecoveryAllowed);
    }

    /** Explicit primary identity; it is never inferred from an arbitrary component after construction. */
    public GTMaterial material() {
        return primaryMaterial;
    }

    public long amount() {
        if (!primaryMaterial.isValid()) return 0;
        return components.stream()
                .filter(component -> component.material().resolve() == primaryMaterial)
                .mapToLong(MaterialComponent::amount)
                .findFirst()
                .orElse(0);
    }

    /** Compatibility accessor for recovery producers migrating in plan 05. */
    public boolean recoverable() {
        return bulkRecoveryAllowed;
    }

    public String source() {
        return sources.stream().map(ItemCompositionSource::description).collect(Collectors.joining("; "));
    }

    public ItemComposition withSources(List<ItemCompositionSource> replacementSources) {
        return new ItemComposition(prefix, components, primaryMaterial, replacementSources, bulkRecoveryAllowed);
    }
}
