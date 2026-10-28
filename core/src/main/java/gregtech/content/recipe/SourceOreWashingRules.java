package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Pinned GT6 Ores 383-446 (W 387-450), OM 339-353 and Petrotheum listener rules.
 * The caller supplies actual registration facts: generation flags are not availability.
 * No registries, ownership decisions or recipe mutations are performed here.
 */
public final class SourceOreWashingRules {
    private SourceOreWashingRules() {}

    public enum Availability {
        PRESENT,
        ABSENT,
        UNKNOWN
    }

    public static final int SLUICE_POWER = 16, BATH_POWER = 0, STEAM_PER_WATER = 160;
    public static final boolean WATER_OPTIMIZE = true,
            BATH_OPTIMIZE = true,
            REAGENT_BATH_OPTIMIZE = true,
            PETROTHEUM_OPTIMIZE = false;
    public static final List<Long> SLUICE_CHANCES = List.of(10000L, 300L, 300L, 300L, 300L, 300L, 300L, 300L, 300L);
    public static final List<Long> PETROTHEUM_CHANCES = List.of(10000L, 5000L, 10000L);
    public static final List<Integer> WHOLE_BATH_BYPRODUCT_COUNTS = List.of(2, 2, 2, 2, 1);
    public static final List<Integer> TINY_BATH_BYPRODUCT_COUNTS = List.of(1);

    public enum Size {
        WHOLE(MaterialPrefix.crushed, MaterialPrefix.crushedPurified, MaterialPrefix.dust, 9, 900, 144, 45, 144),
        TINY(
                MaterialPrefix.crushedTiny,
                MaterialPrefix.crushedPurifiedTiny,
                MaterialPrefix.dustTiny,
                1,
                100,
                16,
                5,
                16);
        private final MaterialPrefix input, purified, sand;
        private final int byproductCount, waterMb, mercuryMb, petrotheumMb, ticks;

        Size(
                MaterialPrefix input,
                MaterialPrefix purified,
                MaterialPrefix sand,
                int byproductCount,
                int waterMb,
                int mercuryMb,
                int petrotheumMb,
                int ticks) {
            this.input = input;
            this.purified = purified;
            this.sand = sand;
            this.byproductCount = byproductCount;
            this.waterMb = waterMb;
            this.mercuryMb = mercuryMb;
            this.petrotheumMb = petrotheumMb;
            this.ticks = ticks;
        }

        public MaterialPrefix input() {
            return input;
        }

        public MaterialPrefix purified() {
            return purified;
        }

        public MaterialPrefix sand() {
            return sand;
        }

        public int byproductCount() {
            return byproductCount;
        }

        public int waterMb() {
            return waterMb;
        }

        public int mercuryMb() {
            return mercuryMb;
        }

        public int petrotheumMb() {
            return petrotheumMb;
        }

        public int ticks() {
            return ticks;
        }

        public int steamMb() {
            return waterMb * STEAM_PER_WATER;
        }
    }

    public record BathShape(
            MaterialPrefix input, int inputCount, MaterialPrefix reagent, int reagentCount, Size outputSize) {}

    private static final List<BathShape> BATH_SHAPES = List.of(
            new BathShape(MaterialPrefix.crushed, 1, MaterialPrefix.dust, 1, Size.WHOLE),
            new BathShape(MaterialPrefix.crushedTiny, 9, MaterialPrefix.dust, 1, Size.WHOLE),
            new BathShape(MaterialPrefix.crushed, 1, MaterialPrefix.dustTiny, 9, Size.WHOLE),
            new BathShape(MaterialPrefix.crushedTiny, 1, MaterialPrefix.dustTiny, 1, Size.TINY));

    public static List<BathShape> bathShapes() {
        return BATH_SHAPES;
    }

    public record DustForm(MaterialPrefix prefix, int count) {}

