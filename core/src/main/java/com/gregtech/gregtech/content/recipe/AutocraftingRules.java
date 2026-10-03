/* Adapted from GregTech-6 Team's RecipeMapAutocrafting (2024), LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Source blueprint cell aggregation, before native crafting and item-container handling. */
public final class AutocraftingRules {
    private AutocraftingRules() {}
    public static final String BLUEPRINT_KEY = "gt.blueprint.craft";
    public static final String LEGACY_KEY = "gt.CraftingPattern";
    public static final int CELLS = 9, SLOT_X = 80, SLOT_Y = 43;
    public static final long DURATION = 1024, POWER = 16;
    public record Input<T>(T sample, int count, boolean retained) {}
    public record Amount<T>(T sample, int count) {}
    public record Plan<T>(List<Input<T>> inputs, List<Amount<T>> outputs, long duration) {}

    /** The source rejects hand tools and retains infinite inputs as a presence requirement. */
    public static <T> List<Input<T>> inputs(List<T> blueprint, Predicate<T> empty,
            Predicate<T> handTool, BiPredicate<T,T> same, Predicate<T> infinite) {
        if (blueprint.size() != CELLS) return null;
        for (T cell : blueprint) if (!empty.test(cell) && handTool.test(cell)) return null;
        var result = new ArrayList<Input<T>>();
        for (T cell : blueprint) {
            if (empty.test(cell)) continue;
            int index = -1;
            for (int i = 0; i < result.size(); i++) if (same.test(result.get(i).sample(), cell)) { index = i; break; }
            if (index < 0) result.add(new Input<>(cell, 1, infinite.test(cell)));
            else {
                Input<T> old = result.get(index);
                result.set(index, new Input<>(old.sample(), old.count() + 1, old.retained()));
            }
        }
        return List.copyOf(result);
    }

    /** Recipe:904-939: ignore zero-size catalysts when deriving the optimization bound. */
    public static <T> Plan<T> optimize(List<Input<T>> inputs, List<Amount<T>> outputs,
            BiPredicate<T,T> same) {
        var in = new ArrayList<Input<T>>();
        var out = new ArrayList<>(outputs);
        int limit = (int)(DURATION / 16);
        for (Input<T> cell : inputs) {
            int count = cell.retained() ? 0 : cell.count();
            in.add(new Input<>(cell.sample(), count, cell.retained()));
            if (count > 0) limit = Math.min(limit, count);
        }
        for (Amount<T> cell : out) if (cell.count() > 0) limit = Math.min(limit, cell.count());
        for (int i = 0; i < in.size(); i++) for (int j = 0; j < out.size(); j++) {
            Input<T> input = in.get(i);
            Amount<T> output = out.get(j);
            if (output.count() <= 0 || !same.test(input.sample(), output.sample())) continue;
            if (input.count() >= output.count()) {
                int remaining = input.count() - output.count();
                in.set(i, new Input<>(input.sample(), remaining, input.retained() || remaining == 0));
                out.set(j, new Amount<>(output.sample(), 0));
                limit = Math.min(limit, remaining);
            } else {
                int remaining = output.count() - input.count();
                out.set(j, new Amount<>(output.sample(), remaining));
                limit = Math.min(limit, remaining);
            }
        }
        long duration = DURATION;
        for (; limit > 1; limit--) {
            final int divisor = limit;
            if (in.stream().anyMatch(v -> v.count() % divisor != 0)
                    || out.stream().anyMatch(v -> v.count() % divisor != 0)) continue;
            for (int i = 0; i < in.size(); i++) {
                Input<T> cell = in.get(i);
                in.set(i, new Input<>(cell.sample(), cell.count() / divisor, cell.retained()));
            }
            for (int i = 0; i < out.size(); i++) {
                Amount<T> cell = out.get(i);
                out.set(i, new Amount<>(cell.sample(), cell.count() / divisor));
            }
            duration /= divisor;
            break;
        }
        return new Plan<>(List.copyOf(in), out.stream().filter(v -> v.count() > 0).toList(), duration);
    }
}
