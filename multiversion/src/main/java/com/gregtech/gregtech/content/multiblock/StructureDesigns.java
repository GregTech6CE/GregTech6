package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.List;

/** Writes a cell's design index onto the part when a structure claims it. */
public final class StructureDesigns {
    private StructureDesigns() {}

    public static void apply(Level level, BlockPos origin, Direction front, List<LargeMachineLayouts.Cell> cells) {
        if (level == null || cells == null || origin == null || front == null) return;
        for (LargeMachineLayouts.Cell cell : cells) {
            if (cell.design() < 0 || cell.plainBlock()) continue;
            BlockPos pos = cell.at(origin, front);
            if (level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)
                part.applyStructureDesign(cell.design());
        }
    }
}
