/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;




import java.util.List;

/**
 * Built-in machine overrides for one addon. List this class in
 * {@code META-INF/services/gregtech.api.addon.machine.MachineTuningContributor}.
 * GregTech loads it before the built-in catalog is turned into specs.
 * A second addon may repeat the same value; a different value for the same field fails startup.
 */
public interface MachineTuningContributor {
    String addonId();

    List<MachineTuning> tunings();
}
