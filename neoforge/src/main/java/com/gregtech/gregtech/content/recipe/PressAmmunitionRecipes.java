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
import com.gregtech.gregtech.content.material.Materials;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Original two-stage ammunition: shafts/charged casings are consumed, only actual press shapes are catalysts. */
public final class PressAmmunitionRecipes {
    /** One registered ammunition route, for tests and reports. */
    public record Entry(String route, RecipeMap map, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of();

    /** GT6 Press rows: (input prefix, input count, consumed EMPTY-form prefix, output prefix, ticks). */


    private static final List<PressRow> PRESS_ROWS = PressAmmunitionRecipeRows.PRESS_ROWS;

    /** GT6 Unboxinator rows: arrow heads/bullet metal and the EMPTY shaft/casing are recovered. */


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
        ItemStack component = GTItems.getStack(row.component(), Materials.Empty, 1);
        if (input.isEmpty() || output.isEmpty() || component.isEmpty()) return;
        Recipe recipe = MachineRecipeMaps.Press.addRecipe2(true, 16, row.ticks(), input, component, output);
        if (recipe != null) {
            ENTRIES.add(new Entry(row.input().getName() + " x" + row.inCount() + " -> "
                    + row.output().getName(), MachineRecipeMaps.Press, recipe));
        }
    }

    private static void unbox(GTMaterial material, UnboxRow row) {
        ItemStack input = GTItems.getStack(row.input(), material, 1);
        ItemStack dust = GTItems.getStack(row.output(), material, row.outCount());
        ItemStack component = GTItems.getStack(row.component(), Materials.Empty, 1);
        if (input.isEmpty() || dust.isEmpty()) return;
        // GT6 recovers the shaft/charged casing along with the arrow head or bullet metal.
        if (component.isEmpty()) return;
        Recipe recipe = MachineRecipeMaps.Unboxinator.addRecipe1(true, 16, 16, input, dust, component);
        if (recipe != null) {
            ENTRIES.add(new Entry(row.input().getName() + " -> " + row.output().getName()
                    + " x" + row.outCount(), MachineRecipeMaps.Unboxinator, recipe));
        }
    }

}
