package com.gregtech.gregtech.api.machine;

/** Input-energy and piston state for GT6 electric/flux engines; no world or registry dependencies. */
public final class PistonEngineState {
    private long energy;
    private int mode = 15, piston;
    private boolean active, stopped;
    public long energy() { return energy; }
    public int mode() { return mode; }
    public int piston() { return piston; }
    public boolean active() { return active; }
    public boolean stopped() { return stopped; }
    /** GT6 tierMax: the first 8, 32, 128, ... threshold covering the voltage. */
    public static int voltageTier(long voltage) {
        if (voltage == Long.MIN_VALUE) return 30;
        voltage = Math.abs(voltage);
        int tier = 0;
        long threshold = 8;
        while (voltage > threshold) {
            tier++;
            if (threshold > Long.MAX_VALUE / 4) break;
            threshold *= 4;
        }
        return tier;
    }
    public int coreColor() {
        int step = mode <= 15 ? mode : mode - 16;
        return mode <= 15 ? (step * 17 << 8) | (255 - step * 17)
                : (step * 17 << 16) | ((255 - step * 17) << 8);
    }
    public void cycleMode() { mode = (mode + 1) & 31; }
    /** Source external 4-bit controller selects internal odd power steps. */
    public void setControlMode(int value){mode=Math.max(0,Math.min(15,value))*2+1;}
    public int controlMode(){return mode/2;}
    public void setStopped(boolean value){stopped=value;}
    public void toggleStopped() { stopped = !stopped; }
    public long input(long rated) { return scaled(rated, true); }
    public long output(long rated) { return scaled(rated, false); }
    private long scaled(long rated, boolean ceil) {
        long product = Math.multiplyExact(rated, mode + 1L);
        return product / 16 + (ceil && product % 16 != 0 ? 1 : 0);
    }
    /** Whole packets may cross the capacity boundary, matching GT6's rounded-up acceptance. */
    public long accept(long size, long packets, long capacity, boolean execute) {
        if (stopped || size == 0 || size == Long.MIN_VALUE || packets <= 0) return 0;
        size = Math.abs(size);
        long space = Math.max(0, capacity - energy);
        long accepted = Math.min(packets, space / size + (space % size == 0 ? 0 : 1));
        if (execute) energy = Math.addExact(energy, Math.multiplyExact(size, accepted));
        return accepted;
    }
    /** Consume work even when no receiver accepts it. Returns one signed KU packet, or zero. */
    public long tick(long timer, long ratedInput, long ratedOutput) {
        if (active && timer % (32 - mode) == 0) piston = (piston + 1) & 3;
        long cost = input(ratedInput);
        active = cost > 0 && energy >= cost;
        if (!active) return 0;
        energy -= cost;
        long output = output(ratedOutput);
        return piston > 1 ? -output : output;
    }
    public void restore(long stored, int mode, int piston, boolean active, boolean stopped) {
        energy = Math.max(0, stored);
        this.mode = Math.max(0, Math.min(31, mode));
        this.piston = piston & 3;
        this.active = active;
        this.stopped = stopped;
    }
}
