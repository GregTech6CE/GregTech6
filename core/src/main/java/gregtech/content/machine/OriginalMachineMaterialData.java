/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from CR.shaped, OreDictItemData, OP and Loader_MultiTileEntities. */
package gregtech.content.machine;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.ImportedMaterialData;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ReversibleCraftingData;
import java.util.*;

/** Source compositions for devices and multiblock construction. */
public final class OriginalMachineMaterialData {
    private OriginalMachineMaterialData() {}

    private static final Map<String, ItemComposition> MULTIBLOCK_DATA = new HashMap<>();
    private static final Map<String, ItemComposition> BLOCK_DATA = new HashMap<>();

    static {
        initMultiblocks();
        blocks0();
        blocks1();
        blocks2();
        blocks3();
    }

    static {
        for (var spec : gregtech.content.energy.HeatExchangerCatalog.ALL) BLOCK_DATA.put(spec.id(), spec.composition());
    }

    static {
        for (var tier : gregtech.content.energy.FluxRotaryCatalog.TIERS.subList(2, 5))
            for (String family : List.of("motor", "dynamo")) {
                var original = BLOCK_DATA.get("electric_" + family + "_" + tier.tier());
                var parts = new ArrayList<MaterialComponent>(original.components());
                parts.add(MaterialComponent.of(tier.material(), 8 * GTValues.U));
                BLOCK_DATA.put(
                        "flux_" + family + "_" + tier.tier(),
                        ReversibleCraftingData.perItem(parts, 1, "GT6 original RF rotary conversion CR.REV"));
            }
    }

    static Optional<ItemComposition> multiblock(String machine, int tier) {
        return Optional.ofNullable(MULTIBLOCK_DATA.get(machine + "/" + tier));
    }

    public static Optional<ItemComposition> block(String path) {
        return Optional.ofNullable(BLOCK_DATA.get(path));
    }

    public static Map<String, ItemComposition> blocks() {
        return Collections.unmodifiableMap(BLOCK_DATA);
    }

    private static MaterialComponent part(GTMaterial material, long amount) {
        return MaterialComponent.of(material, amount);
    }

    private static void add(String machine, int tier, int sourceId, MaterialComponent... parts) {
        MULTIBLOCK_DATA.put(
                machine + "/" + tier,
                ReversibleCraftingData.perItem(
                        List.of(parts), 1, "GT6 Loader_MultiTileEntities " + sourceId + " known CR.REV inputs"));
    }

