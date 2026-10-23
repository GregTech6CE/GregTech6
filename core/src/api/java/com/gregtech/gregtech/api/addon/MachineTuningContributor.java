package com.gregtech.gregtech.api.addon;

import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * Built-in machine overrides for one addon. List this class in
 * {@code META-INF/services/com.gregtech.gregtech.api.addon.MachineTuningContributor}.
 * GregTech loads it before the built-in catalog is turned into specs.
 * A second addon may repeat the same value; a different value for the same field fails startup.
 */
public interface MachineTuningContributor {
    String addonId();

    List<MachineTuning> tunings();
}
