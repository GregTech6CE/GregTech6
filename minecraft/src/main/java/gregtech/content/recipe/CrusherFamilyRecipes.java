package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.recipe.CrusherFamilyRecipeRows.Row;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * GT6's Crusher handler rows: {@code Loader_Recipes_Handlers:64,69-77}.
 * <p>
 * The gem ladder ({@code gemLegendary → 2 gemExquisite → 2 gemFlawless → 2 gem → 2 gemFlawed →
 * 2 gemChipped → pulverized remains}), the {@code bouleGt → 4 gem} row and the rock row. Every row runs
 * at 16 EU/t with GT6's multiplier (256 for the ladder, 16 for rock). Optional source ore forms
 * ({@code rawOreChunk}, {@code chunk}, {@code rubble}, {@code pebbles}) are handled after tag binding
 * by {@link ExternalOreProcessing}; they have no GT-owned items.
 */
public final class CrusherFamilyRecipes {
    /** One registered crushing row, for tests and reports. */
    public record Entry(String route, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of(
            "Optional rawOreChunk/chunk/rubble/pebbles routes (:62/:65-67) bind actual external items after tag loading",
            "RecipeMapHandlerCrushing() (:77) is the generic crushing handler, already covered by"
                    + " Loader_Recipes_OreProcessing#pulverizeMulti");

    /** GT6 row: input and output pair with the row's material multiplier. */
    private static final List<Row> ROWS = CrusherFamilyRecipeRows.ROWS;

    private static boolean registered;

    private CrusherFamilyRecipes() {}

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<String> skipped() {
        return SKIPPED;
    }

    public static int register() {
        if (registered) throw new IllegalStateException("Crusher family registered twice");
        registered = true;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER)) continue;
            for (Row row : ROWS) crush(material, row);
        }
        Mc.logger().info("Registered {} GT6 crusher rows ({} notes: {})", ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    private static void crush(GTMaterial material, Row row) {
        ItemStack input = GTItems.getStack(row.input(), material, 1);
        if (input.isEmpty()) return;
        ItemStack output = row.output() != null
                ? GTItems.getStack(row.output(), material, row.outCount())
                : MortarGrindingRecipes.pulverize(material, row.input().getMaterialWeight());
        if (output.isEmpty()) return;
        long units = Math.max(
                row.input().getMaterialWeight(),
                row.output() == null ? 0 : row.output().getMaterialWeight() * row.outCount());
        long duration =
                Math.max(1, (units * row.multiplier() * (material.getToolQuality() + 1) + GTValues.U - 1) / GTValues.U);
        // One rule per (material, input form): every equivalent item of that form is admitted,
        // and the batch is derived from the form's material units instead of a fixed count.
        var template = RecipeMap.make(
                true, new ItemStack[] {input}, new ItemStack[] {output}, null, null, null, null, duration, 16, 0);
        if (template == null) return;
        var rule = new gregtech.engine.recipe.MaterialUnitRecipe(
                template,
                0,
                material,
                row.input().getMaterialWeight(),
                gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding.WHOLE_OUTPUTS);
        rule.source(input, row.input().getMaterialWeight());
        Recipe recipe = rule.publish(MachineRecipeMaps.Crusher);
        ENTRIES.add(new Entry(
                row.input().getName() + " -> "
                        + (row.output() == null ? "remains" : row.output().getName() + " x" + row.outCount()),
                recipe));
    }
}
