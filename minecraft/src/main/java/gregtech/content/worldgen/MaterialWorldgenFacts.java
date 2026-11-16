package gregtech.content.worldgen;

import gregtech.api.addon.material.FormKey;
import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialFormBinding;
import gregtech.api.addon.material.MaterialFormKind;
import gregtech.api.addon.material.MaterialKey;
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

        public Snapshot(List<Fact> facts) {
            this.facts = List.copyOf(facts);
            long hash = 1_125_899_906_842_589L;
            for (Fact fact : facts) {
                hash = hash(hash, fact.id());
                hash = hash(hash, fact.name());
                hash = hash(hash, fact.kind().name());
                hash = hash(hash, fact.dimension());
                hash = hash(hash, Integer.toString(fact.minY()));
                hash = hash(hash, Integer.toString(fact.maxY()));
                hash = hash(hash, Integer.toString(fact.weight()));
                hash = hash(hash, Integer.toString(fact.totalWeight()));
                hash = hash(hash, Integer.toString(fact.ores().size()));
                for (Ore ore : fact.ores()) {
                    hash = hash(hash, ore.material().resolve().getName());
                    hash = hash(hash, ore.itemState());
                    hash = hash(hash, Integer.toString(ore.minY()));
                    hash = hash(hash, Integer.toString(ore.maxY()));
                    hash = hash(hash, Integer.toString(ore.amount()));
                    hash = hash(hash, Integer.toString(ore.chanceDivisor()));
                    hash = hash(hash, Integer.toString(ore.details().size()));
                    for (String detail : ore.details()) hash = hash(hash, detail);
                }
            }
            revision = hash & Long.MAX_VALUE;
        }

        private static long hash(long hash, String value) {
            hash = 31 * hash + value.length();
            for (int index = 0; index < value.length(); index++) hash = 31 * hash + value.charAt(index);
            return hash;
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
        if (gregtech.GregTechConfig.Worldgen.GEOLOGY.enabled()) {
            appendLargeVeins(facts);
            appendSmallOres(facts);
            appendStoneLayers(facts);
        }
        if (gregtech.GregTechConfig.Worldgen.BEDROCK_ORES.enabled()) appendBedrock(facts);
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
                if (ores.isEmpty()) continue;
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
        Item item = oreItem(material.resolve());
        if (item == Items.AIR) return;
        out.add(new Ore(material.resolve(), itemState(item), minY, maxY, 1, 0, List.of("vein layer: " + layer)));
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
                Item item = smallOreItem(material);
                if (item == Items.AIR) continue;
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
                                material, itemState(item), minY, maxY, ore.amount(), 0, List.of("blocks per chunk")))));
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
                List<String> oreDetails = rule.biomes().isEmpty()
                        ? List.of("stone-layer roll")
                        : List.of("stone-layer roll; biomes: " + rule.biomes());
                Item explicit = explicitItem(material, rule);
                if (explicit != Items.AIR) {
                    ores.add(new Ore(
                            material,
                            itemState(explicit),
                            min,
                            max,
                            1,
                            Math.max(1, definition.chanceDiv()),
                            oreDetails));
                }
                if (explicit == Items.AIR || explicit == oreItem(material)) {
                    // Layer interiors use normal ore in the core and small ore at the edges; contacts
                    // choose between both. Keep both real resources rather than collapsing to ore_.
                    for (Item item : explicit == Items.AIR
                            ? List.of(oreItem(material), smallOreItem(material))
                            : List.of(smallOreItem(material))) {
                        if (item == Items.AIR) continue;
                        ores.add(new Ore(
                                material,
                                itemState(item),
                                min,
                                max,
                                1,
                                Math.max(1, definition.chanceDiv()),
                                oreDetails));
                    }
                }
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
        long totalWeight = rows.stream()
                .mapToLong(row -> MineralWorldgenRules.bedrockWeight(row.chance()))
                .sum();
        for (GTBedrockOres.BedrockOre row : rows) {
            GTMaterial material = row.material().resolve();
            if (!material.isValid()) continue;
            Item item = oreItem(material);
            if (item == Items.AIR) continue;
            int weight = (int) MineralWorldgenRules.bedrockWeight(row.chance());
            facts.add(new Fact(
                    "bedrock/" + dimension + "/" + row.name(),
                    row.name(),
                    Kind.BEDROCK,
                    dimension,
                    GTWorldgenScale.WORLD_FLOOR,
                    GTWorldgenScale.WORLD_FLOOR + 2,
                    weight,
                    Math.toIntExact(totalWeight),
                    List.of(new Ore(
                            material,
                            itemState(item),
                            GTWorldgenScale.WORLD_FLOOR,
                            GTWorldgenScale.WORLD_FLOOR + 2,
                            1,
                            0,
                            List.of("bedrock rarity: 1 in " + row.chance())))));
        }
    }

    private static Item explicitItem(GTMaterial material, StoneLayerOreRules.Rule rule) {
        var state = StoneLayerOrePlacement.explicitState(rule);
        return state == null ? oreItem(material) : state.getBlock().asItem();
    }

    private static Item oreItem(GTMaterial material) {
        Item bound = boundItem(material, "ore", "oreVanillastone");
        if (bound != null) return bound;
        String path = material.getName().toLowerCase(Locale.ROOT);
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "ore_" + path));
    }

    private static Item smallOreItem(GTMaterial material) {
        Item bound = boundItem(material, "oreSmall");
        if (bound != null) return bound;
        String path = material.getName().toLowerCase(Locale.ROOT);
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "ore_small_" + path));
    }

    private static Item boundItem(GTMaterial material, String... forms) {
        try {
            for (String name : forms)
                for (MaterialFormBinding binding :
                        GregTechMaterials.formBindings().find(new MaterialKey(material.getName()), new FormKey(name)))
                    if (binding.kind() == MaterialFormKind.ITEM) {
                        ResourceLocation id = ResourceLocation.tryParse(binding.id());
                        if (id != null) {
                            Item item = BuiltInRegistries.ITEM.get(id);
                            if (item != Items.AIR) return item;
                        }
                    }
        } catch (IllegalStateException notReady) {
            // Naming fallback below remains valid during early publication.
        }
        return null;
    }

    private static String itemState(Item item) {
        return "item:" + BuiltInRegistries.ITEM.getKey(item);
    }

    private static String requireState(String state) {
        if (state == null || state.isBlank()) throw new IllegalArgumentException("Missing worldgen item state");
        return state;
    }
}
