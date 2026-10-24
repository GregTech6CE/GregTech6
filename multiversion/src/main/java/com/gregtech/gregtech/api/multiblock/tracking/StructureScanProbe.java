package com.gregtech.gregtech.api.multiblock.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Arrays;

/**
 * Counts structure reads while a scan is on the stack. The counters stay at zero unless a test enables them.
 * Read coordinates are kept for the test-build footprint audit only; the watch footprint comes from scan results.
 */
public final class StructureScanProbe {
    private static boolean enabled;
    private static boolean audit;
    private static int activeScans;
    private static int threadLookups;
    private static int scans;
    private static int blockReads;
    private static int entityReads;
    private static int chunkChecks;
    private static int matchers;
    private static int binds;
    private static int releases;
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<java.util.ArrayDeque<Integer>> PAUSES =
            ThreadLocal.withInitial(java.util.ArrayDeque::new);
    private static final ThreadLocal<long[]> LAST = new ThreadLocal<>();
    private static final ThreadLocal<long[]> LAST_CHUNKS = new ThreadLocal<>();
    private static final ThreadLocal<String[]> LAST_CALLERS = new ThreadLocal<>();

    private StructureScanProbe() {}

    public static void enable(boolean on) {
        enabled = on;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void audit(boolean on) {
        audit = on;
    }

    public static boolean auditing() {
        return audit;
    }

    public static void reset() {
        scans = 0;
        threadLookups = 0;
        blockReads = 0;
        entityReads = 0;
        chunkChecks = 0;
        matchers = 0;
        binds = 0;
        releases = 0;
    }

    public static int scans() { return scans; }
    public static int threadLookups() { return threadLookups; }
    public static int blockReads() { return blockReads; }
    public static int entityReads() { return entityReads; }
    public static int chunkChecks() { return chunkChecks; }
    public static int matchers() { return matchers; }
    public static int binds() { return binds; }
    public static int releases() { return releases; }

    public static boolean inScan() {
        Frame frame = CURRENT.get();
        return frame != null && frame.depth > 0;
    }

    /**
     * Opens a frame for incremental evaluation: reads and matchers are counted, but the frame is not
     * a scan. Nested frames share the outer one, and {@link #close()} restores it as usual.
     */
    public static void openIncremental() {
        Frame frame = CURRENT.get();
        if (frame == null) {
            frame = new Frame();
            CURRENT.set(frame);
            activeScans++;
        }
        frame.depth++;
    }

    /** Opens a scan frame. Nested calls share the outer frame so a scan counts once. */
    public static void open() {
        Frame frame = CURRENT.get();
        if (frame == null) {
            frame = new Frame();
            CURRENT.set(frame);
            activeScans++;
            if (enabled) scans++;
        }
        frame.depth++;
    }

    public static void close() {
        Frame frame = CURRENT.get();
        if (frame == null) return;
        frame.depth--;
        if (frame.depth > 0) return;
        LAST.set(java.util.Arrays.copyOf(frame.packed, frame.count));
        LAST_CHUNKS.set(java.util.Arrays.copyOf(frame.chunks, frame.chunkCount));
        LAST_CALLERS.set(frame.callers == null ? null : java.util.Arrays.copyOf(frame.callers, frame.count));
        CURRENT.remove();
        activeScans = Math.max(0, activeScans - 1);
    }

    /** Pauses read recording and returns the depth to restore. No thread lookup when no scan is active. */
    public static int pause() {
        if (activeScans == 0) return -1;
        Frame frame = CURRENT.get();
        threadLookups++;
        return frame == null ? -1 : frame.pause++;
    }

    public static void resume() {
        if (activeScans == 0) return;
        Frame frame = CURRENT.get();
        threadLookups++;
        if (frame != null && frame.pause > 0) frame.pause--;
    }

    /** Restores the pause depth captured by {@link #pause()}, dropping deeper unpaired pauses. */
    public static void resume(int depth) {
        if (activeScans == 0) return;
        Frame frame = CURRENT.get();
        threadLookups++;
        if (frame == null) return;
        frame.pause = Math.max(0, depth);
    }

    /** Head of a probe call that must restore its own depth on the matching return. */
    public static void pushPause() {
        if (activeScans == 0) return;
        PAUSES.get().push(pause());
    }

    public static void popPause() {
        if (activeScans == 0) return;
        java.util.ArrayDeque<Integer> stack = PAUSES.get();
        if (!stack.isEmpty()) resume(stack.pop());
    }

    public static void matcher() {
        if (activeScans == 0) return;
        if (enabled && inScan()) matchers++;
    }

    public static void onBind() {
        if (enabled) binds++;
    }

    public static void onRelease() {
        if (enabled) releases++;
    }

    public static void noteBlock(BlockPos pos) { note(pos, Kind.BLOCK); }
    public static void noteEntity(BlockPos pos) { note(pos, Kind.ENTITY); }
    public static void noteChunk(BlockPos pos) { note(pos, Kind.CHUNK); }

    /**
     * A chunk-presence query. {@code hasChunkAt(BlockPos)} is an interface default, so the probe
     * observes {@code ServerChunkCache.hasChunk} and keeps the chunk inside the declared footprint.
     */
    public static void noteChunkQuery(int chunkX, int chunkZ) {
        if (activeScans == 0) return;
        Frame frame = CURRENT.get();
        threadLookups++;
        if (frame == null || frame.depth <= 0 || frame.pause > 0) return;
        if (enabled) chunkChecks++;
        frame.addChunk(ChunkPos.asLong(chunkX, chunkZ));
    }

    private static void note(BlockPos pos, Kind kind) {
        if (activeScans == 0) return;
        Frame frame = CURRENT.get();
        threadLookups++;
        if (frame == null || frame.depth <= 0 || frame.pause > 0) return;
        if (enabled) {
            switch (kind) {
                case BLOCK -> blockReads++;
                case ENTITY -> entityReads++;
                case CHUNK -> chunkChecks++;
            }
        }
        frame.add(pos.immutable().asLong(), audit ? caller() : null);
    }

    public static void assertInside(BlockEntity entity, Footprint declared) {
        if (!audit) return;
        long[] reads = LAST.get();
        if (reads == null || declared == null) return;
        BlockPos controller = entity.getBlockPos();
        String[] callers = LAST_CALLERS.get();
        for (int index = 0; index < reads.length; index++) {
            BlockPos pos = BlockPos.of(reads[index]);
            if (pos.equals(controller) || declared.contains(pos)) continue;
            BlockPos min = declared.size() == 0 ? controller : declared.cell(0);
            BlockPos max = min;
            for (int cell = 0; cell < declared.size(); cell++) {
                BlockPos at = declared.cell(cell);
                min = new BlockPos(Math.min(min.getX(), at.getX()), Math.min(min.getY(), at.getY()), Math.min(min.getZ(), at.getZ()));
                max = new BlockPos(Math.max(max.getX(), at.getX()), Math.max(max.getY(), at.getY()), Math.max(max.getZ(), at.getZ()));
            }
            throw new IllegalStateException("Structure scan read " + pos + " outside the declared footprint of "
                    + entity.getClass().getSimpleName() + " at " + controller
                    + " rel=" + pos.subtract(controller) + " cells=" + declared.size()
                    + " box=" + min + ".." + max
                    + " facing=" + LayoutStructures.facing(entity)
                    + (callers != null && index < callers.length && callers[index] != null ? callers[index] : ""));
        }
        long[] chunks = LAST_CHUNKS.get();
        if (chunks == null) return;
        for (long chunk : chunks) {
            if (covers(declared.chunks(), chunk)) continue;
            ChunkPos pos = new ChunkPos(chunk);
            throw new IllegalStateException("Structure scan checked chunk " + pos.x + "," + pos.z
                    + " outside the declared footprint of " + entity.getClass().getSimpleName() + " at " + controller);
        }
    }

    private static boolean covers(long[] chunks, long packed) {
        if (chunks == null) return false;
        for (long chunk : chunks) if (chunk == packed) return true;
        return false;
    }

    private static String caller() {
        StringBuilder text = new StringBuilder();
        int shown = 0;
        for (StackTraceElement frame : new Throwable().getStackTrace()) {
            String name = frame.getClassName();
            if (name.contains("StructureScanProbe") || name.contains("LevelStructureProbeMixin")) continue;
            if (!name.startsWith("com.gregtech") && !name.startsWith("net.minecraft")) continue;
            text.append(" | ").append(name.substring(name.lastIndexOf('.') + 1)).append('.').append(frame.getMethodName())
                    .append(':').append(frame.getLineNumber());
            if (++shown == 6) break;
        }
        return text.toString();
    }

    private enum Kind { BLOCK, ENTITY, CHUNK }

    private static final class Frame {
        private int depth;
        private int pause;
        private long[] packed = new long[16];
        private String[] callers;
        private int count;
        private long[] chunks = new long[8];
        private int chunkCount;

        private void add(long value, String caller) {
            if (count > 0 && packed[count - 1] == value) return;
            if (count == packed.length) packed = Arrays.copyOf(packed, packed.length * 2);
            packed[count] = value;
            if (caller != null) {
                if (callers == null || callers.length < packed.length) {
                    callers = callers == null ? new String[packed.length] : Arrays.copyOf(callers, packed.length);
                }
                callers[count] = caller;
            }
            count++;
        }

        private void addChunk(long value) {
            if (chunkCount > 0 && chunks[chunkCount - 1] == value) return;
            if (chunkCount == chunks.length) chunks = Arrays.copyOf(chunks, chunks.length * 2);
            chunks[chunkCount++] = value;
        }
    }
}
