package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's Sharpening (grindstone) handler block: {@code Loader_Recipes_Handlers:113-145}.
 * <p>
 * Twenty rows in two groups:
 * </p>
 * <ul>
 *   <li>Thirteen "<em>raw tool head → finished tool head</em>" rows ({@code :128-140}), EU 16 with the
 *       quality-scaled multiplier 16.</li>
 *   <li>Seven grinding rows ({@code :113-131}): {@code nugget → round}, {@code plateGem → lens},
 *       {@code gem/ingot/billet → stick}, {@code gemChipped → 2 arrow heads},
 *       {@code rockGt → 8 arrow heads}, EU 16 with multiplier 256. Where the input holds more material
 *       than the output, GT6 adds the pulverized remainder as a second output
 *       ({@code aOutputPulverizedRemains}).</li>
 * </ul>
 * <p>
 * GT6's extra {@code COATED.NOT} condition has no port counterpart; {@code ANTIMATTER.NOT} is applied.
 * </p>
 */
public final class SharpeningRecipes {
    /** One registered sharpening row, for tests and reports. */
    public record Entry(String route, RecipeMap map, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of(
            "GT6's COATED.NOT condition on the grinding rows (:113-127) has no port counterpart;"
                    + " ANTIMATTER.NOT is applied");

    /** Raw head → finished head (GT6 :128-140), EU 16 with multiplier 16. */
    private static final List<String[]> TOOL_HEAD_ROWS = List.of(
            new String[]{"toolHeadRawArrow", "toolHeadArrow"},
            new String[]{"toolHeadRawSaw", "toolHeadSaw"},
            new String[]{"toolHeadRawChisel", "toolHeadChisel"},
            new String[]{"toolHeadRawSword", "toolHeadSword"},
            new String[]{"toolHeadRawPickaxe", "toolHeadPickaxe"},
            new String[]{"toolHeadRawShovel", "toolHeadShovel"},
            new String[]{"toolHeadRawSpade", "toolHeadSpade"},
            new String[]{"toolHeadRawUniversalSpade", "toolHeadUniversalSpade"},
            new String[]{"toolHeadRawAxe", "toolHeadAxe"},
            new String[]{"toolHeadRawAxeDouble", "toolHeadAxeDouble"},
            new String[]{"toolHeadRawHoe", "toolHeadHoe"},
            new String[]{"toolHeadRawSense", "toolHeadSense"},
            new String[]{"toolHeadRawPlow", "toolHeadPlow"});

    /** Grinding rows (GT6 :113-127), EU 16 with multiplier 256; the 4th field marks pulverized remains. */
    private static final List<String[]> GRINDING_ROWS = List.of(
            new String[]{"nugget", "round", "1", "F"},
            new String[]{"plateGem", "lens", "1", "T"},
            new String[]{"gem", "stick", "1", "T"},
            new String[]{"ingot", "stick", "1", "T"},
            new String[]{"billet", "stick", "1", "T"},
            new String[]{"gemChipped", "toolHeadArrow", "2", "F"},
            new String[]{"rockGt", "toolHeadArrow", "8", "T"});

    private static boolean registered;

    private SharpeningRecipes() {}

    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return SKIPPED; }

    public static int register() {
        if (registered) throw new IllegalStateException("Sharpening recipes registered twice");
        registered = true;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER)) continue;
            for (String[] row : TOOL_HEAD_ROWS) sharpen(material, row[0], 1, row[1], 1, 16, false);
            for (String[] row : GRINDING_ROWS) {
                sharpen(material, row[0], 1, row[1], Integer.parseInt(row[2]), 256, "T".equals(row[3]));
            }
        }
        GregTech.LOGGER.info("Registered {} GT6 sharpening recipes ({} notes: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    private static void sharpen(GTMaterial material, String inName, int inAmount, String outName, int outCount,
                                long multiplier, boolean pulverizedRemains) {
        MaterialPrefix in = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(inName);
        MaterialPrefix out = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(outName);
        if (in == null || out == null) return;
        ItemStack input = GTItems.getStack(in, material, inAmount);
        ItemStack output = GTItems.getStack(out, material, outCount);
        if (input.isEmpty() || output.isEmpty()) return;
        long unitsIn = in.getMaterialWeight() * inAmount;
        long unitsOut = out.getMaterialWeight() * outCount;
        long units = Math.max(unitsIn, unitsOut);
        // GT6 RecipeMapHandlerPrefix#getCosts with the row's multiplier (16 for tool heads, 256 for grinding).
        long duration = Math.max(1, (units * multiplier * (material.getToolQuality() + 1)
                + GTValues.U - 1) / GTValues.U);
        ItemStack remains = pulverizedRemains && unitsIn > unitsOut
                ? MortarGrindingRecipes.pulverize(material, unitsIn - unitsOut) : ItemStack.EMPTY;
        Recipe recipe = remains.isEmpty()
                ? MachineRecipeMaps.Sharpening.addRecipe1(true, 16, duration, input, output)
                : MachineRecipeMaps.Sharpening.addRecipe1(true, 16, duration, input, output, remains);
        if (recipe != null) {
            ENTRIES.add(new Entry(inName + " x" + inAmount + " -> " + outName + " x" + outCount
                    + (remains.isEmpty() ? "" : " + remains"), MachineRecipeMaps.Sharpening, recipe));
        }
    }
}
