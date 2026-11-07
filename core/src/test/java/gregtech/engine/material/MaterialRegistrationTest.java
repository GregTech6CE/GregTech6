package gregtech.engine.material;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.material.GTMaterial;
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
                "infoOwnerFailure",
                "infoNamespaceFailure",
                "infoProcessTypeFailure",
                "relations",
                "relationTagCycleFailure",
                "relationUnknownTagFailure",
                "relationNamespaceFailure",
                "relationUnknownGroupFailure",
                "relationGroupCycleFailure",
                "processes",
                "processNamespaceFailure",
                "processUnknownMaterialFailure")) {
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
