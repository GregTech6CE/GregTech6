package gregtech.compat.jei;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.addon.worldgen.SmallOreDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.content.worldgen.AddonOreVeins;
import gregtech.content.worldgen.GTOreVeins;
import gregtech.content.worldgen.GTStoneLayersGen;
import gregtech.content.worldgen.GTWorldgenScale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

import java.util.ArrayList;
import java.util.List;


/** Actual world generation catalogs, independent of optional recipe browsers. */
public class WorldgenInfoData {
    protected WorldgenInfoData() {}
    public record VeinInfo(String name, String dimension, int minY, int maxY, int weight, int totalWeight,
                           GTMaterial top, GTMaterial bottom, GTMaterial between, GTMaterial spread,
                           boolean remapped, StratumInfo layer) {
        public VeinInfo(String name, String dimension, int minY, int maxY, int weight, int totalWeight,
                        GTMaterial top, GTMaterial bottom, GTMaterial between, GTMaterial spread, boolean remapped) {
            this(name, dimension, minY, maxY, weight, totalWeight, top, bottom, between, spread, remapped, null);
        }
    }

    public record SmallOreInfo(String name, String dimension, int minY, int maxY, int amount,
                               GTMaterial material, boolean remapped) {}

    public record LayerInfo(String stoneDisplay, ItemStack stone, List<GTMaterial> ores, List<GTStoneLayersGen.OreDef> definitions) {}

