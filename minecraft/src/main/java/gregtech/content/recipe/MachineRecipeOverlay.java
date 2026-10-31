package gregtech.content.recipe;

import gregtech.engine.material.MaterialDisplayBinding;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.data.FluidComponent;
import gregtech.engine.recipe.data.ItemComponent;
import gregtech.engine.recipe.data.ItemResult;
import gregtech.engine.recipe.data.MachineRecipeFields;
import gregtech.engine.recipe.data.MachineRecipeMatch;
import gregtech.engine.recipe.data.MachineRecipeRemovalSpec;
import gregtech.engine.recipe.data.MachineRecipeSpec;
import gregtech.compat.common.CompatRecipes;
import gregtech.content.recipe.ExternalOreProcessing;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?} else {
//?}
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;

/** Restores the startup rows, rebuilds tag-owned rows, then applies datapack additions and removals. */
public final class MachineRecipeOverlay {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int logged;
    private static int applications;
    private static boolean overlayDumped;
    private static Result lastResult = Result.unchanged();
    private static List<Recipe> furnaceAdded = List.of();
    private static List<Recipe> furnaceRemoved = List.of();

    private MachineRecipeOverlay() {}

    public record Result(int removed, int added, int rejected, long restoreMillis, int baselineRows, boolean skipped) {
        public static Result unchanged() { return new Result(0, 0, 0, 0, 0, true); }
    }

    public static int staticRows() {
        int rows = 0;
        for (var map : RecipeMap.RECIPE_MAP_LIST) rows += map.mRecipeList.size();
        return rows;
    }

    public static int baselineRows() {
        int rows = 0;
        for (var map : RecipeMap.RECIPE_MAP_LIST) rows += map.baselineSize();
        return rows;
    }

    /** Times one full index rebuild of the captured baseline. The caller applies the overlay again afterwards. */
    public static long reindexMillis() {
        ensureBaseline();
        long started = System.nanoTime();
        int rows = 0;
        for (var map : RecipeMap.RECIPE_MAP_LIST) rows += map.reindexBaseline();
        long millis = (System.nanoTime() - started) / 1_000_000L;
        LOGGER.info("[gregtech] Machine recipe baseline reindexed {} rows in {} ms", rows, millis);
        return millis;
    }

    public static Result apply(RecipeManager manager) {
        // Undo the previous overlay before clearing mirrors: DISPLACED may otherwise
        // restore a previous world's datapack row after the baseline was restored.
        gregtech.content.recipe.OvenRecipeBridge.clearMirrors();
        ensureBaseline();
        long started = System.nanoTime();
        int moved = 0;
        for (var map : RecipeMap.RECIPE_MAP_LIST) moved += map.restoreBaseline();
        long restoreMillis = (System.nanoTime() - started) / 1_000_000L;
        long producersStarted = System.nanoTime();
        ExternalOreProcessing.rebuild();
        gregtech.content.recipe.NonOreMacerationRows.rebuild();
        gregtech.content.recipe.SourceOreOutputRows.rebuild();
        SharedPurifiedCentrifugeRows.rebuild();
        SharedMineralSteamCrackingRows.rebuild();
        SharedOreWashingRows.rebuild();
        SharedOreMagneticRows.rebuild();
        SharedOreSiftingRows.rebuild();
        CompatRecipes.rebuild();
        // OPL capture waits until static/addon and tag-owned producers can retain their independent rows.
        SharedOreCrushingRows.rebuild();
        SharedOilSandRows.rebuild();
        // New purified-acid calls do not displace the earlier imported/static Bath producers.
        SharedPurifiedAcidRows.rebuild();
        SharedThermalInputRows.rebuild();
        SharedThermalRecyclingRows.rebuild();
        long producersMillis = (System.nanoTime() - producersStarted) / 1_000_000L;
        MaterialDisplayBinding.invalidate();
        logged = 0;
        var tally = new Tally();
        var markers = markers(manager);
        var furnaceBefore = furnaceRows();
        var removeStarted = System.nanoTime();
        removeMarkers(markers, tally);
        long removeMillis = (System.nanoTime() - removeStarted) / 1_000_000L;
        var installStarted = System.nanoTime();
        for (var marker : markers) if (marker.addition() != null) install(marker.id(), marker.addition(), tally);
        long installMillis = (System.nanoTime() - installStarted) / 1_000_000L;
        rememberFurnaceChanges(furnaceBefore);
        LOGGER.info("[gregtech] Machine recipe overlay baseline {} rows, restore {} ms, producers {} ms, moved {}, "
                        + "removed {} ({} ms), added {} ({} ms), rejected {}",
                baselineRows(), restoreMillis, producersMillis, moved, tally.removed, removeMillis, tally.added,
                installMillis, tally.rejected);
        lastResult = new Result(tally.removed, tally.added, tally.rejected, restoreMillis, baselineRows(), false);
        applications++;
        if (!overlayDumped) {
            overlayDumped = true;
            RecipeDump.afterOverlay();
        }
        return lastResult;
    }

    public static int applications() { return applications; }
    public static Result lastResult() { return lastResult; }

    /**
     * Return only Furnace to its state before the last overlay. The oven bridge calls this
     * before tracking displaced rows, so its bookkeeping never owns a datapack addition.
     * Repeating the call is a no-op; providers and every other recipe map stay untouched.
     */
    public static void restoreFurnaceBeforeMirror() {
        var furnace = gregtech.content.data.MachineRecipeMaps.Furnace;
        furnace.removeAll(furnaceAdded);
        for (var recipe : furnaceRemoved) if (!furnace.mRecipeList.contains(recipe)) furnace.add(recipe);
        furnaceAdded = List.of();
        furnaceRemoved = List.of();
    }

    /** The oven has finished importing vanilla smelting; its rows now obey the same Furnace markers. */
    public static void reapplyFurnace(RecipeManager manager) {
        restoreFurnaceBeforeMirror();
        var furnace = gregtech.content.data.MachineRecipeMaps.Furnace;
        var before = furnaceRows();
        var tally = new Tally();
        var markers = markers(manager);
        var drop = new ArrayList<Recipe>();
        for (var recipe : furnace.mRecipeList) {
            var view = view(recipe);
            for (var marker : markers)
                if (marker.removal() != null && marker.removal().map().equals(furnace.mNameInternal)
                        && MachineRecipeMatch.matches(marker.removal(), view, MachineRecipeOverlay::itemInTag)) {
                    drop.add(recipe);
                    break;
                }
        }
        furnace.removeAll(drop);
        for (var marker : markers)
            if (marker.addition() != null && marker.addition().map().equals(furnace.mNameInternal))
                install(marker.id(), marker.addition(), tally);
        rememberFurnaceChanges(before);
    }

    private static java.util.Set<Recipe> furnaceRows() {
        var rows = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        rows.addAll(gregtech.content.data.MachineRecipeMaps.Furnace.mRecipeList);
        return rows;
    }

    private static void rememberFurnaceChanges(java.util.Set<Recipe> before) {
        var after = furnaceRows();
        var removed = new ArrayList<Recipe>();
        for (var recipe : before) if (!after.contains(recipe)) removed.add(recipe);
        var added = new ArrayList<Recipe>();
        for (var recipe : after) if (!before.contains(recipe)) added.add(recipe);
        furnaceRemoved = List.copyOf(removed);
        furnaceAdded = List.copyOf(added);
    }

    private static void ensureBaseline() {
        var exclude = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        exclude.addAll(CompatRecipes.owned());
        exclude.addAll(ExternalOreProcessing.owned());
        exclude.addAll(gregtech.content.recipe.NonOreMacerationRows.owned());
        exclude.addAll(gregtech.content.recipe.SourceOreOutputRows.owned());
        exclude.addAll(SharedPurifiedCentrifugeRows.owned());
        exclude.addAll(SharedMineralSteamCrackingRows.owned());
        exclude.addAll(SharedOreWashingRows.owned());
        exclude.addAll(SharedOreMagneticRows.owned());
        exclude.addAll(SharedOreSiftingRows.owned());
        exclude.addAll(SharedOreCrushingRows.baselineExcluded());
        exclude.addAll(SharedOilSandRows.baselineExcluded());
        exclude.addAll(SharedPurifiedAcidRows.owned());
        exclude.addAll(SharedThermalInputRows.owned());
        exclude.addAll(SharedThermalRecyclingRows.owned());
        // Maps created after the first reload still get one baseline. A captured map keeps its first snapshot.
        for (var map : RecipeMap.RECIPE_MAP_LIST) map.captureBaseline(exclude);
    }

    private record Marker(String id, MachineRecipeSpec addition, MachineRecipeRemovalSpec removal) {}

    private static List<Marker> markers(RecipeManager manager) {
        var markers = new ArrayList<Marker>();
//? if >=1.21.1 {
/*        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.value() instanceof MachineDatapackRecipe marker)
                markers.add(new Marker(holder.id().toString(), marker.addition(), marker.removal()));
*///?} else {
        for (var recipe : manager.getRecipes()) {
            if (recipe instanceof MachineDatapackRecipe marker)
                markers.add(new Marker(marker.getId().toString(), marker.addition(), marker.removal()));
//?}
        }
        markers.sort(Comparator.comparing(Marker::id));
        return markers;
    }

    private static void install(String id, MachineRecipeSpec spec, Tally tally) {
        var map = RecipeMap.RECIPE_MAPS.get(spec.map());
        if (map == null) {
            tally.rejected++;
            problem(id + " unknown recipe map " + spec.map());
            return;
        }
        List<List<ItemStack>> slots;
        try {
            slots = itemSlots(spec.itemInputs());
        } catch (IllegalArgumentException failure) {
            tally.rejected++;
            problem(id + " " + failure.getMessage());
            return;
        }
        long product = 1;
        for (var slot : slots) {
            product *= slot.size();
            if (product > MachineRecipeFields.EXPANSION_LIMIT) {
                tally.rejected++;
                problem(id + " tag expansion exceeds " + MachineRecipeFields.EXPANSION_LIMIT);
                return;
            }
        }
        FluidStack[] fluidInputs;
        FluidStack[] fluidOutputs;
        ItemStack[] outputs;
        try {
            fluidInputs = fluids(spec.fluidInputs(), true);
            fluidOutputs = fluids(spec.fluidOutputs(), false);
            outputs = outputs(spec.itemOutputs());
        } catch (IllegalArgumentException failure) {
            tally.rejected++;
            problem(id + " " + failure.getMessage());
            return;
        }
        var chances = new long[spec.itemOutputs().size()];
        for (int i = 0; i < chances.length; i++) chances[i] = spec.itemOutputs().get(i).chance();
        if (slots.isEmpty()) {
            add(map, new ItemStack[0], outputs, chances, fluidInputs, fluidOutputs, spec, tally);
            return;
        }
        var index = new int[slots.size()];
        while (true) {
            var items = new ItemStack[slots.size()];
            for (int i = 0; i < slots.size(); i++) items[i] = slots.get(i).get(index[i]).copy();
            add(map, items, outputs, chances, fluidInputs, fluidOutputs, spec, tally);
            int cursor = 0;
            while (cursor < index.length) {
                index[cursor]++;
                if (index[cursor] < slots.get(cursor).size()) break;
                index[cursor] = 0;
                cursor++;
            }
            if (cursor == index.length) return;
        }
    }

    private static void add(RecipeMap map, ItemStack[] items, ItemStack[] outputs, long[] chances,
            FluidStack[] fluidInputs, FluidStack[] fluidOutputs, MachineRecipeSpec spec, Tally tally) {
        if (gregtech.engine.addon.ExclusiveOutputMaps.enabled(map)
                && !gregtech.engine.addon.ExclusiveOutputRules.isPair(outputs.length,
                gregtech.engine.addon.ExclusiveOutputMaps.nonEmptyItems(outputs))) {
            tally.rejected++;
            problem(spec.map() + " exclusive outputs need exactly 2 item outputs: "
                    + gregtech.engine.addon.ExclusiveOutputMaps.nonEmptyItems(outputs));
            return;
        }
        var copiedOutputs = new ItemStack[outputs.length];
        for (int i = 0; i < outputs.length; i++) copiedOutputs[i] = outputs[i].copy();
        var copiedFluidInputs = new FluidStack[fluidInputs.length];
        for (int i = 0; i < fluidInputs.length; i++) copiedFluidInputs[i] = fluidInputs[i].copy();
        var copiedFluidOutputs = new FluidStack[fluidOutputs.length];
        for (int i = 0; i < fluidOutputs.length; i++) copiedFluidOutputs[i] = fluidOutputs[i].copy();
        var recipe = new Recipe(items, copiedOutputs, null, chances, copiedFluidInputs, copiedFluidOutputs,
                spec.duration(), gregtech.content.data.FuelRecipeMaps.storedEu(map, spec.euPerTick()), spec.special());
        recipe.withPresenceFluidInputs(java.util.stream.IntStream.range(0, spec.fluidInputs().size())
                .filter(i -> spec.fluidInputs().get(i).amount() == 0).toArray());
        if (map.addRecipe(recipe, true, false, false, false) == null) tally.rejected++;
        else tally.added++;
    }

    /**
     * Applies every datapack removal marker. One map's rows are compared once against all of that
     * map's markers, in the markers' original order, so each row builds one comparison view and the
     * first matching marker keeps the attribution.
     */
    private static void removeMarkers(List<Marker> markers, Tally tally) {
        var perMap = new java.util.LinkedHashMap<RecipeMap, List<Marker>>();
        for (var marker : markers) {
            if (marker.removal() == null) continue;
            var map = RecipeMap.RECIPE_MAPS.get(marker.removal().map());
            if (map == null) {
                tally.rejected++;
                problem(marker.id() + " unknown recipe map " + marker.removal().map());
                continue;
            }
            perMap.computeIfAbsent(map, unused -> new ArrayList<>()).add(marker);
        }
        for (var entry : perMap.entrySet()) {
            var drop = new ArrayList<Recipe>();
            for (var recipe : entry.getKey().mRecipeList) {
                var view = view(recipe);
                for (var marker : entry.getValue())
                    if (MachineRecipeMatch.matches(marker.removal(), view, MachineRecipeOverlay::itemInTag)) {
                        drop.add(recipe);
                        break;
                    }
            }
            tally.removed += entry.getKey().removeAll(drop);
        }
    }

    private static boolean itemInTag(String itemId, String tagId) {
        var item = item(itemId);
        var tag = ResourceLocation.tryParse(tagId);
        return item != null && tag != null && item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, tag));
    }

    private static MachineRecipeMatch.RecipeView view(Recipe recipe) {
        var items = new ArrayList<ItemStack>();
        var itemCounts = new ArrayList<Integer>();
        for (var stack : recipe.mInputs) {
            if (stack == null || stack.isEmpty()) continue;
            items.add(stack);
            itemCounts.add(stack.getCount());
        }
        var fluids = new ArrayList<FluidStack>();
        var fluidAmounts = new ArrayList<Integer>();
        for (int i = 0; i < recipe.mFluidInputs.length; i++) {
            var fluid = recipe.mFluidInputs[i];
            if (fluid != null && !fluid.isEmpty()) {
                fluids.add(fluid);
                fluidAmounts.add(recipe.isPresenceFluidInput(i) ? 0 : fluid.getAmount());
            }
        }
        var outputs = new ArrayList<ItemStack>();
        var chances = new ArrayList<Integer>();
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            var stack = recipe.mOutputs[i];
            if (stack == null || stack.isEmpty()) continue;
            outputs.add(stack);
            chances.add(recipe.getOutputChance(i));
        }
        var fluidOutputs = new ArrayList<FluidStack>();
        for (var fluid : recipe.mFluidOutputs) if (fluid != null && !fluid.isEmpty()) fluidOutputs.add(fluid);
        return new MachineRecipeMatch.RecipeView() {
            public int inputItems() { return items.size(); }
            public String inputItem(int index) { return BuiltInRegistries.ITEM.getKey(items.get(index).getItem()).toString(); }
            public int inputItemCount(int index) { return itemCounts.get(index); }
            public int inputFluids() { return fluids.size(); }
            public String inputFluid(int index) { return fluidKey(fluids.get(index)); }
            public long inputFluidAmount(int index) { return fluidAmounts.get(index); }
            public int outputItems() { return outputs.size(); }
            public String outputItem(int index) { return BuiltInRegistries.ITEM.getKey(outputs.get(index).getItem()).toString(); }
            public int outputItemCount(int index) { return outputs.get(index).getCount(); }
            public int outputItemChance(int index) { return chances.get(index); }
            public int outputFluids() { return fluidOutputs.size(); }
            public String outputFluid(int index) { return fluidKey(fluidOutputs.get(index)); }
            public long outputFluidAmount(int index) { return fluidOutputs.get(index).getAmount(); }
        };
    }

    private static List<List<ItemStack>> itemSlots(List<ItemComponent> inputs) {
        var slots = new ArrayList<List<ItemStack>>();
        for (var input : inputs) {
            if (input.item() != null) {
                var item = item(input.item());
                if (item == null) throw new IllegalArgumentException("unknown item " + input.item());
                slots.add(List.of(new ItemStack(item, input.count())));
                continue;
            }
            var tagId = ResourceLocation.tryParse(input.tag());
            if (tagId == null) throw new IllegalArgumentException("bad tag " + input.tag());
            var choices = new ArrayList<ItemStack>();
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, tagId)))
                choices.add(new ItemStack(holder.value(), input.count()));
            if (choices.isEmpty()) throw new IllegalArgumentException("empty tag " + input.tag());
            choices.sort(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            slots.add(choices);
        }
        return slots;
    }

    private static ItemStack[] outputs(List<ItemResult> results) {
        var stacks = new ItemStack[results.size()];
        for (int i = 0; i < results.size(); i++) {
            var item = item(results.get(i).item());
            if (item == null) throw new IllegalArgumentException("unknown item " + results.get(i).item());
            stacks[i] = new ItemStack(item, results.get(i).count());
        }
        return stacks;
    }

    private static FluidStack[] fluids(List<FluidComponent> components, boolean input) {
        var stacks = new FluidStack[components.size()];
        for (int i = 0; i < components.size(); i++) {
            var parsed = ResourceLocation.tryParse(components.get(i).fluid());
            Fluid fluid = parsed == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(parsed);
            if (fluid == null || fluid == Fluids.EMPTY) throw new IllegalArgumentException("unknown fluid " + components.get(i).fluid());
            stacks[i] = new FluidStack(fluid, (int) (input ? Math.max(1, components.get(i).amount()) : components.get(i).amount()));
        }
        return stacks;
    }

    private static Item item(String id) {
        var parsed = ResourceLocation.tryParse(id);
        if (parsed == null) return null;
        var item = BuiltInRegistries.ITEM.get(parsed);
        return item == null || item == Items.AIR ? null : item;
    }

    private static String fluidKey(FluidStack fluid) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
*///?} else {
        return fluid.getFluid().builtInRegistryHolder().key().location().toString();
//?}
    }

    private static void problem(String message) {
        if (logged++ < 20) LOGGER.warn("[gregtech] Machine recipe overlay {}", message);
    }

    private static final class Tally {
        private int removed;
        private int added;
        private int rejected;
    }
}
