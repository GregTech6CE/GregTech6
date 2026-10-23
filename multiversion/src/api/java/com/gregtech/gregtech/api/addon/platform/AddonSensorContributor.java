package com.gregtech.gregtech.api.addon.platform;

import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Sensor kinds for one addon. List this class in
 * {@code META-INF/services/com.gregtech.gregtech.api.addon.platform.AddonSensorContributor}.
 * GregTech loads it before block registration.
 */
public interface AddonSensorContributor {
    String addonId();

    List<AddonSensor> sensorKinds();
}
