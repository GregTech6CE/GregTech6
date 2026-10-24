package com.gregtech.gregtech.api.multiblock.tracking;

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

    Footprint watchFootprint(TrackedMultiblock machine);

    Footprint declaredFootprint(TrackedMultiblock machine);

    default void onFormed(TrackedMultiblock machine) {}

    default void onBroken(TrackedMultiblock machine) {}

    enum ClaimPolicy { ATOMIC, PER_CELL }

    non-sealed interface Declarative extends StructureDefinition {}

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
