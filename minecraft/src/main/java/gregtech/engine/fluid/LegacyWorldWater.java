package gregtech.engine.fluid;

import java.util.Set;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Save aliases only: oceans and rivers now use vanilla water, while geothermal water remains distinct. */
public final class LegacyWorldWater {
    public static final Set<String> BLOCK_PATHS = Set.of("riverwater", "seawater");

    private LegacyWorldWater() {}

    public static Fluid fluid(String path) {
        if (BLOCK_PATHS.contains(path)) return Fluids.WATER;
        if (path.endsWith("_flowing") && BLOCK_PATHS.contains(path.substring(0, path.length() - 8))) {
            return Fluids.FLOWING_WATER;
        }
        return null;
    }
}
