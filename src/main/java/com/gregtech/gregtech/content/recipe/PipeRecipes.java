package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.registry.GTFluidPipes;
import com.gregtech.gregtech.registry.GTItemPipes;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6's Welder rows that turn curved plates into pipes: {@code Loader_Recipes_Handlers:324-328} (hard
 * materials) and {@code :339-343} (easy-heatable ones).
 *
 * <pre>
 * plateCurved  1 + circuit tag 1 -> pipeTiny   2
 * plateCurved  1 + circuit tag 2 -> pipeSmall  1
 * plateCurved  3 + circuit tag 3 -> pipeMedium 1
 * plateCurved  6 + circuit tag 4 -> pipeLarge  1
 * plateCurved 12 + circuit tag 5 -> pipeHuge   1
 * </pre>
 *
 * <p>GT6 registers one <em>prefix</em> per pipe size and maps it onto a block item per material
 * ({@code OreDictManager.setTarget_(OP.pipeMedium, aMat, …)}), which is why the same row yields a
 * fluid pipe for steel but an item pipe for brass. The port has no ore-dictionary target layer, so the
 * mapping is done here: a material with an item pipe of that size gets the item pipe, every other
 * material gets the fluid pipe.</p>
 *
 * <p>The port previously skipped these rows because it assumed it had no per-size pipe forms; the
 * blocks ({@code GTFluidPipes}/{@code GTItemPipes}) have existed all along, which left every pipe in
 * the mod unobtainable outside of creative mode.</p>
 */
public final class PipeRecipes {
    private static final List<PipeWeldingRules.Row> ROWS=PipeWeldingRules.ROWS;

    /** Item pipes only exist for these sizes in GT6 ({@code MultiTileEntityPipeItem:77-79}). */
    private static final Map<PipeSpec.PipeSize, ItemPipeSpec.ItemPipeSize> ITEM_PIPE_SIZES = Map.of(
            PipeSpec.PipeSize.MEDIUM, ItemPipeSpec.ItemPipeSize.MEDIUM,
            PipeSpec.PipeSize.LARGE, ItemPipeSpec.ItemPipeSize.LARGE,
            PipeSpec.PipeSize.HUGE, ItemPipeSpec.ItemPipeSize.HUGE);

    /** One registered row, for tests and reports. */
    public record Entry(String material, PipeSpec.PipeSize size, String output, boolean itemPipe,
                        Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of(
            "GT6's COATED.NOT condition has no port counterpart; SMITHABLE and FLAMMABLE.NOT are applied");

    private static boolean registered;

    private PipeRecipes() {}

    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return SKIPPED; }

