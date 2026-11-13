package gregtech.content.material;

import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.MaterialRecoveryRules;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemCompositionSource;
import gregtech.engine.material.VanillaUnificationDefinitions;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        var composition =
                new ItemComposition(null, List.of(iron, copper), Materials.Iron, "normalization fixture", true);
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
        var plan = MaterialRecoveryRules.dustPlan(72 * GTValues.U);
        assertTrue(plan.complete());
        assertEquals(72 * GTValues.U, plan.representedUnits());
        assertEquals(0, plan.remainderUnits());
        assertEquals(2, plan.stackCount());
        assertEquals(
                List.of(
                        new MaterialRecoveryRules.DustPile(MaterialPrefix.dust, 64),
                        new MaterialRecoveryRules.DustPile(MaterialPrefix.dust, 8)),
                plan.piles());
        var slot = MaterialRecoveryRules.dustSlotPlan(72 * GTValues.U);
        assertFalse(slot.complete());
        assertEquals(64 * GTValues.U, slot.representedUnits());
        assertEquals(8 * GTValues.U, slot.remainderUnits());
        assertEquals(List.of(new MaterialRecoveryRules.DustPile(MaterialPrefix.dust, 64)), slot.piles());
        var eighth = MaterialRecoveryRules.dustSlotPlan(9 * GTValues.U72);
        assertTrue(eighth.complete());
        assertEquals(9 * GTValues.U72, eighth.representedUnits());
        assertEquals(List.of(new MaterialRecoveryRules.DustPile(MaterialPrefix.dustDiv72, 9)), eighth.piles());

        var composition = new ItemComposition(
                null,
                List.of(MaterialComponent.of(Materials.Gold, 72 * GTValues.U)),
                Materials.Gold,
                List.of(new ItemCompositionSource(
                        ItemCompositionSource.Kind.EXPLICIT, "recovery-test", "test", "", "")),
                true);
        var shredder = MaterialRecoveryRules.planShredder(composition, 4);
        assertTrue(shredder.accepted());
        assertEquals(2, shredder.stackCount());
        assertEquals(
                MaterialRecoveryRules.shredderWork(Materials.Gold),
                MaterialRecoveryRules.planShredderDamaged(
                                new ItemComposition(
                                        null,
                                        List.of(MaterialComponent.of(Materials.Gold, GTValues.U)),
                                        Materials.Gold,
                                        List.of(new ItemCompositionSource(
                                                ItemCompositionSource.Kind.EXPLICIT,
                                                "damaged-dynamic-work",
                                                "test",
                                                "",
                                                "")),
                                        true),
                                4,
                                false)
                        .duration());
        assertEquals(
                MaterialRecoveryRules.staticShredderWork(Materials.Gold),
                MaterialRecoveryRules.planShredderDamaged(
                                new ItemComposition(
                                        null,
                                        List.of(MaterialComponent.of(Materials.Gold, GTValues.U)),
                                        Materials.Gold,
                                        List.of(new ItemCompositionSource(
                                                ItemCompositionSource.Kind.EXPLICIT,
                                                "damaged-static-work",
                                                "test",
                                                "",
                                                "")),
                                        true),
                                4,
                                true)
                        .duration());
        assertEquals(
                MaterialRecoveryRules.ShredderRejection.OUTPUT_LIMIT,
                MaterialRecoveryRules.planShredder(composition, 1).rejection());
        var subFraction = new ItemComposition(
                null,
                List.of(MaterialComponent.of(Materials.Gold, 1)),
                Materials.Gold,
                List.of(new ItemCompositionSource(
                        ItemCompositionSource.Kind.EXPLICIT, "recovery-remainder-test", "test", "", "")),
                true);
        assertEquals(
                MaterialRecoveryRules.ShredderRejection.UNREPRESENTABLE_AMOUNT,
                MaterialRecoveryRules.planShredder(subFraction, 4).rejection());
        var damagedFraction = new ItemComposition(
                null,
                List.of(MaterialComponent.of(Materials.Gold, GTValues.U * 24 / 25)),
                Materials.Gold,
                List.of(new ItemCompositionSource(
                        ItemCompositionSource.Kind.EXPLICIT, "damaged-remainder-test", "test", "", "")),
                true);
        assertEquals(
                MaterialRecoveryRules.ShredderRejection.UNREPRESENTABLE_AMOUNT,
                MaterialRecoveryRules.planShredder(damagedFraction, 4).rejection());
        var damagedPlan = MaterialRecoveryRules.planShredderDamaged(damagedFraction, 4, false);
        assertTrue(damagedPlan.accepted());
        assertTrue(damagedPlan.outputs().get(0).dust().remainderUnits() > 0);
        assertTrue(damagedPlan.skippedComponents().stream()
                .anyMatch(component ->
                        component.reason() == MaterialRecoveryRules.ShredderRejection.UNREPRESENTABLE_AMOUNT));
    }

    @Test
    void shredderProfilesKeepStaticAndDynamicMultipliersSeparate() {
        assertEquals(
                256L * Math.max(1, Materials.NetherStar.getToolQuality() + 1),
                MaterialRecoveryRules.staticShredderWork(Materials.NetherStar));
        assertEquals(
                16L * Math.max(1, Materials.Rubber.getToolQuality() + 1),
                MaterialRecoveryRules.staticShredderWork(Materials.Rubber));
        assertEquals(
                64L * Math.max(1, Materials.Firestone.getToolQuality() + 1),
                MaterialRecoveryRules.shredderWork(Materials.Firestone));
        assertEquals(
                2L * Math.max(1, Materials.Rubber.getToolQuality() + 1),
                MaterialRecoveryRules.shredderWork(Materials.Rubber));
        assertEquals(
                2L * Math.max(1, Materials.MeatRotten.getToolQuality() + 1),
                MaterialRecoveryRules.shredderWork(Materials.MeatRotten));
    }

    @Test
    void recoveryOreClassificationKeepsRawItemsAndBlocksDistinct() {
        assertSame(
                VanillaUnificationDefinitions.OreInputKind.ORE,
                VanillaUnificationDefinitions.oreInputKind("copper_ore"));
        assertSame(
                VanillaUnificationDefinitions.OreInputKind.RAW_METAL,
                VanillaUnificationDefinitions.oreInputKind("raw_iron"));
        assertSame(
                VanillaUnificationDefinitions.OreInputKind.RAW_METAL_BLOCK,
                VanillaUnificationDefinitions.oreInputKind("raw_iron_block"));
        assertSame(
                VanillaUnificationDefinitions.OreInputKind.OTHER,
                VanillaUnificationDefinitions.oreInputKind("iron_ingot"));
    }
}
