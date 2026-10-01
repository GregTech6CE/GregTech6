package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.content.recipe.PressAmmunitionRecipeRows.PressRow;
import com.gregtech.gregtech.content.recipe.PressAmmunitionRecipeRows.UnboxRow;


import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's ammunition rows: the Press handler block ({@code Loader_Recipes_Handlers:254-258}) and the
 * ammunition recovery rows of the Unboxinator ({@code :454-456}).
 * <p>
 * GT6 expresses the bullet molds as {@code OP.bulletGtSmall.mat(MT.Empty, 1)} of the empty material
 * (the "empty" forms of a prefix are the molds); the port has real mold items instead
 * ({@code press_bullet_casing_shape_small/medium/large}, see {@code GTTechnological}), which is what
 * the original models as well. Rows whose catalyst or extra form the port cannot express stay recorded
 * in {@link #skipped()} rather than being guessed:
 * </p>
 * <ul>
 *   <li>{@code toolHeadArrow 1 -> arrowGtWood / arrowGtPlastic 1} needs the arrow templates
 *       ({@code OP.arrowGtWood.mat(MT.Empty, 1)}), which the port has no item for yet.</li>
 *   <li>{@code gemFlawed 2 + toolHeadRawPickaxe -> toolHeadPickaxeGem 1} is gated on GT6's
 *       {@code ANY.Iron.mToThis} group ("Any Iron Or Steel"), which the port has no equivalent of.</li>
 * </ul>
 */
public final class PressAmmunitionRecipes {
    /** One registered ammunition route, for tests and reports. */
    public record Entry(String route, RecipeMap map, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of(
            "toolHeadArrow 1 -> arrowGtWood / arrowGtPlastic 1 (Loader_Recipes_Handlers:252-253):"
                    + " the port has no arrow template items (OP.arrowGt*.mat(MT.Empty, 1))",
            "gemFlawed 2 + toolHeadRawPickaxe -> toolHeadPickaxeGem 1 (:250-251):"
                    + " gated on GT6 ANY.Iron.mToThis, which the port does not model");

    /** GT6 Press rows: (input prefix, input count, mold registry id, output prefix, ticks). */


    private static final List<PressRow> PRESS_ROWS = PressAmmunitionRecipeRows.PRESS_ROWS;

    /** GT6 Unboxinator rows: bullets come back as tiny dust piles, the mold is recovered. */


    private static final List<UnboxRow> UNBOX_ROWS = PressAmmunitionRecipeRows.UNBOX_ROWS;

    private static boolean registered;

    private PressAmmunitionRecipes() {}

    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return SKIPPED; }

    public static int register() {
        if (registered) throw new IllegalStateException("Ammunition recipes registered twice");
        registered = true;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER)) continue;   // GT6: ANTIMATTER.NOT on every row
            if (material.getName().equals("Empty")) continue;          // GT6: MT.Empty.NOT
            for (PressRow row : PRESS_ROWS) press(material, row);
            for (UnboxRow row : UNBOX_ROWS) unbox(material, row);
        }
        com.mojang.logging.LogUtils.getLogger().info("Registered {} GT6 ammunition recipes ({} GT6 rows skipped: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    private static void press(GTMaterial material, PressRow row) {
        ItemStack input = GTItems.getStack(row.input(), material, row.inCount());
        ItemStack output = GTItems.getStack(row.output(), material, 1);
        ItemStack mold = tech(row.moldId());
        if (input.isEmpty() || output.isEmpty() || mold.isEmpty()) return;
        Recipe recipe = MachineRecipeMaps.Press.addRecipe2(true, 16, row.ticks(), input, mold, output);
        if (recipe != null) {
            ENTRIES.add(new Entry(row.input().getName() + " x" + row.inCount() + " -> "
                    + row.output().getName(), MachineRecipeMaps.Press, recipe));
        }
    }

    private static void unbox(GTMaterial material, UnboxRow row) {
        ItemStack input = GTItems.getStack(row.input(), material, 1);
        ItemStack dust = GTItems.getStack(row.output(), material, row.outCount());
        ItemStack mold = tech(row.moldId());
        if (input.isEmpty() || dust.isEmpty()) return;
        // GT6 returns the mold as the additional output; when the map cannot hold two outputs the
        // recovery row is registered with the dust alone.
        Recipe recipe = mold.isEmpty()
                ? MachineRecipeMaps.Unboxinator.addRecipe1(true, 16, 16, input, dust)
                : MachineRecipeMaps.Unboxinator.addRecipe1(true, 16, 16, input, dust, mold);
        if (recipe == null && !mold.isEmpty()) {
            recipe = MachineRecipeMaps.Unboxinator.addRecipe1(true, 16, 16, input, dust);
        }
        if (recipe != null) {
            ENTRIES.add(new Entry(row.input().getName() + " -> " + row.output().getName()
                    + " x" + row.outCount(), MachineRecipeMaps.Unboxinator, recipe));
        }
    }

    private static ItemStack tech(String id) {
        net.minecraft.world.item.Item item = GTTechnological.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
