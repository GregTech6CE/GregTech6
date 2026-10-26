package gregtech.compat.jei;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.api.material.GTMaterial;
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

    public record LayerInfo(String stoneDisplay, ItemStack stone, List<GTMaterial> ores) {}

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
        if (gregtech.content.worldgen.GTWorldgenDefaults.STONE_LAYERS) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (GTStoneLayersGen.LayerDef layer : GTStoneLayersGen.LAYERS) {
                appendStratumOres(out, seen, layer.material(), null, false, layer.noDeep(), layer.ores());
            }
            for (GTStoneLayersGen.ContactDef contact : GTStoneLayersGen.CONTACTS) {
                appendStratumOres(out, seen, contact.materialA(), contact.materialB(), contact.bothSides(), false, contact.ores());
            }
        }
        return out;
    }

    public record StratumInfo(Component host, boolean contact, int chanceDiv) {}

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
        for (GTStoneLayersGen.OreDef def : definitions) {
            GTMaterial material = gregtech.engine.material.GTMaterialRegistry.get(def.material());
            if (material == null || !material.resolve().isValid()) continue;
            material = material.resolve();
            int min = Math.max(GTWorldgenScale.WORLD_FLOOR + 1, GTWorldgenScale.remapY(def.minY()));
            if (noDeep) min = Math.max(min, GTWorldgenScale.remapY(24));
            int max = Math.min(GTWorldgenScale.BAND_TOP - 1, GTWorldgenScale.remapY(def.maxY()));
            if (min > max) continue;
            int chance = Math.max(1, def.chanceDiv());
            String key = above + "/" + below + "/" + bothSides + "/" + material.getName() + "/" + min + "/" + max + "/" + chance;
            if (!seen.add(key)) continue;
            out.add(new VeinInfo(material.getName(), "minecraft:overworld", min, max, 1, chance,
                    material, null, null, null, false, new StratumInfo(host, below != null, chance)));
        }
    }

    /** Captions shared by JEI and EMI for the conditional, per-position stratum ore roll. */
    public static List<Component> stratumCaptions(VeinInfo vein) {
        StratumInfo info = vein.layer();
        return List.of(
                Component.translatable("gt6ce.jei.info.layer_stone", info.host()),
                Component.translatable("gt6ce.jei.info.dimension", vein.dimension()),
                Component.translatable("gt6ce.jei.info.height", vein.minY(), vein.maxY()),
                Component.translatable("gt6ce.jei.info.stratum_roll", info.chanceDiv(),
                        String.format(java.util.Locale.ROOT, "%.2f", 100d / info.chanceDiv())),
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
                GTMaterial material = gregtech.engine.material.GTMaterialRegistry.get(ore.material());
                if (material != null && material.resolve().isValid()) ores.add(material.resolve());
            }
            String key = display + "/" + ores.stream().map(GTMaterial::getName).sorted().toList();
            if (!seen.add(key)) continue; // collapse duplicate layer entries
            out.add(new LayerInfo(display, stone, ores));
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
