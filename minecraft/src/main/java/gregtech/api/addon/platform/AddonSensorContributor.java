/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import java.util.List;

/**
 * Sensor kinds for one addon. List this class in
 * {@code META-INF/services/gregtech.api.addon.platform.AddonSensorContributor}.
 * GregTech loads it before block registration.
 */
public interface AddonSensorContributor {
    String addonId();

    List<AddonSensor> sensorKinds();
}
