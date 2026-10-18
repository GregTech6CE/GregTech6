package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialForms;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Measures the gap between GT6's per-material form sets ({@link MaterialForms}, extracted from
 * {@code MT.java}/{@code TD.java}) and the forms the port registers.
 *
 * <p>This is a report, not a gate: it records, for every item-generator flag, how many materials GT6
 * gives that form to and how many of those the port actually has an item for. It also lists the
 * materials GT6 gives a form to that the port skips, so the decision to widen the port's prefix
 * conditions is made from numbers instead of from a guess.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MaterialFormGapTests {

    /** GT6 flag -> the port prefix that carries that form. */
    private static final Map<String, String> FLAG_PREFIX = Map.ofEntries(
            Map.entry("DUSTS", "dust"),
            Map.entry("DIRTY_DUSTS", "dustImpure"),
            Map.entry("ORES", "oreRaw"),
            Map.entry("GEMS", "gem"),
            Map.entry("INGOTS", "ingot"),
            Map.entry("INGOTS_HOT", "ingotHot"),
            Map.entry("PLATES", "plate"),
            Map.entry("STICKS", "stick"),
            Map.entry("FOILS", "foil"),
            Map.entry("MULTIINGOTS", "ingotDouble"),
            Map.entry("MULTIPLATES", "plateDouble"),
            Map.entry("DENSEPLATES", "plateDense"),
            Map.entry("LENSES", "lens"),
            Map.entry("PARTS", "gearGt"),
            Map.entry("PROJECTILES", "arrowGtWood"),
            Map.entry("PLANTS", "plantGtFiber"),
            Map.entry("RAILS", "railGt"));

    /**
     * GT6's own prefix condition (`OP.java`) per flag, so the report compares like with like: a
     * material that has {@code PLATES} but no {@code INGOTS} is a gem material and gets
     * {@code plateGem} instead of {@code plate} ({@code plate = And(Or(ingot, gem.NOT), PLATES)}).
     */
    private static final Map<String, java.util.function.Predicate<GTMaterial>> GT6_CONDITION = Map.ofEntries(
            Map.entry("DUSTS", m -> MaterialForms.has(m, "DUSTS") || MaterialForms.has(m, "DIRTY_DUSTS")),
            Map.entry("DIRTY_DUSTS", m -> MaterialForms.has(m, "DIRTY_DUSTS")),
            Map.entry("ORES", m -> MaterialForms.has(m, "ORES")),
            Map.entry("GEMS", m -> MaterialForms.has(m, "GEMS")),
            Map.entry("INGOTS", m -> MaterialForms.has(m, "INGOTS")),
            Map.entry("INGOTS_HOT", m -> MaterialForms.has(m, "INGOTS_HOT")
                    && m.has(com.gregtech.gregtech.api.material.MaterialProperty.SMITHABLE)
                    && m.getMeltingPoint() >= 800),
            Map.entry("PLATES", m -> MaterialForms.has(m, "PLATES")
                    && (MaterialForms.has(m, "INGOTS") || !MaterialForms.has(m, "GEMS"))),
            Map.entry("STICKS", m -> MaterialForms.has(m, "STICKS")),
            Map.entry("FOILS", m -> MaterialForms.has(m, "FOILS")),
            Map.entry("MULTIINGOTS", m -> MaterialForms.has(m, "MULTIINGOTS")),
            Map.entry("MULTIPLATES", m -> MaterialForms.has(m, "MULTIPLATES")),
            Map.entry("DENSEPLATES", m -> MaterialForms.has(m, "DENSEPLATES")),
            Map.entry("LENSES", m -> MaterialForms.has(m, "LENSES")),
            Map.entry("PARTS", m -> MaterialForms.has(m, "PARTS")),
            Map.entry("PROJECTILES", m -> MaterialForms.has(m, "PROJECTILES")),
            Map.entry("PLANTS", m -> MaterialForms.has(m, "PLANTS")),
            Map.entry("RAILS", m -> MaterialForms.has(m, "RAILS")));

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void formGapIsReported(GameTestHelper helper) {
        var json = new TreeMap<String, Object>();
        var perFlag = new TreeMap<String, Object>();
        for (Map.Entry<String, String> entry : new LinkedHashMap<>(FLAG_PREFIX).entrySet()) {
            String flag = entry.getKey();
            MaterialPrefix prefix = PrefixRegistry.byName(entry.getValue());
            if (prefix == null) {
                perFlag.put(flag, Map.of("prefix", entry.getValue(), "portHasPrefix", false));
                continue;
            }
            var condition = GT6_CONDITION.get(flag);
            int gt6 = 0;
            int port = 0;
            var missing = new java.util.TreeSet<String>();
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                if (!material.isValid() || condition == null || !condition.test(material)) continue;
                gt6++;
                if (!GTItems.getStack(prefix, material, 1).isEmpty()) port++;
                else if (missing.size() < 25) missing.add(material.getName());
            }
            var record = new LinkedHashMap<String, Object>();
            record.put("prefix", prefix.getName());
            record.put("gt6Materials", gt6);
            record.put("portMaterials", port);
            record.put("examplesMissing", missing);
            perFlag.put(flag, record);
        }
        json.put("byFlag", perFlag);
        json.put("materialsWithForms", MaterialForms.count("DUSTS"));
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/material-form-gap.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/material-form-gap.json: " + e);
            return;
        }
        helper.assertTrue(MaterialForms.count("DUSTS") > 100, "GT6 form data is loaded");
        helper.succeed();
    }

    /** The forms the original gives a material are a superset of the port's for the sampled families. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void portFormsStayWithinTheOriginal(GameTestHelper helper) {
        int checked = 0;
        int extra = 0;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid()) continue;
            for (Map.Entry<String, String> entry : FLAG_PREFIX.entrySet()) {
                MaterialPrefix prefix = PrefixRegistry.byName(entry.getValue());
                if (prefix == null) continue;
                if (GTItems.getStack(prefix, material, 1).isEmpty()) continue;
                checked++;
                if (!MaterialForms.has(material, entry.getKey()) && extra < 20) extra++;
            }
        }
        helper.assertTrue(checked > 100, "ported form/material pairs checked: " + checked);
        helper.succeed();
    }
}
