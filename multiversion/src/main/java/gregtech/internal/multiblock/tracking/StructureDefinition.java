package gregtech.internal.multiblock.tracking;

import net.minecraft.core.Direction;

import java.util.List;

/** What a controller is made of. Phase 1 uses this for footprints and projections; the scan stays on the controller. */
public sealed interface StructureDefinition permits StructureDefinition.Declarative, StructureDefinition.Custom {
    ClaimPolicy claimPolicy();

    /** No layout: formed on attach, and again on epoch if a layout still has not appeared. */
    default boolean formsImmediately() {
        return false;
    }

    /** After a scan, watch only the cells that scan read, plus the controller. */
    default boolean tightenWatch() {
        return false;
    }

    /** The layout contains tag cells, so a tag reload can change the verdict. */
    default boolean usesTags() {
        return false;
    }

    /** The addon layout name this definition reads, or null when it cannot be overridden. */
    default String overlayMachine() {
        return null;
    }

    Footprint watchFootprint(TrackedMultiblock machine);

    Footprint declaredFootprint(TrackedMultiblock machine);

    enum ClaimPolicy { ATOMIC, PER_CELL }

    non-sealed interface Declarative extends StructureDefinition {
        /** Compiled offsets and matchers for one facing. Rebuilt when the facing or the epoch changes. */
        CompiledPattern compile(Direction front);

        /** Extra constraints over matched cells, evaluated from cached choices. */
        default List<Aggregate> aggregates() {
            return List.of();
        }

        /** False keeps this definition on the full scan path while its family has not migrated yet. */
        default boolean incremental() {
            return true;
        }
    }

    non-sealed interface Custom extends StructureDefinition {}

    /** Behavior machines whose casing has no layout. */
    final class Immediate implements Custom {
        public static final Immediate INSTANCE = new Immediate();

        private Immediate() {}

        @Override public ClaimPolicy claimPolicy() { return ClaimPolicy.ATOMIC; }
        @Override public boolean formsImmediately() { return true; }
        @Override public Footprint watchFootprint(TrackedMultiblock machine) { return Footprint.EMPTY; }
        @Override public Footprint declaredFootprint(TrackedMultiblock machine) { return Footprint.EMPTY; }
    }
}
