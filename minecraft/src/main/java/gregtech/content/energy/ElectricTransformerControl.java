package gregtech.content.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.engine.energy.TransformerControlState;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;

/** Native NBT/control interface over the single original activity/mode state machine. */
public final class ElectricTransformerControl extends TransformerControlState implements MachineControl {
    private final BooleanSupplier available;

    public ElectricTransformerControl(Runnable changed, BooleanSupplier available) {
        super(changed);
        this.available = available;
    }

    @Override
    public boolean available() {
        return available.getAsBoolean();
    }

    @Override
    public int setMode(int mode) {
        return available() ? super.setMode(mode) : 0;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        return available() && super.setEnabled(enabled);
    }

    public void save(CompoundTag tag) {
        var s = snapshot();
        tag.putBoolean("gt.transformer_stopped", !s.enabled());
        tag.putInt("gt.transformer_mode", s.mode());
        tag.putBoolean("gt.transformer_possible", s.possible());
        tag.putBoolean("gt.transformer_emitted", s.emitted());
    }

    public void load(CompoundTag tag) {
        restore(
                new Snapshot(
                        !tag.getBoolean("gt.transformer_stopped"),
                        tag.getInt("gt.transformer_mode"),
                        tag.getBoolean("gt.transformer_possible"),
                        tag.getBoolean("gt.transformer_emitted")),
                tag.getBoolean("gt.inverted"));
    }
}
