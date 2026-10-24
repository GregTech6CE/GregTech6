package gregtech.content.energy;

import gregtech.internal.energy.item.IItemEnergy;
import gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

/** Native item/world/NBT boundary around the original shared battery-box state rules. */
public final class BatteryBoxEnergy {
    private final BatteryBoxState state;
    private Level transferLevel;
    private BlockPos transferPos;

    public BatteryBoxEnergy(List<ItemStack> inventory, long voltage) {
        this(inventory, voltage, GregTechTags.Energy.EU);
    }

    public BatteryBoxEnergy(List<ItemStack> inventory, long voltage, GregTechTags.Tag type) {
        // Resolve each stack by its slot on every call, so replacements do not leave a stale item reference.
        var slots = java.util.stream.IntStream.range(0, inventory.size()).mapToObj(slot -> (BatteryBoxState.Battery) new BatteryBoxState.Battery() {
            private ItemStack stack() {
                return inventory.get(slot);
            }

            private IItemEnergy energy() {
                return (IItemEnergy) stack().getItem();
            }

            public boolean accepts() {
                return BatteryBoxEnergy.accepts(stack(), type);
            }

            public boolean canInject(long v) {
                return energy().canEnergyInjection(stack(), type, v);
            }

            public boolean canExtract(long v) {
                return energy().canEnergyExtraction(stack(), type, v);
            }

            public long stored() {
                return energy().getEnergyStored(stack(), type);
            }

            public long capacity() {
                return energy().getEnergyCapacity(stack(), type);
            }

            public long extract(long v, long p) {
                IItemEnergy item = energy();
                ItemStack original = stack();
                long extracted = item.doEnergyExtraction(type, original, v, p, transferLevel, transferPos, true);
                inventory.set(slot, item.afterEnergyTransfer(original));
                return extracted;
            }

            public long inject(long v, long p) {
                return energy().doEnergyInjection(type, stack(), v, p, transferLevel, transferPos, true);
            }
        }).toList();
        state = new BatteryBoxState(slots, voltage);
    }

    public static boolean accepts(ItemStack stack) {
        return accepts(stack, GregTechTags.Energy.EU);
    }

    public static boolean accepts(ItemStack stack, GregTechTags.Tag type) {
        return !stack.isEmpty() && stack.getItem() instanceof IItemEnergy energy && energy.isEnergyType(stack, type);
    }

    public boolean canReceive() {
        return state.canReceive();
    }

    public long buffer() {
        return state.buffer();
    }

    public long bufferCapacity() {
        return state.bufferCapacity();
    }

    public int displayState() {
        return state.displayState();
    }

    public int mode() {
        return state.mode();
    }

    public void mode(int value) {
        state.mode(value);
    }

    public boolean enabled() {
        return state.enabled();
    }

    public void enabled(boolean value) {
        state.enabled(value);
    }

    public boolean running() {
        return state.running();
    }

    public boolean emitted() {
        return state.emitted();
    }

    public int providers() {
        return state.providers();
    }

    public long stored() {
        return state.stored();
    }

    public long capacity() {
        return state.capacity();
    }

    public long offered() {
        return state.offered();
    }

    public void emitted(long packets) {
        state.emitted(packets);
    }

    public long inject(long size, long amount, boolean execute) {
        return state.inject(size, amount, execute);
    }

    /** Native world context exists only for the duration of item callbacks, including nested calls. */
    public void tick(long time, Level level, BlockPos pos) {
        var oldLevel = transferLevel;
        var oldPos = transferPos;
        transferLevel = level;
        transferPos = pos;
        try {
            state.tick(time);
        } finally {
            transferLevel = oldLevel;
            transferPos = oldPos;
        }
    }

    public void save(CompoundTag tag) {
        var saved = state.snapshot();
        tag.putLong("gt.buffer", saved.buffer());
        tag.putBoolean("gt.battery_stopped", saved.stopped());
        tag.putInt("gt.battery_mode", saved.mode());
    }

    public void load(CompoundTag tag) {
        state.restore(new BatteryBoxState.Snapshot(
                tag.getLong("gt.buffer"),
                tag.getBoolean("gt.battery_stopped"),
                tag.getInt("gt.battery_mode")));
    }
}
