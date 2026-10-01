package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.generated.GT6Materials;

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
            new Spec("lead", com.gregtech.gregtech.content.material.generated.ElementMaterials.Lead, 4.0F, 4.0F),
            new Spec("bismuth", com.gregtech.gregtech.content.material.generated.ElementMaterials.Bismuth, 4.0F, 4.0F),
            new Spec("antimony", com.gregtech.gregtech.content.material.generated.ElementMaterials.Antimony, 4.0F, 4.0F),
            new Spec("nickel", com.gregtech.gregtech.content.material.generated.ElementMaterials.Nickel, 4.0F, 4.0F),
            new Spec("constantan", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Constantan, 4.0F, 4.0F),
            new Spec("bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze, 7.0F, 7.0F),
            new Spec("arsenic_copper", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicCopper, 7.5F, 7.5F),
            new Spec("aluminium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Aluminium, 2.0F, 2.0F),
            new Spec("brass", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Brass, 2.5F, 2.5F),
            new Spec("tin_alloy", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TinAlloy, 3.0F, 3.0F),
            new Spec("cobalt", com.gregtech.gregtech.content.material.generated.ElementMaterials.Cobalt, 4.0F, 4.0F),
            new Spec("ardite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ardite, 2.0F, 2.0F),
            new Spec("arsenic_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ArsenicBronze, 8.0F, 8.0F),
            new Spec("bismuth_bronze", com.gregtech.gregtech.content.material.generated.CompoundMaterials.BismuthBronze, 8.0F, 8.0F),
            new Spec("germanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Germanium, 4.0F, 4.0F),
            new Spec("invar", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar, 4.0F, 4.0F),
            new Spec("steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel, 6.0F, 6.0F),
            new Spec("hsla", com.gregtech.gregtech.content.material.generated.CompoundMaterials.HSLASteel, 6.0F, 6.0F),
            new Spec("gold", com.gregtech.gregtech.content.material.generated.ElementMaterials.Gold, 3.0F, 3.0F),
            new Spec("silver", com.gregtech.gregtech.content.material.generated.ElementMaterials.Silver, 3.0F, 3.0F),
            new Spec("manganese", com.gregtech.gregtech.content.material.generated.ElementMaterials.Manganese, 6.0F, 6.0F),
            new Spec("manyullyn", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Manyullyn, 4.0F, 4.0F),
            new Spec("lumium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Lumium, 2.0F, 2.0F),
            new Spec("knightmetal", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Knightmetal, 7.0F, 7.0F),
            new Spec("steel_galvanized", com.gregtech.gregtech.content.material.generated.CompoundMaterials.SteelGalvanized, 6.0F, 6.0F),
            new Spec("meteorite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Meteorite, 7.0F, 7.0F),
            new Spec("meteoric_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.MeteoricSteel, 8.0F, 8.0F),
            new Spec("gilded_iron", com.gregtech.gregtech.content.material.generated.CompoundMaterials.GildedIron, 6.0F, 6.0F),
            new Spec("molybdenum", com.gregtech.gregtech.content.material.generated.ElementMaterials.Molybdenum, 6.0F, 6.0F),
            new Spec("syrmorite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Syrmorite, 4.0F, 4.0F),
            new Spec("electrum", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Electrum, 3.0F, 3.0F),
            new Spec("stainless_steel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.StainlessSteel, 5.0F, 5.0F),
            new Spec("thaumium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Thaumium, 9.0F, 9.0F),
            new Spec("manasteel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Manasteel, 9.0F, 9.0F),
            new Spec("efrine", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Efrine, 8.0F, 8.0F),
            new Spec("tungsten_alloy", com.gregtech.gregtech.content.material.generated.CompoundMaterials.HSLATungstenAlloy, 8.0F, 8.0F),
            new Spec("titanium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium, 9.0F, 9.0F),
            new Spec("netherite", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite, 10.0F, 10.0F),
            new Spec("chromium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium, 4.0F, 4.0F),
            new Spec("platinum", com.gregtech.gregtech.content.material.generated.ElementMaterials.Platinum, 2.0F, 2.0F),
            new Spec("octine", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Octine, 8.0F, 8.0F),
            new Spec("desh", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Desh, 15.0F, 15.0F),
            new Spec("terrasteel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Terrasteel, 15.0F, 15.0F),
            new Spec("tungstensteel", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel, 12.5F, 12.5F),
            new Spec("tungsten_carbide", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TungstenCarbide, 12.5F, 12.5F),
            new Spec("duranium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Duranium, 20.0F, 20.0F),
            new Spec("draconium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Draconium, 50.0F, 50.0F),
            new Spec("ultimet", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ultimet, 12.5F, 12.5F),
            new Spec("desh_alloy", com.gregtech.gregtech.content.material.generated.CompoundMaterials.WorkersAlloy, 15.0F, 15.0F),
            new Spec("tungsten", com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten, 10.0F, 10.0F),
            new Spec("palladium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Palladium, 15.0F, 15.0F),
            new Spec("iridium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Iridium, 15.0F, 15.0F),
            new Spec("osmium", com.gregtech.gregtech.content.material.generated.ElementMaterials.OsmiumElemental, 9.0F, 9.0F),
            new Spec("void_metal", com.gregtech.gregtech.content.material.generated.CompoundMaterials.VoidMetal, 30.0F, 30.0F),
            new Spec("elven_elementium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.ElvenElementium, 30.0F, 30.0F),
            new Spec("tritanium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tritanium, 30.0F, 30.0F),
            new Spec("adamantium", com.gregtech.gregtech.content.material.generated.ElementMaterials.Adamantium, 100.0F, 100.0F),
            new Spec("bedrock_hsla", com.gregtech.gregtech.content.material.generated.CompoundMaterials.BedrockHSLAAlloy, 100.0F, 100.0F),
            new Spec("draconium_awakened", com.gregtech.gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened, 100.0F, 100.0F),
            new Spec("infinity", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Infinity, 100.0F, 100.0F)
    );
}
