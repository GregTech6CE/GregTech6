package gregtech.content.blockentity.machine;

import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.content.cover.CoverAttachmentBehaviors;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.CoverStackData;
import gregtech.content.cover.CoverStore;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.content.cover.CoverUtilityBehaviors.FilterAction;
import gregtech.content.cover.MachineCoverSpec;
import gregtech.content.cover.PanelCover;
import gregtech.content.cover.PanelCoverRuntime;
import gregtech.engine.machine.RecipeProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Cover lifecycle, controller inputs and redstone outputs of a basic machine. */
final class BasicMachineCovers {
    private final BasicMachineBlockEntity machine;
    private final CoverStore store;
    private boolean controlStopped;
    private long coverTicks;
    private final byte[] externalSignals = new byte[6];
    private final int[] coverSignals = new int[6];
    private final boolean[] coverStrong = new boolean[6];
    private boolean successful;
    private boolean workPossible;

    BasicMachineCovers(BasicMachineBlockEntity machine) {
        this.machine = machine;
        this.store = new CoverStore(machine);
    }

    PanelCoverRuntime panels() { return store.panels(); }
    ItemStack get(Direction side) { return store.get(side); }
    boolean attach(Direction side, ItemStack stack) { return store.attach(side, stack); }
    ItemStack remove(Direction side) { return store.remove(side); }
    void drop() { store.drop(); }

    void save(CompoundTag tag, HolderLookup.Provider lookup) { store.save(tag, lookup); }
    void load(CompoundTag tag, HolderLookup.Provider lookup) { store.load(tag, lookup); }

    void releaseControl(ItemStack removed) {
        var spec = MachineCoverSpec.of(removed);
        if ((spec != null && !spec.detector()) || PanelCover.of(removed) == PanelCover.STATUS) {
            controlStopped = false;
            machine.updateAdjacentToggleableEnergySources();
        }
    }

    boolean stopped() { return controlStopped; }

    boolean setStopped(boolean stopped) {
        boolean changed = controlStopped != stopped;
        controlStopped = stopped;
        return changed;
    }

    int externalSignal(Direction side) { return externalSignals[side.ordinal()] & 15; }

    void setExternalSignal(Direction side, int strength) {
        if (externalSignals[side.ordinal()] != (byte) strength) {
            externalSignals[side.ordinal()] = (byte) strength;
            machine.setChanged();
        }
    }

    void saveControl(CompoundTag tag) {
        tag.putBoolean("gt.control_stopped", controlStopped);
        int packed = 0;
        for (int i = 0; i < externalSignals.length; i++) packed |= (externalSignals[i] & 15) << (4 * i);
        tag.putInt("gt.external_signals", packed);
        tag.putLong("gt.cover_ticks", coverTicks);
    }

    void loadControl(CompoundTag tag) {
        controlStopped = tag.getBoolean("gt.control_stopped");
        int packed = tag.getInt("gt.external_signals");
        for (int i = 0; i < externalSignals.length; i++) externalSignals[i] = (byte) ((packed >>> (4 * i)) & 15);
        coverTicks = Math.max(0, tag.getLong("gt.cover_ticks"));
        clearWorkState();
    }

    boolean successful() { return successful; }
    void setSuccessful(boolean completed) { successful = completed; }

    void clearWorkState() {
        successful = false;
        workPossible = false;
    }

    MachineCoverSpec.State state(boolean running, boolean active, long totalWork) {
        return new MachineCoverSpec.State(workPossible || totalWork > 0, running, active, successful);
    }

    void refreshWorkPossible(Level level, RecipeProcessor processor, boolean inventoryChanged) {
        boolean needed = false;
        for (ItemStack stack : store.faces()) {
            var spec = MachineCoverSpec.of(stack);
            if (spec == MachineCoverSpec.POSSIBLE || spec == MachineCoverSpec.AUTOMATIC
                    || PanelCover.of(stack) == PanelCover.STATUS) {
                needed = true;
                break;
            }
        }
        if (!needed) {
            workPossible = false;
            return;
        }
        if (processor.totalWork() > 0) {
            workPossible = true;
            return;
        }
        if (!inventoryChanged && level.getGameTime() % 12 != 0) return;
        workPossible = false;
        var evaluation = processor.evaluation();
        workPossible = evaluation != null && evaluation.parallel() > 0 && evaluation.cost() != null;
    }

