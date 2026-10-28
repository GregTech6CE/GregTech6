/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */
package gregtech.content.multiblock;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureGrid;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import java.util.ArrayList;
import java.util.List;

/** GT6 17000: centred side valve, 25 fire bricks, one air cell and a 40 tick ignition window. */
public final class OriginalCokeOvenRules {
    private OriginalCokeOvenRules() {}

    public static final int IGNITION_TICKS = 40;

    private static final List<SharedLargeMachineLayouts.Cell> CELLS =
            MultiblockPatterns.cells(StructureLayout.pattern("cokeoven")
                    .layer(-1, "BBB", "BBB", "BBB")
                    .layer(0, "BSB", "B B", "BBB")
                    .layer(1, "BBB", "BBB", "BBB")
                    .where('B', CellMatch.part("fire_bricks"), StructureRole.ITEM_FLUID_ENERGY)
                    .where(' ', CellMatch.air(), StructureRole.AIR)
                    .count('B', 25, 25)
                    .count(' ', 1, 1));

    public static List<SharedLargeMachineLayouts.Cell> cells() {
        return CELLS;
    }

    public static List<StructureGrid.Position> fluidTargets() {
        var targets = new ArrayList<StructureGrid.Position>();
        for (int x = -1; x <= 1; x++) for (int z = 0; z <= 2; z++) targets.add(new StructureGrid.Position(x, -2, z));
        return List.copyOf(targets);
    }

    public static int tickIgnition(int remaining) {
        return Math.max(0, Math.min(IGNITION_TICKS, remaining) - 1);
    }
}
