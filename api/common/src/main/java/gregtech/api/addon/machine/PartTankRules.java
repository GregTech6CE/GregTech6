/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.machine.MachineFace;

/** Which faces may move fluid. The behavior still has to allow the fluid. */
public final class PartTankRules {
    private PartTankRules() {}

    public static boolean inserts(PartTanks tanks, MachineFace face) {
        return tanks != null && face != null && tanks.insertFaces().contains(face);
    }

    public static boolean extracts(PartTanks tanks, MachineFace face) {
        return tanks != null && face != null && tanks.extractFaces().contains(face);
    }
}
