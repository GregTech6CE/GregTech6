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

package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.StructureGrid;
import java.util.ArrayList;
import java.util.List;

/** GT6 17000: centred side valve, 25 fire bricks, one air cell and a 40 tick ignition window. */
public final class OriginalCokeOvenRules {
    private OriginalCokeOvenRules() {}
    public static final int IGNITION_TICKS = 40;
    public static List<SharedLargeMachineLayouts.Cell> cells() {
        var cells = new ArrayList<SharedLargeMachineLayouts.Cell>();
        for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=0;z<=2;z++) {
            if (x==0 && y==0 && z==0) continue;
            boolean air=x==0 && y==0 && z==1;
            cells.add(new SharedLargeMachineLayouts.Cell(x,y,z,air?0:18000,
                    air?StructureGrid.Role.AIR:StructureGrid.Role.ITEM_FLUID_ENERGY));
        }
        return List.copyOf(cells);
    }
    public static List<StructureGrid.Position> fluidTargets() {
        var targets = new ArrayList<StructureGrid.Position>();
        for (int x=-1;x<=1;x++) for (int z=0;z<=2;z++) targets.add(new StructureGrid.Position(x,-2,z));
        return List.copyOf(targets);
    }
    public static int tickIgnition(int remaining) { return Math.max(0,Math.min(IGNITION_TICKS,remaining)-1); }
}
