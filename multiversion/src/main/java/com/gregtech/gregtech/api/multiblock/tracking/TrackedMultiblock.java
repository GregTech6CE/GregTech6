package com.gregtech.gregtech.api.multiblock.tracking;

import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A controller that can answer {@code isStructureOk} from a tracker instead of scanning. */
public interface TrackedMultiblock {
    boolean scanStructure();

    StructureDefinition structureDefinition();

    default BlockEntity trackedEntity() {
        return (BlockEntity) this;
    }

    /** Server-only mirror of the tracker bit for fields the machine already stored itself. */
    default void structureFormedChanged(boolean formed) {}

    /** Part acceptance beyond block identity, as the full scan body applies it. */
    default boolean acceptsTrackedPart(LargeMachineLayouts.Cell cell, Block expected, Block actual) {
        return expected == actual;
    }

    /** Claim a complete incremental match. Controllers that do not override fall back to a full scan. */
    default boolean applyStructure(PatternMatch match) {
        return scanStructure();
    }

    /** A failed incremental match: release what the controller bound for the previous one. */
    default void releaseStructureClaims() {}

    /** An incomplete match. Families with partial tool bindings can retain the matched cells. */
    default void applyIncompleteStructure(PatternMatch match) {
        releaseStructureClaims();
    }

    /** Controller-specific checks that read the world once per evaluation, such as part counts. */
    default boolean structurePartsValid() {
        return true;
    }
}
