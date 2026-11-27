package gregtech.content.worldgen;

/** Spring catalog and crater write order shared by the platform features.
 * The source amount is a per-tick roll bound, not a drainable tank capacity. */
public final class FluidSpringRules {

    private static final int CRATER_LAYERS = 7;
    private static final int CRATER_SPRING_CHANCE = 16;

    private FluidSpringRules() {}

    public record Spring(String name, String fluidId, String blockId, int probability, int indicatorType, int amount) {}

    public static final java.util.List<Spring> SPRINGS = java.util.List.of(
            new Spring(
                    "overworld.fluid.oil.extraheavy",
                    "gt6ce:liquid_extra_heavy_oil",
                    "gt6ce:liquid_extra_heavy_oil",
                    400,
                    2,
                    6000),
            new Spring("overworld.fluid.oil.heavy", "gt6ce:liquid_heavy_oil", "gt6ce:liquid_heavy_oil", 400, 2, 6000),
            new Spring(
                    "overworld.fluid.oil.medium", "gt6ce:liquid_medium_oil", "gt6ce:liquid_medium_oil", 400, 2, 6000),
            new Spring("overworld.fluid.oil.light", "gt6ce:liquid_light_oil", "gt6ce:liquid_light_oil", 400, 2, 6000),
            new Spring("overworld.fluid.gas.natural", "gt6ce:gas_natural_gas", "gt6ce:gas_natural_gas", 200, 1, 3000),
            new Spring("overworld.fluid.water", "gt6ce:watergeothermal", "gt6ce:watergeothermal", 100, 3, 500),
            new Spring("overworld.fluid.lava", "minecraft:lava", "minecraft:lava", 200, 1, 1000));

    /** Returns the fixed spring definition, or {@code null} for an unknown or null fluid ID. */
    public static Spring byFluid(String id) {
        return SPRINGS.stream().filter(s -> s.fluidId().equals(id)).findFirst().orElse(null);
    }

    public static String blockPath(Spring spring) {
        return "fluid_spring_" + spring.fluidId().substring(spring.fluidId().indexOf(':') + 1);
    }

    public static int positiveAmount(int amount) {
        return Math.max(1, amount);
    }

    public static boolean rolls(java.util.function.IntUnaryOperator random, int amount) {
        return random.applyAsInt(positiveAmount(amount)) == 0;
    }

    public interface CraterSink {
        void filler(int x, int relativeY, int z);

        boolean fluid(int x, int relativeY, int z);

        boolean bedrock(int x, int z);

        boolean spring(int x, int z);
    }

    /** Returns whether the field placed any fluid or a spring outlet. */
    public static boolean carve(int minX, int minZ, java.util.function.IntUnaryOperator random, CraterSink sink) {
        boolean placed = false;
        for (int i = 0; i < CRATER_LAYERS; i++)
            for (int x = minX + i; x <= minX + 15 - i; x++)
                for (int z = minZ + i; z <= minZ + 15 - i; z++) {
                    sink.filler(x, 1 + i, z);
                    if (i > 0) placed |= sink.fluid(x, i, z);
                    if (i > 2 && random.applyAsInt(CRATER_SPRING_CHANCE) == 0 && sink.bedrock(x, z)) {
                        placed |= sink.spring(x, z);
                    }
                }
        return placed;
    }
}
