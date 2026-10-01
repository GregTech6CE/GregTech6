package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.data.generated.RecyclablePrefixes;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * GT6's generic shredder (recycling) rows: {@code Loader_Recipes_Handlers:152-155}.
 * <p>
 * GT6 loops over every prefix that is {@code RECYCLABLE}, declares no byproducts and is neither an ore,
 * ore-processing or dust-based form, a container, a cable, a wire or a pipe, and registers
 * "<em>1 item → its pulverized remains</em>" in the Shredder. The loop runs twice: the multiplier is 256
 * for materials that are not {@code MORTAR} grindable and 16 for the ones that are, so brittle materials
 * recycle cheaply. The port imports the prefix filter result through
 * {@code tools/extract_gt6_recyclable_prefixes.py} ({@link RecyclablePrefixes}, 107 names) and the
 * {@code MORTAR} flag through {@code tools/extract_gt6_workability.py}.
 * <p>
 * This is GT6's "recycle anything into dust" mechanic: a broken tool head, a spare gear or an old casing
 * either comes back as material or the player has to store it forever.
 */
public final class ShredderRecyclingRecipes {
    /** One registered recycling route, for tests and reports. */
    public record Entry(String input, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();

    private static boolean registered;

    private ShredderRecyclingRecipes() {}

    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return Collections.unmodifiableList(SKIPPED); }

    public static int register() {
        if (registered) throw new IllegalStateException("Shredder recycling recipes registered twice");
        registered = true;
        TreeSet<String> missing = new TreeSet<>();
        for (String name : RecyclablePrefixes.NAMES) {
            MaterialPrefix prefix = PrefixRegistry.byName(name);
            if (prefix == null) {
                missing.add(name);
                continue;
            }
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                if (material.has(MaterialProperty.ANTIMATTER)) continue;   // GT6: ANTIMATTER.NOT
                recycle(material, prefix);
            }
        }
        if (!missing.isEmpty()) SKIPPED.add("prefixes the port does not have: " + missing);
        GregTech.LOGGER.info("Registered {} GT6 shredder recycling recipes ({} prefixes missing: {})",
                ENTRIES.size(), missing.size(), missing);
        return ENTRIES.size();
    }

    private static void recycle(GTMaterial material, MaterialPrefix prefix) {
        ItemStack input = GTItems.getStack(prefix, material, 1);
        if (input.isEmpty()) return;
        long units = prefix.getMaterialWeight();
        ItemStack output = MortarGrindingRecipes.pulverize(material, units);
        if (output.isEmpty()) return;                                       // GT6's no-dust family is a no-op
        // Two GT6 passes: multiplier 256 for hard materials, 16 for MORTAR grindable ones.
        long multiplier = MaterialWorkability.isMortarGrindable(material) ? 16 : 256;
        long duration = Math.max(1, (units * multiplier * (material.getToolQuality() + 1)
                + GTValues.U - 1) / GTValues.U);
        Recipe recipe = MachineRecipeMaps.Shredder.addRecipe1(true, 16, duration, input, output);
        if (recipe != null) {
            ENTRIES.add(new Entry(prefix.getName(), recipe));
        }
    }
}
