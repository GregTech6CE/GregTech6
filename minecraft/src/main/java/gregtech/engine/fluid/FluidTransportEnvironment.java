package gregtech.engine.fluid;

import gregtech.content.data.GregTechConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

/** Environment temperature helper for fluid transport blocks. */
public final class FluidTransportEnvironment {
    private FluidTransportEnvironment() {}

    public static long environmentTemperature(LevelReader level, BlockPos pos) {
        return level == null
                ? GregTechConstants.DEF_ENV_TEMP
                : GregTechConstants.C + Math.round(level.getBiome(pos).value().getBaseTemperature() * 20F);
    }
}
