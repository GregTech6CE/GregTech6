/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

/**
 * Rendering geometry of the default behavior-machine GUI: widget sizes and texture atlas
 * coordinates. The fixed sizes also bound the widget placement that
 * {@code gregtech.api.addon.machine.BehaviorMachine} accepts at declaration time; keep both
 * sides in sync.
 */
public final class BehaviorMachineGui {
    public static final int SLOT = value(18);
    public static final int TANK_WIDTH = value(18);
    public static final int TANK_HEIGHT = value(54);
    public static final int ENERGY_WIDTH = value(10);
    public static final int ENERGY_HEIGHT = value(54);
    public static final int PROGRESS_WIDTH = value(24);
    public static final int PROGRESS_HEIGHT = value(17);
    /** Player inventory starts this far above the bottom of the gui height. */
    public static final int PLAYER_INVENTORY_GAP = value(82);
    public static final int UV_SLOT_U = value(176);
    public static final int UV_SLOT_V = value(0);
    public static final int UV_ENERGY_U = value(194);
    public static final int UV_ENERGY_V = value(0);
    public static final int UV_ENERGY_FULL_U = value(204);
    public static final int UV_TANK_U = value(214);
    public static final int UV_TANK_V = value(0);
    public static final int UV_TANK_MARKS_U = value(232);
    public static final int UV_ARROW_U = value(176);
    public static final int UV_ARROW_V = value(54);
    public static final int UV_ARROW_FULL_V = value(71);
    public static final int UV_TOGGLE_OFF_U = value(176);
    public static final int UV_TOGGLE_ON_U = value(194);
    public static final int UV_TOGGLE_V = value(88);

    /** Keeps a public value out of the compile-time constant pool so javac cannot inline it. */
    private static int value(int value) { return value; }

    private BehaviorMachineGui() {}
}
