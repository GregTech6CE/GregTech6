package com.gregtech.gregtech.jei;


import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.worldgen.GTOreVeins;
import com.gregtech.gregtech.worldgen.GTStoneLayersGen;
import com.gregtech.gregtech.worldgen.GTWorldgenScale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

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
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",
                "ore_" + material.resolve().getName().toLowerCase()));
        return item == null || item == Items.AIR ? new ItemStack(Items.STONE) : new ItemStack(item);
    }

    public static ItemStack smallOreStack(GTMaterial material) {
        if (material == null || !material.resolve().isValid()) return new ItemStack(Items.STONE);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",
                "ore_small_" + material.resolve().getName().toLowerCase()));
        return item == null || item == Items.AIR ? oreStack(material) : new ItemStack(item);
    }

    public static List<VeinInfo> buildVeins() {
        List<VeinInfo> out = new ArrayList<>();
        for (GTOreVeins.OreVein v : GTOreVeins.OVERWORLD_VEINS) {
            out.add(new VeinInfo(v.name(), "minecraft:overworld", v.minY(), v.maxY(), v.weight(),
                    GTOreVeins.TOTAL_VEIN_WEIGHT, v.top(), v.bottom(), v.between(), v.spread(), true));
        }
        for (GTOreVeins.OreVein v : GTOreVeins.END_VEINS) {
            out.add(new VeinInfo(v.name(), "minecraft:the_end", v.minY(), v.maxY(), v.weight(),
                    GTOreVeins.TOTAL_END_VEIN_WEIGHT, v.top(), v.bottom(), v.between(), v.spread(), false));
        }
        return out;
    }

    public static List<SmallOreInfo> buildSmallOres() {
        List<SmallOreInfo> out = new ArrayList<>();
        for (GTOreVeins.SmallOre o : GTOreVeins.OVERWORLD_SMALL_ORES) {
            out.add(new SmallOreInfo(o.name(), "minecraft:overworld", o.minY(), o.maxY(), o.amount(), o.material(), true));
        }
        for (GTOreVeins.SmallOre o : GTOreVeins.NETHER_SMALL_ORES) {
            out.add(new SmallOreInfo(o.name(), "minecraft:the_nether", o.minY(), o.maxY(), o.amount(), o.material(), false));
        }
        for (GTOreVeins.SmallOre o : GTOreVeins.END_SMALL_ORES) {
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
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", id));
                if (item == null || item == Items.AIR) continue;
                stone = new ItemStack(item);
                display = def.material();
            } else if (def.blockId() != null) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", def.blockId()));
                if (item == null || item == Items.AIR) continue;
                stone = new ItemStack(item);
                display = def.material();
            } else {
                continue; // vanilla stone layers carry no display block
            }
            List<GTMaterial> ores = new ArrayList<>();
            for (GTStoneLayersGen.OreDef ore : def.ores()) {
                GTMaterial material = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(ore.material());
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
        for (com.gregtech.gregtech.worldgen.GTBedrockOres.BedrockOre ore
                : com.gregtech.gregtech.worldgen.GTBedrockOres.OVERWORLD) {
            Item flower = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", ore.flowerId()));
            out.add(new BedrockInfo(ore.name(), ore.material(), ore.chance(),
                    flower == null || flower == Items.AIR ? new ItemStack(Items.POPPY) : new ItemStack(flower)));
        }
        return out;
    }
}
