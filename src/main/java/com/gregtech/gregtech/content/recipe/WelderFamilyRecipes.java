package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's remaining Welder rows: {@code Loader_Recipes_Handlers:316-360}.
 * <p>
 * Beyond the machine casings (which {@link MachineCasingRecipes} owns), GT6 welds
 * </p>
 * <ul>
 *   <li>{@code ingot 2/3/4/5/9 + ST.tag(n) -> ingotDouble/Triple/Quadruple/Quintuple/blockSolid}
 *       ({@code :320-323}),</li>
 *   <li>{@code bolt 4 + tag4 -> stick}, {@code bolt 8 + tag8 -> stickLong},
 *       {@code stick 2 + tag2 -> stickLong} ({@code :330-332}),</li>
 *   <li>{@code plateCurved 4 + ring 1 -> rotor} ({@code :355}/{@code :359}).</li>
 * </ul>
 * <p>
 * Every row exists twice in GT6: an easy-heatable pass with a fixed duration (16 ticks per material
 * unit) and a hard pass with the quality-scaled multiplier 64. GT6 keys that split on
 * {@code tEasyHeatable = Or(FURNACE)} — the port imports {@code FURNACE} through
 * {@code tools/extract_gt6_workability.py}, so both passes are registered for the materials that get
 * them in the original. The common conditions {@code SMITHABLE} / {@code FLAMMABLE.NOT} are material
 * properties in the port; GT6's {@code COATED.NOT} has no port counterpart and is noted in
 * {@link #skipped()}.
 */
public final class WelderFamilyRecipes {
    /** One registered welding row, for tests and reports. */
    public record Entry(String route, RecipeMap map, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of(
            "plateCurved 1/3/6/12 -> pipeTiny/Small/Medium/Large/Huge (:324-328, :339-343):"
                    + " the port has no per-size pipe forms (registry/GTFluidPipes only models medium pipes)",
            "casingSmall 2 -> plate 1 (:329): the port has no MaterialPrefix.casingSmall",
            "GT6's COATED.NOT condition has no port counterpart; SMITHABLE and FLAMMABLE.NOT are applied");

    /** One GT6 row: input prefix, input count, optional second input, circuit tag, output. */
    private record Row(MaterialPrefix input, int inCount, MaterialPrefix second, int secondCount,
                       int circuit, MaterialPrefix output,
                       com.gregtech.gregtech.api.prefix.BlockMaterialPrefix blockOutput) {
        static Row of(MaterialPrefix input, int inCount, int circuit, MaterialPrefix output) {
            return new Row(input, inCount, null, 0, circuit, output, null);
        }
        static Row of(MaterialPrefix input, int inCount, MaterialPrefix second, int secondCount, int circuit,
                      MaterialPrefix output) {
            return new Row(input, inCount, second, secondCount, circuit, output, null);
        }
        static Row block(MaterialPrefix input, int inCount, int circuit,
                         com.gregtech.gregtech.api.prefix.BlockMaterialPrefix output) {
            return new Row(input, inCount, null, 0, circuit, null, output);
        }
        long inUnits() {
            return input.getMaterialWeight() * inCount
                    + (second == null ? 0 : second.getMaterialWeight() * secondCount);
        }
        long outUnits() {
            return output != null ? output.getMaterialWeight() : blockOutput.getMaterialWeight();
        }
    }

    private static final List<Row> ROWS = List.of(
            Row.of(MaterialPrefix.ingot, 2, 2, MaterialPrefix.ingotDouble),
            Row.of(MaterialPrefix.ingot, 3, 3, MaterialPrefix.ingotTriple),
            Row.of(MaterialPrefix.ingot, 4, 4, MaterialPrefix.ingotQuadruple),
            Row.of(MaterialPrefix.ingot, 5, 5, MaterialPrefix.ingotQuintuple),
            Row.block(MaterialPrefix.ingot, 9, 9, BlockMaterialPrefix.blockSolid),
            Row.of(MaterialPrefix.bolt, 4, 4, MaterialPrefix.stick),
            Row.of(MaterialPrefix.bolt, 8, 8, MaterialPrefix.stickLong),
            Row.of(MaterialPrefix.stick, 2, 2, MaterialPrefix.stickLong),
            Row.of(MaterialPrefix.plateCurved, 4, MaterialPrefix.ring, 1, 0, MaterialPrefix.rotor));

    private static boolean registered;

    private WelderFamilyRecipes() {}

    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return SKIPPED; }

    public static int register() {
        if (registered) throw new IllegalStateException("Welder family registered twice");
        registered = true;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER) || material.has(MaterialProperty.FLAMMABLE)) continue;
            if (!material.has(MaterialProperty.SMITHABLE)) continue;
            for (Row row : ROWS) weld(material, row);
        }
        com.gregtech.gregtech.GregTech.LOGGER.info(
                "Registered {} GT6 welder rows ({} GT6 rows skipped: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    private static void weld(GTMaterial material, Row row) {
        ItemStack input = GTItems.getStack(row.input(), material, row.inCount());
        ItemStack second = row.second() == null ? ItemStack.EMPTY
                : GTItems.getStack(row.second(), material, row.secondCount());
        ItemStack output = row.output() != null ? GTItems.getStack(row.output(), material, 1)
                : GTBlocks.getStack(row.blockOutput(), material);
        if (input.isEmpty() || output.isEmpty()) return;
        if (row.second() != null && second.isEmpty()) return;

        boolean easyHeatable = MaterialWorkability.isFurnace(material);   // GT6 tEasyHeatable = Or(FURNACE)
        long unitsIn = row.inUnits();
        long units = Math.max(unitsIn, row.outUnits());
        // Easy pass: GT6 passes the duration explicitly (16 ticks per material unit, :335-348).
        // Hard pass: RecipeMapHandlerPrefix#getCosts with multiplier 64 (:320-332).
        long easyTicks = 16L * unitsIn / com.gregtech.gregtech.api.material.GTValues.U;
        long hardTicks = Math.max(1, (units * 64L * (material.getToolQuality() + 1)
                + com.gregtech.gregtech.api.material.GTValues.U - 1) / com.gregtech.gregtech.api.material.GTValues.U);
        long ticks = easyHeatable ? Math.max(1, easyTicks) : hardTicks;

        ItemStack tag = row.circuit() <= 0 ? ItemStack.EMPTY
                : new ItemStack(GTTechnological.selectorTag(row.circuit()));
        Recipe recipe = tag.isEmpty()
                ? (second.isEmpty()
                    ? MachineRecipeMaps.Welder.addRecipe1(true, 16, ticks, input, output)
                    : MachineRecipeMaps.Welder.addRecipe2(true, 16, ticks, input, second, output))
                : (second.isEmpty()
                    ? MachineRecipeMaps.Welder.addRecipe2(true, 16, ticks, input, tag, output)
                    : MachineRecipeMaps.Welder.addRecipeX(true, 16, ticks, new ItemStack[]{input, second, tag}, output));
        if (recipe != null) {
            ENTRIES.add(new Entry(row.input().getName() + " x" + row.inCount()
                    + (row.second() == null ? "" : " + " + row.second().getName() + " x" + row.secondCount())
                    + " -> " + (row.output() != null ? row.output().getName() : row.blockOutput().getName())
                    + (easyHeatable ? " (easy)" : " (hard)"), MachineRecipeMaps.Welder, recipe));
        }
    }
}
