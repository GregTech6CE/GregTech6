package com.gregtech.gregtech.worldgen.center;

/** Platform operations used by the original Nexus and streets algorithms. Writes are chunk scoped. */
public interface OriginWorld {
    record Block(String id, int slabSide, boolean opaque, boolean liquid, boolean wood, boolean leaves, boolean water) {
        public Block(String id) { this(id, -1, true, false, false, false, false); }
        public Block slab(int side) { return new Block(id, side, false, false, false, false, false); }
    }
    record Biome(String biomeName, String displayName) {}
    record Chunk(OriginWorld world, int minX, int minZ) {}
    int minY();
    default boolean canWrite(int x, int y, int z) { return true; }
    Block getBlock(int x, int y, int z);
    boolean setBlock(int x, int y, int z, Block block, int metadata, int flags);
    Biome getBiomeGenForCoords(int x, int z);
    boolean isInfiniteWaterBiome(String name);
    void setSpawnLocation(int x, int y, int z);
    void sign(int x, int y, int z, int side, int rotation, String... lines);
    void beacon(int x, int y, int z, String primary, String secondary);
    void clearNonPlayerEntities(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);
}
