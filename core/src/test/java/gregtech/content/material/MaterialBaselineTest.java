package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.MaterialRecoveryRules;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MaterialBaselineTest {
    @BeforeAll
    static void init() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void cleanJvmSnapshotsAreRepeatableAndSpiIsIsolated() throws Exception {
        Path output = MaterialReferenceAudit.root().resolve("work/material-system-refactor/core");
        runSnapshot(output.resolve("first"), null);
        runSnapshot(output.resolve("second"), null);
        for (String file : List.of("materials.tsv", "prefixes.tsv", "aliases.tsv", "references.tsv"))
            assertEquals(
                    Files.readString(output.resolve("first/" + file)),
                    Files.readString(output.resolve("second/" + file)),
                    file);
        Path spi = output.resolve("spi");
        Path service = spi.resolve("META-INF/services/gregtech.api.addon.material.AddonMaterialContributor");
        Files.createDirectories(service.getParent());
        Files.writeString(service, MaterialBaselineTestAddon.class.getName() + "\n");
        runSnapshot(output.resolve("addon"), spi);
        var addon = Files.readAllLines(output.resolve("addon/materials.tsv"));
        var base = Files.readAllLines(output.resolve("first/materials.tsv"));
        assertTrue(base.stream().noneMatch(line -> line.contains("BaselineCompound")));
        String row = addon.stream()
                .filter(line -> line.startsWith("material_baseline_test:BaselineCompound\t"))
                .findFirst()
                .orElseThrow();
        assertTrue(row.contains("\tdynamic\ttrue\ttrue\t"), row);
        assertTrue(row.contains("CENTRIFUGE"), row);
        assertTrue(row.contains("core:Iron=" + GTValues.U * 2 + ",core:Copper=" + GTValues.U), row);
        assertEquals(
                base,
                addon.stream()
                        .filter(line -> !line.startsWith("material_baseline_test:"))
                        .toList());
    }

    private static void runSnapshot(Path output, Path spi) throws Exception {
        Files.createDirectories(output);
        var classpath = new ArrayList<String>();
        for (Class<?> type : List.of(MaterialBaselineSnapshot.class, GTMaterialRegistry.class, GTMaterial.class))
            classpath.add(Path.of(type.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toString());
        classpath.add(Path.of(MaterialBaselineTest.class
                        .getResource("/gregtech/material-reference-history.tsv")
                        .toURI())
                .getParent()
                .getParent()
                .toString());
        var command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-cp",
                String.join(File.pathSeparator, classpath),
                MaterialBaselineSnapshot.class.getName(),
                output.toString()));
        if (spi != null) command.add(spi.toString());
        var process = new ProcessBuilder(command)
                .directory(MaterialReferenceAudit.root().toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.resolve("startup.log").toFile())
                .start();
        boolean finished = process.waitFor(45, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        assertTrue(finished, "snapshot JVM timed out: " + output);
        assertEquals(0, process.exitValue(), () -> "snapshot JVM failed; see " + output.resolve("startup.log"));
    }

    @Test
    void componentNormalizationKeepsThePrimaryAndSemanticOrder() {
        var iron = MaterialComponent.of(Materials.Iron, GTValues.U);
        var copper = MaterialComponent.of(Materials.Copper, GTValues.U * 2);
        assertEquals(
                MaterialBaselineSnapshot.components(List.of(iron, copper), false),
                MaterialBaselineSnapshot.components(List.of(copper, iron), false));
        assertNotEquals(
                MaterialBaselineSnapshot.components(List.of(iron, copper), true),
                MaterialBaselineSnapshot.components(List.of(copper, iron), true));
        var composition = new ItemComposition(null, List.of(iron, copper), "normalization fixture", true);
        assertSame(Materials.Iron, composition.material());
        assertThrows(
                IllegalArgumentException.class,
                () -> new ItemComposition(
                        null, List.of(MaterialComponent.of(Materials.Iron, -1)), "invalid fixture", true));
        var reversed = new ArrayList<>(GTMaterialRegistry.allMaterials());
        java.util.Collections.reverse(reversed);
        assertEquals(
                MaterialBaselineSnapshot.materials(GTMaterialRegistry.allMaterials()),
                MaterialBaselineSnapshot.materials(reversed));
        for (var material : GTMaterialRegistry.allMaterials()) {
            if (!material.resolve().isValid()) continue;
            for (var part : material.getCompositionComponents()) {
                assertTrue(part.material().resolve().isValid(), material.getName() + " component identity");
                assertTrue(part.amount() > 0, material.getName() + " component amount");
            }
            if (material.hasComposition()) assertTrue(material.getCompositionDivider() > 0, material.getName());
        }
    }

    @Test
    void referencesAreClassifiedAndNewInvalidReferencesFailWithContext() throws Exception {
        var rows = MaterialReferenceAudit.collect().stream()
                .map(MaterialReferenceAudit::describe)
                .sorted()
                .toList();
        Path output = MaterialReferenceAudit.root().resolve("work/material-system-refactor/core");
        Files.createDirectories(output);
        Files.write(output.resolve("reference-audit.tsv"), rows);
        var failures = rows.stream()
                .filter(row -> row.endsWith("\tDANGLING") || row.endsWith("\tID_NAME_MISMATCH"))
                .toList();
        try (var stream = getClass().getResourceAsStream("/gregtech/material-reference-history.tsv")) {
            assertNotNull(stream);
            var seen = new java.util.HashSet<String>();
            for (String line : new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .lines()
                    .toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                var cells = line.split("\t", -1);
                String key = String.join("\t", java.util.Arrays.copyOf(cells, 5));
                assertTrue(seen.add(key), "duplicate historical reference: " + key);
                assertTrue(rows.contains(key + "\t" + cells[5]), "stale historical classification: " + line);
            }
        }
        assertTrue(
                failures.isEmpty(),
                () -> "Invalid material references: " + failures + "; see " + output.resolve("reference-audit.tsv"));
        var missing = new MaterialReferenceAudit.Reference(
                "composition", "Iron", "component[0]", "BaselineMissingMaterial", 0);
        var error = assertThrows(IllegalArgumentException.class, () -> MaterialReferenceAudit.requireValid(missing));
        assertTrue(error.getMessage().contains("composition\tIron\tcomponent[0]\tBaselineMissingMaterial"));
        assertThrows(
                IllegalArgumentException.class,
                () -> MaterialReferenceAudit.requireValid(new MaterialReferenceAudit.Reference(
                        "decomposition", "Iron", "owner", "Copper", Materials.Iron.getId())));
    }

    @Test
    void auditedParityExpectationsRemainSeparateFromObservedDifferences() {
        // MT.Fe / MT.Cu and the explicit GT6CE Graphite decision are correctness expectations.
        assertSame(GTMaterialRegistry.get("Fe"), GTMaterialRegistry.get(260));
        assertSame(GTMaterialRegistry.get("Cu"), GTMaterialRegistry.get(290));
        var graphite = GTMaterialRegistry.get("Graphite");
        assertEquals(GTValues.U, graphite.getTargetSmeltingAmount());
        assertSame(GTMaterialRegistry.get("C"), graphite.getTargetSmeltingMaterial());
        // Observed truncation is a migration alarm, not authorization to discard eight units.
        var plan = MaterialRecoveryRules.dust(72 * GTValues.U);
        assertNotNull(plan);
        assertSame(MaterialPrefix.dust, plan.prefix());
        assertEquals(64, plan.count(), "known 72U -> 64U difference; phase 05 must replace this observation");
    }
}
