package com.gregtech.gregtech.api.multiblock.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Watched cells of one controller. Lookup is a dense index inside the bounding box. */
public class Footprint {
    public static final Footprint EMPTY = new Footprint(new BlockPos[0], new Projection[0], 0, 0, 0, 0, 0, 0,
            new int[0], new long[0], new long[0]);

    private final BlockPos[] cells;
    private final Projection[] projections;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int[] dense;
    private final long[] chunks;
    private final long[] sections;

    private Footprint(BlockPos[] cells, Projection[] projections, int minX, int minY, int minZ,
                      int sizeX, int sizeY, int sizeZ, int[] dense, long[] chunks, long[] sections) {
        this.cells = cells;
        this.projections = projections;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.dense = dense;
        this.chunks = chunks;
        this.sections = sections;
    }

    public int size() {
        return cells.length;
    }

    public BlockPos cell(int index) {
        return cells[index];
    }

    public Projection projection(int index) {
        return projections[index];
    }

    public long[] chunks() {
        return chunks;
    }

    public long[] sections() {
        return sections;
    }

    public int indexOf(BlockPos pos) {
        if (dense.length == 0) return -1;
        int x = pos.getX() - minX;
        int y = pos.getY() - minY;
        int z = pos.getZ() - minZ;
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) return -1;
        int slot = dense[(y * sizeZ + z) * sizeX + x];
        return slot == 0 ? -1 : slot - 1;
    }

    public boolean contains(BlockPos pos) {
        return indexOf(pos) >= 0;
    }

    public static final class Builder {
        private final Map<Long, Integer> order = new HashMap<>();
        private final List<BlockPos> cells = new ArrayList<>();
        private final List<Projection> projections = new ArrayList<>();

        public Builder add(BlockPos pos, Projection projection) {
            BlockPos fixed = pos.immutable();
            long key = fixed.asLong();
            Integer existing = order.get(key);
            if (existing == null) {
                order.put(key, cells.size());
                cells.add(fixed);
                projections.add(projection);
            } else {
                projections.set(existing, Projection.and(projections.get(existing), projection));
            }
            return this;
        }

        public Builder addController(BlockPos pos) {
            return add(pos, Projection.FACING);
        }

        public Footprint build() {
            if (cells.isEmpty()) return EMPTY;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos cell : cells) {
                minX = Math.min(minX, cell.getX());
                minY = Math.min(minY, cell.getY());
                minZ = Math.min(minZ, cell.getZ());
                maxX = Math.max(maxX, cell.getX());
                maxY = Math.max(maxY, cell.getY());
                maxZ = Math.max(maxZ, cell.getZ());
            }
            int sizeX = maxX - minX + 1;
            int sizeY = maxY - minY + 1;
            int sizeZ = maxZ - minZ + 1;
            long volume = (long) sizeX * sizeY * sizeZ;
            int[] dense = volume > 0 && volume <= 8L * cells.size() ? new int[(int) volume] : new int[0];
            Map<Long, Integer> overflow = dense.length == 0 ? new HashMap<>() : null;
            for (int index = 0; index < cells.size(); index++) {
                BlockPos cell = cells.get(index);
                if (dense.length != 0) {
                    int slot = ((cell.getY() - minY) * sizeZ + (cell.getZ() - minZ)) * sizeX + (cell.getX() - minX);
                    dense[slot] = index + 1;
                } else {
                    overflow.put(cell.asLong(), index);
                }
            }
            Map<Long, Boolean> chunkSet = new HashMap<>();
            Map<Long, Boolean> sectionSet = new HashMap<>();
            for (BlockPos cell : cells) {
                chunkSet.put(ChunkPos.asLong(cell.getX() >> 4, cell.getZ() >> 4), Boolean.TRUE);
                sectionSet.put(SectionPos.asLong(SectionPos.blockToSectionCoord(cell.getX()),
                        SectionPos.blockToSectionCoord(cell.getY()),
                        SectionPos.blockToSectionCoord(cell.getZ())), Boolean.TRUE);
            }
            Footprint footprint = new Footprint(cells.toArray(BlockPos[]::new), projections.toArray(Projection[]::new),
                    minX, minY, minZ, sizeX, sizeY, sizeZ, dense, longs(chunkSet), longs(sectionSet));
            if (overflow != null) return new OverflowFootprint(footprint, overflow);
            return footprint;
        }

        private static long[] longs(Map<Long, Boolean> keys) {
            long[] values = new long[keys.size()];
            int index = 0;
            for (Long key : keys.keySet()) values[index++] = key;
            Arrays.sort(values);
            return values;
        }
    }

    /** Hash lookup when a footprint's box is too sparse for a dense array. */
    private static final class OverflowFootprint extends Footprint {
        private final Map<Long, Integer> index;

        private OverflowFootprint(Footprint shape, Map<Long, Integer> index) {
            super(shape.cells, shape.projections, shape.minX, shape.minY, shape.minZ, shape.sizeX, shape.sizeY,
                    shape.sizeZ, shape.dense, shape.chunks, shape.sections);
            this.index = index;
        }

        @Override
        public int indexOf(BlockPos pos) {
            Integer found = index.get(pos.asLong());
            return found == null ? -1 : found;
        }
    }
}
