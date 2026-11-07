package gregtech.engine.material;

import gregtech.api.addon.material.FuelRule;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialTarget;
import gregtech.api.addon.material.OreProcessingRule;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Checks the complete keyed graph before applying any of its associations. */
final class MaterialLinker {
    private MaterialLinker() {}

    static void validate(List<MaterialDefinition.Declaration> declarations, Function<String, GTMaterial> lookup) {
        for (var declaration : declarations) {
            var definition = declaration.material();
            var info = declaration.info();
            var owner = lookup.apply(definition.name());
            if (owner == null) throw new IllegalArgumentException("Missing definition owner: " + definition.name());
            for (var part : info.composition().parts()) {
                GTMaterial target = require(definition.name(), "composition", part.material(), lookup);
                if (!target.isValid() || target == owner)
                    throw new IllegalArgumentException(
                            "Invalid composition of " + definition.name() + ": " + part.material());
            }
            for (var name : info.relations().members()) require(definition.name(), "group member", name, lookup);
            MaterialTarget burning =
                    fuel(definition.name()).flatMap(FuelRule::residue).orElse(null);
            for (var target : new MaterialTarget[] {
                burning,
                first(definition.name(), MaterialProcessKeys.CRUSHING),
                first(definition.name(), MaterialProcessKeys.SMELTING),
                first(definition.name(), MaterialProcessKeys.PULVERIZING)
            })
                if (target != null)
                    require(definition.name(), "target", target.material().name(), lookup);
            var oreProcessing = oreProcessing(declaration);
            if (oreProcessing != null)
                for (var byproduct : oreProcessing.byproducts())
                    requireReal(definition.name(), "ore processing byproduct", byproduct.name(), lookup);
            for (var target : processing(definition.name()).values())
                require(definition.name(), "processing", target.material().name(), lookup);
        }
        var groups = new java.util.HashMap<String, List<String>>();
        for (var declaration : declarations)
            if (declaration.info().relations().group())
                groups.put(declaration.name(), declaration.info().relations().members());
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

    private static GTMaterial requireReal(
            String owner, String field, String name, Function<String, GTMaterial> lookup) {
        GTMaterial target = require(owner, field, name, lookup);
        if (!target.isValid())
            throw new IllegalArgumentException("Invalid material reference: " + owner + " / " + field + " -> " + name);
        return target;
    }

    static void link(
            MaterialDefinition.Declaration declaration, GTMaterial material, Function<String, GTMaterial> lookup) {
        MaterialDefinition definition = declaration.material();
        var info = declaration.info();
        var fuel = fuel(definition.name());
        material.setFurnaceBurnTime(fuel.map(FuelRule::burnTime).orElse(0L));
        fuel.flatMap(FuelRule::residue)
                .ifPresent(target ->
                        material.setBurning(lookup.apply(target.material().name()), target.amount()));
        var crushing = first(definition.name(), MaterialProcessKeys.CRUSHING);
        if (crushing != null)
            material.setCrushing(lookup.apply(crushing.material().name()), crushing.amount());
        var smelting = first(definition.name(), MaterialProcessKeys.SMELTING);
        if (smelting != null)
            material.setSmelting(lookup.apply(smelting.material().name()), smelting.amount());
        var pulverizing = first(definition.name(), MaterialProcessKeys.PULVERIZING);
        if (pulverizing != null)
            material.setPulver(lookup.apply(pulverizing.material().name()), pulverizing.amount());
        if (!info.composition().parts().isEmpty())
            material.setComposition(
                    info.composition().divider(),
                    info.composition().parts().stream()
                            .map(part -> MaterialComponent.of(lookup.apply(part.material()), part.amount()))
                            .toArray(MaterialComponent[]::new));
        var oreProcessing = oreProcessing(declaration);
        if (oreProcessing != null)
            material.addOreByProducts(oreProcessing.byproducts().stream()
                    .map(key -> requireReal(definition.name(), "ore processing byproduct", key.name(), lookup))
                    .toArray(GTMaterial[]::new));
        material.addReRegistrationToThis(
                info.relations().members().stream().map(lookup).toArray(GTMaterial[]::new));
        material.setOreMultiplier(oreProcessing == null ? 1 : oreProcessing.oreMultiplier())
                .setOreProcessingMultiplier(oreProcessing == null ? 1 : oreProcessing.processingMultiplier());
    }

    private static Optional<FuelRule> fuel(String name) {
        return Optional.ofNullable(first(name, MaterialProcessKeys.FUEL));
    }

    private static <T> T first(String name, gregtech.api.addon.material.MaterialProcessKey<T> key) {
        var rules = MaterialInfoRegistry.processesBeforeFreeze(name).find(key);
        return rules.isEmpty() ? null : rules.get(0).value();
    }

    private static OreProcessingRule oreProcessing(MaterialDefinition.Declaration declaration) {
        var rules = declaration.info().processes().find(MaterialProcessKeys.ORE_PROCESSING);
        return rules.isEmpty() ? null : rules.get(0).value();
    }

    private static java.util.Map<String, MaterialTarget> processing(String name) {
        var result = new java.util.LinkedHashMap<String, MaterialTarget>();
        var rulesByKey = MaterialInfoRegistry.processesBeforeFreeze(name).rules();
        for (var entry : rulesByKey.entrySet()) {
            String kind = MaterialProcessKeys.legacyTargetName(entry.getKey());
            if (kind.isEmpty()) continue;
            var rules = entry.getValue();
            if (!rules.isEmpty() && rules.get(0).value() instanceof MaterialTarget target) result.put(kind, target);
        }
        return result;
    }
}
