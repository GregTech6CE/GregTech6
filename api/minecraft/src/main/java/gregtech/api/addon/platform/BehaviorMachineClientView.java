/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import gregtech.api.addon.machine.*;

/**
 * Behavior machine state GregTech has synced to the client. A renderer reads this.
 * Only keys listed by {@code BehaviorMachine.Builder.clientState} are present.
 * A missing named field reads as zero, false or an empty string.
 * A key that was not declared throws.
 */
public interface BehaviorMachineClientView {
    String addonId();

    /** The declared machine type. */
    String machineType();

    int getInt(String key);

    long getLong(String key);

    boolean getBoolean(String key);

    String getString(String key);
}
