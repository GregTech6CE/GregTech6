package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.RecyclablePrefixes;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.world.item.ItemStack;

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

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<String> skipped() {
        return Collections.unmodifiableList(SKIPPED);
    }

    public static int register() {
        if (registered) throw new IllegalStateException("Shredder recycling recipes registered twice");
        registered = true;
        TreeSet<String> missing = new TreeSet<>();
        for (String name : RecyclablePrefixes.NAMES) {
            MaterialPrefix prefix = PrefixRegistry.byName(name);
            if (prefix == null) {
                var blockPrefix = java.util.Optional.ofNullable(
                                gregtech.content.data.MaterialPrefixes.all().get(name))
                        .map(gregtech.content.data.MaterialPrefixes.Entry::getBlockDelegate)
                        .orElse(null);
                if (blockPrefix == null) missing.add(name);
                else
                    for (var material : GTMaterialRegistry.allMaterials()) {
                        if (material.has(MaterialProperty.ANTIMATTER)) continue;
                        var input = gregtech.content.registry.GTBlocks.getStack(blockPrefix, material);
                        recycle(material, name, blockPrefix.getMaterialWeight(), input);
                    }
                continue;
            }
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                if (material.has(MaterialProperty.ANTIMATTER)) continue; // GT6: ANTIMATTER.NOT
                recycle(material, prefix);
            }
        }
        for (MaterialPrefix prefix : PrefixRegistry.addonPrefixes()) {
            // The source listener only walks RECYCLABLE prefixes; an addon's own prefix gets a
            // row only when it resolves to one of those source names.
            if (!RecyclablePrefixes.NAMES.contains(PrefixRegistry.sourceName(prefix.getName()))) continue;
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                if (material.has(MaterialProperty.ANTIMATTER) || !prefix.isValidFor(material)) continue;
                recycle(material, prefix);
            }
        }
        if (!missing.isEmpty()) SKIPPED.add("prefixes the port does not have: " + missing);
        Mc.logger()
                .info(
                        "Registered {} GT6 shredder recycling recipes ({} prefixes missing: {})",
                        ENTRIES.size(),
                        missing.size(),
                        missing);
        return ENTRIES.size();
    }

    private static void recycle(GTMaterial material, MaterialPrefix prefix) {
        ItemStack input = GTItems.getStack(prefix, material, 1);
        recycle(material, prefix.getName(), prefix.getMaterialWeight(), input);
    }

    private static void recycle(GTMaterial material, String name, long units, ItemStack input) {
        if (input.isEmpty()) return;
        ItemStack output = MortarGrindingRecipes.pulverize(material, units);
        if (output.isEmpty()) return; // GT6's no-dust family is a no-op
        // Two GT6 passes: multiplier 256 for hard materials, 16 for MORTAR grindable ones.
        long multiplier = gregtech.content.material.MaterialProcessingProperties.isMortarGrindable(material) ? 16 : 256;
        long duration =
                Math.max(1, (units * multiplier * (material.getToolQuality() + 1) + GTValues.U - 1) / GTValues.U);
        var template = RecipeMap.make(
                true, new ItemStack[] {input}, new ItemStack[] {output}, null, null, null, null, duration, 16, 0);
        if (template == null) return;
        var rule = new gregtech.engine.recipe.MaterialUnitRecipe(
                template, 0, material, units, gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding.WHOLE_OUTPUTS);
        rule.source(input, units);
        ENTRIES.add(new Entry(name, rule.publish(MachineRecipeMaps.Shredder)));
    }
}
