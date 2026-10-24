package gregtech.internal.multiblock.tracking;

/** Mixin duck for the section bitset stored on {@code LevelChunk}. */
public interface StructureChunkWatch {
    void gt$structureWatch(int sectionIndex, boolean watched);

    boolean gt$structureWatched(int sectionIndex);
}
