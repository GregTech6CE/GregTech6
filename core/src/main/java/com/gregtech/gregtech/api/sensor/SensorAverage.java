package com.gregtech.gregtech.api.sensor;

/** Bounded original averaging window; constant-time samples and no per-tick allocations. */
public final class SensorAverage {
    private int[] values = new int[1];
    private int index;
    private long sum;
    public int size() { return values.length; }
    public void resize(int size) { values = new int[Math.max(1,Math.min(32767,size))]; index = 0; sum = 0; }
    public long sample(long value) {
        int next = (int)Math.max(Integer.MIN_VALUE,Math.min(Integer.MAX_VALUE,value));
        sum += (long)next - values[index]; values[index] = next; index = (index+1)%values.length;
        return sum / values.length;
    }
    public int[] values() { return values.clone(); }
    public int index() { return index; }
    public void restore(int[] saved, int next) {
        resize(saved.length);
        System.arraycopy(saved,0,values,0,Math.min(saved.length,values.length));
        for (int value : values) sum += value;
        index = Math.floorMod(next,values.length);
    }
}
