package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialForms;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6 decides every ore prefix from its item-generator flags ({@code OP.java}, imported into
 * {@code MaterialForms}); the port approximates those conditions with its own {@code MaterialProperty}
 * values and consults the flags where the approximation would be wrong. These tests check the outcome
 * for the flags that were aligned in §23 — an ore block for every {@code ORES} material, a machine
 * casing for every {@code PARTS} material, dusts for {@code DUSTS}/{@code DIRTY_DUSTS}, rounds for
 * {@code PARTS} — so a regression in the condition shows up as a missing form rather than as a
 * silently missing item in the world.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class PrefixConditionParityTests {

    private static final String[] KNOWN_GAPS = {
            // materials the port gives a form GT6 does not need, or the other way round, listed by name
    };

    /**
     * Flags {@code TD.java} declares but no material in {@code MT.java} carries: conditions written
     * over them are dead in the original. Aligning a condition to one of these would invent content
     * ({@code dustImpure = DIRTY_DUSTS} is the case that came up), so they are pinned here.
     */
    private static final List<String> DEAD_FLAGS = List.of(
            "CONTAINERS_PLASMA", "CONTAINERS_SOLID", "DIRTY_DUSTS", "PIPES", "PLASMA", "TOOLS",
            "VAPORS", "WEAPONS");

    /**
     * The port registers no items or blocks for {@code HIDDEN} materials ({@code Loader_Items:181},
     * {@code Loader_Blocks:52}) — they are GT6's pseudo-materials used inside chemical formulas
     * ({@code Ma} "Magic", the particles, the sentinels), so they are the one legitimate exception to
     * "GT6's flag implies the form".
     */
    private static boolean registerable(GTMaterial material) {
        return material != null && material.isValid()
                && !material.has(com.gregtech.gregtech.api.material.MaterialProperty.HIDDEN);
    }

    /** Every material GT6 gives an ore to must have an ore block (and a small ore) in the port. */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void oreFlagDrivesOreBlocks(GameTestHelper h) {
        int checked = 0;
        List<String> missing = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!registerable(material)) continue;
            if (!MaterialForms.has(material, "ORES")) continue;
            checked++;
            if (GTBlocks.getStack(BlockMaterialPrefix.ore, material).isEmpty()
                    || GTBlocks.getStack(BlockMaterialPrefix.oreSmall, material).isEmpty()) {
                missing.add(material.getName());
            }
        }
        h.assertTrue(checked > 400, "materials with GT6's ORES flag: " + checked);
        h.assertTrue(missing.isEmpty(), "ORES materials without an ore block (" + missing.size()
                + "): " + missing.subList(0, Math.min(12, missing.size())));
        h.succeed();
    }

    /** {@code PARTS} drives machine casings and rounds (GT6 {@code OP.java:186}, {@code :209}). */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void partsFlagDrivesCasingsAndRounds(GameTestHelper h) {
        int casings = 0;
        int rounds = 0;
        List<String> missingCasing = new ArrayList<>();
        List<String> missingRound = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!registerable(material)) continue;
            if (!MaterialForms.has(material, "PARTS")) continue;
            if (GTBlocks.getStack(BlockMaterialPrefix.casingMachine, material).isEmpty()) {
                missingCasing.add(material.getName());
            } else {
                casings++;
            }
            if (GTItems.getStack(MaterialPrefix.round, material, 1).isEmpty()) {
                missingRound.add(material.getName());
            } else {
                rounds++;
            }
        }
        h.assertTrue(casings > 120, "PARTS materials with a machine casing: " + casings);
        h.assertTrue(missingCasing.isEmpty(), "PARTS materials without a machine casing ("
                + missingCasing.size() + "): " + missingCasing.subList(0, Math.min(12, missingCasing.size())));
        h.assertTrue(rounds > 120, "PARTS materials with rounds: " + rounds);
        h.assertTrue(missingRound.isEmpty(), "PARTS materials without rounds (" + missingRound.size()
                + "): " + missingRound.subList(0, Math.min(12, missingRound.size())));
        h.succeed();
    }

    /** {@code DUSTS} drives the dust form (GT6 {@code OP.java:158}); {@code DIRTY_DUSTS} is dead. */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void dustFlagsDriveDustForms(GameTestHelper h) {
        int dusts = 0;
        int dirtyFlags = 0;
        List<String> missingDust = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!registerable(material)) continue;
            if (MaterialForms.has(material, "DIRTY_DUSTS")) dirtyFlags++;
            if (!MaterialForms.has(material, "DUSTS")) continue;
            if (GTItems.getStack(MaterialPrefix.dust, material, 1).isEmpty()) {
                missingDust.add(material.getName());
            } else {
                dusts++;
            }
        }
        h.assertTrue(dusts > 900, "materials with GT6's DUSTS flag and a dust item: " + dusts);
        h.assertTrue(missingDust.isEmpty(), "DUSTS materials without a dust item (" + missingDust.size()
                + "): " + missingDust.subList(0, Math.min(12, missingDust.size())));
        // GT6's `dustImpure` is conditioned on the unused DIRTY_DUSTS flag (`TD.java:577` declares it,
        // `MT.java` never uses it), so the original registers no impure dusts; the port registers its
        // own (they drive its ore-processing chain) — this pins the flag as dead, not the port's items.
        h.assertTrue(dirtyFlags == 0, "GT6's DIRTY_DUSTS flag is dead in the original, "
                + "but is set for " + dirtyFlags + " materials");
        h.succeed();
    }

    /** The flags GT6 declares but never assigns must stay unassigned (see {@link #DEAD_FLAGS}). */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void deadGt6FlagsStayDead(GameTestHelper h) {
        for (String flag : DEAD_FLAGS) {
            int carriers = 0;
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                if (registerable(material) && MaterialForms.has(material, flag)) carriers++;
            }
            h.assertTrue(carriers == 0, "GT6's " + flag + " flag is dead in MT.java but the flag table "
                    + "sets it for " + carriers + " materials — aligning a condition to it would invent "
                    + "content");
        }
        h.succeed();
    }

    /** Writes the flag/condition coverage report for the batch documentation. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void prefixParityReportIsWritten(GameTestHelper h) {        var json = new java.util.TreeMap<String, Object>();
        var counts = new java.util.TreeMap<String, Integer>();
        for (String flag : MaterialForms.FLAGS) {
            int total = 0;
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                if (registerable(material) && MaterialForms.has(material, flag)) total++;
            }
            counts.put(flag, total);
        }
        json.put("materialsWithFlag", counts);
        json.put("knownGaps", List.of(KNOWN_GAPS));
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/prefix-condition-parity.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            h.fail("cannot write docs/prefix-condition-parity.json: " + e);
            return;
        }
        h.assertTrue(counts.getOrDefault("ORES", 0) > 400, "report counts the ORES materials");
        h.succeed();
    }
}
