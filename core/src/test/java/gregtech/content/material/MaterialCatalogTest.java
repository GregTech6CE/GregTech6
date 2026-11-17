package gregtech.content.material;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialKey;
import gregtech.content.fluid.FluidDefinitions;
import gregtech.content.storage.ItemBarrelCatalog;
import gregtech.content.tool.AnvilVariantCatalog;
import gregtech.engine.material.GTMaterialRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaterialCatalogTest {
    @Test
    void reviewedRecipeFactsAreFullyProjectedIntoMaterialDeclarations() {
        var declarations = MaterialDefinitions.declarations();
        assertEquals(198, processCount(declarations, MaterialFactKeys.SOURCE_FURNACE));
        assertEquals(2, processCount(declarations, MaterialFactKeys.SOURCE_GRADED_GEM));
        assertEquals(39, processCount(declarations, MaterialFactKeys.SOURCE_ORE_INPUT));
        assertEquals(1, processCount(declarations, MaterialFactKeys.SOURCE_SIFTING_EXCLUSIONS));
        assertEquals(9, processCount(declarations, MaterialFactKeys.SOURCE_CRUSHED_EXCLUSIONS));
        assertEquals(11, processCount(declarations, MaterialFactKeys.SOURCE_ORE_SHREDDER));
        assertEquals(670, processCount(declarations, MaterialFactKeys.SOURCE_MACERATION));
        assertEquals(142, processCount(declarations, MaterialFactKeys.SOURCE_WASHING));
        assertEquals(51, processCount(declarations, MaterialFactKeys.MATERIAL_FLUID_UNIT));
        assertEquals(134, processCount(declarations, MaterialFactKeys.GENERIFICATION_TARGET));
        assertEquals(733, processCount(declarations, MaterialFactKeys.SOURCE_EXTRUSION));
        // Four additional source lens roles belong to absent optional Hexorium materials.
        assertEquals(53, processCount(declarations, MaterialFactKeys.SOURCE_LENS_ROLE));
        assertEquals(1, processCount(declarations, MaterialFactKeys.SOURCE_CINNABAR_FOLLOWON));
        assertEquals(3, processCount(declarations, MaterialFactKeys.SOURCE_GENERATED_FLUID));
        assertEquals(672, processCount(declarations, MaterialFactKeys.SOURCE_RECYCLING));
        assertEquals(15, processCount(declarations, MaterialFactKeys.SOURCE_MOLTEN_FLUID));
        assertEquals(672, processCount(declarations, MaterialFactKeys.SOURCE_ORE_CRUSHING));
        assertEquals(6, processCount(declarations, MaterialFactKeys.SOURCE_PURIFIED_CENTRIFUGE_PHASE));
        assertEquals(7, processCount(declarations, MaterialFactKeys.SOURCE_PURIFIED_CENTRIFUGE_NO_DEFAULT));
        assertEquals(2, processCount(declarations, MaterialFactKeys.SOURCE_ADDITIONAL_WIRE_FAMILY));
        assertEquals(1, processCount(declarations, MaterialFactKeys.COMPRESSOR_DUST_TO_GEM_PLATE));
        assertEquals(
                14, processCount(declarations, gregtech.content.material.SpecialProcessMethods.AQUA_REGIA_LEACHING));
        assertTrue(processCount(
                        declarations,
                        gregtech.content.material.SpecialProcessMethods.HYDROCHLORIC_ACID_MANGANESE_LEACHING)
                > 1);
    }

    @Test
    void standaloneMaterialsAndDeviceCatalogsResolveWithoutLegacyMods() {
        MaterialTestFixture.ensureInitialized();
        FluidDefinitions.prepare();
        verifyCanonicalFluids();
        verifyAlloyMeltingAndMoltenTemperature();
        var processes = GregTechMaterials.processes();
        // The material process payload verifies every source phase binding and target identity.
        assertTrue(processes
                .findSingle(new MaterialKey("Iron"), MaterialFactKeys.SOURCE_RECYCLING)
                .isPresent());
        assertTrue(processes
                .findSingle(new MaterialKey("Gold"), MaterialFactKeys.SOURCE_RECYCLING)
                .isPresent());
        assertTrue(processes
                .findSingle(new MaterialKey("Abyssalnite"), MaterialFactKeys.SOURCE_RECYCLING)
                .isEmpty());
        assertTrue(processes
                .findSingle(new MaterialKey("Manyullyn"), MaterialFactKeys.SOURCE_RECYCLING)
                .isEmpty());
        assertFalse(processes
                .findSingle(new MaterialKey("Water"), MaterialFactKeys.SOURCE_RECYCLING)
                .orElseThrow()
                .value()
                .inputEligible());
        assertTrue(processes
                .findSingle(new MaterialKey("Iron"), MaterialFactKeys.SOURCE_EXTRUSION)
                .isPresent());
        var aquaRegia = processes
                .findSingle(new MaterialKey("AquaRegia"), MaterialFactKeys.MATERIAL_FLUID_UNIT)
                .orElseThrow()
                .value();
        assertTrue(GregTechMaterials.catalog()
                .specialProcesses()
                .find(new gregtech.api.addon.material.DataKey("gt6ce", "special_process/aqua_regia_leaching"))
                .isPresent());
        assertTrue(
                processes
                        .find(
                                new MaterialKey("Gold"),
                                gregtech.content.material.SpecialProcessMethods.AQUA_REGIA_LEACHING)
                        .stream()
                        .anyMatch(rule ->
                                rule.value() instanceof gregtech.api.addon.material.SpecialProcessValue.Target));
        assertTrue(processes
                .find(new MaterialKey("AquaRegia"), gregtech.content.material.SpecialProcessMethods.AQUA_REGIA_LEACHING)
                .stream()
                .anyMatch(rule -> rule.value() instanceof gregtech.api.addon.material.SpecialProcessValue.Reagent));
        assertTrue(aquaRegia.units().stream()
                .anyMatch(unit -> unit.phase().equals("liquid")
                        && unit.fluidField().equals("GenLiquid_AquaRegia")
                        && unit.referenceMillibuckets() == 1000));
        for (var phase : gregtech.content.material.catalog.SpecialProcessData.Phase.values()) {
            var units = processes
                    .findSingle(new MaterialKey(phase.material().name()), MaterialFactKeys.MATERIAL_FLUID_UNIT)
                    .orElseThrow(() -> new AssertionError("Missing acid fluid units: " + phase))
                    .value()
                    .units();
            assertTrue(
                    units.stream()
                            .anyMatch(unit ->
                                    unit.phase().equals(phase.state().name().toLowerCase(java.util.Locale.ROOT))),
                    phase.name());
        }
        var titaniumFluid = processes
                .findSingle(new MaterialKey("Titanium"), MaterialFactKeys.SOURCE_GENERATED_FLUID)
                .orElseThrow()
                .value()
                .rules()
                .stream()
                .findFirst()
                .orElseThrow();
        assertTrue(titaniumFluid.wholeDustMetal());
        assertEquals(4.54F, titaniumFluid.materialDensity());
        assertTrue(processes
                .findSingle(new MaterialKey("Carbon14"), MaterialFactKeys.SOURCE_MOLTEN_FLUID)
                .orElseThrow()
                .value());
        var cinnabarRule = processes
                .findSingle(new MaterialKey("Cinnabar"), MaterialFactKeys.SOURCE_CINNABAR_FOLLOWON)
                .orElseThrow()
                .value();
        assertEquals(9114, cinnabarRule.cinnabar().id());
        for (String byproduct : java.util.List.of("Redstone", "Sulfur", "Glowstone", "Selenium", "SluiceSand")) {
            assertTrue(processes
                    .findSingle(new MaterialKey(byproduct), MaterialFactKeys.SOURCE_CINNABAR_FOLLOWON)
                    .isEmpty());
        }
        assertTrue(GTMaterialRegistry.get("Iron").has(gregtech.api.material.MaterialProperty.MAGNETIC));
        assertEquals(
                MaterialFactKeys.Decision.YES,
                processes
                        .findSingle(new MaterialKey("Lithium6"), MaterialFactKeys.SOURCE_FURNACE)
                        .orElseThrow()
                        .value());
        assertEquals(
                MaterialFactKeys.Decision.YES,
                processes
                        .findSingle(new MaterialKey("HydratedCoal"), MaterialFactKeys.SOURCE_FURNACE)
                        .orElseThrow()
                        .value());
        assertTrue(processes
                .findSingle(new MaterialKey("Netherite"), MaterialFactKeys.SOURCE_ADDITIONAL_WIRE_FAMILY)
                .orElseThrow()
                .value());
        assertTrue(processes
                .findSingle(new MaterialKey("Naquadah"), MaterialFactKeys.SOURCE_ADDITIONAL_WIRE_FAMILY)
                .orElseThrow()
                .value());
        assertTrue(processes
                .findSingle(new MaterialKey("Titanium"), MaterialFactKeys.SOURCE_ADDITIONAL_WIRE_FAMILY)
                .isEmpty());
        assertTrue(processes
                .findSingle(new MaterialKey("Ice"), MaterialFactKeys.SOURCE_SIFTING_EXCLUSIONS)
                .orElseThrow()
                .value()
                .excludes("gemLegendary"));
        assertTrue(gregtech.content.energy.WireCatalog.specifications().stream()
                .anyMatch(wire -> wire.material() == Materials.Gold && wire.insulated() && wire.size() == 4));
        for (int id : new int[] {
            1190, 1200, 2390, 8004, 8005, 8106, 8198, 8199, 8202, 8213, 8227, 8228, 8243, 8244, 8246, 8279, 8317, 8350,
            8351, 8352, 8353, 8354, 8355, 8356, 8357, 8358, 8362, 8363, 8364, 8373, 8388, 8394, 8398, 8411, 8414, 8422,
            8463, 8466, 8469, 8509, 8512, 8513, 8523, 8525, 8526, 8527, 8606, 8613, 8637, 8639, 8642, 8649, 8650, 8654,
            8678, 8683, 8688, 8690, 8691, 8692, 8693, 8694, 8695, 8696, 8701, 8711, 8718, 8725, 8728, 8731, 8736, 8753,
            8754, 8755, 8757, 8766, 8769, 8770, 8771, 8776, 8791, 8792, 8794, 8795, 8801, 8805, 8806, 8807, 8808, 8809,
            8810, 8811, 8812, 8813, 9172, 9173, 9178, 9181, 9182, 9183, 9191, 9244, 9245, 9249, 9250, 9251, 9252, 9253,
            9254, 9255, 9256, 9257, 9258, 9259, 9260, 9262, 9263, 9264, 9265, 9266, 9267, 9268, 9269, 9270, 9320, 9324,
            9325, 9326, 9327, 9328, 9331, 9332, 9333, 9337, 9338, 9339, 9340, 9341, 9358, 9371, 9406, 9704, 9706, 9707,
            9708, 9714, 9716, 9717, 9718, 9719, 9778, 9786, 9787, 9788, 9789, 9795, 9797, 9798, 9799, 9807, 9887
        }) assertTrue(!GTMaterialRegistry.get(id).isValid(), "Retired material " + id);
        for (int i = 0; i < 16; i++) {
            var dye = gregtech.content.data.ImportedMaterialData.DATA.DYE_MATERIALS[i];
            assertEquals(8250 + i, dye.getId());
            assertTrue(dye.getName().startsWith("Dye"));
            assertTrue(!GTMaterialRegistry.get(9500 + i).isValid());
            assertTrue(processes
                    .findSingle(new MaterialKey(dye.getName()), MaterialFactKeys.SOURCE_RECYCLING)
                    .isPresent());
        }
        for (String retained : java.util.List.of(
                "Greatwood",
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
        assertEquals(
                "watergeothermal",
                gregtech.content.data.FluidCatalog.get("Water_Geothermal").registryName());
        assertNotNull(gregtech.content.worldgen.FluidSpringRules.byFluid("gt6ce:watergeothermal"));
        assertTrue(gregtech.content.data.FluidCatalog.get("Lava").usesVanillaFluid());
        assertEquals(
                gregtech.content.data.FluidCatalog.FluidTextureMode.DEDICATED,
                gregtech.content.data.FluidCatalog.get("Lava_Pahoehoe").textureMode());
        assertNotNull(gregtech.content.data.FluidCatalog.get("MatterNeutral"));
        assertNotNull(gregtech.content.data.FluidCatalog.get("MatterCharged"));
        // Loader_Fluids creates mercury as one ordinary simple liquid. GT6's metal factory does
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

    private static long processCount(
            java.util.List<gregtech.engine.material.MaterialDefinition.Declaration> declarations,
            gregtech.api.addon.material.MaterialProcessKey<?> key) {
        return declarations.stream()
                .filter(declaration -> !declaration.info().processes().find(key).isEmpty())
                .count();
    }
}