    public static int register() {
        if (registered) throw new IllegalStateException("Pipe recipes registered twice");
        registered = true;

        Map<String, Map<PipeSpec.PipeSize, ItemStack>> fluid = new HashMap<>();
        for (var holder : GTFluidPipes.all()) {
            if (!holder.isPresent()) continue;
            PipeSpec spec = holder.get().spec();
            fluid.computeIfAbsent(spec.material().getName(), k -> new HashMap<>())
                    .put(spec.size(), new ItemStack(holder.get().asItem()));
        }
        Map<String, Map<ItemPipeSpec.ItemPipeSize, ItemStack>> item = new HashMap<>();
        for (var holder : GTItemPipes.all()) {
            if (!holder.isPresent()) continue;
            ItemPipeSpec spec = holder.get().spec();
            item.computeIfAbsent(spec.material().getName(), k -> new HashMap<>())
                    .put(spec.size(), new ItemStack(holder.get().asItem()));
        }

        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid()) continue;
            if (material.has(MaterialProperty.ANTIMATTER) || material.has(MaterialProperty.FLAMMABLE)) continue;
            if (!material.has(MaterialProperty.SMITHABLE)) continue;      // GT6 SMITHABLE
            for (PipeWeldingRules.Row row : ROWS) weld(material, row, fluid, item);
            bundle(material, fluid);
        }
        GregTech.LOGGER.info("Registered {} GT6 pipe welding rows ({} GT6 rows skipped: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    /**
     * GT6 {@code Loader_Recipes_Handlers:492} (Boxinator) and {@code :458-459} (Unboxinator): four
     * medium pipes pack into one quadruple pipe, nine small ones into a nonuple — and both split again.
     * The crafting-grid direction lives in {@code Loader_FormConversionCraftingRecipes}
     * ({@code MultiTileEntityPipeFluid:100-101}).
     */
    private static void bundle(GTMaterial material, Map<String, Map<PipeSpec.PipeSize, ItemStack>> fluid) {
        Map<PipeSpec.PipeSize, ItemStack> sizes = fluid.getOrDefault(material.getName(), Map.of());
        ItemStack medium = sizes.get(PipeSpec.PipeSize.MEDIUM);
        ItemStack quadruple = sizes.get(PipeSpec.PipeSize.QUADRUPLE);
        ItemStack small = sizes.get(PipeSpec.PipeSize.SMALL);
        ItemStack nonuple = sizes.get(PipeSpec.PipeSize.NONUPLE);
        if (medium != null && quadruple != null) {
            ItemStack four = medium.copy();
            four.setCount(4);
            ItemStack tag = new ItemStack(GTTechnological.selectorTag(4));
            Recipe pack = MachineRecipeMaps.Boxinator.addRecipe2(true, 16, 16, four, tag, quadruple.copy());
            if (pack != null) ENTRIES.add(new Entry(material.getName(), PipeSpec.PipeSize.QUADRUPLE,
                    quadruple.getItem().toString(), false, pack));
            Recipe unpack = MachineRecipeMaps.Unboxinator.addRecipe1(true, 16, 16, quadruple.copy(), four.copy());
            if (unpack != null) ENTRIES.add(new Entry(material.getName(), PipeSpec.PipeSize.MEDIUM,
                    medium.getItem().toString(), false, unpack));
        }
        if (small != null && nonuple != null) {
            ItemStack nine = small.copy();
            nine.setCount(9);
            Recipe unpack = MachineRecipeMaps.Unboxinator.addRecipe1(true, 16, 16, nonuple.copy(), nine.copy());
            if (unpack != null) ENTRIES.add(new Entry(material.getName(), PipeSpec.PipeSize.SMALL,
                    small.getItem().toString(), false, unpack));
        }
    }

    private static void weld(GTMaterial material, PipeWeldingRules.Row row,
                             Map<String, Map<PipeSpec.PipeSize, ItemStack>> fluid,
                             Map<String, Map<ItemPipeSpec.ItemPipeSize, ItemStack>> item) {
        ItemStack input = GTItems.getStack(MaterialPrefix.plateCurved, material, row.curved());
        if (input.isEmpty()) return;

        ItemStack output = ItemStack.EMPTY;
        boolean itemPipe = false;
        ItemPipeSpec.ItemPipeSize itemSize = ITEM_PIPE_SIZES.get(row.size());
        if (itemSize != null) {
            ItemStack candidate = item.getOrDefault(material.getName(), Map.of()).get(itemSize);
            if (candidate != null && !candidate.isEmpty()) {
                output = candidate.copy();
                itemPipe = true;
            }
        }
        if (output.isEmpty()) {
            ItemStack candidate = fluid.getOrDefault(material.getName(), Map.of()).get(row.size());
            if (candidate == null || candidate.isEmpty()) return;
            output = candidate.copy();
        }
        int count = row.size() == PipeSpec.PipeSize.TINY ? 2 : 1;   // GT6 pipeTiny 2, all others 1
        output.setCount(count);

        // GT6's RecipeMapHandlerPrefix#getCosts with multiplier 64 for the hard pass (:324-328), and an
        // explicit 16 * <curved plates> for the easy-heatable pass (:339-343) - the same two-branch cost
        // WelderFamilyRecipes uses for the other prefix rows.
        long ticks = PipeWeldingRules.ticks(material,row);

        ItemStack tag = new ItemStack(GTTechnological.selectorTag(row.circuit()));
        Recipe recipe = MachineRecipeMaps.Welder.addRecipe2(true, 16, ticks, input, tag, output);
        if (recipe != null) {
            ENTRIES.add(new Entry(material.getName(), row.size(), output.getItem().toString(), itemPipe, recipe));
        }
    }
}
