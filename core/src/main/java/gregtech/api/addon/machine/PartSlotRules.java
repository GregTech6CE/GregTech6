/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;




import gregtech.api.machine.*;

/** Which faces may move items, and how many of a stack fit. The behavior still has to allow the item. */
public final class PartSlotRules {
    private PartSlotRules() {}

    public static boolean inserts(PartSlots slots, MachineFace face) {
        return slots != null && face != null && slots.insertFaces().contains(face);
    }

    public static boolean extracts(PartSlots slots, MachineFace face) {
        return slots != null && face != null && slots.extractFaces().contains(face);
    }

    /** How many of {@code offered} fit on top of {@code stored}, capped by {@code limit}. */
    public static int accepted(int stored, int limit, int offered) {
        if (offered <= 0 || limit < 1) return 0;
        int held = Math.max(0, Math.min(stored, limit));
        return Math.min(offered, limit - held);
    }
}
