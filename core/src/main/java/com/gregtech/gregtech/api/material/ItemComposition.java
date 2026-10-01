package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.LinkedHashMap;
import java.util.List;

/** Original immutable item composition model; stack inspection stays in platform registries. */
public record ItemComposition(MaterialPrefix prefix, List<MaterialComponent> components, String source, boolean recoverable) {
        public ItemComposition {
            var merged = new LinkedHashMap<GTMaterial, Long>();
            for (var c : components) {
                if (!c.material().isValid() || c.amount() <= 0) throw new IllegalArgumentException("Invalid item material component: " + c);
                merged.merge(c.material().resolve(), c.amount(), Math::addExact);
            }
            components = merged.entrySet().stream().map(e -> MaterialComponent.of(e.getKey(), e.getValue())).toList();
        }
        public ItemComposition(MaterialPrefix prefix, GTMaterial material, long amount) {
            this(prefix, List.of(MaterialComponent.of(material, amount)), "GT6 unification", true);
        }
        // Compatibility accessors for callers that need the primary material.
        public GTMaterial material() { return components.isEmpty() ? MaterialSentinels.Invalid : components.get(0).material(); }
        public long amount() { return components.isEmpty() ? 0 : components.get(0).amount(); }
    }
