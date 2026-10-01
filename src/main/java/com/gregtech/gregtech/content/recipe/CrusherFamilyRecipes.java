package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's Crusher handler rows: {@code Loader_Recipes_Handlers:146-157}.
 * <p>
 * The gem ladder ({@code gemLegendary → 2 gemExquisite → 2 gemFlawless → 2 gem → 2 gemFlawed →
 * 2 gemChipped → pulverized remains}), the {@code bouleGt → 4 gem} row and the rock row. Every row runs
 * at 16 EU/t with GT6's multiplier (256 for the ladder, 16 for rock). Rows for the port's missing
 * prefixes ({@code rawOreChunk}, {@code chunk}, {@code rubble}, {@code pebbles}) stay recorded in
 * {@link #skipped()}.
 */
public final class CrusherFamilyRecipes {
    /** One registered crushing row, for tests and reports. */
    public record Entry(String route, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of(
            "rawOreChunk 1 -> crushedTiny 3 (:147), chunk 1 -> rubble 1 (:148), rubble 1 -> pebbles 1 (:149),"
                    + " pebbles 1 -> dust 3 in the Sifter (:146): the port has no such prefixes",
            "RecipeMapHandlerCrushing() (:157) is the generic crushing handler, already covered by"
                    + " Loader_Recipes_OreProcessing#pulverizeMulti");

    /** GT6 row: input and output pair with the row's material multiplier. */
    private record Row(MaterialPrefix input, MaterialPrefix output, int outCount, long multiplier) {}

    private static final List<Row> ROWS = List.of(
            new Row(MaterialPrefix.gemLegendary, MaterialPrefix.gemExquisite, 2, 256),
            new Row(MaterialPrefix.gemExquisite, MaterialPrefix.gemFlawless, 2, 256),
            new Row(MaterialPrefix.gemFlawless, MaterialPrefix.gem, 2, 256),
            new Row(MaterialPrefix.gem, MaterialPrefix.gemFlawed, 2, 256),
            new Row(MaterialPrefix.gemFlawed, MaterialPrefix.gemChipped, 2, 256),
            new Row(MaterialPrefix.bouleGt, MaterialPrefix.gem, 4, 256),
            new Row(MaterialPrefix.gemChipped, null, 0, 256),
            new Row(MaterialPrefix.rockGt, null, 0, 16));

    private static boolean registered;

    private CrusherFamilyRecipes() {}

    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return SKIPPED; }

    public static int register() {
        if (registered) throw new IllegalStateException("Crusher family registered twice");
        registered = true;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER)) continue;
            for (Row row : ROWS) crush(material, row);
        }
        GregTech.LOGGER.info("Registered {} GT6 crusher rows ({} notes: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    private static void crush(GTMaterial material, Row row) {
        ItemStack input = GTItems.getStack(row.input(), material, 1);
        if (input.isEmpty()) return;
        ItemStack output = row.output() != null ? GTItems.getStack(row.output(), material, row.outCount())
                : MortarGrindingRecipes.pulverize(material, row.input().getMaterialWeight());
        if (output.isEmpty()) return;
        long units = Math.max(row.input().getMaterialWeight(),
                row.output() == null ? 0 : row.output().getMaterialWeight() * row.outCount());
        long duration = Math.max(1, (units * row.multiplier() * (material.getToolQuality() + 1)
                + GTValues.U - 1) / GTValues.U);
        Recipe recipe = MachineRecipeMaps.Crusher.addRecipe1(true, 16, duration, input, output);
        if (recipe != null) {
            ENTRIES.add(new Entry(row.input().getName()
                    + " -> " + (row.output() == null ? "remains" : row.output().getName() + " x" + row.outCount()),
                    recipe));
        }
    }
}
