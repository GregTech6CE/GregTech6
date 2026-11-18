package gregtech.content.blockentity.machine;

import gregtech.api.addon.platform.MachinePanel;
import net.minecraft.nbt.CompoundTag;

final class BehaviorMachinePanel implements MachinePanel {
    private final int toggleCount;
    private final Runnable markChanged;
    private int toggles;
    private int progress;
    private boolean working;

    BehaviorMachinePanel(int toggleCount, int initialToggles, Runnable markChanged) {
        this.toggleCount = toggleCount;
        this.toggles = initialToggles;
        this.markChanged = markChanged;
    }

    @Override
    public boolean toggle(int index) {
        return (toggles & (1 << index)) != 0;
    }

    @Override
    public void setToggle(int index, boolean on) {
        if (index < 0 || index >= toggleCount) throw new IllegalArgumentException("toggle " + index);
        toggles = on ? toggles | (1 << index) : toggles & ~(1 << index);
        markChanged.run();
    }

    @Override
    public float progress() {
        return progress / 1000.0F;
    }

    @Override
    public void setProgress(float fraction) {
        float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
        int next = Math.round(clamped * 1000.0F);
        if (next != progress) {
            progress = next;
            markChanged.run();
        }
    }

    @Override
    public void working(boolean value) {
        working = value;
    }

    boolean working() {
        return working;
    }

    int progressUnits() {
        return progress;
    }

    int toggleMask() {
        return toggles;
    }

    void write(CompoundTag tag) {
        tag.putInt("gt.progress", progress);
        tag.putInt("gt.toggles", toggles);
    }

    void read(CompoundTag tag) {
        progress = tag.getInt("gt.progress");
        toggles = tag.getInt("gt.toggles");
    }
}
