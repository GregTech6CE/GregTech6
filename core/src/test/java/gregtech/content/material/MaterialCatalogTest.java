package gregtech.content.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.fluid.FluidDefinitions;
import gregtech.content.recipe.SourceRecyclingFacts;
import gregtech.content.recipe.SourceWaxMaterials;
import gregtech.content.storage.ItemBarrelCatalog;
import gregtech.content.tool.AnvilVariantCatalog;
import gregtech.engine.material.GTMaterialRegistry;
import org.junit.jupiter.api.Test;

class MaterialCatalogTest {
    @Test
    void standaloneMaterialsAndDeviceCatalogsResolveWithoutLegacyMods() {
        MaterialTestFixture.ensureInitialized();
        FluidDefinitions.prepare();
        verifyCanonicalFluids();
        verifyAlloyMeltingAndMoltenTemperature();
        // Building the thermal facts also verifies every phase binding and target has an identity.
        assertTrue(!SourceRecyclingFacts.all().isEmpty());
        assertNotNull(SourceRecyclingFacts.find(260, "Iron"));
        assertNotNull(SourceRecyclingFacts.find(790, "Gold"));
        assertNull(SourceRecyclingFacts.find(1850, "Abyssalnite"));
        assertNull(SourceRecyclingFacts.find(8703, "Manyullyn"));
        assertTrue(gregtech.content.energy.WireCatalog.specifications().stream()
                .anyMatch(wire -> wire.material() == Materials.Gold && wire.insulated() && wire.size() == 4));
        for (int id : new int[] {
            1190, 1200, 2390, 8004, 8005, 8106, 8198, 8199, 8202, 8213, 8227, 8228, 8243, 8244, 8246, 8279, 8317, 8350,
            8351, 8352, 8353, 8354, 8355, 8356, 8357, 8358, 8362, 8363, 8364, 8373, 8388, 8394, 8411, 8414, 8463, 8466,
            8469, 8509, 8512, 8513, 8523, 8525, 8526, 8527, 8606, 8613, 8637, 8639, 8642, 8649, 8650, 8654, 8678, 8683,
            8688, 8690, 8691, 8692, 8693, 8694, 8695, 8696, 8701, 8711, 8718, 8725, 8728, 8731, 8736, 8753, 8754, 8755,
            8757, 8766, 8769, 8770, 8771, 8776, 8794, 8795, 8805, 8806, 8807, 8808, 8809, 8810, 8811, 8812, 8813, 9172,
            9173, 9178, 9181, 9182, 9183, 9191, 9244, 9245, 9249, 9250, 9251, 9253, 9254, 9255, 9256, 9257, 9258, 9259,
            9260, 9262, 9263, 9264, 9265, 9266, 9267, 9268, 9269, 9270, 9320, 9324, 9325, 9326, 9327, 9328, 9331, 9332,
            9333, 9337, 9338, 9339, 9340, 9341, 9358, 9371, 9406, 9704, 9706, 9707, 9708, 9714, 9716, 9717, 9718, 9719,
            9778, 9786, 9787, 9788, 9789, 9795, 9797, 9798, 9799, 9807, 9887
        }) assertTrue(!GTMaterialRegistry.get(id).isValid(), "Retired material " + id);
        for (int i = 0; i < 16; i++) {
            var dye = gregtech.content.data.ImportedMaterialData.DATA.DYE_MATERIALS[i];
            assertEquals(8250 + i, dye.getId());
            assertTrue(dye.getName().startsWith("Dye"));
            assertTrue(!GTMaterialRegistry.get(9500 + i).isValid());
            assertNotNull(SourceRecyclingFacts.find(dye.getId(), dye.getName()));
        }
        for (String retained : java.util.List.of(
                "Greatwood",
                "DominicanAmber",
                "ElvenQuartz",
                "Adamantine",
                "TitaniumIridium",
                "Duranium",
                "Trinium",
                "Naquadah",
                "TritaniumElemental",
                "Tritanium",
                "Efrine",
                "Firestone")) assertTrue(GTMaterialRegistry.get(retained).isValid(), retained);
        for (int id : new int[] {1250, 8752, 9204, 9205, 9206, 9207, 9208, 9209})
            assertTrue(GTMaterialRegistry.get(id).isValid(), "Restored Tritanium material " + id);
        assertSame(GTMaterialRegistry.get("Tn"), GTMaterialRegistry.get(1250), "Tn alias");
        var waxes = SourceWaxMaterials.materials();
        assertEquals(
                java.util.List.of(8235, 8236, 8238, 8239),
                waxes.stream().map(material -> material.getId()).toList());
        assertEquals(
                java.util.List.of("Wax", "WaxBee", "WaxParaffin", "WaxPlant"),
                waxes.stream().map(material -> material.getName()).toList());
        for (String name : new String[] {
            "Iron",
            "Amethyst",
            "Apatite",
            "Honey",
            "Ethanol",
            "Maple",
            "Willow",
            "BlueMahoe",
            "WoodDead",
            "Cherrywood",
            "WoodTreated"
        }) assertTrue(GTMaterialRegistry.get(name).isValid(), name);
        for (var barrel : ItemBarrelCatalog.ALL) {
            assertTrue(GTMaterialRegistry.get(barrel.material()).isValid(), barrel.id());
        }
        for (var anvil : AnvilVariantCatalog.ALL)
            assertTrue(GTMaterialRegistry.get(anvil.material()).isValid(), anvil.id());
    }

