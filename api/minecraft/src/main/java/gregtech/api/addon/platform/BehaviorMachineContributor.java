/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import java.util.Set;

/**
 * Creates the behavior for one addon. List this class in
 * {@code META-INF/services/gregtech.api.addon.platform.BehaviorMachineContributor}.
 * {@link #machineTypes()} must cover every behavior machine this addon declares.
 * {@link #create} is called once per placed block entity.
 */
public interface BehaviorMachineContributor {
    String addonId();

    Set<String> machineTypes();

    MachineBehavior create(String machineType);
}
