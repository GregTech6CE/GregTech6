package gregtech.content.worldgen;

import gregtech.content.material.Materials;
import gregtech.internal.material.GTMaterial;

import java.util.List;

/**
 * Current port catalogs for weighted bedrock selection, not the complete original registration lists.
 * Original shared-floor membership and per-row attempts require separate dispatch logic.
 */
public final class GTBedrockOres {

    private GTBedrockOres() {
    }

    public record BedrockOre(String name, GTMaterial material, int chance, String flowerId) {
    }

    public static final List<BedrockOre> OVERWORLD = List.of(
        new BedrockOre("diamond",       Materials.Diamond,                                                          128000, "flower_pandanus_candelabrum"),
        new BedrockOre("tungstate",     gregtech.content.material.generated.OreMaterials.Tungstate,    96000,  "flower_tungstus"),
        new BedrockOre("ferberite",     gregtech.content.material.generated.OreMaterials.Ferberite,    96000,  "flower_tungstus"),
        new BedrockOre("wolframite",    gregtech.content.material.generated.OreMaterials.Wolframite,   96000,  "flower_tungstus"),
        new BedrockOre("stolzite",      gregtech.content.material.generated.OreMaterials.Stolzite,     96000,  "flower_tungstus"),
        new BedrockOre("scheelite",     gregtech.content.material.generated.OreMaterials.Scheelite,    96000,  "flower_tungstus"),
        new BedrockOre("huebnerite",    gregtech.content.material.generated.OreMaterials.Huebnerite,   96000,  "flower_tungstus"),
        new BedrockOre("russellite",    gregtech.content.material.generated.OreMaterials.Russellite,   96000,  "flower_tungstus"),
        new BedrockOre("pinalite",      gregtech.content.material.generated.OreMaterials.Pinalite,     96000,  "flower_tungstus"),
        new BedrockOre("uraninite",     gregtech.content.material.generated.OreMaterials.Uraninite,    60000,  "flower_tufted_evening_primrose"),
        new BedrockOre("pitchblende",   gregtech.content.material.generated.OreMaterials.Pitchblende,  60000,  "flower_thompsons_locoweed"),
        new BedrockOre("gold_a",        Materials.Gold,                                                             32000,  "flower_altered_andesite_buckwheat"),
        new BedrockOre("gold_b",        Materials.Gold,                                                             32000,  "flower_desert_trumpet"),
        new BedrockOre("cooperite",     gregtech.content.material.generated.OreMaterials.Cooperite,    16000,  "flower_narcissus_sheldonia"),
        new BedrockOre("copper",        Materials.Copper,                                                           16000,  "flower_copper_plant"),
        new BedrockOre("monazite",      Materials.Monazite,                                                         16000,  "flower_vindicator_flower"),
        new BedrockOre("powellite",     gregtech.content.material.generated.OreMaterials.Powellite,    14000,  "flower_orechid"),
        new BedrockOre("bastnasite",    gregtech.content.material.generated.OreMaterials.Bastnasite,   8000,   "flower_vindicator_flower"),
        new BedrockOre("arsenopyrite",  gregtech.content.material.generated.OreMaterials.Arsenopyrite, 8000,   "flower_sagebrush"),
        new BedrockOre("redstone",      Materials.Redstone,                                                         7000,   "flower_prince_s_plume"),
        new BedrockOre("vanadium",      Materials.VanadiumPentoxide,                                                6000,   "flower_orechid"),
        new BedrockOre("galena",        gregtech.content.material.generated.OreMaterials.Galena,       6000,   "flower_crosby_buckwheat"),
        new BedrockOre("coal",          Materials.Coal,                                                             5000,   "flower_orechid"),
        new BedrockOre("graphite",      Materials.Graphite,                                                         5000,   "flower_orechid"),
        new BedrockOre("stibnite",      gregtech.content.material.generated.OreMaterials.Stibnite,     4000,   "flower_four_wing_saltbush"),
        new BedrockOre("zinc",          Materials.Zinc,                                                             4000,   "flower_viola_calaminaria"),
        new BedrockOre("nickel",        Materials.Nickel,                                                           4000,   "flower_thlaspi_lereschianum"),
        new BedrockOre("alpine_copper", gregtech.content.material.generated.OreMaterials.Chalcopyrite, 4000,   "flower_alpine_catchfly")
    );

    /**
     * The seven Nether-specific rows ({@code Loader_Worldgen:758-764}), registered without an
     * indicator flower — only surface rocks — so {@code flowerId} is null.
     */
    public static final List<BedrockOre> NETHER = List.of(
        new BedrockOre("voidquartz",    Materials.VoidQuartz,    4000, null),
        new BedrockOre("glowstone",     Materials.Glowstone,     4000, null),
        new BedrockOre("gloomstone",    Materials.Gloomstone,    4000, null),
        new BedrockOre("efrine",        Materials.Efrine,        2000, null),
        new BedrockOre("netherquartz",  Materials.NetherQuartz,  2000, null),
        new BedrockOre("firestone",     Materials.Firestone,     8000, null),
        new BedrockOre("ancientdebris", Materials.AncientDebris, 4000, null)
    );

}
