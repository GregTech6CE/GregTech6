package gregtech.engine.material;

import gregtech.api.addon.material.MaterialProcessKey;
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.material.MaterialToolProfile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Auxiliary declarations associated with a material identity.
 *
 * <p>This is an authoring and installation boundary, not the material's canonical identity.
 * Runtime queries read the frozen per-material snapshot owned by {@link MaterialInfoRegistry}.</p>
 */
public record MaterialInfoDefinition(
        Tooling tooling, Composition composition, Relations relations, Processes processes) {
    public MaterialInfoDefinition {
        Objects.requireNonNull(tooling, "tooling");
        Objects.requireNonNull(composition, "composition");
        Objects.requireNonNull(relations, "relations");
        Objects.requireNonNull(processes, "processes");
    }

    public static MaterialInfoDefinition empty() {
        return new MaterialInfoDefinition(
                Tooling.none(),
                new Composition(0, List.of(), false),
                new Relations(false, Set.of(), Set.of(), List.of()),
                new Processes(Map.of()));
    }

    public MaterialToolProfile toolProfile() {
        return tooling.profile();
    }

    public record Tooling(MaterialToolProfile profile, boolean correctedToolStats) {
        public Tooling {
            Objects.requireNonNull(profile, "profile");
        }

        public static Tooling none() {
            return new Tooling(new MaterialToolProfile(0, 1F, 0, 0), false);
        }
    }

    public record Composition(long divider, List<MaterialInfoDefinition.Part> parts, boolean crucible) {
        public Composition {
            parts = List.copyOf(parts);
            if (divider < 0 || (!parts.isEmpty() && divider == 0))
                throw new IllegalArgumentException("Invalid composition divider");
            long total = 0;
            for (MaterialInfoDefinition.Part part : parts) total = Math.addExact(total, part.amount());
        }

        public Composition(long divider, List<MaterialInfoDefinition.Part> parts) {
            this(divider, parts, false);
        }
    }

    public record Part(String material, long amount) {
        public Part {
            if (material == null
                    || material.isBlank()
                    || !gregtech.api.material.GTMaterial.sanitize(material).equals(material))
                throw new IllegalArgumentException("Expected canonical material key: " + material);
            if (amount <= 0) throw new IllegalArgumentException("Component amount must be positive: " + material);
        }
    }

    public record Relations(boolean group, Set<String> forms, Set<String> classifications, List<String> members) {
        public Relations {
            forms = Set.copyOf(forms);
            classifications = Set.copyOf(classifications);
            members = List.copyOf(members);
        }
    }

    public record Processes(Map<MaterialProcessKey<?>, List<MaterialProcessRule<?>>> rules) {
        public Processes {
            var copied = new LinkedHashMap<MaterialProcessKey<?>, List<MaterialProcessRule<?>>>();
            rules.forEach((key, values) -> {
                if (key.cardinality() == gregtech.api.addon.material.MaterialProcessCardinality.SINGLE
                        && values.size() != 1)
                    throw new IllegalArgumentException("Single-valued process " + key.id()
                            + " must have exactly one rule, found " + values.size());
                for (MaterialProcessRule<?> rule : values)
                    if (!key.type().isInstance(rule.value()))
                        throw new IllegalArgumentException("Process value type mismatch for " + key.id());
                copied.put(key, List.copyOf(values));
            });
            rules = Map.copyOf(copied);
        }

        @SuppressWarnings("unchecked")
        public <T> List<MaterialProcessRule<T>> find(MaterialProcessKey<T> key) {
            return (List<MaterialProcessRule<T>>) (List<?>) rules.getOrDefault(key, List.of());
        }

        public <T> java.util.Optional<MaterialProcessRule<T>> findSingle(MaterialProcessKey<T> key) {
            if (key.cardinality() != gregtech.api.addon.material.MaterialProcessCardinality.SINGLE)
                return java.util.Optional.empty();
            var values = find(key);
            return values.size() == 1 ? java.util.Optional.of(values.get(0)) : java.util.Optional.empty();
        }
    }
}
