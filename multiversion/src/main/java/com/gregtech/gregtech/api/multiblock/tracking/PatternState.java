package com.gregtech.gregtech.api.multiblock.tracking;

import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cached per-cell verdicts for one {@link CompiledPattern}. A cell whose {@code seen} reference is
 * {@code null} is unknown and is re-read on the next evaluation; everything else is reused.
 */
public final class PatternState {
    private static final int NONE = Integer.MAX_VALUE;

    private final CompiledPattern pattern;
    private final int capacity;
    private final int[] groupLimit;
    private final int[] groupFirst;
    private final int[] groupCells;
    private final int[] copies;
    private final int[] groupDirty;

    private final int[] fixedChoice;
    private final BlockState[] fixedState;
    private int fixedFailures;

    private final int[] repeatChoice;
    private final BlockState[] repeatState;

    public PatternState(CompiledPattern pattern, int worldHeight) {
        this.pattern = pattern;
        this.capacity = pattern.capacity(worldHeight);
        this.groupLimit = new int[pattern.groupCount()];
        this.groupFirst = new int[pattern.groupCount()];
        this.groupCells = new int[pattern.groupCount()];
        for (int group = 0; group < groupLimit.length; group++) {
            groupLimit[group] = pattern.groupLimit(group, worldHeight);
            groupFirst[group] = pattern.groupFirstCell(group);
            groupCells[group] = pattern.groupCellCount(group);
        }
        this.copies = new int[groupLimit.length];
        this.groupDirty = new int[groupLimit.length];
        this.fixedChoice = new int[pattern.fixedCount()];
        this.fixedState = new BlockState[pattern.fixedCount()];
        this.repeatChoice = new int[pattern.repeatCellCount() * capacity];
        this.repeatState = new BlockState[pattern.repeatCellCount() * capacity];
        reset();
    }

    public CompiledPattern pattern() {
        return pattern;
    }

    public int capacity() {
        return capacity;
    }

    /** Everything is unknown again: first evaluation, rotation, or a new epoch. */
    public void reset() {
        java.util.Arrays.fill(fixedChoice, CompiledPattern.UNKNOWN);
        java.util.Arrays.fill(fixedState, null);
        fixedFailures = 0;
        java.util.Arrays.fill(copies, 0);
        java.util.Arrays.fill(groupDirty, 0);
        java.util.Arrays.fill(repeatChoice, CompiledPattern.UNKNOWN);
        java.util.Arrays.fill(repeatState, null);
    }

    /** Marks the cells that changed, plus every cell of a chunk that reloaded. */
    public void applyChanges(BlockPos origin, Iterable<Long> changedCells, Iterable<Long> reloadedChunks) {
        if (changedCells != null) {
            for (long packed : changedCells) {
                BlockPos pos = BlockPos.of(packed);
                int dx = pos.getX() - origin.getX();
                int dy = pos.getY() - origin.getY();
                int dz = pos.getZ() - origin.getZ();
                pattern.locate(dx, dy, dz, this::invalidate);
            }
        }
        if (reloadedChunks != null) {
            for (long chunk : reloadedChunks) invalidateChunk(origin, chunk);
        }
    }

    private void invalidate(boolean repeat, int index, int copy) {
        if (!repeat) {
            fixedState[index] = null;
            return;
        }
        if (copy >= capacity) return;
        int slot = index * capacity + copy;
        repeatState[slot] = null;
        int group = pattern.repeatGroup(index);
        groupDirty[group] = Math.min(groupDirty[group], copy);
    }

