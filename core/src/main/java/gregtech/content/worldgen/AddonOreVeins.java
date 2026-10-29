package gregtech.content.worldgen;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.addon.worldgen.SmallOreDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.worldgen.GTOreVeins.OreVein;
import gregtech.content.worldgen.GTOreVeins.SmallOre;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Addon large veins and small ores, appended after the built-in tables. */
public final class AddonOreVeins {
    public record Submission(String addonId, List<OreVeinDeclaration> veins, List<SmallOreDeclaration> smallOres) {}

    private record Key(String dimension, String kind, String name) {}

    private static volatile Map<String, List<OreVein>> veinsByDimension = Map.of();
    private static volatile Map<String, List<SmallOre>> smallOresByDimension = Map.of();
    private static Set<GTMaterial> builtinOres;

    private AddonOreVeins() {}

    /** Addon large veins declared for {@code dimension}. */
    public static List<OreVein> veins(String dimension) {
        return veinsByDimension.getOrDefault(dimension, List.of());
    }

    /** Addon small ores declared for {@code dimension}. */
    public static List<SmallOre> smallOres(String dimension) {
        return smallOresByDimension.getOrDefault(dimension, List.of());
    }

    /** Dimensions with at least one addon large vein. */
    public static Set<String> veinDimensions() {
        return veinsByDimension.keySet();
    }

    /** Dimensions with at least one addon small ore. */
    public static Set<String> smallOreDimensions() {
        return smallOresByDimension.keySet();
    }

    public static List<OreVein> overworldVeins() {
        return veins(OreVeinDeclaration.OVERWORLD);
    }

    public static List<OreVein> endVeins() {
        return veins(OreVeinDeclaration.END);
    }

    public static List<SmallOre> overworldSmall() {
        return smallOres(SmallOreDeclaration.OVERWORLD);
    }

    public static List<SmallOre> netherSmall() {
        return smallOres(SmallOreDeclaration.NETHER);
    }

    public static List<SmallOre> endSmall() {
        return smallOres(SmallOreDeclaration.END);
    }

    public static void publish(List<Submission> submissions) {
        var taken = new HashMap<Key, String>();
        claimBuiltIn(taken);
        var veinBuckets = new HashMap<String, List<OreVein>>();
        var smallBuckets = new HashMap<String, List<SmallOre>>();
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
                        ore(submission.addonId, vein.name(), vein.top()),
                        ore(submission.addonId, vein.name(), vein.bottom()),
                        ore(submission.addonId, vein.name(), vein.between()),
                        ore(submission.addonId, vein.name(), vein.spread()));
                veinBuckets.computeIfAbsent(vein.dimension(), key -> new ArrayList<>()).add(resolved);
            }
            for (SmallOreDeclaration ore : submission.smallOres) {
                claim(taken, submission.addonId, ore.dimension(), "small", ore.name());
                SmallOre resolved = new SmallOre(
                        ore.name(),
                        ore.minY(),
                        ore.maxY(),
                        ore.amount(),
                        ore(submission.addonId, ore.name(), ore.material()));
                smallBuckets.computeIfAbsent(ore.dimension(), key -> new ArrayList<>()).add(resolved);
            }
        }
        var veins = new HashMap<String, List<OreVein>>();
        for (var entry : veinBuckets.entrySet()) {
            fit(entry.getKey(), entry.getValue());
            veins.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        var smallOres = new HashMap<String, List<SmallOre>>();
        for (var entry : smallBuckets.entrySet()) smallOres.put(entry.getKey(), List.copyOf(entry.getValue()));
        veinsByDimension = Map.copyOf(veins);
        smallOresByDimension = Map.copyOf(smallOres);
    }

    private static void claimBuiltIn(java.util.Map<Key, String> taken) {
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

    private static void claim(
            java.util.Map<Key, String> taken, String addonId, String dimension, String kind, String name) {
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

    private static GTMaterial ore(String addonId, String vein, String name) {
        GTMaterial found = GTMaterialRegistry.get(name);
        if (!found.isValid())
            throw new IllegalStateException(
                    "Addon '" + addonId + "' ore vein '" + vein + "' uses unknown material '" + name + "'");
        GTMaterial material = found.resolve();
        if (material.has(MaterialProperty.HIDDEN))
            throw new IllegalStateException("Addon '" + addonId + "' ore vein '" + vein
                    + "' cannot use hidden material '" + material.getName() + "'");
        if (AddonMaterials.hasOreForm(material) || material.has(MaterialProperty.GENERATE_ORE) || builtinOre(material))
            return material;
        throw new IllegalStateException(
                "Addon '" + addonId + "' ore vein '" + vein + "' material '" + material.getName() + "' has no ore");
    }

    private static boolean builtinOre(GTMaterial material) {
        if (builtinOres == null) builtinOres = builtinOreMaterials();
        return builtinOres.contains(material);
    }

    /** Materials {@link GTWorldgenMaterials#flagOreMaterials()} will give an ore block. */
    private static Set<GTMaterial> builtinOreMaterials() {
        Set<GTMaterial> materials = Collections.newSetFromMap(new IdentityHashMap<>());
        for (OreVein vein : GTOreVeins.OVERWORLD_VEINS) addVein(materials, vein);
        for (OreVein vein : GTOreVeins.END_VEINS) addVein(materials, vein);
        for (SmallOre ore : GTOreVeins.OVERWORLD_SMALL_ORES) add(materials, ore.material());
        for (SmallOre ore : GTOreVeins.NETHER_SMALL_ORES) add(materials, ore.material());
        for (SmallOre ore : GTOreVeins.END_SMALL_ORES) add(materials, ore.material());
        for (GTBedrockOres.BedrockOre ore : GTBedrockOres.OVERWORLD) add(materials, ore.material());
        for (GTStoneLayersGen.LayerDef layer : GTStoneLayersGen.LAYERS) {
            for (GTStoneLayersGen.OreDef ore : layer.ores()) addNamed(materials, ore.material());
        }
        for (GTStoneLayersGen.ContactDef contact : GTStoneLayersGen.CONTACTS) {
            for (GTStoneLayersGen.OreDef ore : contact.ores()) addNamed(materials, ore.material());
        }
        return materials;
    }

    private static void addVein(Set<GTMaterial> materials, OreVein vein) {
        add(materials, vein.top());
        add(materials, vein.bottom());
        add(materials, vein.between());
        add(materials, vein.spread());
    }

    private static void add(Set<GTMaterial> materials, GTMaterial material) {
        if (material != null && material.resolve().isValid()) materials.add(material.resolve());
    }

    private static void addNamed(Set<GTMaterial> materials, String name) {
        GTMaterial found = GTMaterialRegistry.get(name);
        if (found.isValid()) materials.add(found.resolve());
    }
}