    boolean switchesAllowRunning(Level level, BlockPos pos) {
        if (level == null) return !controlStopped;
        var state = machine.coverState();
        boolean controlled = false;
        boolean allowed = true;
        for (Direction side : Direction.values()) {
            var stack = machine.getCover(side);
            var spec = MachineCoverSpec.of(stack);
            if (spec == null || spec.detector()) continue;
            controlled = true;
            BlockPos neighbor = pos.relative(side);
            int signal = Math.max(level.hasChunkAt(neighbor) ? level.getSignal(neighbor, side) : 0, externalSignal(side));
            allowed = spec.allows(state, signal, MachineCoverSpec.inverted(stack), coverTicks);
        }
        if (controlled && controlStopped == allowed) {
            controlStopped = !allowed;
            machine.setChanged();
            machine.updateAdjacentToggleableEnergySources();
        }
        return !controlStopped;
    }

    void beginTick() {
        coverTicks++;
        machine.panels().beforeTick();
    }

    void tick(Level level, BlockPos pos) {
        if (machine.panels().stopped()) return;
        for (Direction side : Direction.values()) {
            String id = coverId(side);
            if (id == null) continue;
            switch (id) {
                case CoverItems.PUMP, CoverItems.CONVEYOR, CoverItems.ROBOT_ARM ->
                        ComponentCoverRuntime.tick(machine, side, level.getGameTime());
                // The drain and vent use the persisted machine clock in place of the GT6 global clock.
                case CoverAttachmentBehaviors.DRAIN ->
                        CoverAttachmentBehaviors.tickDrain(level, pos, side, machine, coverTicks);
                case CoverAttachmentBehaviors.AIR_VENT ->
                        CoverAttachmentBehaviors.tickVent(level, pos, side, machine, coverTicks);
            }
        }
    }

    boolean hasRedstoneCover() {
        for (Direction side : Direction.values()) {
            String id = coverId(side);
            var spec = MachineCoverSpec.of(machine.getCover(side));
            if (spec != null && spec.detector()) return true;
            if (CoverItems.REDSTONE_TORCH.equals(id) || CoverItems.REDSTONE_REPEATER.equals(id)) return true;
        }
        return false;
    }

    int signal(Direction side) {
        return PanelCover.of(machine.getCover(side)) != null
                ? machine.panels().signal(side)
                : coverSignals[side.ordinal()];
    }

    int strongSignal(Direction side) {
        if (PanelCover.of(machine.getCover(side)) != null) return machine.panels().strongSignal(side);
        return coverStrong[side.ordinal()] ? machine.getRedstoneSignal(side) : 0;
    }

    void updateSignals(Level level, BlockPos pos) {
        if (level == null || level.isClientSide || machine.isRemoved()) return;
        machine.panels().afterTick();
        boolean changed = false;
        var state = machine.coverState();
        for (Direction side : Direction.values()) {
            var stack = machine.getCover(side);
            var spec = MachineCoverSpec.of(stack);
            int signal = spec != null ? spec.signal(state, MachineCoverSpec.inverted(stack)) : redstoneSignal(side);
            boolean strong = spec != null && spec.detector() && MachineCoverSpec.strong(stack);
            int index = side.ordinal();
            if (signal == coverSignals[index] && strong == coverStrong[index]) continue;
            boolean wasStrong = coverStrong[index];
            coverSignals[index] = signal;
            coverStrong[index] = strong;
            changed = true;
            BlockPos neighbor = pos.relative(side);
            if ((strong || wasStrong) && level.hasChunkAt(neighbor))
                level.updateNeighborsAt(neighbor, machine.getBlockState().getBlock());
        }
        if (changed) level.updateNeighborsAt(pos, machine.getBlockState().getBlock());
    }

