package com.gregtech.gregtech.api.multiblock.tracking;

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
}
