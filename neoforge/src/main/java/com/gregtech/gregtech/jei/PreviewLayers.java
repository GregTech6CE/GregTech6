package com.gregtech.gregtech.jei;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;

/** Physical floors, numbered from one at the structure bottom; coordinates stay internal. */
public final class PreviewLayers {
    private PreviewLayers() {}
    public static List<Integer> levels(Collection<BlockPos> cells) {
        if (cells.isEmpty()) return List.of();
        var bounds = cells.stream().mapToInt(BlockPos::getY).summaryStatistics();
        return IntStream.rangeClosed(bounds.getMin(), bounds.getMax()).boxed().toList();
    }
}
