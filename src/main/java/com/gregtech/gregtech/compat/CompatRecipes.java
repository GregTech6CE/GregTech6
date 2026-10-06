package com.gregtech.gregtech.compat;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.content.compat.CompatSpecs;
import com.gregtech.gregtech.content.recipe.DyeProcessingRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTItems;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adds only the rows this compat pass owns, then drops those same objects on the next tag reload. */
public final class CompatRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<RecipeMap, Set<Recipe>> OWNED = new IdentityHashMap<>();
    private static final List<String> PROBLEMS = new ArrayList<>();
    private static int added;
    private static int errors;

    private CompatRecipes() {}

    public static synchronized int added() { return added; }
    public static synchronized int errors() { return errors; }
    public static synchronized List<String> problems() { return List.copyOf(PROBLEMS); }

    public static synchronized void clear() {
        OWNED.forEach((map, rows) -> {
            map.mRecipeList.removeAll(rows);
            map.mRecipeItemMap.values().forEach(values -> values.removeAll(rows));
            map.mRecipeFluidMap.values().forEach(values -> values.removeAll(rows));
        });
        OWNED.clear();
        PROBLEMS.clear();
        added = 0;
        errors = 0;
    }

    public static synchronized int rebuild() {
        clear();
        int skipped = 0;
        for (var module : CompatSpecs.modules()) {
            if (!ModList.get().isLoaded(module.modernId())) {
                skipped++;
                continue;
            }
            int before = added;
            int beforeErrors = errors;
            for (var row : module.machines()) apply(row);
            LOGGER.info("[gregtech] Compat {}: {} machine rows, {} unresolved",
                    module.modernId(), added - before, errors - beforeErrors);
        }
        if (skipped == CompatSpecs.modules().size())
            LOGGER.info("[gregtech] Compat skipped, no target mod is loaded");
        return added;
    }

    private static void apply(CompatSpecs.MachineRow row) {
        var map = map(row.map());
        var outputs = resolveAll(row.outputs(), row.source());
        if (outputs == null) return;
        switch (row.op()) {
            case SAW -> {
                for (var input : resolve(row.inputs().get(0), row.source())) saw(map, row, input, outputs);
            }
            case ONE, BATH -> {
                FluidStack fluid = fluid(row);
                if (row.op() == CompatSpecs.Op.BATH && fluid == null) return;
                for (var input : resolve(row.inputs().get(0), row.source())) {
                    var recipe = row.op() == CompatSpecs.Op.BATH
                            ? map.addRecipe1(row.optimize(), row.eut(), row.duration(), input.copy(), fluid.copy(), (FluidStack) null, copy(outputs))
                            : map.addRecipe1(row.optimize(), row.eut(), row.duration(), input.copy(), copy(outputs));
                    keep(map, row, recipe);
                }
            }
            case TWO -> {
                var left = resolve(row.inputs().get(0), row.source());
                var right = resolve(row.inputs().get(1), row.source());
                if ((long) left.size() * right.size() > 4096) {
                    problem(row.source(), row.id() + " tag expansion exceeds 4096");
                    return;
                }
                for (var first : left) for (var second : right)
                    keep(map, row, map.addRecipe2(row.optimize(), row.eut(), row.duration(), first.copy(), second.copy(), copy(outputs)));
            }
            case THREE -> {
                if (row.inputs().size() != 3) {
                    problem(row.source(), row.id() + " three-input row is malformed");
                    return;
                }
                var first = resolve(row.inputs().get(0), row.source());
                var second = resolve(row.inputs().get(1), row.source());
                var third = resolve(row.inputs().get(2), row.source());
                if (first.isEmpty() || second.isEmpty() || third.isEmpty()) return;
                if ((long) first.size() * second.size() * third.size() > 4096) {
                    problem(row.source(), row.id() + " tag expansion exceeds 4096");
                    return;
                }
                for (var a : first) for (var b : second) for (var c : third)
                    keep(map, row, map.addRecipeX(row.optimize(), row.eut(), row.duration(),
                            new ItemStack[] {a.copy(), b.copy(), c.copy()}, copy(outputs)));
            }
            case GENERIFY -> {
                var from = resolve(row.inputs().get(0), row.source());
                if (from.size() != 1 || outputs.size() != 1) {
                    problem(row.source(), row.id() + " generify needs one stack on each side");
                    return;
                }
                var before = identity(map.mRecipeList);
                if (!MachineRecipeMaps.generify(from.get(0).copy(), outputs.get(0).copy()))
                    problem(row.source(), row.id() + " generify was rejected");
                for (var recipe : map.mRecipeList) if (!before.contains(recipe)) keep(map, row, recipe);
            }
        }
    }

    private static void saw(RecipeMap map, CompatSpecs.MachineRow row, ItemStack input, List<ItemStack> outputs) {
        var before = identity(map.mRecipeList);
        if (!MachineRecipeMaps.sawing(row.eut(), row.duration(), row.food(), row.lubricant(), input.copy(), copy(outputs)))
            problem(row.source(), row.id() + " sawing was rejected");
        for (var recipe : map.mRecipeList) if (!before.contains(recipe)) keep(map, row, recipe);
    }

    private static void keep(RecipeMap map, CompatSpecs.MachineRow row, Recipe recipe) {
        if (recipe == null) {
            problem(row.source(), row.id() + " was rejected");
            return;
        }
        OWNED.computeIfAbsent(map, unused -> Collections.newSetFromMap(new IdentityHashMap<>())).add(recipe);
        added++;
    }

    private static FluidStack fluid(CompatSpecs.MachineRow row) {
        if (row.fluid() == null) return null;
        try {
            return DyeProcessingRecipes.fluid(row.fluid(), row.fluidAmount());
        } catch (RuntimeException failure) {
            problem(row.source(), row.id() + " missing fluid " + row.fluid());
            return null;
        }
    }

    private static List<ItemStack> resolveAll(List<CompatSpecs.Stack> stacks, String source) {
        var resolved = new ArrayList<ItemStack>();
        for (var stack : stacks) {
            var items = resolve(stack, source);
            if (items.isEmpty()) return null;
            if (items.size() != 1) {
                problem(source, "output must resolve to one item");
                return null;
            }
            resolved.add(items.get(0));
        }
        return resolved;
    }

    private static List<ItemStack> resolve(CompatSpecs.Stack stack, String source) {
        if (stack instanceof CompatSpecs.Stack.Item item) {
            var value = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(item.id()));
            if (value == null || value == Items.AIR) {
                problem(source, "missing item " + item.id());
                return List.of();
            }
            return List.of(new ItemStack(value, item.count()));
        }
        if (stack instanceof CompatSpecs.Stack.Tag tag) return tagItems(tag.forgeTag(), tag.count(), source);
        if (stack instanceof CompatSpecs.Stack.Form form) {
            var prefix = PrefixRegistry.byName(form.prefix());
            var material = GTMaterialRegistry.get(form.material());
            if (prefix == null || !material.isValid()) {
                problem(source, "missing form " + form.prefix() + "/" + form.material());
                return List.of();
            }
            var resolved = GTItems.getStack(prefix, material, form.count());
            if (resolved.isEmpty()) {
                problem(source, "unbound form " + form.prefix() + "/" + form.material());
                return List.of();
            }
            return List.of(resolved);
        }
        throw new IllegalStateException(stack.getClass().getName());
    }

    private static List<ItemStack> tagItems(String tag, int count, String source) {
        var id = ResourceLocation.tryParse(tag);
        var manager = ForgeRegistries.ITEMS.tags();
        if (id == null || manager == null) {
            problem(source, "tag " + tag + " is not bound");
            return List.of();
        }
        var items = new ArrayList<ItemStack>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<Item, Boolean>());
        manager.getTag(TagKey.create(Registries.ITEM, id)).forEach(item -> {
            if (item != Items.AIR && seen.add(item)) items.add(new ItemStack(item, count));
        });
        if (items.isEmpty()) problem(source, "empty tag " + tag);
        return items;
    }

    private static RecipeMap map(String name) {
        return switch (name) {
            case "Cutter" -> MachineRecipeMaps.Cutter;
            case "Compressor" -> MachineRecipeMaps.Compressor;
            case "Shredder" -> MachineRecipeMaps.Shredder;
            case "Loom" -> MachineRecipeMaps.Loom;
            case "Generifier" -> MachineRecipeMaps.Generifier;
            case "Bath" -> MachineRecipeMaps.Bath;
            case "Press" -> MachineRecipeMaps.Press;
            case "Hammer" -> MachineRecipeMaps.Hammer;
            case "Crusher" -> MachineRecipeMaps.Crusher;
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static ItemStack[] copy(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toArray(ItemStack[]::new);
    }

    private static Set<Recipe> identity(Collection<Recipe> recipes) {
        var set = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        set.addAll(recipes);
        return set;
    }

    private static void problem(String source, String message) {
        errors++;
        if (PROBLEMS.size() < 32) PROBLEMS.add(source + " " + message);
        LOGGER.warn("[gregtech] Compat {} {}", source, message);
    }
}