    private void invalidateChunk(BlockPos origin, long chunk) {
        int chunkX = (int) (chunk & 0xFFFFFFFFL);
        int chunkZ = (int) (chunk >>> 32);
        for (int index = 0; index < pattern.fixedCount(); index++) {
            BlockPos pos = origin.offset(pattern.dx(index), pattern.dy(index), pattern.dz(index));
            if ((pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ) fixedState[index] = null;
        }
        for (int cell = 0; cell < pattern.repeatCellCount(); cell++) {
            int group = pattern.repeatGroup(cell);
            int limit = groupLimit[group];
            int dx = pattern.repeatDx(cell);
            int dz = pattern.repeatDz(cell);
            if ((((origin.getX() + dx) >> 4) != chunkX) || (((origin.getZ() + dz) >> 4) != chunkZ)) {
                // A column either sits in this chunk for every copy or for none of them.
                continue;
            }
            for (int copy = 0; copy < limit; copy++) {
                int slot = cell * capacity + copy;
                repeatState[slot] = null;
                groupDirty[group] = Math.min(groupDirty[group], copy);
            }
        }
    }

    public void evaluate(Level level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int index = 0; index < pattern.fixedCount(); index++)
            if (fixedState[index] == null) evaluateFixed(level, origin, cursor, index);
        for (int group = 0; group < copies.length; group++)
            if (groupDirty[group] != NONE) evaluateGroup(level, origin, cursor, group);
    }

    private void evaluateFixed(Level level, BlockPos origin, BlockPos.MutableBlockPos cursor, int index) {
        cursor.setWithOffset(origin, pattern.dx(index), pattern.dy(index), pattern.dz(index));
        StructureScanProbe.matcher();
        BlockState state;
        int choice;
        if (!level.hasChunkAt(cursor)) {
            state = null;
            choice = CompiledPattern.FAILED;
        } else {
            state = level.getBlockState(cursor);
            choice = pattern.air(index)
                    ? (state.isAir() ? 0 : CompiledPattern.FAILED)
                    : pattern.matcher(index).match(level, cursor, state);
        }
        int before = fixedChoice[index];
        if (before == CompiledPattern.FAILED) fixedFailures--;
        if (choice == CompiledPattern.FAILED) fixedFailures++;
        fixedChoice[index] = choice;
        fixedState[index] = state;
    }

    private void evaluateGroup(Level level, BlockPos origin, BlockPos.MutableBlockPos cursor, int group) {
        int limit = groupLimit[group];
        // Copies up to the old frontier were inside the watch footprint, so their cached verdicts are
        // still valid. Anything above it was outside the watch (the footprint shrinks after a break)
        // and must be read again even when a cached value exists.
        int validThrough = copies[group];
        int start = Math.min(groupDirty[group], copies[group]);
        if (start > limit) start = limit;
        copies[group] = Math.min(copies[group], start);
        int first = groupFirst[group];
        int count = groupCells[group];
        for (int copy = start; copy < limit; copy++) {
            boolean complete = true;
            for (int cell = first; cell < first + count; cell++) {
                int slot = cell * capacity + copy;
                if (copy > validThrough || repeatState[slot] == null)
                    evaluateRepeat(level, origin, cursor, cell, copy, slot);
                if (repeatChoice[slot] == CompiledPattern.FAILED) {
                    complete = false;
                    break;
                }
            }
            if (!complete) break;
            copies[group] = copy + 1;
        }
        groupDirty[group] = NONE;
    }

    private void evaluateRepeat(Level level, BlockPos origin, BlockPos.MutableBlockPos cursor,
                                int cell, int copy, int slot) {
        cursor.setWithOffset(origin, pattern.repeatDx(cell), pattern.repeatDy(cell, copy), pattern.repeatDz(cell));
        StructureScanProbe.matcher();
        BlockState state;
        int choice;
        if (!level.hasChunkAt(cursor)) {
            state = null;
            choice = CompiledPattern.FAILED;
        } else {
            state = level.getBlockState(cursor);
            choice = pattern.repeatAir(cell)
                    ? (state.isAir() ? 0 : CompiledPattern.FAILED)
                    : pattern.repeatMatcher(cell).match(level, cursor, state);
        }
        repeatChoice[slot] = choice;
        repeatState[slot] = state;
    }