    private int redstoneSignal(Direction side) {
        // GT6's torch inverts the machine's running state and its repeater follows it.
        if (CoverItems.REDSTONE_TORCH.equals(coverId(side)))
            return CoverUtilityBehaviors.torchSignal(CoverUtilityBehaviors.torchVisual(machine.coverPowered()));
        if (CoverItems.REDSTONE_REPEATER.equals(coverId(side)))
            return CoverUtilityBehaviors.torchSignal(CoverUtilityBehaviors.repeaterVisual(machine.coverPowered()));
        return 0;
    }

    boolean configureControl(Direction side, boolean cutter, Level level, BlockPos pos) {
        var stack = machine.getCover(side);
        var spec = MachineCoverSpec.of(stack);
        if (spec == null || (cutter ? !spec.detector() : !spec.invertible())) return false;
        if (level != null && level.isClientSide) return true;
        String key = cutter ? "gt.cover.strong" : "gt.cover.inverted";
        var tag = CoverStackData.readOrEmpty(stack);
        tag.putBoolean(key, !tag.getBoolean(key));
        CoverStackData.write(stack, tag);
        syncCover(level, pos);
        machine.updateCoverSignals();
        return true;
    }

    int tagSelectorCircuit() {
        for (Direction side : Direction.values()) {
            if (!CoverItems.TAG_SELECTOR.equals(coverId(side))) continue;
            ItemStack cover = machine.getCover(side);
            int tag = CoverItems.selectorTag(cover);
            if (tag >= 0) return tag;
            CompoundTag nbt = CoverStackData.read(cover);
            if (nbt != null && nbt.contains("gt.circuit")) return nbt.getInt("gt.circuit");
            return -1; // Selector present but not configured.
        }
        return -2; // No selector circuit cover.
    }

    boolean itemFilterPermits(Direction side, ItemStack candidate, Level level) {
        return CoverUtilityBehaviors.itemFilterPermits(
                machine.getCover(side), machine.panels().stopped(), candidate, level.registryAccess());
    }

    boolean fluidFilterPermits(Direction side, FluidStack candidate, Level level) {
        return CoverUtilityBehaviors.fluidFilterPermits(
                machine.getCover(side), machine.panels().stopped(), candidate, level.registryAccess());
    }

    ItemStack itemFilter(Direction side, Level level) {
        return CoverUtilityBehaviors.itemFilter(machine.getCover(side), level.registryAccess());
    }

    FluidStack fluidFilter(Direction side, Level level) {
        return CoverUtilityBehaviors.fluidFilter(machine.getCover(side), level.registryAccess());
    }

    boolean clickFilter(Direction side, ItemStack held, Level level) {
        String id = coverId(side);
        if (id == null || held == null || held.isEmpty()) return false;
        ItemStack stack = machine.getCover(side);
        if (CoverUtilityBehaviors.FILTER_ITEM.equals(id)) {
            return CoverUtilityBehaviors.setItemFilter(stack, held, level.registryAccess());
        }
        if (CoverUtilityBehaviors.FILTER_FLUID.equals(id)) {
//? if >=1.21.1 {
/*            var container = held.getCapability(Capabilities.FluidHandler.ITEM);
*///?} else {
            var container = held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
//?}
            if (container == null) return false;
            FluidStack fluid = container.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            return CoverUtilityBehaviors.setFluidFilter(stack, fluid, level.registryAccess());
        }
        return false;
    }

    boolean configureFilter(Direction side, FilterAction action, Level level, BlockPos pos) {
        String id = coverId(side);
        if (!CoverUtilityBehaviors.FILTER_ITEM.equals(id)
                && !CoverUtilityBehaviors.FILTER_FLUID.equals(id)) return false;
        if (level != null && level.isClientSide) return true;
        action.apply(machine.getCover(side));
        syncCover(level, pos);
        return true;
    }

    void syncCover(Level level, BlockPos pos) {
        machine.setChanged();
        if (level != null) level.sendBlockUpdated(pos, machine.getBlockState(), machine.getBlockState(), 3);
    }

    private String coverId(Direction side) {
        return CoverItems.behavior(machine.getCover(side));
    }
}
