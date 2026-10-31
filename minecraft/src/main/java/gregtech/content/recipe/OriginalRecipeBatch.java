package gregtech.content.recipe;

import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.recipe.*;
import gregtech.engine.recipe.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
//?}
import java.util.*;

/** Named source rows make unresolved ingredients and rejected recipes actionable. */
public abstract class OriginalRecipeBatch {
    public record Entry(String id, RecipeMap map, Recipe recipe) {}
    private final Set<String> rowIds = new HashSet<>();
    private final List<Entry> entries = new ArrayList<>();
    public final List<Entry> entries() { return Collections.unmodifiableList(entries); }
    protected final void add(String id, RecipeMap map, long ticks, long power,
            ItemStack[] inputs, ItemStack[] outputs, FluidStack[] fluids, FluidStack[] results) {
        publish(id, map, new Recipe(inputs, outputs, null, null, fluids, results, ticks, power, 0));
    }
    protected final void addCooled(String id, long ticks, long power, ItemStack[] inputs, ItemStack[] outputs,
            int amount, RecipeInputGroups.Coolant coolant) {
        publish(id, MachineRecipeMaps.Cutter, coolant.apply(new Recipe(inputs, outputs, null, null,
                fluids(coolant.group().stack(amount * coolant.multiplier())), null, ticks * coolant.multiplier(), power, 0)));
    }
    /** Row with source per-slot chances (GT6's {@code long[] tChances}). */
    protected final void add(String id, RecipeMap map, long ticks, long power, long[] chances,
            ItemStack[] inputs, ItemStack[] outputs, FluidStack[] fluids, FluidStack[] results) {
        Recipe recipe = RecipeMap.make(false, inputs, outputs, null, chances, fluids, results, ticks, power, 0);
        if (recipe == null) throw new IllegalStateException(id + ": collapsed source chance row");
        publish(id, map, recipe);
    }
    /** Source boolean is opt-in per reviewed row, never a global machine policy. */
    protected final void addSource(boolean optimize, String id, RecipeMap map, long ticks, long power,
            ItemStack[] inputs, ItemStack[] outputs, FluidStack[] fluids, FluidStack[] results) {
        Recipe recipe = RecipeMap.make(optimize, inputs, outputs, null, null, fluids, results, ticks, power, 0);
        if (recipe == null) throw new IllegalStateException("Collapsed source row " + id);
        publish(id, map, recipe);
    }
    protected final void publish(String id, RecipeMap map, Recipe recipe) {
        for (var stack : recipe.mInputs) if (stack.isEmpty()) throw new IllegalStateException(id + ": empty input");
        for (var stack : recipe.mOutputs) if (stack.isEmpty()) throw new IllegalStateException(id + ": empty output");
        if (!rowIds.add(id)) throw new IllegalStateException("Duplicate source row " + id);
        if (map.addRecipe(recipe) == null) throw new IllegalStateException("Rejected " + id + " in " + map);
        entries.add(new Entry(id, map, recipe));
    }
    protected static ItemStack item(String id, int count) {
//? if >=1.21.1 {
        /*var value = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
*///?} else {
        var value = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
//?}
        if (value == null || value == Items.AIR) throw new IllegalStateException("Missing source item " + id);
        return new ItemStack(value, count);
    }
    protected static ItemStack mat(MaterialPrefix prefix, String name, int count) {
        // GT6 LoaderUnificationTargets uses vanilla items for these exact material forms.
        // Keep these rows usable with survival drops rather than requiring duplicate GT items.
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
