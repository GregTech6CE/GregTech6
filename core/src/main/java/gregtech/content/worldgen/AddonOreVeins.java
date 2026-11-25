package gregtech.content.worldgen;

import gregtech.api.addon.worldgen.HeightCoordinates;
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.addon.worldgen.SmallOreDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.worldgen.GTOreVeins.OreVein;
import gregtech.content.worldgen.GTOreVeins.SmallOre;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Addon large veins and small ores, appended after the built-in tables. */
public final class AddonOreVeins {
    public record Submission(String addonId, List<OreVeinDeclaration> veins, List<SmallOreDeclaration> smallOres) {}

    private record Key(String dimension, String kind, String name) {}

    private AddonOreVeins() {}

    public static void publish(
            List<Submission> submissions, Function<String, GTMaterial> lookup, Set<GTMaterial> declaredOreForms) {
        var materials = new OreMaterials(lookup, declaredOreForms);
        var taken = new HashMap<Key, String>();
        claimBuiltIn(taken);
        var veinBuckets = new LinkedHashMap<String, List<OreVein>>();
        var smallBuckets = new LinkedHashMap<String, List<SmallOre>>();
        for (Submission submission : submissions) {
            for (OreVeinDeclaration vein : submission.veins) {
                claim(taken, submission.addonId, vein.dimension(), "large", vein.name());
                OreVein resolved = new OreVein(
                        vein.name(),
                        vein.minY(),
                        vein.maxY(),
                        vein.weight(),
                        vein.density(),
                        vein.size(),
                        materials.resolve(submission.addonId, vein.name(), vein.top()),
                        materials.resolve(submission.addonId, vein.name(), vein.bottom()),
                        materials.resolve(submission.addonId, vein.name(), vein.between()),
                        materials.resolve(submission.addonId, vein.name(), vein.spread()),
                        vein.coordinates());
                veinBuckets
                        .computeIfAbsent(vein.dimension(), key -> new ArrayList<>())
                        .add(resolved);
            }
            for (SmallOreDeclaration ore : submission.smallOres) {
                claim(taken, submission.addonId, ore.dimension(), "small", ore.name());
                SmallOre resolved = new SmallOre(
                        ore.name(),
                        ore.minY(),
                        ore.maxY(),
                        ore.amount(),
                        materials.resolve(submission.addonId, ore.name(), ore.material()),
                        ore.coordinates() == HeightCoordinates.LEGACY
                                ? GTOreVeins.Distribution.LEGACY
                                : GTOreVeins.Distribution.UNIFORM);
                smallBuckets
                        .computeIfAbsent(ore.dimension(), key -> new ArrayList<>())
                        .add(resolved);
            }
        }
        for (var entry : veinBuckets.entrySet()) {
            fit(entry.getKey(), entry.getValue());
        }
        OreSourceCatalog.publish(veinBuckets, smallBuckets);
    }

    private static void claimBuiltIn(Map<Key, String> taken) {
        for (OreVein vein : GTOreVeins.OVERWORLD_VEINS)
            taken.put(new Key(OreVeinDeclaration.OVERWORLD, "large", vein.name()), "a built-in vein");
        for (OreVein vein : GTOreVeins.END_VEINS)
            taken.put(new Key(OreVeinDeclaration.END, "large", vein.name()), "a built-in vein");
        for (SmallOre ore : GTOreVeins.OVERWORLD_SMALL_ORES)
            taken.put(new Key(SmallOreDeclaration.OVERWORLD, "small", ore.name()), "a built-in vein");
        for (SmallOre ore : GTOreVeins.NETHER_SMALL_ORES)
            taken.put(new Key(SmallOreDeclaration.NETHER, "small", ore.name()), "a built-in vein");
        for (SmallOre ore : GTOreVeins.END_SMALL_ORES)
            taken.put(new Key(SmallOreDeclaration.END, "small", ore.name()), "a built-in vein");
    }

    private static void claim(Map<Key, String> taken, String addonId, String dimension, String kind, String name) {
        String previous = taken.putIfAbsent(new Key(dimension, kind, name), "addon '" + addonId + "'");
        if (previous == null) return;
        String label = "large".equals(kind) ? "ore vein" : "small ore";
        throw new IllegalStateException(
                "Addon '" + addonId + "' " + label + " '" + name + "' for " + dimension + " is already " + previous);
    }

    /** Built-in entries of the dimension count toward its weight total; other dimensions are addon-only. */
    private static void fit(String dimension, List<OreVein> added) {
        long sum;
        if (OreVeinDeclaration.OVERWORLD.equals(dimension)) sum = weight(GTOreVeins.OVERWORLD_VEINS, 0);
        else if (OreVeinDeclaration.END.equals(dimension))
            sum = weight(GTOreVeins.END_VEINS, GTOreVeins.END_EMPTY_VEIN_WEIGHT);
        else sum = 0;
        for (OreVein vein : added) sum = Math.addExact(sum, vein.weight());
        if (sum > Integer.MAX_VALUE) throw new IllegalStateException("Ore vein weights do not fit in an int");
    }

    private static long weight(List<OreVein> veins, int emptyWeight) {
        long sum = emptyWeight;
        for (OreVein vein : veins) sum = Math.addExact(sum, vein.weight());
        return sum;
    }

    private record OreMaterials(Function<String, GTMaterial> lookup, Set<GTMaterial> declaredOreForms) {
        private GTMaterial resolve(String addonId, String vein, String name) {
            GTMaterial found = lookup.apply(name);
            if (!found.isValid())
                throw new IllegalStateException(
                        "Addon '" + addonId + "' ore vein '" + vein + "' uses unknown material '" + name + "'");
            GTMaterial material = found.resolve();
            if (material.has(MaterialProperty.HIDDEN))
                throw new IllegalStateException("Addon '" + addonId + "' ore vein '" + vein
                        + "' cannot use hidden material '" + material.getName() + "'");
            if (declaredOreForms.contains(material)
                    || material.has(MaterialProperty.GENERATE_ORE)
                    || OreSourceCatalog.hasBuiltinOreForm(material)) return material;
            throw new IllegalStateException(
                    "Addon '" + addonId + "' ore vein '" + vein + "' material '" + material.getName() + "' has no ore");
        }
    }
}
