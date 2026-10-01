package com.gregtech.gregtech.content.recipe;


import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's Anvil shredding rows: {@code Loader_Recipes_Handlers:157-172}
 * ({@code RecipeMapHandlerPrefixShredding} in {@code RM.Anvil}).
 * <p>
 * These are the hand-grinding rows of the ore chain: an anvil (hammer) turns rock and ore chunks into
 * dust and dust piles before any machine exists. Rows GT6 registers for prefixes the port does not have
 * ({@code chunk}, {@code rubble}, {@code pebbles}, {@code clump}, {@code reduced}, {@code crystalline},
 * {@code cleanGravel}, {@code cluster}) stay recorded in {@link #skipped()}.
 * <p>
 * The {@code selfcrush()} condition is GT6's
 * {@code aMaterial.mTargetCrushing.mMaterial == aMaterial} ({@code OreDictMaterialCondition.java:47-50}),
 * modelled here with {@link GTMaterial#getTargetCrushingMaterial()}; {@code MORTAR} comes from the
 * workability import. GT6's {@code ST.emptySlot()} second input (the anvil slot has to stay empty) is
 * expressed as an empty stack in the second input slot, the idiom the port's other anvil rows use.
 * Rows whose recipe already exists in the map are dropped by the collision check — the {@code rockGt}
 * row, for instance, is registered by {@code StoneAndToolSurvivalRecipes}, which ports the same original
 * row ({@code Loader_Recipes_Handlers:157}).
 */
public final class AnvilShreddingRecipes {
    /** One registered anvil grinding row, for tests and reports. */
    public record Entry(String route, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of(
            "chunk / rubble / pebbles / clump / reduced / crystalline / cleanGravel / cluster rows"
                    + " (:158-165): the port has no such prefixes");

    /** GT6 row; {@code selfCrush} mirrors GT6's selfcrush() condition, {@code mortar} its MORTAR flag. */
    private record Row(MaterialPrefix input, MaterialPrefix output, int outCount,
                       MaterialPrefix byproduct, int byproductCount, boolean selfCrush, boolean mortar) {}

    private static final List<Row> ROWS = List.of(
            new Row(MaterialPrefix.rockGt, MaterialPrefix.dustSmall, 9, null, 0, false, false),
            new Row(MaterialPrefix.oreRaw, MaterialPrefix.crushed, 1, MaterialPrefix.crushedTiny, 6, true, true),
            new Row(MaterialPrefix.crushed, MaterialPrefix.dust, 1, MaterialPrefix.dustDiv72, 9, false, true),
            new Row(MaterialPrefix.crushedPurified, MaterialPrefix.dust, 1, MaterialPrefix.dustDiv72, 18, false, true),
            new Row(MaterialPrefix.crushedCentrifuged, MaterialPrefix.dust, 1, MaterialPrefix.dustDiv72, 27, false, true),
            new Row(MaterialPrefix.crushedTiny, MaterialPrefix.dustDiv72, 9, null, 0, false, true),
            new Row(MaterialPrefix.crushedPurifiedTiny, MaterialPrefix.dustDiv72, 10, null, 0, false, true),
            new Row(MaterialPrefix.crushedCentrifugedTiny, MaterialPrefix.dustDiv72, 11, null, 0, false, true));

    private static boolean registered;

    private AnvilShreddingRecipes() {}

    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return SKIPPED; }

    public static int register() {
        if (registered) throw new IllegalStateException("Anvil shredding recipes registered twice");
        registered = true;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER)) continue;
            for (Row row : ROWS) grind(material, row);
        }
        com.mojang.logging.LogUtils.getLogger().info("Registered {} GT6 anvil shredding recipes ({} notes: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    private static void grind(GTMaterial material, Row row) {
        if (row.mortar() && !MaterialWorkability.isMortarGrindable(material)) return;
        if (row.selfCrush() && material.getTargetCrushingMaterial() != material) return;
        ItemStack input = GTItems.getStack(row.input(), material, 1);
        ItemStack output = GTItems.getStack(row.output(), material, row.outCount());
        ItemStack byproduct = row.byproduct() == null ? ItemStack.EMPTY
                : GTItems.getStack(row.byproduct(), material, row.byproductCount());
        if (input.isEmpty() || output.isEmpty()) return;
        if (row.byproduct() != null && byproduct.isEmpty()) return;
        long units = Math.max(row.input().getMaterialWeight(),
                row.output().getMaterialWeight() * row.outCount()
                        + (row.byproduct() == null ? 0 : row.byproduct().getMaterialWeight() * row.byproductCount()));
        // GT6 Row 16 EU/t with multiplier 16 on the shredding handler.
        long duration = Math.max(1, (units * 16L * (material.getToolQuality() + 1) + GTValues.U - 1) / GTValues.U);
        // GT6's ST.emptySlot() second input (the second anvil slot has to stay empty). RecipeMap#make
        // rejects an empty stack ("declared input failed to resolve"), so the recipe is built directly
        // the way AnvilRecipeDefinitions does it — the anvil's 2-item minimum counts slots, not entries.
        Recipe recipe = new Recipe(new ItemStack[]{input, ItemStack.EMPTY},
                byproduct.isEmpty() ? new ItemStack[]{output} : new ItemStack[]{output, byproduct},
                null, null, null, null, duration, 16, 0);
        if (MachineRecipeMaps.Anvil.addRecipe(recipe) != null) {
            ENTRIES.add(new Entry(row.input().getName() + " -> " + row.output().getName() + " x" + row.outCount()
                    + (byproduct.isEmpty() ? "" : " + " + row.byproduct().getName() + " x" + row.byproductCount()),
                    recipe));
        }
    }
}
