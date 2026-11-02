package gregtech.content.transport;

import gregtech.engine.machine.HopperSpec;

/** Complete original hopper/queuehopper material registry metadata, shared by both loaders. */
public final class HopperCatalog {
    private HopperCatalog() {}

    public record Entry(boolean queue, HopperSpec spec) {}

    public static final java.util.List<Entry> ALL = java.util.List.of(
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_lead", gregtech.content.material.generated.ElementMaterials.Lead, 1, 4.0F, 4.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_bismuth",
                            gregtech.content.material.generated.ElementMaterials.Bismuth,
                            2,
                            4.0F,
                            4.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_antimony",
                            gregtech.content.material.generated.ElementMaterials.Antimony,
                            2,
                            4.0F,
                            4.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_nickel",
                            gregtech.content.material.generated.ElementMaterials.Nickel,
                            3,
                            4.0F,
                            4.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_constantan",
                            gregtech.content.material.generated.CompoundMaterials.Constantan,
                            3,
                            4.0F,
                            4.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_bronze",
                            gregtech.content.material.generated.CompoundMaterials.Bronze,
                            3,
                            7.0F,
                            7.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_arsenic_copper",
                            gregtech.content.material.generated.CompoundMaterials.ArsenicCopper,
                            4,
                            7.5F,
                            7.5F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_aluminium",
                            gregtech.content.material.generated.ElementMaterials.Aluminium,
                            4,
                            2.0F,
                            2.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_brass",
                            gregtech.content.material.generated.CompoundMaterials.Brass,
                            4,
                            2.5F,
                            2.5F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_tin_alloy",
                            gregtech.content.material.generated.CompoundMaterials.TinAlloy,
                            4,
                            3.0F,
                            3.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_cobalt",
                            gregtech.content.material.generated.ElementMaterials.Cobalt,
                            4,
                            4.0F,
                            4.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_arsenic_bronze",
                            gregtech.content.material.generated.CompoundMaterials.ArsenicBronze,
                            5,
                            8.0F,
                            8.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_bismuth_bronze",
                            gregtech.content.material.generated.CompoundMaterials.BismuthBronze,
                            5,
                            8.0F,
                            8.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_germanium",
                            gregtech.content.material.generated.ElementMaterials.Germanium,
                            5,
                            4.0F,
                            4.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_invar",
                            gregtech.content.material.generated.CompoundMaterials.Invar,
                            5,
                            4.0F,
                            4.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_steel",
                            gregtech.content.material.generated.CompoundMaterials.Steel,
                            5,
                            6.0F,
                            6.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_gold", gregtech.content.material.generated.ElementMaterials.Gold, 6, 3.0F, 3.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_silver",
                            gregtech.content.material.generated.ElementMaterials.Silver,
                            6,
                            3.0F,
                            3.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_manganese",
                            gregtech.content.material.generated.ElementMaterials.Manganese,
                            6,
                            6.0F,
                            6.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_lumium",
                            gregtech.content.material.generated.CompoundMaterials.Lumium,
                            6,
                            2.0F,
                            2.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_knightmetal",
                            gregtech.content.material.generated.CompoundMaterials.Knightmetal,
                            7,
                            7.0F,
                            7.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_steel_galvanized",
                            gregtech.content.material.generated.CompoundMaterials.SteelGalvanized,
                            7,
                            6.0F,
                            6.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_molybdenum",
                            gregtech.content.material.generated.ElementMaterials.Molybdenum,
                            8,
                            6.0F,
                            6.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_electrum",
                            gregtech.content.material.generated.CompoundMaterials.Electrum,
                            9,
                            3.0F,
                            3.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_stainless_steel",
                            gregtech.content.material.generated.CompoundMaterials.StainlessSteel,
                            9,
                            5.0F,
                            5.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_thaumium",
                            gregtech.content.material.generated.CompoundMaterials.Thaumium,
                            9,
                            9.0F,
                            9.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_manasteel",
                            gregtech.content.material.generated.CompoundMaterials.Manasteel,
                            9,
                            9.0F,
                            9.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_titanium",
                            gregtech.content.material.generated.ElementMaterials.Titanium,
                            12,
                            9.0F,
                            9.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_netherite",
                            gregtech.content.material.generated.CompoundMaterials.Netherite,
                            12,
                            10.0F,
                            10.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_chromium",
                            gregtech.content.material.generated.ElementMaterials.Chromium,
                            14,
                            4.0F,
                            4.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_platinum",
                            gregtech.content.material.generated.ElementMaterials.Platinum,
                            18,
                            2.0F,
                            2.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_terrasteel",
                            gregtech.content.material.generated.CompoundMaterials.Terrasteel,
                            18,
                            15.0F,
                            15.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_tungstensteel",
                            gregtech.content.material.generated.CompoundMaterials.Tungstensteel,
                            27,
                            12.5F,
                            12.5F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_tungsten_carbide",
                            gregtech.content.material.generated.CompoundMaterials.TungstenCarbide,
                            27,
                            12.5F,
                            12.5F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_duranium",
                            gregtech.content.material.generated.CompoundMaterials.Duranium,
                            27,
                            20.0F,
                            20.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_draconium",
                            gregtech.content.material.generated.CompoundMaterials.Draconium,
                            27,
                            50.0F,
                            50.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_ultimet",
                            gregtech.content.material.generated.CompoundMaterials.Ultimet,
                            27,
                            12.5F,
                            12.5F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_tungsten",
                            gregtech.content.material.generated.ElementMaterials.Tungsten,
                            36,
                            10.0F,
                            10.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_palladium",
                            gregtech.content.material.generated.ElementMaterials.Palladium,
                            36,
                            15.0F,
                            15.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_iridium",
                            gregtech.content.material.generated.ElementMaterials.Iridium,
                            36,
                            15.0F,
                            15.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_osmium",
                            gregtech.content.material.generated.ElementMaterials.OsmiumElemental,
                            36,
                            9.0F,
                            9.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_elven_elementium",
                            gregtech.content.material.generated.CompoundMaterials.ElvenElementium,
                            36,
                            30.0F,
                            30.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_adamantium",
                            gregtech.content.material.generated.ElementMaterials.Adamantium,
                            36,
                            100.0F,
                            100.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_draconium_awakened",
                            gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened,
                            36,
                            100.0F,
                            100.0F)),
            new Entry(
                    false,
                    HopperSpec.of(
                            "hopper_infinity",
                            gregtech.content.material.generated.CompoundMaterials.Infinity,
                            36,
                            100.0F,
                            100.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_lead",
                            gregtech.content.material.generated.ElementMaterials.Lead,
                            2,
                            4.0F,
                            4.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_bismuth",
                            gregtech.content.material.generated.ElementMaterials.Bismuth,
                            2,
                            4.0F,
                            4.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_antimony",
                            gregtech.content.material.generated.ElementMaterials.Antimony,
                            2,
                            4.0F,
                            4.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_nickel",
                            gregtech.content.material.generated.ElementMaterials.Nickel,
                            3,
                            4.0F,
                            4.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_constantan",
                            gregtech.content.material.generated.CompoundMaterials.Constantan,
                            3,
                            4.0F,
                            4.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_bronze",
                            gregtech.content.material.generated.CompoundMaterials.Bronze,
                            3,
                            7.0F,
                            7.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_arsenic_copper",
                            gregtech.content.material.generated.CompoundMaterials.ArsenicCopper,
                            4,
                            7.5F,
                            7.5F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_aluminium",
                            gregtech.content.material.generated.ElementMaterials.Aluminium,
                            4,
                            2.0F,
                            2.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_brass",
                            gregtech.content.material.generated.CompoundMaterials.Brass,
                            4,
                            2.5F,
                            2.5F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_tin_alloy",
                            gregtech.content.material.generated.CompoundMaterials.TinAlloy,
                            4,
                            3.0F,
                            3.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_cobalt",
                            gregtech.content.material.generated.ElementMaterials.Cobalt,
                            4,
                            4.0F,
                            4.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_arsenic_bronze",
                            gregtech.content.material.generated.CompoundMaterials.ArsenicBronze,
                            5,
                            8.0F,
                            8.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_bismuth_bronze",
                            gregtech.content.material.generated.CompoundMaterials.BismuthBronze,
                            5,
                            8.0F,
                            8.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_germanium",
                            gregtech.content.material.generated.ElementMaterials.Germanium,
                            5,
                            4.0F,
                            4.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_invar",
                            gregtech.content.material.generated.CompoundMaterials.Invar,
                            5,
                            4.0F,
                            4.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_steel",
                            gregtech.content.material.generated.CompoundMaterials.Steel,
                            5,
                            6.0F,
                            6.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_gold",
                            gregtech.content.material.generated.ElementMaterials.Gold,
                            6,
                            3.0F,
                            3.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_silver",
                            gregtech.content.material.generated.ElementMaterials.Silver,
                            6,
                            3.0F,
                            3.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_manganese",
                            gregtech.content.material.generated.ElementMaterials.Manganese,
                            6,
                            6.0F,
                            6.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_lumium",
                            gregtech.content.material.generated.CompoundMaterials.Lumium,
                            6,
                            2.0F,
                            2.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_knightmetal",
                            gregtech.content.material.generated.CompoundMaterials.Knightmetal,
                            7,
                            7.0F,
                            7.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_steel_galvanized",
                            gregtech.content.material.generated.CompoundMaterials.SteelGalvanized,
                            7,
                            6.0F,
                            6.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_molybdenum",
                            gregtech.content.material.generated.ElementMaterials.Molybdenum,
                            8,
                            6.0F,
                            6.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_electrum",
                            gregtech.content.material.generated.CompoundMaterials.Electrum,
                            9,
                            3.0F,
                            3.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_stainless_steel",
                            gregtech.content.material.generated.CompoundMaterials.StainlessSteel,
                            9,
                            5.0F,
                            5.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_thaumium",
                            gregtech.content.material.generated.CompoundMaterials.Thaumium,
                            9,
                            9.0F,
                            9.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_manasteel",
                            gregtech.content.material.generated.CompoundMaterials.Manasteel,
                            9,
                            9.0F,
                            9.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_titanium",
                            gregtech.content.material.generated.ElementMaterials.Titanium,
                            12,
                            9.0F,
                            9.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_netherite",
                            gregtech.content.material.generated.CompoundMaterials.Netherite,
                            12,
                            10.0F,
                            10.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_chromium",
                            gregtech.content.material.generated.ElementMaterials.Chromium,
                            14,
                            4.0F,
                            4.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_platinum",
                            gregtech.content.material.generated.ElementMaterials.Platinum,
                            18,
                            2.0F,
                            2.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_terrasteel",
                            gregtech.content.material.generated.CompoundMaterials.Terrasteel,
                            18,
                            15.0F,
                            15.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_tungstensteel",
                            gregtech.content.material.generated.CompoundMaterials.Tungstensteel,
                            27,
                            12.5F,
                            12.5F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_tungsten_carbide",
                            gregtech.content.material.generated.CompoundMaterials.TungstenCarbide,
                            27,
                            12.5F,
                            12.5F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_duranium",
                            gregtech.content.material.generated.CompoundMaterials.Duranium,
                            27,
                            20.0F,
                            20.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_draconium",
                            gregtech.content.material.generated.CompoundMaterials.Draconium,
                            27,
                            50.0F,
                            50.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_ultimet",
                            gregtech.content.material.generated.CompoundMaterials.Ultimet,
                            27,
                            12.5F,
                            12.5F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_tungsten",
                            gregtech.content.material.generated.ElementMaterials.Tungsten,
                            36,
                            10.0F,
                            10.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_palladium",
                            gregtech.content.material.generated.ElementMaterials.Palladium,
                            36,
                            15.0F,
                            15.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_iridium",
                            gregtech.content.material.generated.ElementMaterials.Iridium,
                            36,
                            15.0F,
                            15.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_osmium",
                            gregtech.content.material.generated.ElementMaterials.OsmiumElemental,
                            36,
                            9.0F,
                            9.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_elven_elementium",
                            gregtech.content.material.generated.CompoundMaterials.ElvenElementium,
                            36,
                            30.0F,
                            30.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_adamantium",
                            gregtech.content.material.generated.ElementMaterials.Adamantium,
                            36,
                            100.0F,
                            100.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_draconium_awakened",
                            gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened,
                            36,
                            100.0F,
                            100.0F)),
            new Entry(
                    true,
                    HopperSpec.of(
                            "queue_hopper_infinity",
                            gregtech.content.material.generated.CompoundMaterials.Infinity,
                            36,
                            100.0F,
                            100.0F)));

    public static HopperSpec get(String id) {
        return ALL.stream()
                .map(Entry::spec)
                .filter(s -> s.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
