package com.gregtech.gregtech.content.logistics;

import java.util.Arrays;

/** MultiTileEntityMiniPortal:113-180: six sided signal inboxes, maximum merge,
 * next tick delivery, and twenty absent ticks before clearing a disconnected side.
 * Advancing before accepting enforces the original global start/scan phases even
 * when modern block entities in different dimensions tick in either order.
 */
public final class MiniPortalSignals {
    private final int[] redstone = new int[6], comparator = new int[6];
    private final int[] incomingRedstone = new int[6], incomingComparator = new int[6];
    private final int[] redstoneWait = new int[6], comparatorWait = new int[6];
    private long advanced = Long.MIN_VALUE;
    private boolean changed;

    public MiniPortalSignals() {
        Arrays.fill(incomingRedstone, -1);
        Arrays.fill(incomingComparator, -1);
    }

    public void advance(long tick) {
        if (advanced == tick) return;
        advanced = tick;
        for (int side = 0; side < 6; side++) {
            changed |= apply(redstone, incomingRedstone, redstoneWait, side);
            changed |= apply(comparator, incomingComparator, comparatorWait, side);
        }
    }

    public void receive(long tick, int side, int power, int comparison) {
        advance(tick);
        incomingRedstone[side] = Math.max(incomingRedstone[side], clamp(power));
        incomingComparator[side] = Math.max(incomingComparator[side], clamp(comparison));
    }

    /** Source setPortalInactive: overwrite the counterpart's pending sides with zero. */
    public void disconnect(long tick) {
        advance(tick);
        Arrays.fill(incomingRedstone, 0);
        Arrays.fill(incomingComparator, 0);
    }

    private static boolean apply(int[] output, int[] input, int[] wait, int side) {
        int previous = output[side];
        if (input[side] >= 0) {
            output[side] = input[side];
            input[side] = -1;
            wait[side] = 0;
        } else if (wait[side] >= 20) output[side] = 0;
        else wait[side]++;
        return previous != output[side];
    }

    public int redstone(int side) { return redstone[side]; }
    public int comparator(int side) { return comparator[side]; }
    public int maximumComparator() { return Arrays.stream(comparator).max().orElse(0); }
    public boolean consumeChanged() { boolean result = changed; changed = false; return result; }
    public void clear() {
        changed |= Arrays.stream(redstone).anyMatch(value -> value != 0)
                || Arrays.stream(comparator).anyMatch(value -> value != 0);
        Arrays.fill(redstone, 0); Arrays.fill(comparator, 0);
        Arrays.fill(incomingRedstone, -1); Arrays.fill(incomingComparator, -1);
        Arrays.fill(redstoneWait, 0); Arrays.fill(comparatorWait, 0);
    }
    private static int clamp(int value) { return Math.max(0, Math.min(15, value)); }
}
