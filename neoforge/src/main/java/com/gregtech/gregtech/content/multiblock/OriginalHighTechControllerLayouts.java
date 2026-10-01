package com.gregtech.gregtech.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/** Finite JEI previews of the variable lightning rod and the original bedrock drill. */
public final class OriginalHighTechControllerLayouts {
    private OriginalHighTechControllerLayouts() {}

    /** GT6 17998: five alternating 3x3 layers, controller at bottom center, then one rod segment. */
    public static Map<BlockPos, Block> lightningRod() {
        var cells = new LinkedHashMap<BlockPos, Block>();
        for (int y = 0; y < 5; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            cells.put(new BlockPos(x, y, z), LargeMachineParts.block(y % 2 == 0 ? 18004 : 18041));
        }
        cells.put(new BlockPos(0, 5, 0), LargeMachineParts.block(18104));
        return Map.copyOf(cells);
    }

    /** GT6 17999: 3x3 bedrock floor, nine drill heads and 35 dense-titanium wall cells. */
    public static Map<BlockPos, Block> bedrockDrill() {
        var cells = new LinkedHashMap<BlockPos, Block>();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = -5; y <= 0; y++) {
            if (x == 0 && y == 0 && z == 0) continue;
            cells.put(new BlockPos(x, y, z), y == -5 ? Blocks.BEDROCK
                    : LargeMachineParts.block(y == -4 ? 18103 : 18026));
        }
        return Map.copyOf(cells);
    }
}
