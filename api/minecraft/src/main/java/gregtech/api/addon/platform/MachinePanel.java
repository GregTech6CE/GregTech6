/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

/**
 * Toggle, progress and working-face state shown by one behavior machine's menu and renderer.
 * Valid only during the same behavior callback as its context.
 */
public interface MachinePanel {
    boolean toggle(int index);

    void setToggle(int index, boolean on);

    float progress();

    /** Sets the progress arrow fraction from 0 to 1. Values outside that range are clamped to it. */
    void setProgress(float fraction);

    /** Lights the working face for this tick. */
    void working(boolean working);
}
