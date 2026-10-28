package gregtech.content.blockentity.machine;

import gregtech.api.material.GTValues;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** GT6 small crucible rain collection; large crucibles retain their separate tenfold catchment. */
public final class SmallCrucibleRain {
    private SmallCrucibleRain() {}

    public static long amount(Level level, BlockPos pos) {
        if (level == null || level.isClientSide || level.getGameTime() % 600 != 10 || !level.isRainingAt(pos.above()))
            return 0;
        var biome = level.getBiome(pos).value();
        float downfall = biome.getModifiedClimateSettings().downfall();
        if (downfall <= 0 || biome.getBaseTemperature() < .2F) return 0;
        return (GTValues.U / 1000) * Math.max(1, (long) (downfall * 100)) * (level.isThundering() ? 2 : 1);
    }
}
