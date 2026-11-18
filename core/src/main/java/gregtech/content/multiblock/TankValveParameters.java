package gregtech.content.multiblock;

import static gregtech.engine.fluid.FluidHazard.ACID;
import static gregtech.engine.fluid.FluidHazard.GAS;
import static gregtech.engine.fluid.FluidHazard.MAGIC;
import static gregtech.engine.fluid.FluidHazard.PLASMA;

import gregtech.engine.fluid.FluidHazard;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GT6 {@code Loader_MultiTileEntities} 17001–17067. The original numeric IDs
 * remain at this registration boundary; each playable valve uses its own wall
 * design and fluid limits rather than a generic tank tier.
 */
public record TankValveParameters(
        int originalId,
        int size,
        int wallId,
        long capacity,
        String material,
        Set<FluidHazard> protections,
        boolean simpleOnly,
        float hardness) {

    private record Grade(String material, float hardness, Set<FluidHazard> protections, boolean simpleOnly) {
        private Grade(String material, float hardness, FluidHazard... protections) {
            this(material, hardness, Set.of(protections), false);
        }

        private static Grade simpleFluidsOnly(String material, float hardness) {
            return new Grade(material, hardness, Set.of(), true);
        }
    }

    private static final Grade TREATED_WOOD = Grade.simpleFluidsOnly("WoodTreated", 5);
    private static final Grade STAINLESS_STEEL = new Grade("StainlessSteel", 6, GAS, ACID);
    private static final Grade TUNGSTEN_STEEL = new Grade("TungstenSteel", 12.5F, GAS, MAGIC);
    private static final Grade TUNGSTEN = new Grade("Tungsten", 10, GAS, ACID, MAGIC);
    private static final Grade ADAMANTIUM = new Grade("Adamantium", 100, GAS, ACID, PLASMA, MAGIC);
    private static final Grade TITANIUM = new Grade("Titanium", 9, GAS);
    private static final Grade INVAR = new Grade("Invar", 6, GAS);

    private static final Map<Integer, TankValveParameters> BY_ID = definitions();

    public TankValveParameters {
        protections = Set.copyOf(protections);
    }

    private static Map<Integer, TankValveParameters> definitions() {
        Map<Integer, TankValveParameters> all = new LinkedHashMap<>();
        put(all, 17001, 3, 18001, 432_000L, TREATED_WOOD);
        put(all, 17002, 3, 18002, 1_728_000L, STAINLESS_STEEL);
        put(all, 17003, 3, 18003, 6_912_000L, TUNGSTEN_STEEL);
        put(all, 17004, 3, 18004, 6_912_000L, TUNGSTEN);
        put(all, 17005, 3, 18005, 110_592_000L, ADAMANTIUM);
        put(all, 17006, 3, 18006, 3_456_000L, TITANIUM);
        put(all, 17007, 3, 18007, 1_728_000L, INVAR);
        put(all, 17022, 3, 18022, 6_912_000L, STAINLESS_STEEL);
        put(all, 17023, 3, 18023, 27_648_000L, TUNGSTEN_STEEL);
        put(all, 17024, 3, 18024, 27_648_000L, TUNGSTEN);
        put(all, 17025, 3, 18025, 442_368_000L, ADAMANTIUM);
        put(all, 17026, 3, 18026, 13_824_000L, TITANIUM);
        put(all, 17027, 3, 18027, 6_912_000L, INVAR);
        put(all, 17042, 5, 18002, 8_000_000L, STAINLESS_STEEL);
        put(all, 17043, 5, 18003, 32_000_000L, TUNGSTEN_STEEL);
        put(all, 17044, 5, 18004, 32_000_000L, TUNGSTEN);
        put(all, 17045, 5, 18005, 512_000_000L, ADAMANTIUM);
        put(all, 17046, 5, 18006, 16_000_000L, TITANIUM);
        put(all, 17047, 5, 18007, 8_000_000L, INVAR);
        put(all, 17062, 5, 18022, 32_000_000L, STAINLESS_STEEL);
        put(all, 17063, 5, 18023, 128_000_000L, TUNGSTEN_STEEL);
        put(all, 17064, 5, 18024, 128_000_000L, TUNGSTEN);
        put(all, 17065, 5, 18025, 2_048_000_000L, ADAMANTIUM);
        put(all, 17066, 5, 18026, 64_000_000L, TITANIUM);
        put(all, 17067, 5, 18027, 32_000_000L, INVAR);
        return Map.copyOf(all);
    }

    private static void put(
            Map<Integer, TankValveParameters> all, int id, int size, int wall, long capacity, Grade grade) {
        var spec = new TankValveParameters(
                id, size, wall, capacity, grade.material(), grade.protections(), grade.simpleOnly(), grade.hardness());
        if (all.putIfAbsent(id, spec) != null) throw new IllegalStateException("Duplicate GT6 tank valve " + id);
    }

    public static TankValveParameters find(int originalId) {
        return BY_ID.get(originalId);
    }

    public static List<TankValveParameters> all() {
        return List.copyOf(BY_ID.values());
    }

    public int meltingPoint() {
        return GTMaterialRegistry.get(material).getMeltingPoint();
    }
}