    /** Generated molten fluids carry the material's final melting point, including clamped alloys. */
    private static void verifyAlloyMeltingAndMoltenTemperature() {
        for (var expected : java.util.Map.of("ArsenicBronze", 1357, "ArsenicCopper", 1070, "HSSE", 2367)
                .entrySet()) {
            var material = GTMaterialRegistry.get(expected.getKey());
            assertEquals(expected.getValue(), material.getMeltingPoint(), expected.getKey());
            var molten = gregtech.content.data.FluidCatalog.get("GenMolten_" + expected.getKey());
            assertNotNull(molten, expected.getKey());
            assertEquals(expected.getValue(), molten.temperature(), expected.getKey());
        }
    }

    private static void verifyCanonicalFluids() {
        var catalog = gregtech.content.data.FluidCatalog.all();
        for (String name : java.util.List.of("Brass", "Zinc", "Calcite")) {
            var molten = gregtech.content.data.FluidCatalog.get("GenMolten_" + name);
            assertNotNull(molten, name);
            assertEquals(GTMaterialRegistry.get(name).getMeltingPoint(), molten.temperature(), name);
            assertEquals(10, molten.luminosity(), name);
            assertEquals(
                    1L,
                    catalog.values().stream()
                            .filter(fluid ->
                                    fluid.registryName().equals("molten." + name.toLowerCase(java.util.Locale.ROOT)))
                            .count(),
                    name);
        }
        for (var pair : java.util.Map.of("WaterDistilled", "DistW", "Ethanol", "BioEthanol", "OliveOil", "Oil_Olive")
                .entrySet()) {
            assertEquals(
                    gregtech.content.data.FluidCatalog.get(pair.getValue()),
                    gregtech.content.data.FluidCatalog.get("GenLiquid_" + pair.getKey()));
            assertEquals(
                    gregtech.content.data.FluidCatalog.get(pair.getValue()),
                    gregtech.content.data.FluidCatalog.get("GenGas_" + pair.getKey()));
        }
        for (String id : java.util.List.of(
                "ic2uumatter",
                "uuamplifier",
                "waterdistilled",
                "ethanol",
                "oliveoil",
                "for.honey",
                "grc.honey",
                "ic2biomass",
                "volcanic_lava_fluid",
                "swampwater",
                "stagnantwater",
                "molten.lava",
                "pyrotheum",
                "cryotheum",
                "petrotheum",
                "aerotheum",
                "mana",
                "water",
                "riverwater",
                "seawater",
                "watergeothermal",
                "heliumplasma",
                "nitrogenplasma",
                "freshwater",
                "molten.al2o3",
                "molten.cryolite",
                "molten.lithiumchloride",
                "molten.lithiumchlorate",
                "molten.aluminiumfluoride")) {
            assertTrue(
                    catalog.values().stream()
                            .noneMatch(fluid -> fluid.registryName().equals(id)),
                    id);
        }
        assertNull(gregtech.content.data.FluidCatalog.get("Water"));
        assertTrue(gregtech.content.data.FluidCatalog.get("Lava").usesVanillaFluid());
        assertEquals(
                gregtech.content.data.FluidCatalog.FluidTextureMode.DEDICATED,
                gregtech.content.data.FluidCatalog.get("Lava_Pahoehoe").textureMode());
        assertNotNull(gregtech.content.data.FluidCatalog.get("MatterNeutral"));
        assertNotNull(gregtech.content.data.FluidCatalog.get("MatterCharged"));
        // Loader_Fluids:618 creates mercury as one ordinary simple liquid. GT6's metal factory does
        // not set MOLTEN, so the generated molten ID is a port compatibility entry, not a second phase.
        var mercuryLiquid = gregtech.content.data.FluidCatalog.get("GenLiquid_Mercury");
        assertNotNull(mercuryLiquid);
        assertEquals("mercury", mercuryLiquid.registryName());
        assertTrue(mercuryLiquid.hasFlag(gregtech.content.data.FluidCatalog.FluidFlags.LIQUID));
        assertTrue(mercuryLiquid.hasFlag(gregtech.content.data.FluidCatalog.FluidFlags.SIMPLE));
        assertNull(gregtech.content.data.FluidCatalog.get("GenMolten_Mercury"));
        // MT.Hg.liquid(U, T) is one material unit; the drink rows are 250 mB portions.
        assertEquals(144, gregtech.content.food.GTBottles.amountOf("mercury_bottle"));
        assertEquals(250, gregtech.content.food.GTBottles.amountOf("soda"));
        assertTrue(!GTMaterialRegistry.get(9884).isValid());
        assertTrue(!GTMaterialRegistry.get(9885).isValid());
    }

    @Test
    void netherBedrockOresKeepOnlyTheRetainedMaterials() {
        MaterialTestFixture.ensureInitialized();
        var names = gregtech.content.worldgen.GTBedrockOres.NETHER.stream()
                .map(gregtech.content.worldgen.GTBedrockOres.BedrockOre::name)
                .toList();
        assertEquals(java.util.List.of("glowstone", "netherquartz", "ancientdebris", "efrine", "firestone"), names);
        assertSame(
                Materials.Efrine,
                gregtech.content.worldgen.GTBedrockOres.NETHER.get(3).material());
        assertSame(
                Materials.Firestone,
                gregtech.content.worldgen.GTBedrockOres.NETHER.get(4).material());
        assertTrue(!GTMaterialRegistry.get(8456).isValid());
        assertTrue(!GTMaterialRegistry.get(8457).isValid());
    }
}