    private static void initMultiblocks() {
        add(
                "cokeoven",
                1,
                17000,
                part(ImportedMaterialData.Fe, 1765764000L),
                part(ImportedMaterialData.Clay, 2594592000L),
                part(ImportedMaterialData.Brick, 2594592000L));
        add(
                "implosioncompressor",
                1,
                17110,
                part(ImportedMaterialData.TungstenSteel, 40864824000L),
                part(ImportedMaterialData.Cu, 10702692000L),
                part(ImportedMaterialData.Rubber, 5837832000L),
                part(ImportedMaterialData.Al, 6306300000L),
                part(ImportedMaterialData.SteelMagnetic, 972972000L),
                part(ImportedMaterialData.Pt, 1297296000L),
                part(ImportedMaterialData.Ruby, 1297296000L));
        add(
                "fusionreactor",
                1,
                17198,
                part(ImportedMaterialData.Os, 83026944000L),
                part(ImportedMaterialData.Ti, 20756736000L),
                part(ImportedMaterialData.NetherStar, 5189184000L),
                part(ImportedMaterialData.TungstenSteel, 2594592000L));
        add("distillationtower", 1, 17101, part(ImportedMaterialData.StainlessSteel, 48936888000L));
        add(
                "cryodistillationtower",
                1,
                17111,
                part(ImportedMaterialData.Cu, 46702656000L),
                part(ImportedMaterialData.StainlessSteel, 2234232000L));
        add(
                "largecentrifuge",
                1,
                17100,
                part(ImportedMaterialData.TungstenSteel, 10666656000L),
                part(ImportedMaterialData.Pt, 1297296000L),
                part(ImportedMaterialData.Ruby, 1297296000L));
        add(
                "largeelectrolyzer",
                1,
                17103,
                part(ImportedMaterialData.Pt, 2594592000L),
                part(ImportedMaterialData.StainlessSteel, 5189184000L),
                part(ImportedMaterialData.Ruby, 1297296000L));
        add(
                "largecoagulator",
                1,
                17105,
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L),
                part(ImportedMaterialData.StainlessSteel, 31783752000L));
        add(
                "largeautoclave",
                1,
                17112,
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L),
                part(ImportedMaterialData.StainlessSteel, 52540488000L));
        add(
                "largebath",
                1,
                17104,
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L),
                part(ImportedMaterialData.StainlessSteel, 20108088000L),
                part(ImportedMaterialData.Cu, 21405384000L),
                part(ImportedMaterialData.Rubber, 11675664000L),
                part(ImportedMaterialData.Al, 12612600000L),
                part(ImportedMaterialData.SteelMagnetic, 1945944000L));
        add(
                "largemixer",
                1,
                17102,
                part(ImportedMaterialData.StainlessSteel, 27243216000L),
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L));
        add(
                "largefermenter",
                1,
                17113,
                part(ImportedMaterialData.StainlessSteel, 31783752000L),
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L));
        add(
                "largeoven",
                1,
                17106,
                part(ImportedMaterialData.Invar, 31783752000L),
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L));
        add(
                "largesluice",
                1,
                17107,
                part(ImportedMaterialData.Ti, 11027016000L),
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L));
        add(
                "largecrusher",
                1,
                17108,
                part(ImportedMaterialData.TungstenSteel, 12324312000L),
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L));
        add(
                "largeshredder",
                1,
                17109,
                part(ImportedMaterialData.TungstenSteel, 12324312000L),
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L));
        add(
                "largesqueezer",
                1,
                17114,
                part(ImportedMaterialData.Steel, 14270256000L),
                part(ImportedMaterialData.Pt, 648648000L),
                part(ImportedMaterialData.Ruby, 648648000L));
        add(
                "largemassfab",
                1,
                17199,
                part(ImportedMaterialData.Os, 83026944000L),
                part(ImportedMaterialData.Ti, 20756736000L),
                part(ImportedMaterialData.NetherStar, 5189184000L),
                part(ImportedMaterialData.Pb, 23351328000L));
    }

    private static void blocks0() {
        BLOCK_DATA.put(
                "sensor_fluidometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Fe, 1945944000L),
                                part(ImportedMaterialData.TinAlloy, 1459458000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31006 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_itemometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(gregtech.content.data.generated.GT6Materials.Woods.Wood, 2594592000L),
                                part(ImportedMaterialData.TinAlloy, 1459458000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31004 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_electrometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Glass, 5837832000L),
                                part(ImportedMaterialData.Cu, 648648000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31015 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_progressmeter",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Glass, 5837832000L),
                                part(ImportedMaterialData.Brass, 648648000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31018 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_thermometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Redstone, 1945944000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31000 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_tachometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Glass, 5837832000L),
                                part(ImportedMaterialData.Brass, 5189184000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31019 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_weightometric",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.TinAlloy, 2756754000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31012 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_weightometer_light",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.TinAlloy, 2756754000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31010 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_weightometer_medium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.TinAlloy, 2756754000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31011 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_weightometer_super_heavy",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.TinAlloy, 2756754000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31013 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_bucketometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Fe, 1945944000L),
                                part(ImportedMaterialData.TinAlloy, 1459458000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31007 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_kilobucketometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Fe, 1945944000L),
                                part(ImportedMaterialData.TinAlloy, 1459458000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31022 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_gibblometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.TinAlloy, 2756754000L),
                                part(ImportedMaterialData.SiO2, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31001 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_stackometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(gregtech.content.data.generated.GT6Materials.Woods.Wood, 2594592000L),
                                part(ImportedMaterialData.TinAlloy, 1459458000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31005 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_luminometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Glass, 5837832000L),
                                part(ImportedMaterialData.Cu, 162162000L),
                                part(ImportedMaterialData.Si, 648648000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31002 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_playercounter",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Glass, 5837832000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31017 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_chronometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Glass, 17513496000L),
                                part(ImportedMaterialData.Au, 2594592000L),
                                part(ImportedMaterialData.Redstone, 648648000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31003 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_geiger",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Glass, 5837832000L),
                                part(ImportedMaterialData.Pb, 2594592000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31020 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_laserometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Glass, 5837832000L),
                                part(ImportedMaterialData.Cu, 162162000L),
                                part(ImportedMaterialData.SteelGalvanized, 2594592000L),
                                part(ImportedMaterialData.SiO2, 648648000L),
                                part(ImportedMaterialData.Sn, 324324000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31021 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_tpsmeter",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.Glass, 5837832000L),
                                part(ImportedMaterialData.Au, 7783776000L),
                                part(ImportedMaterialData.Redstone, 1945944000L),
                                part(ImportedMaterialData.TinAlloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31016 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sensor_kilogibblometer",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.RedAlloy, 324324000L),
                                part(ImportedMaterialData.TinAlloy, 2756754000L),
                                part(ImportedMaterialData.Diamond, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 31023 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_wood_1",
                ReversibleCraftingData.perItem(
                        List.of(part(gregtech.content.data.generated.GT6Materials.Woods.WoodTreated, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24800 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_wood_2",
                ReversibleCraftingData.perItem(
                        List.of(part(gregtech.content.data.generated.GT6Materials.Woods.WoodTreated, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24801 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_wood_3",
                ReversibleCraftingData.perItem(
                        List.of(part(gregtech.content.data.generated.GT6Materials.Woods.WoodTreated, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24802 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_wood",
                ReversibleCraftingData.perItem(
                        List.of(part(gregtech.content.data.generated.GT6Materials.Woods.WoodTreated, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24809 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_wood",
                ReversibleCraftingData.perItem(
                        List.of(part(gregtech.content.data.generated.GT6Materials.Woods.WoodTreated, 8432424000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24808 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_wood",
                ReversibleCraftingData.perItem(
                        List.of(part(gregtech.content.data.generated.GT6Materials.Woods.WoodTreated, 9729720000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24807 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_bronze_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24810 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_bronze_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24811 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_bronze_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24812 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_bronze_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24813 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24819 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24818 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24817 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_brass_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brass, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24770 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_brass_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brass, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24771 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_brass_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brass, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24772 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_brass_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brass, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24773 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_brass",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brass, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24779 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_brass",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brass, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24778 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_brass",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brass, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24777 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_arsenic_copper_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24780 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_arsenic_copper_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24781 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_arsenic_copper_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24782 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_arsenic_copper_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24783 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24789 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24788 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24787 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_arsenic_bronze_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24790 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_arsenic_bronze_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24791 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_arsenic_bronze_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24792 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_arsenic_bronze_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24793 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24799 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24798 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24797 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_steel_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24820 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_steel_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24821 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_steel_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24822 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_steel_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24823 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24829 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24828 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24827 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_titanium_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24830 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_titanium_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24831 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_titanium_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24832 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_titanium_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24833 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24839 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24838 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24837 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_tungstensteel_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24840 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_tungstensteel_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24841 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_tungstensteel_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24842 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_tungstensteel_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24843 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_tungstensteel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24849 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_tungstensteel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24848 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24847 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_iridium_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24850 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_iridium_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24851 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_iridium_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24852 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_iridium_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24853 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_iridium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24859 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_iridium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24858 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_iridium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24857 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_iritanium_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Iritanium, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24860 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_iritanium_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Iritanium, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24861 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_iritanium_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Iritanium, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24862 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_iritanium_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Iritanium, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24863 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_iritanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Iritanium, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24869 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_iritanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Iritanium, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24868 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_iritanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Iritanium, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24867 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_trinitanium_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinitanium, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24870 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_trinitanium_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinitanium, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24871 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_trinitanium_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinitanium, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24872 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_trinitanium_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinitanium, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24873 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_trinitanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinitanium, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24879 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_trinitanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinitanium, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24878 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_trinitanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinitanium, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24877 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_trinaquadalloy_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinaquadalloy, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24880 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_trinaquadalloy_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinaquadalloy, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24881 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_trinaquadalloy_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinaquadalloy, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24882 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_trinaquadalloy_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinaquadalloy, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24883 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_trinaquadalloy",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinaquadalloy, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24889 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_trinaquadalloy",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinaquadalloy, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24888 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_trinaquadalloy",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Trinaquadalloy, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24887 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_adamantium_1",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24890 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_adamantium_2",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24891 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_adamantium_3",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24892 known CR.REV inputs"));
        BLOCK_DATA.put(
                "axle_adamantium_4",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24893 known CR.REV inputs"));
        BLOCK_DATA.put(
                "gearbox_adamantium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 9081072000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24899 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotation_transformer_adamantium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 16864848000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24898 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_rotation_adamantium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 14270256000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 24897 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_motor_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.SteelGalvanized, 11891880000L),
                                part(ImportedMaterialData.IronMagnetic, 648648000L),
                                part(ImportedMaterialData.Cu, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10021 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_dynamo_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.SteelGalvanized, 11891880000L),
                                part(ImportedMaterialData.Cu, 648648000L),
                                part(ImportedMaterialData.IronMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10111 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_heater_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.SteelGalvanized, 9297288000L),
                                part(ImportedMaterialData.Cu, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10001 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_cooler_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Sn, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 1297296000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10161 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_heater_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 5189184000L),
                                part(ImportedMaterialData.SteelGalvanized, 9297288000L),
                                part(ImportedMaterialData.Cu, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11001 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_cooler_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 5189184000L),
                                part(ImportedMaterialData.Sn, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 1297296000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11161 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electromagnet_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cu, 1945944000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10031 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_magnet_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 5189184000L),
                                part(ImportedMaterialData.Cu, 1945944000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11031 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_motor_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 5189184000L),
                                part(ImportedMaterialData.SteelGalvanized, 11891880000L),
                                part(ImportedMaterialData.IronMagnetic, 648648000L),
                                part(ImportedMaterialData.Cu, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11021 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_dynamo_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 5189184000L),
                                part(ImportedMaterialData.SteelGalvanized, 11891880000L),
                                part(ImportedMaterialData.Cu, 648648000L),
                                part(ImportedMaterialData.IronMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11111 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_motor_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Al, 11891880000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L),
                                part(ImportedMaterialData.Cu, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10022 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_dynamo_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Al, 11891880000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10112 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_heater_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Al, 9297288000L),
                                part(ImportedMaterialData.Constantan, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10002 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_cooler_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cu, 3243240000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 2594592000L),
                                part(ImportedMaterialData.Al, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10162 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_heater_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 5189184000L),
                                part(ImportedMaterialData.Al, 9297288000L),
                                part(ImportedMaterialData.Constantan, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11002 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_cooler_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 5189184000L),
                                part(ImportedMaterialData.Cu, 3243240000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 2594592000L),
                                part(ImportedMaterialData.Al, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11162 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electromagnet_mv",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cu, 3891888000L), part(ImportedMaterialData.Al, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10032 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_magnet_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 5189184000L),
                                part(ImportedMaterialData.Cu, 3891888000L),
                                part(ImportedMaterialData.Al, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11032 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_motor_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 5189184000L),
                                part(ImportedMaterialData.Al, 11891880000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L),
                                part(ImportedMaterialData.Cu, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11022 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_dynamo_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 5189184000L),
                                part(ImportedMaterialData.Al, 11891880000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11112 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_motor_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.StainlessSteel, 11891880000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10023 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_dynamo_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.StainlessSteel, 11891880000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10113 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_heater_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.StainlessSteel, 9297288000L),
                                part(ImportedMaterialData.Kanthal, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10003 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_cooler_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Au, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 3891888000L),
                                part(ImportedMaterialData.Cu, 3891888000L),
                                part(ImportedMaterialData.StainlessSteel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10163 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_heater_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Electrum, 5189184000L),
                                part(ImportedMaterialData.StainlessSteel, 9297288000L),
                                part(ImportedMaterialData.Kanthal, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11003 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_cooler_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Electrum, 5189184000L),
                                part(ImportedMaterialData.Au, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 3891888000L),
                                part(ImportedMaterialData.Cu, 3891888000L),
                                part(ImportedMaterialData.StainlessSteel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11163 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electromagnet_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 7783776000L),
                                part(ImportedMaterialData.StainlessSteel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10033 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_magnet_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Electrum, 5189184000L),
                                part(ImportedMaterialData.AnnealedCopper, 7783776000L),
                                part(ImportedMaterialData.StainlessSteel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11033 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_motor_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 11891880000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 648648000L),
                                part(ImportedMaterialData.AnnealedCopper, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10024 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_dynamo_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 11891880000L),
                                part(ImportedMaterialData.AnnealedCopper, 5189184000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10114 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_heater_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 9297288000L),
                                part(ImportedMaterialData.Nichrome, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10004 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_cooler_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Al, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 5189184000L),
                                part(ImportedMaterialData.Cu, 5189184000L),
                                part(ImportedMaterialData.Cr, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10164 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_heater_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.EnderiumBase, 5189184000L),
                                part(ImportedMaterialData.Cr, 9297288000L),
                                part(ImportedMaterialData.Nichrome, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11004 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_cooler_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.EnderiumBase, 5189184000L),
                                part(ImportedMaterialData.Al, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 5189184000L),
                                part(ImportedMaterialData.Cu, 5189184000L),
                                part(ImportedMaterialData.Cr, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11164 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electromagnet_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 15567552000L),
                                part(ImportedMaterialData.Cr, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10034 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_magnet_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.EnderiumBase, 5189184000L),
                                part(ImportedMaterialData.AnnealedCopper, 15567552000L),
                                part(ImportedMaterialData.Cr, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11034 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_motor_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 11891880000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 648648000L),
                                part(ImportedMaterialData.AnnealedCopper, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10025 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_dynamo_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 11891880000L),
                                part(ImportedMaterialData.AnnealedCopper, 10378368000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10115 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_heater_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 9297288000L),
                                part(ImportedMaterialData.SiC, 20756736000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10005 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electric_cooler_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 6486480000L),
                                part(ImportedMaterialData.Cu, 6486480000L),
                                part(ImportedMaterialData.Ti, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10165 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_heater_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Enderium, 5189184000L),
                                part(ImportedMaterialData.Ti, 9297288000L),
                                part(ImportedMaterialData.SiC, 20756736000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11005 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_cooler_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Enderium, 5189184000L),
                                part(ImportedMaterialData.Pt, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Si, 6486480000L),
                                part(ImportedMaterialData.Cu, 6486480000L),
                                part(ImportedMaterialData.Ti, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11165 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electromagnet_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 31135104000L),
                                part(ImportedMaterialData.Ti, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10035 known CR.REV inputs"));
        BLOCK_DATA.put(
                "flux_magnet_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Enderium, 5189184000L),
                                part(ImportedMaterialData.AnnealedCopper, 31135104000L),
                                part(ImportedMaterialData.Ti, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11035 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotational_pump_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bronze, 14270256000L),
                                part(ImportedMaterialData.StainlessSteel, 7459452000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 16001 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotational_pump_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steel, 14270256000L),
                                part(ImportedMaterialData.StainlessSteel, 11351340000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 16002 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotational_pump_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 14270256000L),
                                part(ImportedMaterialData.StainlessSteel, 17189172000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 16003 known CR.REV inputs"));
        BLOCK_DATA.put(
                "rotational_pump_tungstensteel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 14270256000L),
                                part(ImportedMaterialData.StainlessSteel, 28864836000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 16004 known CR.REV inputs"));
        BLOCK_DATA.put(
                "transformer_ulv_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cu, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.TinAlloy, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10040 known CR.REV inputs"));
        BLOCK_DATA.put(
                "transformer_lv_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cu, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10041 known CR.REV inputs"));
        BLOCK_DATA.put(
                "transformer_mv_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cu, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Al, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10042 known CR.REV inputs"));
        BLOCK_DATA.put(
                "transformer_hv_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.StainlessSteel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10043 known CR.REV inputs"));
        BLOCK_DATA.put(
                "transformer_ev_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Cr, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10044 known CR.REV inputs"));
        BLOCK_DATA.put(
                "transformer_iv_luv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Ti, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10045 known CR.REV inputs"));
        BLOCK_DATA.put(
                "transformer_luv_zpm",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Ir, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10046 known CR.REV inputs"));
        BLOCK_DATA.put(
                "transformer_zpm_uv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Os, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10047 known CR.REV inputs"));
        BLOCK_DATA.put(
                "transformer_uv_xv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Trinitanium, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10048 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_ulv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 1945944000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.TinAlloy, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10080 known CR.REV inputs"));
        BLOCK_DATA.put(
                "energy_storage_ulv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 7783776000L),
                                part(ImportedMaterialData.Rubber, 2594592000L),
                                part(ImportedMaterialData.Cu, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.TinAlloy, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10090 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Sn, 1945944000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10081 known CR.REV inputs"));
        BLOCK_DATA.put(
                "energy_storage_lv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Sn, 7783776000L),
                                part(ImportedMaterialData.Rubber, 2594592000L),
                                part(ImportedMaterialData.Cu, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10091 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cu, 1945944000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Al, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10082 known CR.REV inputs"));
        BLOCK_DATA.put(
                "energy_storage_mv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cu, 10378368000L),
                                part(ImportedMaterialData.Rubber, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Al, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10092 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Au, 1945944000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.StainlessSteel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10083 known CR.REV inputs"));
        BLOCK_DATA.put(
                "energy_storage_hv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Au, 7783776000L),
                                part(ImportedMaterialData.Rubber, 2594592000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.StainlessSteel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10093 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Al, 1945944000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Cr, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10084 known CR.REV inputs"));
    }

    private static void blocks1() {
        BLOCK_DATA.put(
                "energy_storage_ev",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Al, 7783776000L),
                                part(ImportedMaterialData.Rubber, 2594592000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Cr, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10094 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 1945944000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Ti, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10085 known CR.REV inputs"));
        BLOCK_DATA.put(
                "energy_storage_iv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 7783776000L),
                                part(ImportedMaterialData.Rubber, 2594592000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Ti, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10095 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_luv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Graphene, 1945944000L),
                                part(ImportedMaterialData.Ir, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10086 known CR.REV inputs"));
        BLOCK_DATA.put(
                "energy_storage_luv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Graphene, 7783776000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Ir, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10096 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_zpm",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Graphene, 1945944000L),
                                part(ImportedMaterialData.Os, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10087 known CR.REV inputs"));
        BLOCK_DATA.put(
                "energy_storage_zpm",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Graphene, 7783776000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Os, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10097 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_uv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Graphene, 1945944000L),
                                part(ImportedMaterialData.Trinitanium, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10088 known CR.REV inputs"));
        BLOCK_DATA.put(
                "energy_storage_uv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Graphene, 7783776000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.Fe, 2594592000L),
                                part(ImportedMaterialData.Trinitanium, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10098 known CR.REV inputs"));
        BLOCK_DATA.put(
                "battery_box_xv",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Graphene, 1945944000L),
                                part(ImportedMaterialData.Trinaquadalloy, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10089 known CR.REV inputs"));
        BLOCK_DATA.put(
                "solar_panel_silicon",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Si, 2594592000L),
                                part(ImportedMaterialData.Cu, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.TinAlloy, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10050 known CR.REV inputs"));
        BLOCK_DATA.put(
                "solar_panel_germanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ge, 2594592000L),
                                part(ImportedMaterialData.AnnealedCopper, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Al, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10051 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 25945920000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1512 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_brass",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Brass, 11027016000L),
                                part(ImportedMaterialData.Bronze, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1515 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 11027016000L),
                                part(ImportedMaterialData.Bronze, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1518 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 25945920000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1522 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 11027016000L),
                                part(ImportedMaterialData.Steel, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1525 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_ironwood",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.IronWood, 11027016000L),
                                part(ImportedMaterialData.Steel, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1527 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_steeleaf",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steeleaf, 11027016000L),
                                part(ImportedMaterialData.Steel, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1528 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_thaumium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Thaumium, 11027016000L),
                                part(ImportedMaterialData.Steel, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1529 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 25945920000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1530 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_fiery_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.FierySteel, 11027016000L),
                                part(ImportedMaterialData.Ti, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1531 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_aluminium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Al, 11027016000L),
                                part(ImportedMaterialData.Ti, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1535 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_magnalium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Magnalium, 11027016000L),
                                part(ImportedMaterialData.Ti, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1538 known CR.REV inputs"));

        BLOCK_DATA.put(
                "steam_turbine_trinitanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Trinitanium, 11027016000L),
                                part(ImportedMaterialData.TungstenSteel, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1545 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_turbine_graphene",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Graphene, 11027016000L),
                                part(ImportedMaterialData.TungstenSteel, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1548 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mortar_block",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Fe, 648648000L),
                                part(ImportedMaterialData.Ceramic, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32735 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mortar_netherite",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Netherite, 648648000L),
                                part(ImportedMaterialData.Ceramic, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32094 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mortar_sapphire",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Sapphire, 648648000L),
                                part(ImportedMaterialData.Ceramic, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32075 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mortar_diamond",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Diamond, 648648000L),
                                part(ImportedMaterialData.Ceramic, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32076 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mortar_amethyst",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Amethyst, 648648000L),
                                part(ImportedMaterialData.Ceramic, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32089 known CR.REV inputs"));
        BLOCK_DATA.put(
                "grindstone_block",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Fe, 6810804000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32703 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sifting_table",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Fe, 2900898000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32702 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mixing_bowl",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ceramic, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32706 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mixing_bowl_table",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ceramic, 3243240000L),
                                part(ImportedMaterialData.Brick, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32705 known CR.REV inputs"));
        BLOCK_DATA.put(
                "juicer",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ceramic, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32722 known CR.REV inputs"));
        BLOCK_DATA.put(
                "bathing_pot",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32708 known CR.REV inputs"));
        BLOCK_DATA.put(
                "bathing_pot_table",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.StainlessSteel, 3243240000L),
                                part(ImportedMaterialData.Brick, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32707 known CR.REV inputs"));
        BLOCK_DATA.put(
                "bathing_pot_wood",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(gregtech.content.data.generated.GT6Materials.Woods.Wood, 3243240000L),
                                part(ImportedMaterialData.Pb, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32721 known CR.REV inputs"));
        BLOCK_DATA.put(
                "bathing_pot_table_wood",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(gregtech.content.data.generated.GT6Materials.Woods.Wood, 3243240000L),
                                part(ImportedMaterialData.Pb, 648648000L),
                                part(ImportedMaterialData.Brick, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 32720 known CR.REV inputs"));
        BLOCK_DATA.put(
                "coke_oven_main",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Fe, 1765764000L),
                                part(ImportedMaterialData.Clay, 2594592000L),
                                part(ImportedMaterialData.Brick, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17000 known CR.REV inputs"));
        BLOCK_DATA.put(
                "logistics_core",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 1297296000L),
                                part(ImportedMaterialData.Emerald, 1297296000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17997 known CR.REV inputs"));
        BLOCK_DATA.put(
                "implosion_compressor_main",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 40864824000L),
                                part(ImportedMaterialData.Cu, 10702692000L),
                                part(ImportedMaterialData.Rubber, 5837832000L),
                                part(ImportedMaterialData.Al, 6306300000L),
                                part(ImportedMaterialData.SteelMagnetic, 972972000L),
                                part(ImportedMaterialData.Pt, 1297296000L),
                                part(ImportedMaterialData.Ruby, 1297296000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17110 known CR.REV inputs"));
        BLOCK_DATA.put(
                "fusion_reactor_main",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Os, 83026944000L),
                                part(ImportedMaterialData.Ti, 20756736000L),
                                part(ImportedMaterialData.NetherStar, 5189184000L),
                                part(ImportedMaterialData.TungstenSteel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17198 known CR.REV inputs"));
        BLOCK_DATA.put(
                "heat_exchanger_main",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.AnnealedCopper, 35026992000L),
                                part(ImportedMaterialData.Cu, 15567552000L),
                                part(ImportedMaterialData.W, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17197 known CR.REV inputs"));
        BLOCK_DATA.put(
                "distillation_tower_main",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 48936888000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17101 known CR.REV inputs"));
        BLOCK_DATA.put(
                "cryo_distillation_main",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cu, 46702656000L),
                                part(ImportedMaterialData.StainlessSteel, 2234232000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17111 known CR.REV inputs"));
        BLOCK_DATA.put(
                "fire_bricks",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Clay, 2594592000L),
                                part(ImportedMaterialData.Brick, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18000 known CR.REV inputs"));
        BLOCK_DATA.put(
                "tank_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18002 known CR.REV inputs"));
        BLOCK_DATA.put(
                "tank_wall_dense",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18022 known CR.REV inputs"));
        BLOCK_DATA.put(
                "implosion_compressor_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18023 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_niobium_titanium_coil",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.NiobiumTitanium, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18041 known CR.REV inputs"));
        BLOCK_DATA.put(
                "centrifuge_part",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 10666656000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18100 known CR.REV inputs"));
        BLOCK_DATA.put(
                "heat_transmitter",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 2594592000L),
                                part(ImportedMaterialData.Cu, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18101 known CR.REV inputs"));
        BLOCK_DATA.put(
                "distillation_tower_part",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 2234232000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18102 known CR.REV inputs"));
        BLOCK_DATA.put(
                "electrolyzer_part",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 1297296000L),
                                part(ImportedMaterialData.StainlessSteel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18105 known CR.REV inputs"));
        BLOCK_DATA.put(
                "dense_lead_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Pb, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18031 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_osmium_coil",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Os, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18044 known CR.REV inputs"));
        BLOCK_DATA.put(
                "conversion_processor_unit",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 2594592000L),
                                part(ImportedMaterialData.Sapphire, 2594592000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18204 known CR.REV inputs"));
        BLOCK_DATA.put(
                "galvanized_steel_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.SteelGalvanized, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18008 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_iridium_coil",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18045 known CR.REV inputs"));
        BLOCK_DATA.put(
                "fusion_ventilation_unit",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Fe, 10702692000L),
                                part(ImportedMaterialData.SteelGalvanized, 7135128000L),
                                part(ImportedMaterialData.Sn, 1297296000L),
                                part(ImportedMaterialData.Rubber, 2594592000L),
                                part(ImportedMaterialData.Cu, 2594592000L),
                                part(ImportedMaterialData.IronMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18299 known CR.REV inputs"));
        BLOCK_DATA.put(
                "versatile_processor_unit",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 2594592000L),
                                part(ImportedMaterialData.Diamond, 648648000L),
                                part(ImportedMaterialData.Sapphire, 648648000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L),
                                part(ImportedMaterialData.Ruby, 648648000L),
                                part(ImportedMaterialData.Emerald, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18200 known CR.REV inputs"));
        BLOCK_DATA.put(
                "logic_processor_unit",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 2594592000L),
                                part(ImportedMaterialData.Diamond, 2594592000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18201 known CR.REV inputs"));
        BLOCK_DATA.put(
                "control_processor_unit",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 2594592000L),
                                part(ImportedMaterialData.Ruby, 2594592000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18202 known CR.REV inputs"));
        BLOCK_DATA.put(
                "dense_adamantium_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18025 known CR.REV inputs"));
        BLOCK_DATA.put(
                "dense_tungsten_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18024 known CR.REV inputs"));
        BLOCK_DATA.put(
                "dense_titanium_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18026 known CR.REV inputs"));
        BLOCK_DATA.put(
                "tungstensteel_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18003 known CR.REV inputs"));
        BLOCK_DATA.put(
                "titanium_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18006 known CR.REV inputs"));
        BLOCK_DATA.put(
                "invar_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18007 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steel_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18009 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_nichrome_coil",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Nichrome, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18042 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_carborundum_coil",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.SiC, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18043 known CR.REV inputs"));
        BLOCK_DATA.put(
                "sluice_part",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 20756736000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18106 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crusher_wheels",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Diamond, 2594592000L),
                                part(ImportedMaterialData.TungstenSteel, 19459440000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18107 known CR.REV inputs"));
        BLOCK_DATA.put(
                "wood_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 972972000L),
                                part(gregtech.content.data.generated.GT6Materials.Woods.WoodTreated, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17001 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_stainless_steel_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 2918916000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17002 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_tungstensteel_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 2918916000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17003 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_tungsten_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 2918916000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17004 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_adamantium_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 2918916000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17005 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_titanium_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 2918916000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17006 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_invar_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 2918916000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17007 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_dense_stainless_steel_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 23675652000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17022 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_dense_tungstensteel_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 23675652000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17023 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_dense_tungsten_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 23675652000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17024 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_dense_adamantium_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 23675652000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17025 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_dense_titanium_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 23675652000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17026 known CR.REV inputs"));
        BLOCK_DATA.put(
                "small_dense_invar_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 23675652000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17027 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_stainless_steel_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 6810804000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17042 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_tungstensteel_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 6810804000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17043 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_tungsten_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 6810804000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17044 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_adamantium_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 6810804000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17045 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_titanium_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 6810804000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17046 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_invar_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 6810804000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17047 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_dense_stainless_steel_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 58702644000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17062 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_dense_tungstensteel_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 58702644000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17063 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_dense_tungsten_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 58702644000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17064 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_dense_adamantium_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 58702644000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17065 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_dense_titanium_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 58702644000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17066 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_dense_invar_tank_main_valve",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 58702644000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17067 known CR.REV inputs"));
        BLOCK_DATA.put(
                "stainless_steel_boiler_main_barometer",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 58378320000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17201 known CR.REV inputs"));
        BLOCK_DATA.put(
                "titanium_boiler_main_barometer",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 58378320000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17202 known CR.REV inputs"));
        BLOCK_DATA.put(
                "tungstensteel_boiler_main_barometer",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 58378320000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17203 known CR.REV inputs"));
        BLOCK_DATA.put(
                "adamantium_boiler_main_barometer",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 58378320000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17204 known CR.REV inputs"));
        BLOCK_DATA.put(
                "invar_boiler_main_barometer",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 58378320000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17205 known CR.REV inputs"));
        BLOCK_DATA.put(
                "magnalium_steam_turbine_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Magnalium, 46702656000L),
                                part(ImportedMaterialData.StainlessSteel, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17211 known CR.REV inputs"));
        BLOCK_DATA.put(
                "trinitanium_steam_turbine_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Trinitanium, 46702656000L),
                                part(ImportedMaterialData.Ti, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17212 known CR.REV inputs"));
        BLOCK_DATA.put(
                "graphene_steam_turbine_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Graphene, 46702656000L),
                                part(ImportedMaterialData.TungstenSteel, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17213 known CR.REV inputs"));
        BLOCK_DATA.put(
                "vibramantium_steam_turbine_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Vibramantium, 46702656000L),
                                part(ImportedMaterialData.Ad, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17214 known CR.REV inputs"));
        BLOCK_DATA.put(
                "stainless_steel_dynamo_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 25945920000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17221 known CR.REV inputs"));
        BLOCK_DATA.put(
                "titanium_dynamo_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 25945920000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17222 known CR.REV inputs"));
        BLOCK_DATA.put(
                "tungstensteel_dynamo_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 25945920000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17223 known CR.REV inputs"));
        BLOCK_DATA.put(
                "adamantium_dynamo_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 25945920000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17224 known CR.REV inputs"));
        BLOCK_DATA.put(
                "magnalium_gas_turbine_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 23351328000L),
                                part(ImportedMaterialData.Magnalium, 46702656000L),
                                part(ImportedMaterialData.StainlessSteel, 23351328000L),
                                part(ImportedMaterialData.Sn, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.SteelGalvanized, 972972000L),
                                part(ImportedMaterialData.IronMagnetic, 324324000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17231 known CR.REV inputs"));
        BLOCK_DATA.put(
                "trinitanium_gas_turbine_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 23351328000L),
                                part(ImportedMaterialData.Trinitanium, 46702656000L),
                                part(ImportedMaterialData.Ti, 23351328000L),
                                part(ImportedMaterialData.Cu, 3243240000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Al, 972972000L),
                                part(ImportedMaterialData.SteelMagnetic, 324324000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17232 known CR.REV inputs"));
        BLOCK_DATA.put(
                "graphene_gas_turbine_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.W, 23351328000L),
                                part(ImportedMaterialData.Graphene, 46702656000L),
                                part(ImportedMaterialData.TungstenSteel, 23351328000L),
                                part(ImportedMaterialData.Au, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.Cu, 3891888000L),
                                part(ImportedMaterialData.StainlessSteel, 972972000L),
                                part(ImportedMaterialData.SteelMagnetic, 324324000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17233 known CR.REV inputs"));
        BLOCK_DATA.put(
                "vibramantium_gas_turbine_main_housing",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ad, 46702656000L),
                                part(ImportedMaterialData.Vibramantium, 46702656000L),
                                part(ImportedMaterialData.Al, 648648000L),
                                part(ImportedMaterialData.Rubber, 1297296000L),
                                part(ImportedMaterialData.AnnealedCopper, 5189184000L),
                                part(ImportedMaterialData.Cr, 972972000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 324324000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17234 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_stainless_steel_crucible",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17302 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_tungstensteel_crucible",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17303 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_tungsten_crucible",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17304 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_adamantium_crucible",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17305 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_titanium_crucible",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17306 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_invar_crucible",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17307 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_steel_crucible",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17309 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_tantalum_hafnium_carbide_crucible",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta4HfC5, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17312 known CR.REV inputs"));
        BLOCK_DATA.put(
                "von_da_graagg_generator",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.NetherStar, 648648000L),
                                part(ImportedMaterialData.Pt, 1297296000L),
                                part(ImportedMaterialData.Ruby, 1297296000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L),
                                part(ImportedMaterialData.EnderEye, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17996 known CR.REV inputs"));
        BLOCK_DATA.put(
                "lightning_rod_electric_output",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.NiobiumTitanium, 10378368000L),
                                part(ImportedMaterialData.Pt, 1297296000L),
                                part(ImportedMaterialData.Sapphire, 1297296000L),
                                part(ImportedMaterialData.W, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17998 known CR.REV inputs"));
        BLOCK_DATA.put(
                "bedrock_mining_drill_controller",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 2918916000L),
                                part(ImportedMaterialData.Ruby, 1297296000L),
                                part(ImportedMaterialData.Rubber, 7135128000L),
                                part(ImportedMaterialData.AnnealedCopper, 12972960000L),
                                part(ImportedMaterialData.Ti, 38270232000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 648648000L),
                                part(ImportedMaterialData.TungstenSteel, 7783776000L),
                                part(ImportedMaterialData.Steel, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 17999 known CR.REV inputs"));
        BLOCK_DATA.put(
                "wood_wall",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(gregtech.content.data.generated.GT6Materials.Woods.WoodTreated, 2594592000L),
                                part(ImportedMaterialData.Pb, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18001 known CR.REV inputs"));
        BLOCK_DATA.put(
                "tungsten_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18004 known CR.REV inputs"));
        BLOCK_DATA.put(
                "adamantium_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18005 known CR.REV inputs"));
        BLOCK_DATA.put(
                "bronze_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18010 known CR.REV inputs"));
        BLOCK_DATA.put(
                "lead_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Pb, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18011 known CR.REV inputs"));
        BLOCK_DATA.put(
                "tantalum_hafnium_carbide_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta4HfC5, 2594592000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18012 known CR.REV inputs"));
        BLOCK_DATA.put(
                "dense_invar_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18027 known CR.REV inputs"));
        BLOCK_DATA.put(
                "dense_galvanized_steel_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.SteelGalvanized, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18028 known CR.REV inputs"));
        BLOCK_DATA.put(
                "dense_steel_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18029 known CR.REV inputs"));
        BLOCK_DATA.put(
                "dense_bronze_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18030 known CR.REV inputs"));
        BLOCK_DATA.put(
                "dense_tantalum_hafnium_carbide_wall",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta4HfC5, 23351328000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18032 known CR.REV inputs"));
        BLOCK_DATA.put(
                "large_copper_coil",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.AnnealedCopper, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18040 known CR.REV inputs"));
        BLOCK_DATA.put(
                "bedrock_mining_drill_head",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Diamond, 2594592000L),
                                part(ImportedMaterialData.TungstenSteel, 46702656000L),
                                part(ImportedMaterialData.Steel, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18103 known CR.REV inputs"));
        BLOCK_DATA.put(
                "lightning_rod",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.SteelGalvanized, 2594592000L),
                                part(ImportedMaterialData.NiobiumTitanium, 1297296000L),
                                part(ImportedMaterialData.Ir, 324324000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18104 known CR.REV inputs"));
        BLOCK_DATA.put(
                "storage_quadcore_processor_unit",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pt, 2594592000L),
                                part(ImportedMaterialData.Emerald, 2594592000L),
                                part(ImportedMaterialData.SteelGalvanized, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18203 known CR.REV inputs"));
        BLOCK_DATA.put(
                "shredder_blades",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Diamond, 1297296000L),
                                part(ImportedMaterialData.TungstenSteel, 22054032000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 18108 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_brick",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1199 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_lead",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1100 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_bismuth",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bi, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1101 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bronze, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1102 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicCopper, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1111 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicBronze, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1112 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1103 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steel, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1104 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1105 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1106 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_netherite",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Netherite, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1110 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.W, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1107 known CR.REV inputs"));
    }

    private static void blocks2() {
        BLOCK_DATA.put(
                "burning_box_solid_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1108 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ta4HfC5, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1109 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_lead",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1150 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_bismuth",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bi, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1151 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bronze, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1152 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicCopper, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1161 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicBronze, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1162 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1153 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steel, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1154 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1155 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1156 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_netherite",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Netherite, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1160 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.W, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1157 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1158 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_solid_dense_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ta4HfC5, 12972960000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1159 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_lead",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Pb, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1200 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_bismuth",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bi, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1201 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1202 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1210 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1211 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1203 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1204 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_chromium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cr, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1205 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1206 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_netherite",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Netherite, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1209 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1207 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1208 known CR.REV inputs"));
        BLOCK_DATA.put(
                "steam_boiler_ultimet",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ultimet, 6486480000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1212 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_lead",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Pb, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1250 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_bismuth",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bi, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1251 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1252 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1260 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1261 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1253 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1254 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_chromium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cr, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1255 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1256 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_netherite",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Netherite, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1259 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1257 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1258 known CR.REV inputs"));
        BLOCK_DATA.put(
                "strong_steam_boiler_ultimet",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ultimet, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1262 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_lead",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Pb, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1300 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_tin_alloy",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TinAlloy, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1301 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1302 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1312 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1313 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_brass",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brass, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1309 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1303 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_ironwood",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.IronWood, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1310 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1304 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_fiery_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.FierySteel, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1311 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_chromium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cr, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1305 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1306 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1307 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 5999994000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1308 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_lead",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Pb, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1350 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_tin_alloy",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TinAlloy, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1351 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1352 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1362 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1363 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_brass",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Brass, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1359 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1353 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_ironwood",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.IronWood, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1360 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1354 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_fiery_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.FierySteel, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1361 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_chromium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cr, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1355 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1356 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1357 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_steam_strong_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 24648624000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1358 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bronze, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1402 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicCopper, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1411 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicBronze, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1412 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1403 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steel, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1404 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1405 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1406 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_netherite",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Netherite, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1410 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.W, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1407 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1408 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ta4HfC5, 2594592000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1409 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bronze, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1452 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicCopper, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1461 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicBronze, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1462 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1453 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steel, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1454 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1455 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1456 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_netherite",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Netherite, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1460 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.W, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1457 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1458 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_liquid_dense_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ta4HfC5, 14270256000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 7783776000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1459 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bronze, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1602 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicCopper, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1611 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicBronze, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1612 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1603 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steel, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1604 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1605 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1606 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_netherite",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Netherite, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1610 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.W, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1607 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1608 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ta4HfC5, 1945944000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1609 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bronze, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1652 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicCopper, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1661 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicBronze, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1662 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1653 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steel, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1654 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1655 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1656 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_netherite",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Netherite, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1660 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.W, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1657 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1658 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_gas_dense_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ta4HfC5, 10378368000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 10378368000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1659 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_lead",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9000 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_bismuth",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bi, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9001 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bronze, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9002 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicCopper, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9011 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicBronze, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9012 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9003 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steel, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9004 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9005 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9006 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_netherite",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Netherite, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9010 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.W, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9007 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9008 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ta4HfC5, 5351346000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9009 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_lead",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9050 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_bismuth",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bi, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9051 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Bronze, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9052 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicCopper, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9061 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.ArsenicBronze, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9062 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9053 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Steel, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9054 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9055 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9056 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_netherite",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Netherite, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9060 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.W, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9057 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.TungstenSteel, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9058 known CR.REV inputs"));
        BLOCK_DATA.put(
                "burning_box_fluidbed_dense_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ta4HfC5, 10540530000L),
                                part(ImportedMaterialData.Cu, 5837832000L),
                                part(ImportedMaterialData.Brick, 5189184000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9059 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_diesel_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9147 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_diesel_arsenic_copper",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicCopper, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9146 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_diesel_arsenic_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.ArsenicBronze, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9145 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_diesel_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9148 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_diesel_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9149 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_diesel_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9197 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_diesel_tungsten_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.TungstenSteel, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9198 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_diesel_iridium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 14918904000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 9199 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_electric_galvanized_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.SteelGalvanized, 7783776000L),
                                part(ImportedMaterialData.Cu, 648648000L),
                                part(ImportedMaterialData.IronMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10011 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_electric_aluminium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Al, 7783776000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10012 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_electric_stainless_steel",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.StainlessSteel, 7783776000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10013 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_electric_chromium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Cr, 7783776000L),
                                part(ImportedMaterialData.AnnealedCopper, 5189184000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10014 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_electric_titanium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Ti, 7783776000L),
                                part(ImportedMaterialData.AnnealedCopper, 10378368000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 10015 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_flux_lead",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Pb, 5189184000L),
                                part(ImportedMaterialData.SteelGalvanized, 7783776000L),
                                part(ImportedMaterialData.Cu, 648648000L),
                                part(ImportedMaterialData.IronMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11011 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_flux_invar",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Invar, 5189184000L),
                                part(ImportedMaterialData.Al, 7783776000L),
                                part(ImportedMaterialData.Cu, 1297296000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11012 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_flux_electrum",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Electrum, 5189184000L),
                                part(ImportedMaterialData.StainlessSteel, 7783776000L),
                                part(ImportedMaterialData.AnnealedCopper, 2594592000L),
                                part(ImportedMaterialData.SteelMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11013 known CR.REV inputs"));
        BLOCK_DATA.put(
                "engine_flux_enderium_base",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.EnderiumBase, 5189184000L),
                                part(ImportedMaterialData.Cr, 7783776000L),
                                part(ImportedMaterialData.AnnealedCopper, 5189184000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11014 known CR.REV inputs"));
    }

    private static void blocks3() {
        BLOCK_DATA.put(
                "engine_flux_enderium",
                ReversibleCraftingData.perItem(
                        List.of(
                                part(ImportedMaterialData.Enderium, 5189184000L),
                                part(ImportedMaterialData.Ti, 7783776000L),
                                part(ImportedMaterialData.AnnealedCopper, 10378368000L),
                                part(ImportedMaterialData.NeodymiumMagnetic, 648648000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 11015 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_ceramic",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ceramic, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1005 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_ceramic",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ceramic, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1055 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_ceramic",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ceramic, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1755 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_ceramic",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ceramic, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1855 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_ceramic",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ceramic, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1705 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_stone",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.STONES.Stone, 40864824000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1000 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_stone",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.STONES.Stone, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1050 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_stone",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.STONES.Stone, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1750 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_stone",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.STONES.Stone, 29189160000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1850 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_stone",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.STONES.Stone, 17513496000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1700 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_quartz",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.SiO2, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1018 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_quartz",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.SiO2, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1068 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_quartz",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.SiO2, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1768 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_quartz",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.SiO2, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1868 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_quartz",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.SiO2, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1718 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_carbon",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Graphene, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1019 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_carbon",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Graphene, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1069 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_carbon",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Graphene, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1769 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_carbon",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Graphene, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1869 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_carbon",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Graphene, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1719 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1020 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1070 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1770 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1870 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_bronze",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Bronze, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1720 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1021 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1071 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1771 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1871 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_invar",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Invar, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1721 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1022 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1072 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1772 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1872 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Steel, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1722 known CR.REV inputs"));

        BLOCK_DATA.put(
                "smelting_crucible_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1023 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1073 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1773 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1873 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ti, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1723 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1024 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1074 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1774 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1874 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_tungsten",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.W, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1724 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_stainless_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1025 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_stainless_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1075 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_stainless_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1775 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_stainless_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1875 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_stainless_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.StainlessSteel, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1725 known CR.REV inputs"));

        BLOCK_DATA.put(
                "smelting_crucible_knightmetal",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Knightmetal, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1027 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_knightmetal",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Knightmetal, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1077 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_knightmetal",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Knightmetal, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1777 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_knightmetal",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Knightmetal, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1877 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_knightmetal",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Knightmetal, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1727 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_fiery_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.FierySteel, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1028 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_fiery_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.FierySteel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1078 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_fiery_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.FierySteel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1778 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_fiery_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.FierySteel, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1878 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_fiery_steel",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.FierySteel, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1728 known CR.REV inputs"));

        BLOCK_DATA.put(
                "smelting_crucible_thaumium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Thaumium, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1029 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_thaumium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Thaumium, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1079 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_thaumium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Thaumium, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1779 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_thaumium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Thaumium, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1879 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_thaumium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Thaumium, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1729 known CR.REV inputs"));

        BLOCK_DATA.put(
                "smelting_crucible_chromium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cr, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1033 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_chromium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cr, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1083 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_chromium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cr, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1783 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_chromium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cr, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1883 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_chromium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Cr, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1733 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_molybdenum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Mo, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1034 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_molybdenum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Mo, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1084 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_molybdenum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Mo, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1784 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_molybdenum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Mo, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1884 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_molybdenum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Mo, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1734 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_niobium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Nb, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1035 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_niobium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Nb, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1085 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_niobium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Nb, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1785 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_niobium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Nb, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1885 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_niobium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Nb, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1735 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_tantalum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1036 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_tantalum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1086 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_tantalum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1786 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_tantalum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1886 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_tantalum",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1736 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_osmium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Os, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1037 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_osmium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Os, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1087 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_osmium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Os, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1787 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_osmium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Os, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1887 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_osmium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Os, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1737 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_vanadium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.V, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1038 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_vanadium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.V, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1088 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_vanadium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.V, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1788 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_vanadium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.V, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1888 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_vanadium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.V, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1738 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_iridium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1039 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_iridium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1089 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_iridium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1789 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_iridium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1889 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_iridium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ir, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1739 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_niobium_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.NiobiumTitanium, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1040 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_niobium_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.NiobiumTitanium, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1090 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_niobium_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.NiobiumTitanium, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1790 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_niobium_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.NiobiumTitanium, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1890 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_niobium_titanium",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.NiobiumTitanium, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1740 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta4HfC5, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1043 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta4HfC5, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1093 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta4HfC5, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1793 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta4HfC5, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1893 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_ta4hfc5",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ta4HfC5, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1743 known CR.REV inputs"));
        BLOCK_DATA.put(
                "smelting_crucible_netherite",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Netherite, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1044 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_netherite",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Netherite, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1094 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_netherite",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Netherite, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1794 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_netherite",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Netherite, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1894 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_netherite",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Netherite, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1744 known CR.REV inputs"));

        BLOCK_DATA.put(
                "smelting_crucible_ad",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 4540536000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1049 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_ad",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1099 known CR.REV inputs"));
        BLOCK_DATA.put(
                "mold_basin_ad",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1799 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_crossing_ad",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 3243240000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1899 known CR.REV inputs"));
        BLOCK_DATA.put(
                "crucible_faucet_ad",
                ReversibleCraftingData.perItem(
                        List.of(part(ImportedMaterialData.Ad, 1945944000L)),
                        1,
                        "GT6 Loader_MultiTileEntities 1749 known CR.REV inputs"));
    }
}
