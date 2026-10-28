package gregtech.engine.nuclear;

import gregtech.content.nuclear.ReactorRodCatalog;

/** Shared neutron heat and lifetime arithmetic, with bounded conversion to int. */
public final class RodPhysicsMath {
    private RodPhysicsMath() {}

    public static int bound(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }

    public static long ceil(long numerator, long denominator) {
        return numerator / denominator + (numerator % denominator == 0 ? 0 : 1);
    }

    public static long heat(ReactorRodCatalog.Kind kind, int flux) {
        return switch (kind) {
            case NUCLEAR -> flux;
            case ABSORBER -> 2L * flux;
            case BREEDER, PRODUCT -> flux / 2;
            default -> 0;
        };
    }

    public static long loss(ReactorRodCatalog.Kind kind, int flux, long maximum, boolean moderated) {
        long loss = kind == ReactorRodCatalog.Kind.BREEDER
                ? flux
                : flux <= maximum ? 100 : ceil(400L * flux, Math.max(1, maximum));
        return kind == ReactorRodCatalog.Kind.NUCLEAR && moderated ? loss * 4 : loss;
    }
}
