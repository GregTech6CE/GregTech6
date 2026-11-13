package gregtech.engine.material;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.addon.material.*;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.api.material.MaterialSentinels;
import gregtech.content.material.MaterialDefinitions;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MaterialRegistrationTest {
    @Test
    void singleValuedProcessesRejectMultipleRulesDuringConstruction() {
        var process = new MaterialProcessKey<>(
                new DataKey("test_addon", "single"), String.class, MaterialProcessCardinality.SINGLE);
        assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialInfoDefinition.Processes(Map.of(
                        process,
                        List.of(
                                new MaterialProcessRule<>(Set.of(), "first"),
                                new MaterialProcessRule<>(Set.of(), "second")))));
    }

    @Test
    void allIdentityFailuresLeaveEveryIndexUntouched() {
        var state = new MaterialRegistryState();
        var iron = new GTMaterial(260, "Iron", "Iron", 0xFFFFFF);
        state.install(iron, MaterialRegistryState.Mode.BUILTIN, List.of("Fe"));
        var names = Map.copyOf(state.names);
        var ids = Map.copyOf(state.ids);
        var all = Set.copyOf(state.materials);
        List<Runnable> failures = List.of(
                () -> state.install(
                        new GTMaterial(261, "Iron", "Iron", 0), MaterialRegistryState.Mode.BUILTIN, List.of()),
                () -> state.install(
                        new GTMaterial(260, "Copper", "Copper", 0), MaterialRegistryState.Mode.BUILTIN, List.of()),
                () -> state.install(new GTMaterial(290, "Fe", "Fe", 0), MaterialRegistryState.Mode.BUILTIN, List.of()),
                () -> state.install(
                        new GTMaterial(290, "Copper", "Copper", 0), MaterialRegistryState.Mode.BUILTIN, List.of("Fe")),
                () -> state.install(
                        new GTMaterial(10000, "Copper", "Copper", 0), MaterialRegistryState.Mode.BUILTIN, List.of()),
                () -> state.install(
                        new GTMaterial(290, "Copper", "Copper", 0), MaterialRegistryState.Mode.ADDON, List.of()),
                () -> state.install(iron, MaterialRegistryState.Mode.SENTINEL, List.of()),
                () -> state.alias("Missing", "NoSuchMaterial"));
        for (var failure : failures) {
            assertThrows(IllegalArgumentException.class, failure::run);
            assertEquals(names, state.names);
            assertEquals(ids, state.ids);
            assertEquals(all, state.materials);
        }
        state.alias("Fe", "Iron");
        assertEquals(names, state.names);
        state.install(new GTMaterial(290, "Copper", "Copper", 0), MaterialRegistryState.Mode.BUILTIN, List.of());
        var before = Map.copyOf(state.names);
        assertThrows(IllegalArgumentException.class, () -> state.alias("Iron", "Copper"));
        assertThrows(IllegalArgumentException.class, () -> state.alias("Fe", "Copper"));
        assertEquals(before, state.names);
    }

    @Test
    void sentinelAndGroupModesHaveExplicitIdRules() {
        var state = new MaterialRegistryState();
        state.install(MaterialSentinels.INVALID, MaterialRegistryState.Mode.SENTINEL, List.of());
        state.install(MaterialSentinels.EMPTY, MaterialRegistryState.Mode.SENTINEL, List.of());
        state.install(new GTMaterial(-1, "AnyIron", "Iron group", 0), MaterialRegistryState.Mode.GROUP, List.of());
        state.install(new GTMaterial(-1, "AnyCopper", "Copper group", 0), MaterialRegistryState.Mode.GROUP, List.of());
        assertEquals(4, state.materials.size());
        assertSame(MaterialSentinels.INVALID, state.ids.get(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> state.install(
                        new GTMaterial(1, "AnyOther", "Group", 0), MaterialRegistryState.Mode.GROUP, List.of()));
    }

    @Test
    void invalidReferencesAndIncompleteCompositionsFailBeforeLinksAreWritten() {
        var iron = new GTMaterial(260, "Iron", "Iron", 0);
        var invalid = MaterialDefinition.builder(261, "Test")
                .composition(
                        2, new MaterialInfoDefinition.Part("Iron", 1), new MaterialInfoDefinition.Part("TypoCopper", 1))
                .build();
        var target = new GTMaterial(261, "Test", "Test", 0);
        var index = Map.of("Iron", iron, "Test", target);
        var error = assertThrows(
                IllegalArgumentException.class, () -> MaterialLinker.validate(List.of(invalid), index::get));
        assertTrue(error.getMessage().contains("Test / composition -> TypoCopper"));
        assertFalse(target.hasComposition());
        assertThrows(IllegalArgumentException.class, () -> new MaterialInfoDefinition.Part("Iron", 0));
        var unknownByproduct = MaterialDefinition.builder(261, "Test")
                .byproducts("TypoByproduct")
                .build();
        var unresolvedTarget = new GTMaterial(261, "Test", "Test", 0);
        error = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialLinker.validate(List.of(unknownByproduct), Map.of("Test", unresolvedTarget)::get));
        assertTrue(error.getMessage().contains("Test / ore processing byproduct -> TypoByproduct"));
        assertTrue(unresolvedTarget.getByProducts().isEmpty());
        var nonMaterialByproduct =
                MaterialDefinition.builder(262, "Other").byproducts("AnyIron").build();
        var unresolvedGroupTarget = new GTMaterial(262, "Other", "Other", 0);
        error = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialLinker.validate(
                        List.of(nonMaterialByproduct),
                        Map.of("Other", unresolvedGroupTarget, "AnyIron", new GTMaterial(-1, "AnyIron", "Group", 0))
                                ::get));
        assertTrue(error.getMessage().contains("Other / ore processing byproduct -> AnyIron"));
        assertTrue(unresolvedGroupTarget.getByProducts().isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> MaterialDefinition.builder(1, "InvalidDivider")
                        .composition(0, new MaterialInfoDefinition.Part("Iron", 1))
                        .build());
        var a = MaterialDefinition.builder(-1, "AnyA").group().members("AnyB").build();
        var b = MaterialDefinition.builder(-1, "AnyB").group().members("AnyA").build();
        var groups = Map.of("AnyA", new GTMaterial(-1, "AnyA", "A", 0), "AnyB", new GTMaterial(-1, "AnyB", "B", 0));
        assertThrows(IllegalArgumentException.class, () -> MaterialLinker.validate(List.of(a, b), groups::get));
    }

    @Test
    void processingTargetsMustBeRealMaterials() {
        var index = Map.of(
                "Test", new GTMaterial(261, "Test", "Test", 0),
                "AnyIron", new GTMaterial(-1, "AnyIron", "Group", 0));
        var invalidTargets = List.of(
                MaterialDefinition.builder(261, "Test").fuel(1, "AnyIron", 1).build(),
                MaterialDefinition.builder(261, "Test").crushing("AnyIron", 1).build(),
                MaterialDefinition.builder(261, "Test").smelting("AnyIron", 1).build(),
                MaterialDefinition.builder(261, "Test")
                        .pulverizing("AnyIron", 1)
                        .build(),
                MaterialDefinition.builder(261, "Test")
                        .processing("Solidifying", "AnyIron", 1)
                        .build(),
                MaterialDefinition.builder(261, "Test")
                        .processing("Bending", "AnyIron", 1)
                        .build(),
                MaterialDefinition.builder(261, "Test")
                        .processing("Compressing", "AnyIron", 1)
                        .build(),
                MaterialDefinition.builder(261, "Test")
                        .processing("Cutting", "AnyIron", 1)
                        .build(),
                MaterialDefinition.builder(261, "Test")
                        .processing("Forging", "AnyIron", 1)
                        .build(),
                MaterialDefinition.builder(261, "Test")
                        .processing("Smashing", "AnyIron", 1)
                        .build(),
                MaterialDefinition.builder(261, "Test")
                        .processing("Working", "AnyIron", 1)
                        .build());
        for (var declaration : invalidTargets) {
            var error = assertThrows(
                    IllegalArgumentException.class, () -> MaterialLinker.validate(List.of(declaration), index::get));
            assertTrue(error.getMessage().contains("Invalid material reference"));
            assertTrue(error.getMessage().contains("AnyIron"));
        }
    }

    @Test
    void declarationRecordsKeepIdentitySeparateFromAuxiliaryDomains() {
        assertEquals(5, MaterialDefinition.class.getRecordComponents().length);
        assertFalse(java.util.Arrays.stream(MaterialDefinition.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("facts")));
        var definition = MaterialDefinitions.find("Iron");
        assertNotNull(definition);
        assertEquals("Iron", definition.identity().name());
        assertEquals(definition.identity().id(), definition.id());
        var graphite = MaterialDefinitions.info("Graphite");
        assertFalse(graphite.composition().parts().isEmpty());
        assertTrue(graphite.composition().divider() > 0);
    }

    @Test
    void legacyMaterialClassHasNoSecondAuxiliaryStorage() {
        var fieldNames = java.util.Arrays.stream(GTMaterial.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(java.util.stream.Collectors.toSet());
        for (String removed : List.of(
                "compositionDivider",
                "compositionComponents",
                "toolTypes",
                "toolSpeed",
                "toolDurability",
                "toolQuality",
                "furnaceBurnTime",
                "targetBurningMaterial",
                "targetBurningAmount",
                "byProducts",
                "targetPulverMaterial",
                "targetPulverAmount",
                "targetCrushingMaterial",
                "targetCrushingAmount",
                "targetSmeltingMaterial",
                "targetSmeltingAmount",
                "oreMultiplier",
                "oreProcessingMultiplier")) assertFalse(fieldNames.contains(removed), removed);
    }

    @Test
    void fluidPhaseCorrectionsArePartOfCanonicalDefinitions() {
        assertTrue(MaterialDefinitions.find("Mercury").properties().contains(MaterialProperty.LIQUID));
        assertTrue(MaterialDefinitions.find("Carbon").properties().contains(MaterialProperty.MOLTEN));
        assertTrue(MaterialDefinitions.find("NitricAcid").properties().contains(MaterialProperty.LIQUID));
        assertTrue(MaterialDefinitions.find("Alumina").properties().contains(MaterialProperty.MOLTEN));
        assertTrue(MaterialDefinitions.find("Sulfur").properties().contains(MaterialProperty.MOLTEN));
        assertTrue(MaterialDefinitions.find("HydrochloricAcid").properties().contains(MaterialProperty.GAS));
    }

    @Test
    void capabilityFlagsUseOneOrderIndependentClosureAndRejectConflicts() {
        var gas = MaterialDefinition.builder(261, "TestGas")
                .properties(MaterialProperty.GAS)
                .build()
                .material();
        assertTrue(gas.properties().contains(MaterialProperty.LIQUID));
        var decomposable = MaterialDefinition.builder(262, "TestCompound")
                .properties(MaterialProperty.CENTRIFUGE)
                .build()
                .material();
        assertTrue(decomposable.properties().contains(MaterialProperty.DECOMPOSABLE));
        assertThrows(
                IllegalArgumentException.class,
                () -> MaterialDefinition.builder(263, "Conflict")
                        .properties(MaterialProperty.FLAMMABLE, MaterialProperty.UNBURNABLE)
                        .build());
    }

    @Test
    void canonicalDecompositionFlagsComeOnlyFromTheTypedRule() {
        for (var declaration : MaterialDefinitions.declarations())
            for (var property :
                    List.of(MaterialProperty.CENTRIFUGE, MaterialProperty.ELECTROLYSER, MaterialProperty.DECOMPOSABLE))
                assertFalse(
                        declaration.material().properties().contains(property),
                        declaration.name() + " declares decomposition mirror " + property);
    }

    @Test
    void typedDecompositionRuleOwnsRoutesAndLegacyFlagsBecomeItsMirror() {
        var material = new GTMaterial(260, "TestCompound", "TestCompound", 0xFFFFFF).put(MaterialProperty.CENTRIFUGE);
        var explicit =
                new MaterialDecompositionRule(Set.of(MaterialDecompositionRule.Route.ELECTROLYSER), List.of(), false);
        var effective = MaterialInfoRegistry.effectiveDecomposition(explicit, material.getProperties());
        assertSame(explicit, effective);
        assertFalse(effective.routes().contains(MaterialDecompositionRule.Route.CENTRIFUGE));

        MaterialInfoRegistry.syncDecompositionFlags(material, effective);
        assertFalse(material.has(MaterialProperty.CENTRIFUGE));
        assertTrue(material.has(MaterialProperty.ELECTROLYSER));
        assertTrue(material.has(MaterialProperty.DECOMPOSABLE));

        var legacyOnly =
                new GTMaterial(261, "LegacyCompound", "LegacyCompound", 0xFFFFFF).put(MaterialProperty.CENTRIFUGE);
        var fallback = MaterialInfoRegistry.effectiveDecomposition(null, legacyOnly.getProperties());
        assertTrue(fallback.routes().contains(MaterialDecompositionRule.Route.CENTRIFUGE));
        assertFalse(fallback.routes().contains(MaterialDecompositionRule.Route.ELECTROLYSER));
    }

    @Test
    void reviewedBentoniteCompositionAndPhosphorMeltingFollowThePinnedSource() {
        var bentonite = MaterialDefinitions.info("Bentonite").composition();
        assertEquals(18, bentonite.divider());
        assertEquals(
                List.of(
                        new MaterialInfoDefinition.Part("SodiumHydroxide", 648648000L),
                        new MaterialInfoDefinition.Part("Clay", 11675664000L)),
                bentonite.parts());
        assertFalse(MaterialDefinitions.find("Phosphor").properties().contains(MaterialProperty.MELTING));
    }

    @Test
    void everyRealCanonicalMaterialHasEnglishAndChineseTranslations() throws Exception {
        var root = Path.of("").toAbsolutePath();
        if (!Files.isDirectory(root.resolve("core"))) root = root.getParent();
        for (String file : List.of("en_us.json", "zh_cn.json")) {
            String language = Files.readString(
                    root.resolve("core/src/main/resources/assets/gt6ce/lang").resolve(file));
            for (var definition : gregtech.content.material.MaterialDefinitions.all()) {
                if (MaterialDefinitions.info(definition.name()).relations().group()) continue;
                String key = "material.gt6ce." + definition.name().toLowerCase(java.util.Locale.ROOT);
                assertTrue(language.contains("\"" + key + "\""), definition.name() + " lacks " + key + " in " + file);
            }
        }
        var almandine = MaterialDefinitions.info("Almandine");
        assertTrue(almandine.relations().forms().contains("ORES"));
    }

    @Test
    void isolatedLifecyclesCoverEarlyReadsFailuresAndSharedAddonDeclarations() throws Exception {
        for (String scenario : List.of(
                "early",
                "facade",
                "normal",
                "aliasPublication",
                "merge",
                "conflict",
                "definitionFailure",
                "linkFailure",
                "metadata",
                "metadataUnknownFailure",
                "metadataOwnerFailure",
                "metadataNamespaceFailure",
                "metadataTypeFailure",
                "info",
                "infoLegacy",
                "infoLegacyToolConflictFailure",
                "infoOwnerFailure",
                "infoNamespaceFailure",
                "infoProcessTypeFailure",
                "infoProcessFormAlias",
                "infoProcessSingleConflictFailure",
                "infoProcessMultiple",
                "infoProcessMissingFormFailure",
                "infoProcessUnknownFormFailure",
                "relations",
                "relationTagCycleFailure",
                "relationUnknownTagFailure",
                "relationNamespaceFailure",
                "relationUnknownGroupFailure",
                "relationGroupCycleFailure",
                "processDecomposition",
                "infoDecomposition",
                "infoDecompositionBlock",
                "infoDecompositionLiquid",
                "infoDecompositionGas",
                "infoDecompositionConflictFailure",
                "infoDecompositionOwnerFailure",
                "processDecompositionOwnerFailure",
                "processes",
                "processFormAlias",
                "processSingleConflictFailure",
                "processMultiple",
                "processUnknownFormFailure",
                "processNamespaceFailure",
                "processUnknownMaterialFailure",
                "processGroupMaterialFailure")) {
            var root = Path.of("").toAbsolutePath();
            if (!Files.isDirectory(root.resolve("core"))) root = root.getParent();
            var output = root.resolve("work/material-system-refactor/02/lifecycle-" + scenario + ".log");
            Files.createDirectories(output.getParent());
            var classpath = new java.util.ArrayList<String>();
            for (Class<?> type : List.of(MaterialLifecycleProbe.class, GTMaterialRegistry.class, GTMaterial.class))
                classpath.add(Path.of(type.getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI())
                        .toString());
            var process = new ProcessBuilder(
                            Path.of(System.getProperty("java.home"), "bin/java").toString(),
                            "-cp",
                            String.join(File.pathSeparator, classpath),
                            MaterialLifecycleProbe.class.getName(),
                            scenario)
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
            boolean done = process.waitFor(45, TimeUnit.SECONDS);
            if (!done) process.destroyForcibly();
            assertTrue(done, scenario + " timed out");
            assertEquals(0, process.exitValue(), () -> "Lifecycle failed: " + output);
        }
    }
}