    public boolean complete() {
        if (fixedFailures != 0) return false;
        for (int group = 0; group < copies.length; group++)
            if (copies[group] < pattern.groupMinimum(group)) return false;
        if (!pattern.aggregates().isEmpty()) {
            Cells view = new Cells();
            for (Aggregate aggregate : pattern.aggregates())
                if (!aggregate.satisfied(view)) return false;
        }
        return true;
    }

    public int[] matchedLayers() {
        return copies.clone();
    }

    /** The complete match, in the order the full scan body builds its claim lists. */
    public PatternMatch match(BlockPos origin) {
        List<PatternMatch.FixedCell> fixed = new ArrayList<>(pattern.fixedCount());
        for (int index = 0; index < pattern.fixedCount(); index++) {
            int choice = fixedChoice[index];
            if (choice < 0) continue;
            fixed.add(new PatternMatch.FixedCell(pattern.cell(index, choice),
                    origin.offset(pattern.dx(index), pattern.dy(index), pattern.dz(index))));
        }
        List<LargeMachineLayouts.Placed> placed = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int group = 0; group < copies.length; group++) {
            counts.put(pattern.groupKey(group), copies[group]);
            int first = groupFirst[group];
            int count = groupCells[group];
            for (int copy = 0; copy < copies[group]; copy++) {
                for (int cell = first; cell < first + count; cell++) {
                    int choice = repeatChoice[cell * capacity + copy];
                    if (choice < 0) continue;
                    placed.add(new LargeMachineLayouts.Placed(pattern.repeatCell(cell, choice),
                            origin.offset(pattern.repeatDx(cell), pattern.repeatDy(cell, copy), pattern.repeatDz(cell))));
                }
            }
        }
        return new PatternMatch(fixed, placed, counts);
    }

    private final class Cells implements Aggregate.Cells {
        @Override public int size() {
            int size = pattern.fixedCount();
            for (int group = 0; group < copies.length; group++) size += copies[group] * groupCells[group];
            return size;
        }

        @Override public int choice(int index) {
            if (index < pattern.fixedCount()) return fixedChoice[index];
            Slot slot = slot(index);
            return slot == null ? CompiledPattern.FAILED : repeatChoice[slot.cell * capacity + slot.copy];
        }

        @Override public int part(int index) {
            int choice = choice(index);
            if (index < pattern.fixedCount())
                return pattern.part(index, choice < 0 ? 0 : choice);
            Slot slot = slot(index);
            return slot == null ? 0 : pattern.repeatPart(slot.cell, choice < 0 ? 0 : choice);
        }

        @Override public String plain(int index) {
            int choice = choice(index);
            if (index < pattern.fixedCount())
                return pattern.plain(index, choice < 0 ? 0 : choice);
            Slot slot = slot(index);
            return slot == null ? null : pattern.repeatPlain(slot.cell, choice < 0 ? 0 : choice);
        }

        @Override public com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role role(int index) {
            if (index < pattern.fixedCount()) return pattern.role(index);
            Slot slot = slot(index);
            return slot == null ? null : pattern.repeatRole(slot.cell);
        }

        @Override public Block block(int index) {
            BlockState state;
            if (index < pattern.fixedCount()) {
                state = fixedState[index];
            } else {
                Slot slot = slot(index);
                if (slot == null) return null;
                state = repeatState[slot.cell * capacity + slot.copy];
            }
            return state == null ? null : state.getBlock();
        }

        private Slot slot(int index) {
            int ordinal = index - pattern.fixedCount();
            for (int group = 0; group < copies.length; group++) {
                int count = copies[group] * groupCells[group];
                if (ordinal < count) {
                    int cell = ordinal / copies[group];
                    int copy = ordinal % copies[group];
                    return new Slot(groupFirst[group] + cell, copy);
                }
                ordinal -= count;
            }
            return null;
        }
    }

    private record Slot(int cell, int copy) {}
}
