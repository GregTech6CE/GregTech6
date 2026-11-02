/* SPDX-License-Identifier: LGPL-3.0-or-later */
package gregtech.api.multiblock;

import gregtech.api.machine.MachineFace;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Additional restrictions on one transport action. Faces are relative to the controller, using
 * the existing {@link MachineFace} convention, not the casing block's facing. Empty faces disable
 * the action. An unspecified side is accepted only when all six faces are permitted.
 * Slots are zero-based indices in the controller's exposed handler; empty means every slot/tank.
 * Filters are item/fluid registry ids or #tags, combined with OR; empty means any resource.
 * These rules can narrow a role's permissions, never grant a permission the role does not have.
 */
public record PortRule(List<MachineFace> faces, List<Integer> slots, List<String> filters) {
    public static final PortRule ALL = new PortRule(MachineFace.all(), List.of(), List.of());
    public static final PortRule NONE = new PortRule(List.of(), List.of(), List.of());

    public PortRule {
        faces = MachineFace.canonical(Objects.requireNonNull(faces, "faces"), "port face");
        slots = List.copyOf(slots);
        filters = List.copyOf(filters);
        if (slots.stream().anyMatch(i -> i < 0) || slots.stream().distinct().count() != slots.size())
            throw new IllegalArgumentException("Port slots must be unique, non-negative indices");
        if (filters.stream().distinct().count() != filters.size())
            throw new IllegalArgumentException("Duplicate port filter");
        for (String filter : filters) {
            if (filter.startsWith("#")) CellMatch.tag(filter.substring(1));
            else CellMatch.block(filter);
        }
    }

    public static PortRule faces(MachineFace... faces) {
        return new PortRule(List.of(faces), List.of(), List.of());
    }

    public PortRule slots(int... slots) {
        return new PortRule(faces, java.util.Arrays.stream(slots).boxed().toList(), filters);
    }

    public PortRule filter(String... filters) { return new PortRule(faces, slots, List.of(filters)); }

    public boolean allowsFace(@Nullable MachineFace face) {
        return face == null ? faces.size() == MachineFace.values().length : faces.contains(face);
    }

    public boolean allowsSlot(int slot) { return slot >= 0 && (slots.isEmpty() || slots.contains(slot)); }
}
