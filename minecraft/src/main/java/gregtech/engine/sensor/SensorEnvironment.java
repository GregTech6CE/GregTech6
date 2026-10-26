package gregtech.engine.sensor;

import gregtech.content.data.GregTechConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;

/** GT6 WD.temperature fallback for blocks without a temperature provider, including air. */
public final class SensorEnvironment {
    private SensorEnvironment() {}

    public static long temperature(Level level, BlockPos pos) {
        long value = SensorMeasurements.environmentKelvin(level.getBiome(pos).value().getBaseTemperature());
        value = nearbyHeat(level, pos, value);
        for (var side : Direction.values()) value = nearbyHeat(level, pos.relative(side), value);
        return value;
    }

    private static long nearbyHeat(Level level, BlockPos pos, long value) {
        if (!level.hasChunkAt(pos)) return value;
        var state = level.getBlockState(pos);
        if (state.getFluidState().is(FluidTags.LAVA)) return Math.max(value, GregTechConstants.C + 500);
        if (state.getBlock() instanceof BaseFireBlock) return Math.max(value, GregTechConstants.C + 200);
        return value;
    }
}
