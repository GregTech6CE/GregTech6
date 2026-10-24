/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.internal.addon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Internal: not part of the addon contract; may change without notice.
 * Display-only catalyst rows for a behavior machine.
 * An empty name list does not change the index JEI and EMI already built.
 */
public final class BehaviorRecipeCatalysts {
    private BehaviorRecipeCatalysts() {}

    /**
     * Adds {@code machine} to each named map.
     * A null resolved map is skipped. The same machine is not listed twice on one map.
     */
    public static <M, T> void add(Map<M, List<T>> index, List<String> names, T machine,
                                  Function<String, M> maps, BiPredicate<T, T> same) {
        if (names == null || names.isEmpty()) return;
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(machine, "machine");
        Objects.requireNonNull(maps, "maps");
        Objects.requireNonNull(same, "same");
        for (String name : names) {
            M map = name == null ? null : maps.apply(name);
            if (map == null) continue;
            List<T> list = index.computeIfAbsent(map, key -> new ArrayList<>());
            boolean present = false;
            for (T existing : list) {
                if (same.test(existing, machine)) {
                    present = true;
                    break;
                }
            }
            if (!present) list.add(machine);
        }
    }
}
