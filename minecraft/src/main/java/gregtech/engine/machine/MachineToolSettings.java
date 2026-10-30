package gregtech.engine.machine;

import gregtech.engine.energy.FaceConfig;
import net.minecraft.nbt.CompoundTag;

/** GT6's four processing modes and independent automatic-transfer switches. */
public final class MachineToolSettings {
    public enum Transfer {
        ITEM_INPUT("item_input"),
        ITEM_OUTPUT("item_output"),
        FLUID_INPUT("fluid_input"),
        FLUID_OUTPUT("fluid_output");

        private final String key;

        Transfer(String key) {
            this.key = key;
        }

        public int side(FaceConfig faces) {
            return switch (this) {
                case ITEM_INPUT -> faces.itemAutoInput();
                case ITEM_OUTPUT -> faces.itemAutoOutput();
                case FLUID_INPUT -> faces.fluidAutoInput();
                case FLUID_OUTPUT -> faces.fluidAutoOutput();
            };
        }

        public String message(boolean enabled) {
            return "message.gt6ce.machine.auto_" + key + (enabled ? "_enabled" : "_disabled");
        }
    }

    private int mode;
    private int disabledTransfers;

    public void cycleMode() {
        mode = (mode + 1) & 3;
    }

    public boolean emptyOutputsOnly() {
        return (mode & 1) != 0;
    }

    public boolean emptyInputsOnly() {
        return (mode & 2) != 0;
    }

    public boolean enabled(Transfer transfer) {
        return (disabledTransfers & (1 << transfer.ordinal())) == 0;
    }

    public void toggle(Transfer transfer) {
        disabledTransfers ^= 1 << transfer.ordinal();
    }

    public void save(CompoundTag tag) {
        var settings = new CompoundTag();
        settings.putByte("mode", (byte) mode);
        settings.putByte("disabled_transfers", (byte) disabledTransfers);
        tag.put("gt.machine_tools", settings);
    }

    public void load(CompoundTag tag) {
        var settings = tag.getCompound("gt.machine_tools");
        mode = settings.getByte("mode") & 3;
        disabledTransfers = settings.getByte("disabled_transfers") & 15;
    }
}
