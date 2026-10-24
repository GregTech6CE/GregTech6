package gregtech.content.registry;

import gregtech.content.material.Materials;
import gregtech.internal.material.GTMaterial;
import gregtech.content.data.generated.GT6Materials;

import java.util.List;

/**
 * GT6 metalset materials (Loader_MultiTileEntities.metalset) shared by the
 * per-material storage blocks: mass storage, metal chests.
 *
 * <p>GENERATED from the hopper table — keep in GT6 aID order.</p>
 */
public final class GTStorageMetals {
    private GTStorageMetals() {}

    public record Spec(String suffix, GTMaterial material, float hardness, float resistance) {}

    public static final List<Spec> ALL = List.of(
            new Spec("lead",               gregtech.content.material.generated.ElementMaterials.Lead,               4.0F,   4.0F),
            new Spec("bismuth",            gregtech.content.material.generated.ElementMaterials.Bismuth,            4.0F,   4.0F),
            new Spec("antimony",           gregtech.content.material.generated.ElementMaterials.Antimony,           4.0F,   4.0F),
            new Spec("nickel",             gregtech.content.material.generated.ElementMaterials.Nickel,             4.0F,   4.0F),
            new Spec("constantan",         gregtech.content.material.generated.CompoundMaterials.Constantan,        4.0F,   4.0F),
            new Spec("bronze",             gregtech.content.material.generated.CompoundMaterials.Bronze,            7.0F,   7.0F),
            new Spec("arsenic_copper",     gregtech.content.material.generated.CompoundMaterials.ArsenicCopper,     7.5F,   7.5F),
            new Spec("aluminium",          gregtech.content.material.generated.ElementMaterials.Aluminium,          2.0F,   2.0F),
            new Spec("brass",              gregtech.content.material.generated.CompoundMaterials.Brass,             2.5F,   2.5F),
            new Spec("tin_alloy",          gregtech.content.material.generated.CompoundMaterials.TinAlloy,          3.0F,   3.0F),
            new Spec("cobalt",             gregtech.content.material.generated.ElementMaterials.Cobalt,             4.0F,   4.0F),
            new Spec("ardite",             gregtech.content.material.generated.CompoundMaterials.Ardite,            2.0F,   2.0F),
            new Spec("arsenic_bronze",     gregtech.content.material.generated.CompoundMaterials.ArsenicBronze,     8.0F,   8.0F),
            new Spec("bismuth_bronze",     gregtech.content.material.generated.CompoundMaterials.BismuthBronze,     8.0F,   8.0F),
            new Spec("germanium",          gregtech.content.material.generated.ElementMaterials.Germanium,          4.0F,   4.0F),
            new Spec("invar",              gregtech.content.material.generated.CompoundMaterials.Invar,             4.0F,   4.0F),
            new Spec("steel",              gregtech.content.material.generated.CompoundMaterials.Steel,             6.0F,   6.0F),
            new Spec("hsla",               gregtech.content.material.generated.CompoundMaterials.HSLASteel,         6.0F,   6.0F),
            new Spec("gold",               gregtech.content.material.generated.ElementMaterials.Gold,               3.0F,   3.0F),
            new Spec("silver",             gregtech.content.material.generated.ElementMaterials.Silver,             3.0F,   3.0F),
            new Spec("manganese",          gregtech.content.material.generated.ElementMaterials.Manganese,          6.0F,   6.0F),
            new Spec("manyullyn",          gregtech.content.material.generated.CompoundMaterials.Manyullyn,         4.0F,   4.0F),
            new Spec("lumium",             gregtech.content.material.generated.CompoundMaterials.Lumium,            2.0F,   2.0F),
            new Spec("knightmetal",        gregtech.content.material.generated.CompoundMaterials.Knightmetal,       7.0F,   7.0F),
            new Spec("steel_galvanized",   gregtech.content.material.generated.CompoundMaterials.SteelGalvanized,   6.0F,   6.0F),
            new Spec("meteorite",          gregtech.content.material.generated.CompoundMaterials.Meteorite,         7.0F,   7.0F),
            new Spec("meteoric_steel",     gregtech.content.material.generated.CompoundMaterials.MeteoricSteel,     8.0F,   8.0F),
            new Spec("gilded_iron",        gregtech.content.material.generated.CompoundMaterials.GildedIron,        6.0F,   6.0F),
            new Spec("molybdenum",         gregtech.content.material.generated.ElementMaterials.Molybdenum,         6.0F,   6.0F),
            new Spec("syrmorite",          gregtech.content.material.generated.CompoundMaterials.Syrmorite,         4.0F,   4.0F),
            new Spec("electrum",           gregtech.content.material.generated.CompoundMaterials.Electrum,          3.0F,   3.0F),
            new Spec("stainless_steel",    gregtech.content.material.generated.CompoundMaterials.StainlessSteel,    5.0F,   5.0F),
            new Spec("thaumium",           gregtech.content.material.generated.CompoundMaterials.Thaumium,          9.0F,   9.0F),
            new Spec("manasteel",          gregtech.content.material.generated.CompoundMaterials.Manasteel,         9.0F,   9.0F),
            new Spec("efrine",             gregtech.content.material.generated.CompoundMaterials.Efrine,            8.0F,   8.0F),
            new Spec("tungsten_alloy",     gregtech.content.material.generated.CompoundMaterials.HSLATungstenAlloy, 8.0F,   8.0F),
            new Spec("titanium",           gregtech.content.material.generated.ElementMaterials.Titanium,           9.0F,   9.0F),
            new Spec("netherite",          gregtech.content.material.generated.CompoundMaterials.Netherite,         10.0F,  10.0F),
            new Spec("chromium",           gregtech.content.material.generated.ElementMaterials.Chromium,           4.0F,   4.0F),
            new Spec("platinum",           gregtech.content.material.generated.ElementMaterials.Platinum,           2.0F,   2.0F),
            new Spec("octine",             gregtech.content.material.generated.CompoundMaterials.Octine,            8.0F,   8.0F),
            new Spec("desh",               gregtech.content.material.generated.CompoundMaterials.Desh,              15.0F,  15.0F),
            new Spec("terrasteel",         gregtech.content.material.generated.CompoundMaterials.Terrasteel,        15.0F,  15.0F),
            new Spec("tungstensteel",      gregtech.content.material.generated.CompoundMaterials.Tungstensteel,     12.5F,  12.5F),
            new Spec("tungsten_carbide",   gregtech.content.material.generated.CompoundMaterials.TungstenCarbide,   12.5F,  12.5F),
            new Spec("duranium",           gregtech.content.material.generated.CompoundMaterials.Duranium,          20.0F,  20.0F),
            new Spec("draconium",          gregtech.content.material.generated.CompoundMaterials.Draconium,         50.0F,  50.0F),
            new Spec("ultimet",            gregtech.content.material.generated.CompoundMaterials.Ultimet,           12.5F,  12.5F),
            new Spec("desh_alloy",         gregtech.content.material.generated.CompoundMaterials.WorkersAlloy,      15.0F,  15.0F),
            new Spec("tungsten",           gregtech.content.material.generated.ElementMaterials.Tungsten,           10.0F,  10.0F),
            new Spec("palladium",          gregtech.content.material.generated.ElementMaterials.Palladium,          15.0F,  15.0F),
            new Spec("iridium",            gregtech.content.material.generated.ElementMaterials.Iridium,            15.0F,  15.0F),
            new Spec("osmium",             gregtech.content.material.generated.ElementMaterials.OsmiumElemental,    9.0F,   9.0F),
            new Spec("void_metal",         gregtech.content.material.generated.CompoundMaterials.VoidMetal,         30.0F,  30.0F),
            new Spec("elven_elementium",   gregtech.content.material.generated.CompoundMaterials.ElvenElementium,   30.0F,  30.0F),
            new Spec("tritanium",          gregtech.content.material.generated.CompoundMaterials.Tritanium,         30.0F,  30.0F),
            new Spec("adamantium",         gregtech.content.material.generated.ElementMaterials.Adamantium,         100.0F, 100.0F),
            new Spec("bedrock_hsla",       gregtech.content.material.generated.CompoundMaterials.BedrockHSLAAlloy,  100.0F, 100.0F),
            new Spec("draconium_awakened", gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened, 100.0F, 100.0F),
            new Spec("infinity",           gregtech.content.material.generated.CompoundMaterials.Infinity,          100.0F, 100.0F)
    );
}
