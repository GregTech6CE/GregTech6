package com.gregtech.gregtech.api.multiblock.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** One server controller. The formed bit is what queries read. */
public final class StructureTracker {
    public enum State { UNKNOWN, FORMED, BROKEN, SUSPENDED }

    private final TrackedMultiblock machine;
    private State state = State.UNKNOWN;
    private boolean formed;
    private boolean dirty;
    private boolean queued;
    private boolean expedite;
    private boolean unloading;
    private boolean facingDirty;
    private final int safetyBucket;
    private int epoch = -1;
    private int[] matchedLayers;
    private Footprint watch = Footprint.EMPTY;
    private final ClaimMemory claims = new ClaimMemory();

    StructureTracker(TrackedMultiblock machine) {
        this.machine = machine;
        this.safetyBucket = Math.floorMod(Long.hashCode(machine.trackedEntity().getBlockPos().asLong()),
                StructureScheduler.SAFETY_PERIOD);
    }

    public TrackedMultiblock machine() { return machine; }
    public BlockEntity entity() { return machine.trackedEntity(); }
    public BlockPos pos() { return entity().getBlockPos(); }
    public boolean formed() { return formed; }
    public State state() { return state; }
    public boolean dirty() { return dirty; }
    public boolean queued() { return queued; }
    public boolean expedite() { return expedite; }
    public boolean unloading() { return unloading; }
    public int safetyBucket() { return safetyBucket; }
    public int epoch() { return epoch; }
    public int[] matchedLayers() { return matchedLayers; }
    public Footprint watch() { return watch; }
    public int cellCount() { return Math.max(1, watch.size()); }

    void watch(Footprint footprint) { this.watch = footprint == null ? Footprint.EMPTY : footprint; }

    void enqueue(boolean expediteScan) {
        dirty = true;
        if (expediteScan) expedite = true;
    }

    void queued(boolean value) { queued = value; }

    boolean consumeFacing() {
        boolean value = facingDirty;
        facingDirty = false;
        return value;
    }

    void noteFacing() {
        facingDirty = true;
        matchedLayers = null;
    }

    /** Layer counts reported by the scan body for each repeat group, in layout order. */
    void matchedLayers(int[] layers) {
        matchedLayers = layers == null ? null : layers.clone();
    }

    void markUnloading() { unloading = true; }

    boolean claimsMatch(java.util.Map<BlockPos, com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role> parts, int designHash) {
        return claims.matches(parts, designHash);
    }

    void rememberClaims(java.util.Map<BlockPos, com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role> parts, int designHash) {
        claims.remember(parts, designHash);
    }

    void forgetClaims() { claims.forget(); }

    /** A relevant change. Formed controllers are rescanned before the budget queue. Returns true when the formed bit fell. */
    boolean invalidate() {
        boolean was = formed;
        formed = false;
        if (state == State.FORMED || was) state = State.UNKNOWN;
        dirty = true;
        if (was) expedite = true;
        return was;
    }

    /** Chunk of a watched cell unloaded. Claims stay. Returns true when the formed bit fell. */
    boolean suspend() {
        boolean was = formed;
        formed = false;
        state = State.SUSPENDED;
        dirty = false;
        expedite = false;
        return was;
    }

    /** Returns true when the formed bit rose. */
    boolean markFormed(int currentEpoch) {
        boolean was = formed;
        formed = true;
        state = State.FORMED;
        epoch = currentEpoch;
        finish();
        return !was;
    }

    /** Returns true when the formed bit fell. */
    boolean markBroken(int currentEpoch) {
        boolean was = formed;
        formed = false;
        state = State.BROKEN;
        epoch = currentEpoch;
        finish();
        return was;
    }

    private void finish() {
        dirty = false;
        expedite = false;
    }

    void discard() {
        finish();
    }
}
