package gregtech.content.worldgen;

import java.util.List;
import java.util.Map;

/** Restores metadata omitted from the legacy extracted table. Source: GT6 Loader_Worldgen,
 * lines 163-174, 181-192, 227-228, 266, 286, 348-354, 374-402, 459-494 and 563.
 * Keys include the complete ore declaration, so unrelated occurrences of a material are unaffected.
 */
public final class StoneLayerOreRules {
    private StoneLayerOreRules() {}
    public enum BiomeGroup { MOUNTAINS, FROZEN, DESERT, SAVANNA, JUNGLE, OCEAN_BEACH, TAIGA, SWAMP, PLAINS, RIVER_LAKE, MESA, SHROOM }
    public record Rule(String blockId, List<BiomeGroup> biomes) {}
    private static final Rule DEFAULT = new Rule("", List.of());
    private static final Map<GTStoneLayersGen.OreDef, Rule> RULES = Map.ofEntries(
        Map.entry(new GTStoneLayersGen.OreDef("Emerald", false, 48, 16, 60), new Rule("minecraft:emerald_ore", List.of(BiomeGroup.MOUNTAINS))),
        Map.entry(new GTStoneLayersGen.OreDef("Diamond", false, 128, 8, 24), new Rule("minecraft:diamond_ore", List.of())),
        Map.entry(new GTStoneLayersGen.OreDef("Lapis", false, 48, 16, 48), new Rule("minecraft:lapis_ore", List.of())),
        Map.entry(new GTStoneLayersGen.OreDef("Redstone", false, 32, 8, 24), new Rule("minecraft:redstone_ore", List.of())),
        Map.entry(new GTStoneLayersGen.OreDef("Au", false, 64, 8, 32), new Rule("minecraft:gold_ore", List.of())),
        Map.entry(new GTStoneLayersGen.OreDef("Au", false, 32, 33, 64), new Rule("minecraft:gold_ore", List.of(BiomeGroup.MESA))),
        Map.entry(new GTStoneLayersGen.OreDef("Cu", false, 16, 20, 50), new Rule("minecraft:copper_ore", List.of(BiomeGroup.MESA, BiomeGroup.DESERT, BiomeGroup.SAVANNA))),
        Map.entry(new GTStoneLayersGen.OreDef("Fe", false, 16, 40, 80), new Rule("minecraft:iron_ore", List.of())),
        Map.entry(new GTStoneLayersGen.OreDef("Coal", false, 8, 60, 100), new Rule("minecraft:coal_ore", List.of())),
        Map.entry(new GTStoneLayersGen.OreDef("Stone", false, 48, 0, 255), new Rule("minecraft:infested_stone", List.of())),
        Map.entry(new GTStoneLayersGen.OreDef("Jasper", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.MOUNTAINS))),
        Map.entry(new GTStoneLayersGen.OreDef("JasperBlue", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.FROZEN))),
        Map.entry(new GTStoneLayersGen.OreDef("JasperGreen", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.DESERT))),
        Map.entry(new GTStoneLayersGen.OreDef("JasperYellow", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.SAVANNA))),
        Map.entry(new GTStoneLayersGen.OreDef("JasperRainforest", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.JUNGLE))),
        Map.entry(new GTStoneLayersGen.OreDef("JasperOcean", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.OCEAN_BEACH))),
        Map.entry(new GTStoneLayersGen.OreDef("TigerEyeYellow", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.TAIGA))),
        Map.entry(new GTStoneLayersGen.OreDef("TigerEyeGreen", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.SWAMP))),
        Map.entry(new GTStoneLayersGen.OreDef("TigerEyeRed", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.PLAINS))),
        Map.entry(new GTStoneLayersGen.OreDef("TigerEyeBlue", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.RIVER_LAKE))),
        Map.entry(new GTStoneLayersGen.OreDef("TigerEyeBlack", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.MESA))),
        Map.entry(new GTStoneLayersGen.OreDef("TigerIron", true, 64, 32, 64), new Rule("", List.of(BiomeGroup.SHROOM))),
        Map.entry(new GTStoneLayersGen.OreDef("Spinel", true, 48, 24, 48), new Rule("", List.of(BiomeGroup.MOUNTAINS))),
        Map.entry(new GTStoneLayersGen.OreDef("BalasRuby", true, 48, 24, 48), new Rule("", List.of(BiomeGroup.JUNGLE))),
        Map.entry(new GTStoneLayersGen.OreDef("Au", true, 8, 33, 64), new Rule("", List.of(BiomeGroup.MESA))),
        Map.entry(new GTStoneLayersGen.OreDef("Dioptase", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.MOUNTAINS))),
        Map.entry(new GTStoneLayersGen.OreDef("Emerald", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.MOUNTAINS))),
        Map.entry(new GTStoneLayersGen.OreDef("Aquamarine", true, 64, 8, 32), new Rule("", List.of(BiomeGroup.OCEAN_BEACH))),
        Map.entry(new GTStoneLayersGen.OreDef("Morganite", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.JUNGLE))),
        Map.entry(new GTStoneLayersGen.OreDef("Heliodor", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.DESERT))),
        Map.entry(new GTStoneLayersGen.OreDef("Goshenite", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.FROZEN))),
        Map.entry(new GTStoneLayersGen.OreDef("Bixbite", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.SAVANNA))),
        Map.entry(new GTStoneLayersGen.OreDef("Maxixe", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.TAIGA))),
        Map.entry(new GTStoneLayersGen.OreDef("BlueTopaz", true, 64, 8, 32), new Rule("", List.of(BiomeGroup.OCEAN_BEACH))),
        Map.entry(new GTStoneLayersGen.OreDef("Topaz", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.FROZEN))),
        Map.entry(new GTStoneLayersGen.OreDef("Sapphire", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.FROZEN))),
        Map.entry(new GTStoneLayersGen.OreDef("GreenSapphire", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.JUNGLE))),
        Map.entry(new GTStoneLayersGen.OreDef("YellowSapphire", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.MOUNTAINS))),
        Map.entry(new GTStoneLayersGen.OreDef("OrangeSapphire", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.SAVANNA))),
        Map.entry(new GTStoneLayersGen.OreDef("BlueSapphire", true, 64, 8, 32), new Rule("", List.of(BiomeGroup.OCEAN_BEACH))),
        Map.entry(new GTStoneLayersGen.OreDef("PurpleSapphire", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.TAIGA))),
        Map.entry(new GTStoneLayersGen.OreDef("Ruby", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.DESERT))),
        Map.entry(new GTStoneLayersGen.OreDef("Craponite", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.JUNGLE))),
        Map.entry(new GTStoneLayersGen.OreDef("Amethyst", true, 64, 24, 48), new Rule("", List.of(BiomeGroup.TAIGA))),
        Map.entry(new GTStoneLayersGen.OreDef("Niter", true, 6, 40, 70), new Rule("", List.of(BiomeGroup.DESERT))),
        Map.entry(new GTStoneLayersGen.OreDef("Amber", true, 4, 30, 70), new Rule("", List.of(BiomeGroup.OCEAN_BEACH))),
        Map.entry(new GTStoneLayersGen.OreDef("Amber", true, 8, 30, 70), new Rule("", List.of(BiomeGroup.RIVER_LAKE))),
        Map.entry(new GTStoneLayersGen.OreDef("AmberDominican", true, 8, 30, 70), new Rule("", List.of(BiomeGroup.SHROOM))),
        Map.entry(new GTStoneLayersGen.OreDef("DiamondPink", true, 32, 0, 32), new Rule("", List.of(BiomeGroup.JUNGLE)))
    );
    public static Rule rule(GTStoneLayersGen.OreDef ore) { return RULES.getOrDefault(ore, DEFAULT); }
}
