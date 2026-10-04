/* Source conversion policy: Gregorius Techneticies / GregTech-6 Team, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

import java.util.*;

/** Non-GT recipes replaced by source AdvancedCrafting1ToY/XToY cannot bypass their NO_AUTO policy. */
public final class CraftingConversionPermissions<T> {
    private record Rule<T>(T source, int cells, T output) {
        @Override public boolean equals(Object other) {
            return other instanceof Rule<?> rule && source == rule.source && cells == rule.cells && output == rule.output;
        }
        @Override public int hashCode() { return 31 * (31 * System.identityHashCode(source) + cells) + System.identityHashCode(output); }
    }
    private final Map<T, Set<Rule<T>>> inputs;
    private CraftingConversionPermissions(Map<T, Set<Rule<T>>> inputs) { this.inputs = inputs; }

    /** Cells are occupied positions, not the stack quantities; aliases must share one material/form. */
    public boolean disallows(List<T> cells, T output) {
        if (cells.isEmpty() || cells.size() > 9) return false;
        Set<Rule<T>> first = inputs.get(cells.get(0));
        if (first == null) return false;
        for (Rule<T> rule : first) {
            if (rule.cells() != cells.size() || rule.output() != output) continue;
            boolean sameForm = true;
            for (T cell : cells) {
                Set<Rule<T>> candidates = inputs.get(cell);
                if (candidates == null || !candidates.contains(rule)) { sameForm = false; break; }
            }
            if (sameForm) return true;
        }
        return false;
    }

    public static final class Builder<T> {
        private final Map<T, Set<Rule<T>>> inputs = new IdentityHashMap<>();
        public void add(T source, T canonical, int cells, T output, T canonicalOutput) {
            if (source == null || output == null || cells < 1 || cells > 9) throw new IllegalArgumentException("Source conversion input/output");
            addOutput(source, canonical, cells, output);
            if (canonicalOutput != null && canonicalOutput != output) addOutput(source, canonical, cells, canonicalOutput);
        }
        private void addOutput(T source, T canonical, int cells, T output) {
            Rule<T> rule = new Rule<>(source, cells, output);
            inputs.computeIfAbsent(source, unused -> new HashSet<>()).add(rule);
            if (canonical != null) inputs.computeIfAbsent(canonical, unused -> new HashSet<>()).add(rule);
        }
        public CraftingConversionPermissions<T> build() {
            Map<T, Set<Rule<T>>> snapshot = new IdentityHashMap<>();
            inputs.forEach((item, rules) -> snapshot.put(item, Set.copyOf(rules)));
            return new CraftingConversionPermissions<>(Collections.unmodifiableMap(snapshot));
        }
    }
}
