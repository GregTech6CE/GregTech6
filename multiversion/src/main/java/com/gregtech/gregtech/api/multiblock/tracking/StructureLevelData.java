package com.gregtech.gregtech.api.multiblock.tracking;

/** Per-world tracking state supplied by a mixin on {@code ServerLevel}. */
public interface StructureLevelData {
    StructureWatchIndex.WorldData gt$structureData();

    void gt$structureData(StructureWatchIndex.WorldData data);
}
