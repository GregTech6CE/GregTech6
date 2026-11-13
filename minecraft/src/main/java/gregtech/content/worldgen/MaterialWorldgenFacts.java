package gregtech.content.worldgen;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.addon.worldgen.SmallOreDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Fact-only world-generation projection used by the material process snapshot. */
public final class MaterialWorldgenFacts {
    public enum Kind {
        LARGE_VEIN,
        SMALL_ORE,
        STONE_LAYER,
        BEDROCK
    }

    public record Ore(
            GTMaterial material,
            String itemState,
            int minY,
            int maxY,
            int amount,
            int chanceDivisor,
            List<String> details) {
        public Ore {
            itemState = requireState(itemState);
            details = List.copyOf(details);
        }
    }

    public record Fact(
            String id,
            String name,
            Kind kind,
            String dimension,
            int minY,
            int maxY,
            int weight,
            int totalWeight,
            List<Ore> ores) {
        public Fact {
            if (id == null || id.isBlank() || name == null || name.isBlank() || kind == null)
                throw new IllegalArgumentException("Invalid worldgen process fact");
            dimension = dimension == null || dimension.isBlank() ? "unknown" : dimension;
            ores = List.copyOf(ores);
        }
    }

    public static final class Snapshot {
        private final List<Fact> facts;
        private final long revision;

        private Snapshot(List<Fact> facts) {
            this.facts = List.copyOf(facts);
            long hash = 1_125_899_906_842_589L;
            for (Fact fact : facts)
                hash = 31 * hash
                        + (((long) (fact.id.hashCode() * 31
                                        + fact.dimension.hashCode()
                                        + fact.minY
                                        + fact.maxY
                                        + fact.ores.hashCode()))
                                ^ (hash >>> 32));
            revision = hash & Long.MAX_VALUE;
        }

        public List<Fact> facts() {
            return facts;
        }

        public long revision() {
            return revision;
        }
    }

    private MaterialWorldgenFacts() {}

    public static Snapshot capture() {
        var facts = new ArrayList<Fact>();
        appendLargeVeins(facts);
        appendSmallOres(facts);
        appendStoneLayers(facts);
        appendBedrock(facts);
        return new Snapshot(facts);
    }

    private static void appendLargeVeins(List<Fact> facts) {
        var dimensions = new LinkedHashSet<String>();
        dimensions.add(OreVeinDeclaration.OVERWORLD);
        dimensions.add(OreVeinDeclaration.END);
        dimensions.addAll(AddonOreVeins.veinDimensions());
        for (String dimension : dimensions) {
            var veins = GTOreVeins.activeVeins(dimension);
            boolean remapped = OreVeinDeclaration.OVERWORLD.equals(dimension);
            int totalWeight = OreVeinDeclaration.END.equals(dimension)
                    ? GTOreVeins.endSelectionWeight(veins)
                    : veins.stream().mapToInt(GTOreVeins.OreVein::weight).sum();
            for (GTOreVeins.OreVein vein : veins) {
                int minY = remapped ? GTWorldgenScale.remapY(vein.minY()) : vein.minY();
                int maxY = remapped ? GTWorldgenScale.remapY(vein.maxY()) : vein.maxY();
                var ores = new ArrayList<Ore>();
                addVeinOre(ores, vein.top(), minY, maxY, "top");
                addVeinOre(ores, vein.bottom(), minY, maxY, "bottom");
                addVeinOre(ores, vein.between(), minY, maxY, "between");
                addVeinOre(ores, vein.spread(), minY, maxY, "spread");
                facts.add(new Fact(
                        "large/" + dimension + "/" + vein.name(),
                        vein.name(),
                        Kind.LARGE_VEIN,
                        dimension,
                        minY,
                        maxY,
                        vein.weight(),
                        totalWeight,
                        ores));
            }
        }
    }

    private static void addVeinOre(List<Ore> out, GTMaterial material, int minY, int maxY, String layer) {
        if (material == null || !material.resolve().isValid()) return;
        out.add(new Ore(
                material.resolve(),
                itemState(oreItem(material.resolve())),
                minY,
                maxY,
                1,
                0,
                List.of("vein layer: " + layer)));
    }

    private static void appendSmallOres(List<Fact> facts) {
        var dimensions = new LinkedHashSet<String>();
        dimensions.add(SmallOreDeclaration.OVERWORLD);
        dimensions.add(SmallOreDeclaration.NETHER);
        dimensions.add(SmallOreDeclaration.END);
        dimensions.addAll(AddonOreVeins.smallOreDimensions());
        for (String dimension : dimensions) {
            boolean remapped = SmallOreDeclaration.OVERWORLD.equals(dimension);
            for (GTOreVeins.SmallOre ore : GTOreVeins.activeSmallOres(dimension)) {
                GTMaterial material = ore.material().resolve();
                if (!material.isValid()) continue;
                int minY = remapped ? GTWorldgenScale.remapY(ore.minY()) : ore.minY();
                int maxY = remapped ? GTWorldgenScale.remapY(ore.maxY()) : ore.maxY();
                facts.add(new Fact(
                        "small/" + dimension + "/" + ore.name(),
                        ore.name(),
                        Kind.SMALL_ORE,
                        dimension,
                        minY,
                        maxY,
                        0,
                        0,
                        List.of(new Ore(
                                material,
                                itemState(smallOreItem(material)),
                                minY,
                                maxY,
                                ore.amount(),
                                0,
                                List.of("blocks per chunk")))));
            }
        }
    }

