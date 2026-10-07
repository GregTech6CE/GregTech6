/* Copyright (c) 2019-2025 GregTech-6 Team; Gregorius Techneticies.
 * LGPL-3.0-or-later. Source EnergyConverter.setAdjacentOnOff and DistillationTower requests. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import java.util.List;
import java.util.ArrayList;

/** Source contracts only; world lookup and actual output-face checks belong to each platform. */
public final class OriginalAdjacentEnergyRules {
    private OriginalAdjacentEnergyRules() {}
    public record Position(int right, int up, int back) {}
    public static final List<Position> TOWER_SOURCES;
    static {
        var positions = new ArrayList<Position>();
        for (int right = -1; right <= 1; right++) for (int back = 0; back <= 2; back++)
            positions.add(new Position(right, -2, back));
        TOWER_SOURCES = List.copyOf(positions);
    }
    /** These imported source families all explicitly register WASTE_ENERGY=T. */
    public static boolean respondsToAdjacent(EnergyNodeSpec spec) {
        return spec != null && (OriginalRotaryConverter.handles(spec) || OriginalThermalConverter.handles(spec)
                || MagnetMachineDefinitions.handles(spec) || OriginalSteamTurbines.handles(spec));
    }
}
