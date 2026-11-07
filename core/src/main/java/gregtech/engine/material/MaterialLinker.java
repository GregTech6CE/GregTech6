package gregtech.engine.material;

import gregtech.api.addon.material.FuelRule;
import gregtech.api.addon.material.MaterialTarget;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import java.util.List;
import java.util.function.Function;

/** Checks the complete keyed graph before applying any of its associations. */
final class MaterialLinker {
    private MaterialLinker() {}

    static void validate(List<MaterialDefinition> definitions, Function<String, GTMaterial> lookup) {
        for (var definition : definitions) {
            var owner = lookup.apply(definition.name());
            if (owner == null) throw new IllegalArgumentException("Missing definition owner: " + definition.name());
            for (var part : definition.composition().parts()) {
                GTMaterial target = require(definition.name(), "composition", part.material(), lookup);
                if (!target.isValid() || target == owner)
                    throw new IllegalArgumentException(
                            "Invalid composition of " + definition.name() + ": " + part.material());
            }
            for (var name : definition.byproducts()) require(definition.name(), "byproduct", name, lookup);
            for (var name : definition.members()) require(definition.name(), "group member", name, lookup);
            MaterialTarget burning =
                    definition.fuel().flatMap(FuelRule::residue).orElse(null);
            for (var target : new MaterialTarget[] {
                burning, definition.crushing(), definition.smelting(), definition.pulverizing()
            })
                if (target != null)
                    require(definition.name(), "target", target.material().name(), lookup);
            for (var target : definition.processing().values())
                require(definition.name(), "processing", target.material().name(), lookup);
        }
        var groups = new java.util.HashMap<String, List<String>>();
        for (var definition : definitions) if (definition.group()) groups.put(definition.name(), definition.members());
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
        var fuel = definition.fuel();
        material.setFurnaceBurnTime(fuel.map(FuelRule::burnTime).orElse(0L));
        fuel.flatMap(FuelRule::residue)
                .ifPresent(target ->
                        material.setBurning(lookup.apply(target.material().name()), target.amount()));
        if (definition.crushing() != null)
            material.setCrushing(
                    lookup.apply(definition.crushing().material().name()),
                    definition.crushing().amount());
        if (definition.smelting() != null)
            material.setSmelting(
                    lookup.apply(definition.smelting().material().name()),
                    definition.smelting().amount());
        if (definition.pulverizing() != null)
            material.setPulver(
                    lookup.apply(definition.pulverizing().material().name()),
                    definition.pulverizing().amount());
        if (!definition.composition().parts().isEmpty())
            material.setComposition(
                    definition.composition().divider(),
                    definition.composition().parts().stream()
                            .map(part -> MaterialComponent.of(lookup.apply(part.material()), part.amount()))
                            .toArray(MaterialComponent[]::new));
        material.addOreByProducts(definition.byproducts().stream().map(lookup).toArray(GTMaterial[]::new));
        material.addReRegistrationToThis(
                definition.members().stream().map(lookup).toArray(GTMaterial[]::new));
        material.setOreMultiplier(definition.oreMultiplier())
                .setOreProcessingMultiplier(definition.processingMultiplier());
    }
}