    private static void appendStoneLayers(List<Fact> facts) {
        if (!GTWorldgenDefaults.STONE_LAYERS) return;
        var layers = new ArrayList<GTStoneLayersGen.LayerDef>();
        layers.addAll(GTStoneLayersGen.LAYERS);
        for (GTStoneLayersGen.ContactDef contact : GTStoneLayersGen.CONTACTS) {
            layers.add(new GTStoneLayersGen.LayerDef(
                    null, null, contact.materialA() + "/" + contact.materialB(), false, contact.ores()));
            if (contact.bothSides()) {
                layers.add(new GTStoneLayersGen.LayerDef(
                        null, null, contact.materialB() + "/" + contact.materialA(), false, contact.ores()));
            }
        }
        int sequence = 0;
        for (GTStoneLayersGen.LayerDef layer : layers) {
            int layerIndex = sequence++;
            var ores = new ArrayList<Ore>();
            for (GTStoneLayersGen.OreDef definition : layer.ores()) {
                var rule = StoneLayerOreRules.rule(definition);
                if (!StoneLayerOrePlacement.enabled(rule)) continue;
                GTMaterial material = GTMaterialRegistry.get(definition.material());
                if (material == null || !material.resolve().isValid()) continue;
                material = material.resolve();
                int min = Math.max(GTWorldgenScale.WORLD_FLOOR + 1, GTWorldgenScale.stoneLayerOreMinY(definition));
                if (layer.noDeep()) min = Math.max(min, GTWorldgenScale.remapY(24));
                int max = Math.min(GTWorldgenScale.BAND_TOP - 1, GTWorldgenScale.remapY(definition.maxY()));
                if (min > max) continue;
                ores.add(new Ore(
                        material,
                        itemState(explicitItem(material, rule)),
                        min,
                        max,
                        1,
                        Math.max(1, definition.chanceDiv()),
                        rule.biomes().isEmpty()
                                ? List.of("stone-layer roll")
                                : List.of("stone-layer roll; biomes: " + rule.biomes())));
            }
            if (ores.isEmpty()) continue;
            int minY = ores.stream().mapToInt(Ore::minY).min().orElse(0);
            int maxY = ores.stream().mapToInt(Ore::maxY).max().orElse(0);
            facts.add(new Fact(
                    "stratum/" + layerIndex + "/" + layer.material(),
                    layer.material(),
                    Kind.STONE_LAYER,
                    "minecraft:overworld",
                    minY,
                    maxY,
                    0,
                    0,
                    ores));
        }
    }

    private static void appendBedrock(List<Fact> facts) {
        appendBedrock(facts, GTBedrockOres.OVERWORLD, "minecraft:overworld");
        appendBedrock(facts, GTBedrockOres.NETHER, "minecraft:the_nether");
    }

    private static void appendBedrock(List<Fact> facts, List<GTBedrockOres.BedrockOre> rows, String dimension) {
        for (GTBedrockOres.BedrockOre row : rows) {
            GTMaterial material = row.material().resolve();
            if (!material.isValid()) continue;
            facts.add(new Fact(
                    "bedrock/" + dimension + "/" + row.name(),
                    row.name(),
                    Kind.BEDROCK,
                    dimension,
                    GTWorldgenScale.WORLD_FLOOR,
                    GTWorldgenScale.WORLD_FLOOR + 2,
                    row.chance(),
                    0,
                    List.of(new Ore(
                            material,
                            itemState(oreItem(material)),
                            GTWorldgenScale.WORLD_FLOOR,
                            GTWorldgenScale.WORLD_FLOOR + 2,
                            1,
                            0,
                            List.of("bedrock rarity: " + row.chance())))));
        }
    }

    private static Item explicitItem(GTMaterial material, StoneLayerOreRules.Rule rule) {
        var state = StoneLayerOrePlacement.explicitState(rule);
        return state == null ? oreItem(material) : state.getBlock().asItem();
    }

    private static Item oreItem(GTMaterial material) {
        String path =
                material.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "ore_" + path));
        return item == Items.AIR ? Items.STONE : item;
    }

    private static Item smallOreItem(GTMaterial material) {
        String path =
                material.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "ore_small_" + path));
        return item == Items.AIR ? oreItem(material) : item;
    }

    private static String itemState(Item item) {
        return "item:" + BuiltInRegistries.ITEM.getKey(item);
    }

    private static String requireState(String state) {
        if (state == null || state.isBlank()) throw new IllegalArgumentException("Missing worldgen item state");
        return state;
    }
}
