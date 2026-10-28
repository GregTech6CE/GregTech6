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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Addon large veins and small ores, appended after the built-in tables. */
public final class AddonOreVeins {
    public record Submission(String addonId, List<OreVeinDeclaration> veins, List<SmallOreDeclaration> smallOres) {}

    private record Key(String dimension, String kind, String name) {}

    private static volatile List<OreVein> overworldVeins = List.of();
    private static volatile List<OreVein> endVeins = List.of();
    private static volatile List<SmallOre> overworldSmall = List.of();
    private static volatile List<SmallOre> netherSmall = List.of();
    private static volatile List<SmallOre> endSmall = List.of();
    private static Set<GTMaterial> builtinOres;

    private AddonOreVeins() {}

    public static List<OreVein> overworldVeins() {
        return overworldVeins;
    }

    public static List<OreVein> endVeins() {
        return endVeins;
    }

    public static List<SmallOre> overworldSmall() {
        return overworldSmall;
    }

    public static List<SmallOre> netherSmall() {
        return netherSmall;
    }

    public static List<SmallOre> endSmall() {
        return endSmall;
    }

    public static void publish(List<Submission> submissions) {
        var taken = new java.util.HashMap<Key, String>();
        claimBuiltIn(taken);
        var overworld = new ArrayList<OreVein>();
        var end = new ArrayList<OreVein>();
        var overworldSmallOres = new ArrayList<SmallOre>();
        var netherSmallOres = new ArrayList<SmallOre>();
        var endSmallOres = new ArrayList<SmallOre>();
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
                if (OreVeinDeclaration.END.equals(vein.dimension())) end.add(resolved);
                else overworld.add(resolved);
            }
            for (SmallOreDeclaration ore : submission.smallOres) {
                claim(taken, submission.addonId, ore.dimension(), "small", ore.name());
                SmallOre resolved = new SmallOre(
                        ore.name(),
                        ore.minY(),
                        ore.maxY(),
                        ore.amount(),
                        ore(submission.addonId, ore.name(), ore.material()));
                if (SmallOreDeclaration.NETHER.equals(ore.dimension())) netherSmallOres.add(resolved);
                else if (SmallOreDeclaration.END.equals(ore.dimension())) endSmallOres.add(resolved);
                else overworldSmallOres.add(resolved);
            }
        }
        fit(GTOreVeins.OVERWORLD_VEINS, overworld, 0);
        fit(GTOreVeins.END_VEINS, end, GTOreVeins.END_EMPTY_VEIN_WEIGHT);
        overworldVeins = List.copyOf(overworld);
        endVeins = List.copyOf(end);
        overworldSmall = List.copyOf(overworldSmallOres);
        netherSmall = List.copyOf(netherSmallOres);
        endSmall = List.copyOf(endSmallOres);
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

    private static void fit(List<OreVein> base, List<OreVein> added, int emptyWeight) {
        long sum = emptyWeight;
        for (OreVein vein : base) sum = Math.addExact(sum, vein.weight());
        for (OreVein vein : added) sum = Math.addExact(sum, vein.weight());
        if (sum > Integer.MAX_VALUE) throw new IllegalStateException("Ore vein weights do not fit in an int");
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
