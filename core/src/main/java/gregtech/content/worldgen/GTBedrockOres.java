package gregtech.content.worldgen;

import static gregtech.api.addon.worldgen.OreVeinDeclaration.NETHER;
import static gregtech.api.addon.worldgen.OreVeinDeclaration.OVERWORLD;

import gregtech.api.material.GTMaterial;
import gregtech.content.material.Materials;
import gregtech.content.material.generated.OreMaterials;

/** Bedrock-specific eligibility, indicator flowers and baseline rarity from GT6 Loader_Worldgen. */
public final class GTBedrockOres {
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

    static void register(OreSourceCatalog.Builder catalog) {
        catalog.bedrock(OVERWORLD, "diamond", Materials.Diamond, 128000, "flower_pandanus_candelabrum");
        catalog.bedrock(OVERWORLD, "tungstate", OreMaterials.Tungstate, 96000, "flower_tungstus");
        catalog.bedrock(OVERWORLD, "ferberite", OreMaterials.Ferberite, 96000, "flower_tungstus");
        catalog.bedrock(OVERWORLD, "wolframite", OreMaterials.Wolframite, 96000, "flower_tungstus");
        catalog.bedrock(OVERWORLD, "stolzite", OreMaterials.Stolzite, 96000, "flower_tungstus");
        catalog.bedrock(OVERWORLD, "scheelite", OreMaterials.Scheelite, 96000, "flower_tungstus");
        catalog.bedrock(OVERWORLD, "huebnerite", OreMaterials.Huebnerite, 96000, "flower_tungstus");
        catalog.bedrock(OVERWORLD, "russellite", OreMaterials.Russellite, 96000, "flower_tungstus");
        catalog.bedrock(OVERWORLD, "pinalite", OreMaterials.Pinalite, 96000, "flower_tungstus");
        catalog.bedrock(OVERWORLD, "uraninite", OreMaterials.Uraninite, 60000, "flower_tufted_evening_primrose");
        catalog.bedrock(OVERWORLD, "pitchblende", OreMaterials.Pitchblende, 60000, "flower_thompsons_locoweed");
        catalog.bedrock(OVERWORLD, "gold_a", Materials.Gold, 32000, "flower_altered_andesite_buckwheat");
        catalog.bedrock(OVERWORLD, "gold_b", Materials.Gold, 32000, "flower_desert_trumpet");
        catalog.bedrock(OVERWORLD, "cooperite", OreMaterials.Cooperite, 16000, "flower_narcissus_sheldonia");
        catalog.bedrock(OVERWORLD, "copper", Materials.Copper, 16000, "flower_copper_plant");
        catalog.bedrock(OVERWORLD, "monazite", Materials.Monazite, 16000, "flower_vindicator_flower");
        catalog.bedrock(OVERWORLD, "powellite", OreMaterials.Powellite, 14000, "flower_orechid");
        catalog.bedrock(OVERWORLD, "bastnasite", OreMaterials.Bastnasite, 8000, "flower_vindicator_flower");
        catalog.bedrock(OVERWORLD, "arsenopyrite", OreMaterials.Arsenopyrite, 8000, "flower_sagebrush");
        catalog.bedrock(OVERWORLD, "redstone", Materials.Redstone, 7000, "flower_prince_s_plume");
        catalog.bedrock(OVERWORLD, "vanadium", Materials.VanadiumPentoxide, 6000, "flower_orechid");
        catalog.bedrock(OVERWORLD, "galena", OreMaterials.Galena, 6000, "flower_crosby_buckwheat");
        catalog.bedrock(OVERWORLD, "coal", Materials.Coal, 5000, "flower_orechid");
        catalog.bedrock(OVERWORLD, "graphite", Materials.Graphite, 5000, "flower_orechid");
        catalog.bedrock(OVERWORLD, "stibnite", OreMaterials.Stibnite, 4000, "flower_four_wing_saltbush");
        catalog.bedrock(OVERWORLD, "hematite", Materials.Hematite, 4000, "flower_orechid");
        catalog.bedrock(OVERWORLD, "sphalerite", OreMaterials.Sphalerite, 3000, "flower_viola_calaminaria");
        catalog.bedrock(OVERWORLD, "smithsonite", OreMaterials.Smithsonite, 3000, "flower_viola_calaminaria");
        catalog.bedrock(OVERWORLD, "pentlandite", OreMaterials.Pentlandite, 3000, "flower_thlaspi_lereschianum");
        catalog.bedrock(OVERWORLD, "saltpeter", Materials.Niter, 3000, "flower_orechid");
        catalog.bedrock(OVERWORLD, "bauxite", OreMaterials.Bauxite, 2000, "flower_orechid");
        catalog.bedrock(OVERWORLD, "cassiterite", OreMaterials.Cassiterite, 2000, "flower_orechid");
        catalog.bedrock(OVERWORLD, "chalcopyrite", OreMaterials.Chalcopyrite, 2000, "flower_alpine_catchfly");

        catalog.bedrock(NETHER, "glowstone", Materials.Glowstone, 4000, null);
        catalog.bedrock(NETHER, "netherquartz", Materials.NetherQuartz, 2000, null);
        catalog.bedrock(NETHER, "ancientdebris", Materials.AncientDebris, 4000, null);
        catalog.bedrock(NETHER, "efrine", Materials.Efrine, 2000, null);
        catalog.bedrock(NETHER, "firestone", Materials.Firestone, 8000, null);
    }
}
