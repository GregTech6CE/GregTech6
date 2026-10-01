package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

/** Named source rows make unresolved ingredients and rejected recipes actionable. */
public abstract class OriginalRecipeBatch {
    public record Entry(String id, RecipeMap map, Recipe recipe) {}
    private final List<Entry> entries = new ArrayList<>();
    public final List<Entry> entries() { return Collections.unmodifiableList(entries); }
    protected final void add(String id, RecipeMap map, long ticks, long power,
            ItemStack[] inputs, ItemStack[] outputs, FluidStack[] fluids, FluidStack[] results) {
        Recipe recipe = new Recipe(inputs, outputs, null, null, fluids, results, ticks, power, 0);
        for (var stack : recipe.mInputs) if (stack.isEmpty()) throw new IllegalStateException(id + ": empty input");
        for (var stack : recipe.mOutputs) if (stack.isEmpty()) throw new IllegalStateException(id + ": empty output");
        if (entries.stream().anyMatch(e -> e.id().equals(id))) throw new IllegalStateException("Duplicate source row " + id);
        if (map.addRecipe(recipe) == null) throw new IllegalStateException("Rejected " + id + " in " + map);
        entries.add(new Entry(id, map, recipe));
    }
    protected static ItemStack item(String id, int count) {
        var value = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (value == null || value == Items.AIR) throw new IllegalStateException("Missing source item " + id);
        return new ItemStack(value, count);
    }
    protected static ItemStack mat(MaterialPrefix prefix, String name, int count) {
        // GT6 LoaderUnificationTargets uses vanilla items for these exact material forms.
        // Keep this batch usable with survival drops rather than requiring duplicate GT items.
        Item vanilla = switch (prefix.getName() + "/" + name) {
            case "dust/Sugar" -> Items.SUGAR;
            case "dust/Gunpowder" -> Items.GUNPOWDER;
            case "dust/Bone" -> Items.BONE_MEAL;
            case "dustTiny/Blaze" -> Items.BLAZE_POWDER;
            case "nugget/Gold" -> Items.GOLD_NUGGET;
            default -> null;
        };
        if (vanilla != null) return new ItemStack(vanilla, count);
        var value = GTItems.getStack(prefix, GTMaterialRegistry.get(name).resolve(), count);
        if (value.isEmpty()) throw new IllegalStateException("Missing source form " + prefix.getName() + "/" + name);
        return value;
    }
    protected static ItemStack selector(int mode) { return new ItemStack(GTTechnological.selectorTag(mode)); }
    protected static FluidStack fluid(String field, int amount) { return DyeProcessingRecipes.fluid(field, amount); }
    protected static ItemStack[] items(ItemStack... values) { return values; }
    protected static FluidStack[] fluids(FluidStack... values) { return values; }
}