    public static ItemStack oreStack(GTMaterial material) {
        if (material == null || !material.resolve().isValid()) return new ItemStack(Items.STONE);
//? if >=1.21.1 {
        /*Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",
*///?} else {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE,
//?}
                "ore_" + material.resolve().getName().toLowerCase()));
        return item == null || item == Items.AIR ? new ItemStack(Items.STONE) : new ItemStack(item);
    }

    public static ItemStack smallOreStack(GTMaterial material) {
        if (material == null || !material.resolve().isValid()) return new ItemStack(Items.STONE);
//? if >=1.21.1 {
        /*Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",
*///?} else {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE,
//?}
                "ore_small_" + material.resolve().getName().toLowerCase()));
        return item == null || item == Items.AIR ? oreStack(material) : new ItemStack(item);
    }

    public static List<VeinInfo> buildVeins() {
        List<VeinInfo> out = new ArrayList<>();
        var overworld=GTOreVeins.activeOverworldVeins();
        int overworldWeight=overworld.stream().mapToInt(GTOreVeins.OreVein::weight).sum();
        for (GTOreVeins.OreVein v : overworld) {
            out.add(new VeinInfo(v.name(), "minecraft:overworld", v.minY(), v.maxY(), v.weight(),
                    overworldWeight, v.top(), v.bottom(), v.between(), v.spread(), true));
        }
        var end = GTOreVeins.activeEndVeins();
        int endWeight = GTOreVeins.endSelectionWeight(end);
        for (GTOreVeins.OreVein v : end) {
            out.add(new VeinInfo(v.name(), "minecraft:the_end", v.minY(), v.maxY(), v.weight(),
                    endWeight, v.top(), v.bottom(), v.between(), v.spread(), false));
        }
        for (String dimension : AddonOreVeins.veinDimensions()) {
            if (OreVeinDeclaration.OVERWORLD.equals(dimension) || OreVeinDeclaration.END.equals(dimension)) continue;
            var veins = GTOreVeins.activeVeins(dimension);
            int totalWeight = veins.stream().mapToInt(GTOreVeins.OreVein::weight).sum();
            for (GTOreVeins.OreVein v : veins) {
                out.add(new VeinInfo(v.name(), dimension, v.minY(), v.maxY(), v.weight(), totalWeight,
                        v.top(), v.bottom(), v.between(), v.spread(), false));
            }
        }
        if (gregtech.content.worldgen.GTWorldgenDefaults.STONE_LAYERS) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (GTStoneLayersGen.LayerDef layer : GTStoneLayersGen.LAYERS) {
                appendStratumOres(out, seen, layer.material(), null, false, layer.noDeep(), layer.ores());
            }
            // The generator concatenates all rules for the same directional contact.
            record Contact(String above, String below) {}
            var contacts = new java.util.LinkedHashMap<Contact, List<GTStoneLayersGen.OreDef>>();
            for (GTStoneLayersGen.ContactDef contact : GTStoneLayersGen.CONTACTS) {
                contacts.computeIfAbsent(new Contact(contact.materialA(), contact.materialB()), key -> new ArrayList<>()).addAll(contact.ores());
                if (contact.bothSides()) contacts.computeIfAbsent(new Contact(contact.materialB(), contact.materialA()), key -> new ArrayList<>()).addAll(contact.ores());
            }
            contacts.forEach((contact, ores) -> appendStratumOres(out, seen, contact.above(), contact.below(), false, false, ores));
        }
        return out;
    }

    public record StratumOre(GTMaterial material, int minY, int maxY, int chanceDiv, gregtech.content.worldgen.StoneLayerOreRules.Rule rule) {}
    public record StratumInfo(Component host, boolean contact, List<StratumOre> ores) {}

    private static Component materialName(String name) {
        GTMaterial material = gregtech.engine.material.GTMaterialRegistry.get(name);
        String canonical = material == null ? name : material.resolve().getName();
        return Component.translatable("material.gt6ce." + canonical.toLowerCase(java.util.Locale.ROOT));
    }

    private static void appendStratumOres(List<VeinInfo> out, java.util.Set<String> seen,
                                          String above, String below, boolean bothSides, boolean noDeep,
                                          List<GTStoneLayersGen.OreDef> definitions) {
        Component host = below == null ? materialName(above)
                : Component.translatable("gt6ce.jei.info.contact_" + (bothSides ? "both" : "ordered"),
                        materialName(above), materialName(below));
        List<StratumOre> ores = new ArrayList<>();
        for (GTStoneLayersGen.OreDef def : definitions) {
            var rule = gregtech.content.worldgen.StoneLayerOreRules.rule(def);
            if (!gregtech.content.worldgen.StoneLayerOrePlacement.enabled(rule)) continue;
            GTMaterial material = gregtech.engine.material.GTMaterialRegistry.get(def.material());
            if (material == null || !material.resolve().isValid()) continue;
            material = material.resolve();
            int min = Math.max(GTWorldgenScale.WORLD_FLOOR + 1, GTWorldgenScale.remapY(def.minY()));
            if (noDeep) min = Math.max(min, GTWorldgenScale.remapY(24));
            int max = Math.min(GTWorldgenScale.BAND_TOP - 1, GTWorldgenScale.remapY(def.maxY()));
            if (min > max) continue;
            int chance = Math.max(1, def.chanceDiv());
            ores.add(new StratumOre(material, min, max, chance, rule));
        }
        if (ores.isEmpty()) return;
        // Preserve the entire ordered combination, including separate height bands for one mineral.
        String key = above + "/" + below + "/" + bothSides + "/" + ores;
        if (!seen.add(key)) return;
        int min = ores.stream().mapToInt(StratumOre::minY).min().orElseThrow();
        int max = ores.stream().mapToInt(StratumOre::maxY).max().orElseThrow();
        out.add(new VeinInfo(above, "minecraft:overworld", min, max, 0, 0,
                null, null, null, null, false, new StratumInfo(host, below != null, List.copyOf(ores))));
    }

    public static List<GTMaterial> veinMaterials(VeinInfo vein) {
        return vein.layer() == null ? List.of(vein.top(), vein.bottom(), vein.between(), vein.spread())
                : vein.layer().ores().stream().map(StratumOre::material).distinct().toList();
    }

    /** Search aliases, not additional guaranteed products. Small ores use the actual crushing target. */
    public static List<ItemStack> oreAliases(GTMaterial material, boolean small) {
        var prefix = small ? gregtech.content.data.MaterialPrefix.crushed : gregtech.content.data.MaterialPrefix.oreRaw;
        GTMaterial target = small ? material.getTargetCrushingMaterial().resolve() : material.resolve();
        ItemStack stack = gregtech.content.registry.GTItems.getStack(prefix, target);
        return stack.isEmpty() ? List.of() : List.of(stack);
    }

    public static List<ItemStack> veinAliases(VeinInfo vein) {
        List<ItemStack> aliases = new ArrayList<>();
        for (GTMaterial material : veinMaterials(vein)) {
            var rule = vein.layer() == null ? null : vein.layer().ores().stream()
                    .filter(ore -> ore.material() == material).findFirst().orElseThrow().rule();
            if (rule != null && !rule.blockId().isEmpty()) {
                aliases.addAll(explicitAliases(rule));
                continue;
            }
            aliases.addAll(oreAliases(material, false));
            if (vein.layer() != null) {
                aliases.add(smallOreStack(material));
                aliases.addAll(oreAliases(material, true));
            }
        }
        return aliases;
    }

    private static List<ItemStack> explicitAliases(gregtech.content.worldgen.StoneLayerOreRules.Rule rule) {
        Item item = switch (rule.blockId()) {
            case "minecraft:iron_ore" -> Items.RAW_IRON;
            case "minecraft:gold_ore" -> Items.RAW_GOLD;
            case "minecraft:copper_ore" -> Items.RAW_COPPER;
            default -> Items.AIR;
        };
        return item == Items.AIR ? List.of() : List.of(new ItemStack(item));
    }

    private static ItemStack ruleStack(GTMaterial material, gregtech.content.worldgen.StoneLayerOreRules.Rule rule) {
        var state = gregtech.content.worldgen.StoneLayerOrePlacement.explicitState(rule);
        return state == null ? oreStack(material) : new ItemStack(state.getBlock());
    }

    public static ItemStack stratumStack(VeinInfo vein, GTMaterial material) {
        return ruleStack(material, vein.layer().ores().stream().filter(ore -> ore.material() == material)
                .findFirst().orElseThrow().rule());
    }

    private static gregtech.content.worldgen.StoneLayerOreRules.Rule layerRule(LayerInfo layer, GTMaterial material) {
        return layer.definitions().stream().filter(def -> {
                    GTMaterial candidate = gregtech.engine.material.GTMaterialRegistry.get(def.material());
                    return candidate != null && candidate.resolve() == material;
                })
                .map(gregtech.content.worldgen.StoneLayerOreRules::rule).findFirst().orElseThrow();
    }

    public static ItemStack layerOreStack(LayerInfo layer, GTMaterial material) {
        return ruleStack(material, layerRule(layer, material));
    }

    public static List<ItemStack> layerAliases(LayerInfo layer) {
        return layer.ores().stream().flatMap(material -> {
            var rule = layerRule(layer, material);
            return (rule.blockId().isEmpty() ? oreAliases(material, false) : explicitAliases(rule)).stream();
        }).toList();
    }

    public static List<Component> stratumOreCaptions(VeinInfo vein, GTMaterial material) {
        List<Component> lines = new ArrayList<>();
        for (StratumOre ore : vein.layer().ores()) {
            if (ore.material() != material) continue;
            lines.add(Component.translatable("gt6ce.jei.info.height", ore.minY(), ore.maxY()));
            if (!ore.rule().biomes().isEmpty()) {
                Component biomes = Component.empty();
                for (var group : ore.rule().biomes()) {
                    if (!biomes.getString().isEmpty()) biomes = biomes.copy().append(", ");
                    biomes = biomes.copy().append(Component.translatable("gt6ce.jei.info.biome_group." + group.name().toLowerCase(java.util.Locale.ROOT)));
                }
                lines.add(Component.translatable("gt6ce.jei.info.required_biomes", biomes));
            }
            if (!ore.rule().blockId().isEmpty()) lines.add(Component.translatable("gt6ce.jei.info.explicit_ore"));
            lines.add(Component.translatable("gt6ce.jei.info.stratum_roll", ore.chanceDiv(),
                    String.format(java.util.Locale.ROOT, "%.2f", 100d / ore.chanceDiv())));
        }
        return lines;
    }

    /** Captions shared by JEI and EMI; individual ore heights and rolls belong to their slots. */
    public static List<Component> stratumCaptions(VeinInfo vein) {
        StratumInfo info = vein.layer();
        return List.of(
                Component.translatable("gt6ce.jei.info.layer_stone", info.host()),
                Component.translatable("gt6ce.jei.info.dimension", vein.dimension()),
                Component.translatable("gt6ce.jei.info.height", vein.minY(), vein.maxY()),
                Component.translatable("gt6ce.jei.info.stratum_hover"),
                Component.translatable("gt6ce.jei.info.stratum_conditional"),
                Component.translatable("gt6ce.jei.info." + (info.contact() ? "stratum_contact" : "stratum_interior")));
    }

    public static List<SmallOreInfo> buildSmallOres() {
        List<SmallOreInfo> out = new ArrayList<>();
        for (GTOreVeins.SmallOre o : GTOreVeins.activeOverworldSmallOres()) {
            out.add(new SmallOreInfo(o.name(), "minecraft:overworld", o.minY(), o.maxY(), o.amount(), o.material(), true));
        }
        for (GTOreVeins.SmallOre o : GTOreVeins.activeNetherSmallOres()) {
            out.add(new SmallOreInfo(o.name(), "minecraft:the_nether", o.minY(), o.maxY(), o.amount(), o.material(), false));
        }
        for (GTOreVeins.SmallOre o : GTOreVeins.activeEndSmallOres()) {
            out.add(new SmallOreInfo(o.name(), "minecraft:the_end", o.minY(), o.maxY(), o.amount(), o.material(), false));
        }
        for (String dimension : AddonOreVeins.smallOreDimensions()) {
            if (SmallOreDeclaration.OVERWORLD.equals(dimension)
                    || SmallOreDeclaration.NETHER.equals(dimension)
                    || SmallOreDeclaration.END.equals(dimension)) continue;
            for (GTOreVeins.SmallOre o : GTOreVeins.activeSmallOres(dimension)) {
                out.add(new SmallOreInfo(o.name(), dimension, o.minY(), o.maxY(), o.amount(), o.material(), false));
            }
        }
        return out;
    }

    public static List<LayerInfo> buildLayers() {
        List<LayerInfo> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (GTStoneLayersGen.LayerDef def : GTStoneLayersGen.LAYERS) {
            ItemStack stone;
            String display;
            if (def.stoneType() != null) {
                String id = "stone_" + def.stoneType().toLowerCase() + "_stone";
//? if >=1.21.1 {
                /*Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
*///?} else {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, id));
//?}
                if (item == null || item == Items.AIR) continue;
                stone = new ItemStack(item);
                display = def.material();
            } else if (def.blockId() != null) {
//? if >=1.21.1 {
                /*Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", def.blockId()));
*///?} else {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, def.blockId()));
//?}
                if (item == null || item == Items.AIR) continue;
                stone = new ItemStack(item);
                display = def.material();
            } else {
                continue; // vanilla stone layers carry no display block
            }
            List<GTMaterial> ores = new ArrayList<>();
            for (GTStoneLayersGen.OreDef ore : def.ores()) {
                if (!gregtech.content.worldgen.StoneLayerOrePlacement.enabled(gregtech.content.worldgen.StoneLayerOreRules.rule(ore))) continue;
                GTMaterial material = gregtech.engine.material.GTMaterialRegistry.get(ore.material());
                if (material != null && material.resolve().isValid()) ores.add(material.resolve());
            }
            String key = display + "/" + ores.stream().map(GTMaterial::getName).sorted().toList();
            if (!seen.add(key)) continue; // collapse duplicate layer entries
            out.add(new LayerInfo(display, stone, ores, def.ores()));
        }
        return out;
    }

public record BedrockInfo(String name, GTMaterial material, int chance, ItemStack flower) {}
public static List<BedrockInfo> buildBedrockOres() {
        List<BedrockInfo> out = new ArrayList<>();
        for (gregtech.content.worldgen.GTBedrockOres.BedrockOre ore
                : gregtech.content.worldgen.GTBedrockOres.OVERWORLD) {
//? if >=1.21.1 {
            /*Item flower = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", ore.flowerId()));
*///?} else {
            Item flower = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(GregTech.NAMESPACE, ore.flowerId()));
//?}
            out.add(new BedrockInfo(ore.name(), ore.material(), ore.chance(),
                    flower == null || flower == Items.AIR ? new ItemStack(Items.POPPY) : new ItemStack(flower)));
        }
        return out;
    }
}