    private static final List<DustForm> WHOLE_SAND_FORMS = List.of(
            new DustForm(MaterialPrefix.dust, 1), new DustForm(MaterialPrefix.dustSmall, 4),
            new DustForm(MaterialPrefix.dustTiny, 9), new DustForm(MaterialPrefix.dustDiv72, 64));
    private static final List<DustForm> TINY_SAND_FORMS =
            List.of(new DustForm(MaterialPrefix.dustTiny, 1), new DustForm(MaterialPrefix.dustDiv72, 8));
    /** OM.dust(SluiceSand,U/U9), distinct from purified same-count fallback.
     * The source bindStack clamps U/U72=72 to64 if only dustDiv72 is registered. */
    public static List<DustForm> sandForms(Size size) {
        Objects.requireNonNull(size);
        return size == Size.WHOLE ? WHOLE_SAND_FORMS : TINY_SAND_FORMS;
    }

    /** A known failing gate dominates unresolved facts, but UNKNOWN is never evidence of absence. */
    public static Availability requireAll(Availability... facts) {
        boolean unknown = false;
        for (Availability fact : facts) {
            if (fact == Availability.ABSENT) return Availability.ABSENT;
            if (fact == null || fact == Availability.UNKNOWN) unknown = true;
        }
        return unknown ? Availability.UNKNOWN : Availability.PRESENT;
    }

    public static Availability oresEligibility(
            boolean valid,
            boolean antimatter,
            boolean bedrock,
            Availability dust,
            Availability whole,
            Availability tiny) {
        return !valid || antimatter || bedrock ? Availability.ABSENT : requireAll(dust, whole, tiny);
    }
    /** Listener callbacks for whole and tiny are independent: no dust or Bedrock gate. */
    public static Availability petroEligibility(boolean valid, boolean antimatter, Availability input) {
        return !valid || antimatter ? Availability.ABSENT : requireAll(input);
    }

    public record FormSelection(Availability availability, MaterialPrefix prefix) {}
    /** OM's direct same-count fallback, never a material-mass conversion or dust*9 projection. */
    public static FormSelection purifiedPrefix(Size size, Function<MaterialPrefix, Availability> forms) {
        Objects.requireNonNull(size);
        Objects.requireNonNull(forms);
        List<MaterialPrefix> order = size == Size.WHOLE
                ? List.of(MaterialPrefix.crushedPurified, MaterialPrefix.crushedCentrifuged, MaterialPrefix.dust)
                : List.of(
                        MaterialPrefix.crushedPurifiedTiny,
                        MaterialPrefix.crushedCentrifugedTiny,
                        MaterialPrefix.dustTiny);
        for (MaterialPrefix prefix : order) {
            Availability available = forms.apply(prefix);
            if (available == Availability.PRESENT) return new FormSelection(available, prefix);
            if (available == null || available == Availability.UNKNOWN)
                return new FormSelection(Availability.UNKNOWN, null);
        }
        return new FormSelection(Availability.ABSENT, null);
    }

    public record Selection<M>(Availability availability, M material) {}
    /**
     * Walk the complete ordered source list, then self. NO and known absent OM tiny forms skip;
     * an unknown earlier candidate defers instead of silently changing first-match semantics.
     */
    public static <M> Selection<M> selectByproduct(
            M self, List<M> ordered, Function<M, SourceOreWashingData.Decision> tag, Function<M, Availability> tiny) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(ordered);
        Objects.requireNonNull(tag);
        Objects.requireNonNull(tiny);
        var candidates = new ArrayList<M>(ordered);
        candidates.add(self);
        for (M candidate : candidates) {
            SourceOreWashingData.Decision decision = tag.apply(candidate);
            if (decision == SourceOreWashingData.Decision.NO) continue;
            Availability available = tiny.apply(candidate);
            if (available == Availability.ABSENT) continue;
            if (decision == null
                    || decision == SourceOreWashingData.Decision.UNKNOWN
                    || available == null
                    || available == Availability.UNKNOWN) return new Selection<>(Availability.UNKNOWN, null);
            return new Selection<>(Availability.PRESENT, candidate);
        }
        return new Selection<>(Availability.ABSENT, null);
    }
    /** Water alone uses the first eight positions, padding with self and retaining duplicates. */
    public static <M> List<M> waterByproducts(M self, List<M> ordered) {
        Objects.requireNonNull(self);
        Objects.requireNonNull(ordered);
        var result = new ArrayList<M>(8);
        for (int index = 0; index < 8; index++) result.add(index < ordered.size() ? ordered.get(index) : self);
        return List.copyOf(result);
    }
}
