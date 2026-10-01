package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;

import java.util.List;

/**
 * Bedrock ore deposits (GT6 {@code WorldgenOresBedrock}, {@code Loader_Worldgen:725-770}).
 * {@code chance} is the GT6 rarity divisor (bigger = rarer); the indicator flower grows
 * on the surface above the deposit (GT6 BlockFlowersA/B, real-world indicator plants) and is
 * {@code null} for the Nether deposits, which GT6 registers without a flower.
 */
public final class GTBedrockOres {
    private GTBedrockOres() {}

    public record BedrockOre(String name, GTMaterial material, int chance, String flowerId) {}

    public static final List<BedrockOre> OVERWORLD = List.of(
        new BedrockOre("diamond",     Materials.Diamond,              128000, "flower_pandanus_candelabrum"),
        new BedrockOre("tungstate",   com.gregtech.gregtech.content.material.generated.OreMaterials.Tungstate,     96000, "flower_tungstus"),
        new BedrockOre("ferberite",   com.gregtech.gregtech.content.material.generated.OreMaterials.Ferberite,     96000, "flower_tungstus"),
        new BedrockOre("wolframite",  com.gregtech.gregtech.content.material.generated.OreMaterials.Wolframite,    96000, "flower_tungstus"),
        new BedrockOre("stolzite",    com.gregtech.gregtech.content.material.generated.OreMaterials.Stolzite,      96000, "flower_tungstus"),
        new BedrockOre("scheelite",   com.gregtech.gregtech.content.material.generated.OreMaterials.Scheelite,     96000, "flower_tungstus"),
        new BedrockOre("huebnerite",  com.gregtech.gregtech.content.material.generated.OreMaterials.Huebnerite,    96000, "flower_tungstus"),
        new BedrockOre("russellite",  com.gregtech.gregtech.content.material.generated.OreMaterials.Russellite,    96000, "flower_tungstus"),
        new BedrockOre("pinalite",    com.gregtech.gregtech.content.material.generated.OreMaterials.Pinalite,      96000, "flower_tungstus"),
        new BedrockOre("uraninite",   com.gregtech.gregtech.content.material.generated.OreMaterials.Uraninite,     60000, "flower_tufted_evening_primrose"),
        new BedrockOre("pitchblende", com.gregtech.gregtech.content.material.generated.OreMaterials.Pitchblende,   60000, "flower_thompsons_locoweed"),
        new BedrockOre("gold_a",      Materials.Gold,                    32000, "flower_altered_andesite_buckwheat"),
        new BedrockOre("gold_b",      Materials.Gold,                    32000, "flower_desert_trumpet"),
        new BedrockOre("cooperite",   com.gregtech.gregtech.content.material.generated.OreMaterials.Cooperite,     16000, "flower_narcissus_sheldonia"),
        new BedrockOre("copper",      Materials.Copper,                    16000, "flower_copper_plant"),
        new BedrockOre("monazite",    Materials.Monazite,              16000, "flower_orechid"),
        new BedrockOre("powellite",   com.gregtech.gregtech.content.material.generated.OreMaterials.Powellite,     14000, "flower_orechid"),
        new BedrockOre("bastnasite",  com.gregtech.gregtech.content.material.generated.OreMaterials.Bastnasite,     8000, "flower_orechid"),
        new BedrockOre("arsenopyrite",com.gregtech.gregtech.content.material.generated.OreMaterials.Arsenopyrite,   8000, "flower_sagebrush"),
        new BedrockOre("redstone",    Materials.Redstone,               7000, "flower_prince_s_plume"),
        new BedrockOre("vanadium",    Materials.VanadiumPentoxide,                   6000, "flower_orechid"),
        new BedrockOre("galena",      com.gregtech.gregtech.content.material.generated.OreMaterials.Galena,         6000, "flower_crosby_buckwheat"),
        new BedrockOre("coal",        Materials.Coal,                   5000, "flower_orechid"),
        new BedrockOre("graphite",    Materials.Graphite,               5000, "flower_orechid"),
        new BedrockOre("stibnite",    com.gregtech.gregtech.content.material.generated.OreMaterials.Stibnite,       4000, "flower_four_wing_saltbush"),
        new BedrockOre("zinc",        Materials.Zinc,                     4000, "flower_viola_calaminaria"),
        new BedrockOre("nickel",      Materials.Nickel,                     4000, "flower_thlaspi_lereschianum"),
        new BedrockOre("alpine_copper", com.gregtech.gregtech.content.material.generated.OreMaterials.Chalcopyrite, 4000, "flower_alpine_catchfly")
    );

    /**
     * The Nether deposits (GT6 {@code Loader_Worldgen:758-764}). GT6 registers these without an
     * indicator flower — only surface rocks — so {@code flowerId} is null.
     */
    public static final List<BedrockOre> NETHER = List.of(
        new BedrockOre("voidquartz",     Materials.VoidQuartz,     4000, null),
        new BedrockOre("glowstone",      Materials.Glowstone,      4000, null),
        new BedrockOre("gloomstone",     Materials.Gloomstone,     4000, null),
        new BedrockOre("efrine",         Materials.Efrine,         2000, null),
        new BedrockOre("netherquartz",   Materials.NetherQuartz,   2000, null),
        new BedrockOre("firestone",      Materials.Firestone,      8000, null),
        new BedrockOre("ancientdebris",  Materials.AncientDebris,  4000, null)
    );

}
