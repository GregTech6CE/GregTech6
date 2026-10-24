package gregtech.jei;


//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.internal.material.GTMaterial;
import gregtech.worldgen.GTOreVeins;
import gregtech.worldgen.GTStoneLayersGen;
import gregtech.worldgen.GTWorldgenScale;
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
                           boolean remapped) {}

    public record SmallOreInfo(String name, String dimension, int minY, int maxY, int amount,
                               GTMaterial material, boolean remapped) {}

    public record LayerInfo(String stoneDisplay, ItemStack stone, List<GTMaterial> ores) {}

    public static ItemStack oreStack(GTMaterial material) {
        if (material == null || !material.resolve().isValid()) return new ItemStack(Items.STONE);
//? if >=1.21.1 {
        /*Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",
*///?} else {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GregTech.NAMESPACE,
//?}
                "ore_" + material.resolve().getName().toLowerCase()));
        return item == null || item == Items.AIR ? new ItemStack(Items.STONE) : new ItemStack(item);
    }

    public static ItemStack smallOreStack(GTMaterial material) {
        if (material == null || !material.resolve().isValid()) return new ItemStack(Items.STONE);
//? if >=1.21.1 {
        /*Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",
*///?} else {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GregTech.NAMESPACE,
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
        int endWeight = end.stream().mapToInt(GTOreVeins.OreVein::weight).sum();
        for (GTOreVeins.OreVein v : end) {
            out.add(new VeinInfo(v.name(), "minecraft:the_end", v.minY(), v.maxY(), v.weight(),
                    endWeight, v.top(), v.bottom(), v.between(), v.spread(), false));
        }
        return out;
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
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GregTech.NAMESPACE, id));
//?}
                if (item == null || item == Items.AIR) continue;
                stone = new ItemStack(item);
                display = def.material();
            } else if (def.blockId() != null) {
//? if >=1.21.1 {
                /*Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", def.blockId()));
*///?} else {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GregTech.NAMESPACE, def.blockId()));
//?}
                if (item == null || item == Items.AIR) continue;
                stone = new ItemStack(item);
                display = def.material();
            } else {
                continue; // vanilla stone layers carry no display block
            }
            List<GTMaterial> ores = new ArrayList<>();
            for (GTStoneLayersGen.OreDef ore : def.ores()) {
                GTMaterial material = gregtech.internal.material.GTMaterialRegistry.get(ore.material());
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
        for (gregtech.worldgen.GTBedrockOres.BedrockOre ore
                : gregtech.worldgen.GTBedrockOres.OVERWORLD) {
//? if >=1.21.1 {
            /*Item flower = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", ore.flowerId()));
*///?} else {
            Item flower = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GregTech.NAMESPACE, ore.flowerId()));
//?}
            out.add(new BedrockInfo(ore.name(), ore.material(), ore.chance(),
                    flower == null || flower == Items.AIR ? new ItemStack(Items.POPPY) : new ItemStack(flower)));
        }
        return out;
    }
}
