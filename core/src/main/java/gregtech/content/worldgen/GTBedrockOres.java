package gregtech.content.worldgen;

import static gregtech.api.addon.worldgen.OreVeinDeclaration.NETHER;

import gregtech.api.material.GTMaterial;
import gregtech.content.material.Materials;
import gregtech.content.material.generated.OreMaterials;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Bedrock-specific eligibility, indicator flowers and baseline rarity from GT6 Loader_Worldgen. */
public final class GTBedrockOres {
    public static final int DEFAULT_DENSITY_PERCENT = 100;

    private GTBedrockOres() {}

    public enum Depth {
        SHALLOW,
        DEEP,
        NEUTRAL
    }

    public record BedrockOre(String name, GTMaterial material, int chance, String flowerId, Depth depth) {
        public BedrockOre(String name, GTMaterial material, int chance, String flowerId) {
            this(name, material, chance, flowerId, Depth.NEUTRAL);
        }

        public BedrockOre withChance(int divisor) {
            return new BedrockOre(name, material, divisor, flowerId, depth);
        }
    }

    /** Historical overrides keep rare deposits, flower pairs and existing per-ore configuration keys. */
    record DepositOverride(String name, int divisor, String flower) {}

    private static final Map<GTMaterial, List<DepositOverride>> OVERRIDES = Map.ofEntries(
            Map.entry(
                    Materials.Diamond, List.of(new DepositOverride("diamond", 128000, "flower_pandanus_candelabrum"))),
            Map.entry(OreMaterials.Tungstate, List.of(new DepositOverride("tungstate", 96000, "flower_tungstus"))),
            Map.entry(OreMaterials.Ferberite, List.of(new DepositOverride("ferberite", 96000, "flower_tungstus"))),
            Map.entry(OreMaterials.Wolframite, List.of(new DepositOverride("wolframite", 96000, "flower_tungstus"))),
            Map.entry(OreMaterials.Stolzite, List.of(new DepositOverride("stolzite", 96000, "flower_tungstus"))),
            Map.entry(OreMaterials.Scheelite, List.of(new DepositOverride("scheelite", 96000, "flower_tungstus"))),
            Map.entry(OreMaterials.Huebnerite, List.of(new DepositOverride("huebnerite", 96000, "flower_tungstus"))),
            Map.entry(OreMaterials.Russellite, List.of(new DepositOverride("russellite", 96000, "flower_tungstus"))),
            Map.entry(OreMaterials.Pinalite, List.of(new DepositOverride("pinalite", 96000, "flower_tungstus"))),
            Map.entry(
                    OreMaterials.Uraninite,
                    List.of(new DepositOverride("uraninite", 60000, "flower_tufted_evening_primrose"))),
            Map.entry(
                    OreMaterials.Pitchblende,
                    List.of(new DepositOverride("pitchblende", 60000, "flower_thompsons_locoweed"))),
            Map.entry(
                    Materials.Gold,
                    List.of(
                            new DepositOverride("gold_a", 32000, "flower_altered_andesite_buckwheat"),
                            new DepositOverride("gold_b", 32000, "flower_desert_trumpet"))),
            Map.entry(
                    OreMaterials.Cooperite,
                    List.of(new DepositOverride("cooperite", 16000, "flower_narcissus_sheldonia"))),
            Map.entry(Materials.Copper, List.of(new DepositOverride("copper", 16000, "flower_copper_plant"))),
            Map.entry(Materials.Monazite, List.of(new DepositOverride("monazite", 16000, "flower_vindicator_flower"))),
            Map.entry(OreMaterials.Powellite, List.of(new DepositOverride("powellite", 14000, "flower_orechid"))),
            Map.entry(
                    OreMaterials.Bastnasite,
                    List.of(new DepositOverride("bastnasite", 8000, "flower_vindicator_flower"))),
            Map.entry(
                    OreMaterials.Arsenopyrite, List.of(new DepositOverride("arsenopyrite", 8000, "flower_sagebrush"))),
            Map.entry(Materials.Redstone, List.of(new DepositOverride("redstone", 7000, "flower_prince_s_plume"))),
            Map.entry(Materials.VanadiumPentoxide, List.of(new DepositOverride("vanadium", 6000, "flower_orechid"))),
            Map.entry(OreMaterials.Galena, List.of(new DepositOverride("galena", 6000, "flower_crosby_buckwheat"))),
            Map.entry(Materials.Coal, List.of(new DepositOverride("coal", 5000, "flower_orechid"))),
            Map.entry(Materials.Graphite, List.of(new DepositOverride("graphite", 5000, "flower_orechid"))),
            Map.entry(
                    OreMaterials.Stibnite, List.of(new DepositOverride("stibnite", 4000, "flower_four_wing_saltbush"))),
            Map.entry(Materials.Hematite, List.of(new DepositOverride("hematite", 4000, "flower_orechid"))),
            Map.entry(
                    OreMaterials.Sphalerite,
                    List.of(new DepositOverride("sphalerite", 3000, "flower_viola_calaminaria"))),
            Map.entry(
                    OreMaterials.Smithsonite,
                    List.of(new DepositOverride("smithsonite", 3000, "flower_viola_calaminaria"))),
            Map.entry(
                    OreMaterials.Pentlandite,
                    List.of(new DepositOverride("pentlandite", 3000, "flower_thlaspi_lereschianum"))),
            Map.entry(Materials.Niter, List.of(new DepositOverride("saltpeter", 3000, "flower_orechid"))),
            Map.entry(OreMaterials.Bauxite, List.of(new DepositOverride("bauxite", 2000, "flower_orechid"))),
            Map.entry(OreMaterials.Cassiterite, List.of(new DepositOverride("cassiterite", 2000, "flower_orechid"))),
            Map.entry(
                    OreMaterials.Chalcopyrite,
                    List.of(new DepositOverride("chalcopyrite", 2000, "flower_alpine_catchfly"))));

    static Set<GTMaterial> historicalMaterials() {
        return OVERRIDES.keySet();
    }

    static List<DepositOverride> overworld(GTMaterial material, int primaryVeinWeight) {
        var overrides = OVERRIDES.get(material);
        if (overrides != null) return overrides;
        if (primaryVeinWeight == 0) return List.of();
        int divisor = Math.max(8000, 1_600_000 / primaryVeinWeight);
        return List.of(new DepositOverride(material.getName().toLowerCase(Locale.ROOT), divisor, null));
    }

    static void registerNether(OreSourceCatalog.Builder catalog) {
        catalog.bedrock(NETHER, "glowstone", Materials.Glowstone, 4000, null);
        catalog.bedrock(NETHER, "netherquartz", Materials.NetherQuartz, 2000, null);
        catalog.bedrock(NETHER, "ancientdebris", Materials.AncientDebris, 4000, null);
        catalog.bedrock(NETHER, "efrine", Materials.Efrine, 2000, null);
        catalog.bedrock(NETHER, "firestone", Materials.Firestone, 8000, null);
    }
}
