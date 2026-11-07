package gregtech.engine.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import java.util.List;
import java.util.function.Function;

/** Checks the complete keyed graph before applying any of its associations. */
final class MaterialLinker {
    private MaterialLinker() {}

    static void validate(List<MaterialDefinition> definitions, Function<String, GTMaterial> lookup) {
        for (var definition : definitions) {
            var facts = definition.facts();
            var owner = lookup.apply(definition.name());
            if (owner == null) throw new IllegalArgumentException("Missing definition owner: " + definition.name());
            for (var part : facts.composition()) {
                GTMaterial target = require(definition.name(), "composition", part.material(), lookup);
                if (!target.isValid() || target == owner)
                    throw new IllegalArgumentException(
                            "Invalid composition of " + definition.name() + ": " + part.material());
            }
            for (var name : facts.byproducts()) require(definition.name(), "byproduct", name, lookup);
            for (var name : facts.members()) require(definition.name(), "group member", name, lookup);
            for (var target :
                    new MaterialFacts.Target[] {facts.burning(), facts.crushing(), facts.smelting(), facts.pulverizing()
                    }) if (target != null) require(definition.name(), "target", target.material(), lookup);
            for (var target : facts.processing().values())
                require(definition.name(), "processing", target.material(), lookup);
        }
        var groups = new java.util.HashMap<String, List<String>>();
        for (var definition : definitions)
            if (definition.facts().group())
                groups.put(definition.name(), definition.facts().members());
        var complete = new java.util.HashSet<String>();
        for (var group : groups.keySet()) visit(group, groups, new java.util.HashSet<>(), complete);
    }

    private static void visit(
            String group,
            java.util.Map<String, List<String>> groups,
            java.util.Set<String> path,
            java.util.Set<String> complete) {
        if (complete.contains(group) || !groups.containsKey(group)) return;
        if (!path.add(group)) throw new IllegalArgumentException("Material group cycle: " + group);
        for (var member : groups.get(group)) visit(member, groups, path, complete);
        path.remove(group);
        complete.add(group);
    }

    private static GTMaterial require(String owner, String field, String name, Function<String, GTMaterial> lookup) {
        GTMaterial target = lookup.apply(name);
        if (target == null)
            throw new IllegalArgumentException("Unknown material reference: " + owner + " / " + field + " -> " + name);
        return target;
    }

    static void link(MaterialDefinition definition, GTMaterial material, Function<String, GTMaterial> lookup) {
        var facts = definition.facts();
        material.setFurnaceBurnTime(facts.burnTime());
        if (facts.burning() != null)
            material.setBurning(
                    lookup.apply(facts.burning().material()), facts.burning().amount());
        if (facts.crushing() != null)
            material.setCrushing(
                    lookup.apply(facts.crushing().material()), facts.crushing().amount());
        if (facts.smelting() != null)
            material.setSmelting(
                    lookup.apply(facts.smelting().material()), facts.smelting().amount());
        if (facts.pulverizing() != null)
            material.setPulver(
                    lookup.apply(facts.pulverizing().material()),
                    facts.pulverizing().amount());
        if (!facts.composition().isEmpty())
            material.setComposition(
                    facts.divider(),
                    facts.composition().stream()
                            .map(part -> MaterialComponent.of(lookup.apply(part.material()), part.amount()))
                            .toArray(MaterialComponent[]::new));
        material.addOreByProducts(facts.byproducts().stream().map(lookup).toArray(GTMaterial[]::new));
        material.addReRegistrationToThis(facts.members().stream().map(lookup).toArray(GTMaterial[]::new));
        material.setOreMultiplier(facts.oreMultiplier()).setOreProcessingMultiplier(facts.processingMultiplier());
    }
}
