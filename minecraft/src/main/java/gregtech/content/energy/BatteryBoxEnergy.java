package gregtech.content.energy;

import gregtech.content.data.GregTechTags;
import gregtech.engine.energy.BatteryBoxState;
import gregtech.engine.energy.item.IItemEnergy;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Native item/world/NBT boundary around the original shared battery-box state rules. */
public final class BatteryBoxEnergy {
    private final BatteryBoxState state;
    private final Runnable changed;
    private long cellRevision;
    private Level transferLevel;
    private BlockPos transferPos;

    public BatteryBoxEnergy(List<ItemStack> inventory, long voltage) {
        this(inventory, voltage, GregTechTags.Energy.EU);
    }

    public BatteryBoxEnergy(List<ItemStack> inventory, long voltage, GregTechTags.Tag type) {
        this(inventory, voltage, type, () -> {});
    }

    public BatteryBoxEnergy(List<ItemStack> inventory, long voltage, GregTechTags.Tag type, Runnable changed) {
        this.changed = changed;
        // Resolve each stack by its slot on every call, so replacements do not leave a stale item reference.
        var slots = java.util.stream.IntStream.range(0, inventory.size())
                .mapToObj(slot -> (BatteryBoxState.Battery) new BatteryBoxState.Battery() {
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
                        ItemStack before = original.copy();
                        long extracted =
                                item.doEnergyExtraction(type, original, v, p, transferLevel, transferPos, true);
                        ItemStack replacement = item.afterEnergyTransfer(original);
                        inventory.set(slot, replacement);
                        if (extracted > 0 || !ItemStack.matches(before, replacement)) cellRevision++;
                        return extracted;
                    }

                    public long inject(long v, long p) {
                        ItemStack original = stack();
                        ItemStack before = original.copy();
                        long injected =
                                energy().doEnergyInjection(type, original, v, p, transferLevel, transferPos, true);
                        if (injected > 0 || !ItemStack.matches(before, stack())) cellRevision++;
                        return injected;
                    }
                })
                .toList();
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
        int previous = state.mode();
        state.mode(value);
        if (state.mode() != previous) changed.run();
    }

    public boolean enabled() {
        return state.enabled();
    }

    public void enabled(boolean value) {
        if (value == state.enabled()) return;
        state.enabled(value);
        changed.run();
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
        if (state.emitted()) changed.run();
    }

    public long inject(long size, long amount, boolean execute) {
        long used = state.inject(size, amount, execute);
        if (execute && used > 0) changed.run();
        return used;
    }

    /** Native world context exists only for the duration of item callbacks, including nested calls. */
    public void tick(long time, Level level, BlockPos pos) {
        long before = cellRevision;
        var oldLevel = transferLevel;
        var oldPos = transferPos;
        transferLevel = level;
        transferPos = pos;
        try {
            state.tick(time);
        } finally {
            transferLevel = oldLevel;
            transferPos = oldPos;
            if (cellRevision != before) changed.run();
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
                tag.getLong("gt.buffer"), tag.getBoolean("gt.battery_stopped"), tag.getInt("gt.battery_mode")));
    }
}
