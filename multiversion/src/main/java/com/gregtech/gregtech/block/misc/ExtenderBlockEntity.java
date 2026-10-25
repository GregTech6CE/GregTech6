package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.CapabilityRelayBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;

/** Extender/bridge state for covers, routed capabilities, machine controls and signals. */
public class ExtenderBlockEntity extends com.gregtech.gregtech.blockentity.EnergyRelayBlockEntity
        implements com.gregtech.gregtech.api.machine.MachineControl.Provider,
        com.gregtech.gregtech.api.sensor.ProgressSources.Provider,
        com.gregtech.gregtech.content.cover.PanelCoverHost, com.gregtech.gregtech.api.inventory.BlockContents {
    private static final com.gregtech.gregtech.content.logistics.RelayVisitSet<ExtenderBlockEntity> CONTROLS =
            new com.gregtech.gregtech.content.logistics.RelayVisitSet<>();
    private static final com.gregtech.gregtech.content.logistics.RelayVisitSet<ExtenderBlockEntity> SIGNALS =
            new com.gregtech.gregtech.content.logistics.RelayVisitSet<>();
    private final int[] previousSignals = new int[6];
    private int previousComparator;
    private final net.minecraft.world.item.ItemStack[] covers = {
        net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.item.ItemStack.EMPTY,
        net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.item.ItemStack.EMPTY
    };
    private final com.gregtech.gregtech.content.cover.PanelCoverRuntime panels = new com.gregtech.gregtech.content.cover.PanelCoverRuntime(this);

    @Override
    public com.gregtech.gregtech.content.cover.PanelCoverRuntime panels() {
        return panels;
    }

    @Override
    public net.minecraft.world.item.ItemStack getCover(Direction side) {
        return covers[side.ordinal()];
    }

    @Override
    public boolean attachCover(Direction side, net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty() || !getCover(side).isEmpty() || !com.gregtech.gregtech.content.cover.CoverItems.isCover(stack) || !panels.canAttach(side,
                stack)) return false;
        covers[side.ordinal()] = stack.copyWithCount(1);
        panels.attached(side);
        panels.afterTick();
        return true;
    }

    @Override
    public net.minecraft.world.item.ItemStack removeCover(Direction side) {
        var stack = getCover(side);
        covers[side.ordinal()] = net.minecraft.world.item.ItemStack.EMPTY;
        return panels.removed(side, stack);
    }

    @Override
    public void dropContents() {
        for (var side : Direction.values()) {
            if (!com.gregtech.gregtech.content.cover.CoverDrops.retained(this)) com.gregtech.gregtech.api.inventory.BlockContents.drop(this,
                    getCover(side));
            covers[side.ordinal()] = net.minecraft.world.item.ItemStack.EMPTY;
        }
    }
//? if >=1.21.1 {
    /*private void saveCovers(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        for (var side : Direction.values())
            if (!getCover(side).isEmpty()) tag.put("gt_cover_" + side.ordinal(), getCover(side).save(lookup));
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        saveCovers(tag, lookup);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        for (var side : Direction.values())
            covers[side.ordinal()] = net.minecraft.world.item.ItemStack.parseOptional(lookup,
                tag.getCompound("gt_cover_" + side.ordinal()));
        panels.loaded();
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        var tag = new net.minecraft.nbt.CompoundTag();
        saveCovers(tag, lookup);
        return tag;
    }
*///?} else {
    private void saveCovers(net.minecraft.nbt.CompoundTag tag) {
        for (var side : Direction.values())
            if (!getCover(side).isEmpty()) tag.put("gt_cover_" + side.ordinal(),
                getCover(side).save(new net.minecraft.nbt.CompoundTag()));
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        saveCovers(tag);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        for (var side : Direction.values())
            covers[side.ordinal()] = net.minecraft.world.item.ItemStack.of(tag.getCompound(
                "gt_cover_" + side.ordinal()));
        panels.loaded();
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        var tag = new net.minecraft.nbt.CompoundTag();
        saveCovers(tag);
        return tag;
    }
//?}
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
//? if >=1.21.1 {
    /*@Override
    public void onDataPacket(net.minecraft.network.Connection connection, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
            net.minecraft.core.HolderLookup.Provider lookup) {
        if (packet.getTag() != null) loadAdditional(packet.getTag(), lookup);
    }

    @Override
    public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        loadAdditional(tag, lookup);
    }
*///?} else {
    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }

    @Override
    public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag) {
        load(tag);
    }
//?}
    @Override
    protected boolean permitsItem(Direction side, net.minecraft.world.item.ItemStack stack) {
        return !panels.shuttered(side);
    }
//? if >=1.21.1 {
    /*@Override
    protected boolean permitsFluid(Direction side, net.neoforged.neoforge.fluids.FluidStack stack) {
        return !panels.shuttered(side);
    }
*///?} else {
    @Override
    protected boolean permitsFluid(Direction side, net.minecraftforge.fluids.FluidStack stack) {
        return !panels.shuttered(side);
    }
//?}
    public int coverStrongSignal(Direction query) {
        return panels.strongSignal(query.getOpposite());
    }

    public ExtenderBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.EXTENDER.get(), pos, state);
    }

    @Override
    protected Target target(Direction side) {
        if (getBlockState().getBlock() instanceof SourceExtenderBlock source) {
            var state = getBlockState();
            var front = state.getValue(SourceExtenderBlock.FACING);
            if (source.spec().bridge && side == null) return null;
            var exit = Direction.values()[com.gregtech.gregtech.content.logistics.RelayRoutingRules.exit(source.spec().bridge,
                    side == null ? -1 : side.ordinal(), front.ordinal(), state.getValue(SourceExtenderBlock.SECONDARY).ordinal())];
            return new Target(worldPosition.relative(exit), exit.getOpposite());
        }
        var front = getBlockState().getValue(ExtenderBlock.FACING);
        var exit = Direction.values()[com.gregtech.gregtech.content.logistics.RelayRoutingRules.exit(false, side == null ? -1 : side.ordinal(),
                front.ordinal(), front.getOpposite().ordinal())];
        return new Target(worldPosition.relative(exit), exit.getOpposite());
    }

    @Override
    protected boolean supportsItems() {
        return !(getBlockState().getBlock() instanceof SourceExtenderBlock source) || source.spec().items;
    }

    @Override
    protected boolean supportsFluids() {
        return !(getBlockState().getBlock() instanceof SourceExtenderBlock source) || source.spec().fluids;
    }

    public boolean universal() {
        return getBlockState().getBlock() instanceof SourceExtenderBlock source && source.spec().universal();
    }

    @Override
    protected boolean supportsEnergy() {
        return universal();
    }

    private <T> T controlOperation(Direction side, T absent, java.util.function.Function<com.gregtech.gregtech.api.machine.MachineControl,
            T> operation) {
        if (!universal() || level == null || isRemoved()) return absent;
        var visited = CONTROLS.get();
        if (!visited.add(this)) return absent;
        try {
            var destination = target(side);
            if (destination == null || !level.hasChunkAt(destination.position())) return absent;
            var control = com.gregtech.gregtech.api.machine.MachineControl.find(level.getBlockEntity(destination.position()), destination.side());
            return control == null ? absent : operation.apply(control);
        } finally {
            visited.remove(this);
            if (visited.isEmpty()) CONTROLS.remove();
        }
    }

    private long progressOperation(Direction side, boolean maximum) {
        if (!universal() || level == null || isRemoved()) return 0;
        var visited = CONTROLS.get();
        if (!visited.add(this)) return 0;
        try {
            var destination = target(side);
            if (destination == null || !level.hasChunkAt(destination.position())) return 0;
            var progress = com.gregtech.gregtech.api.sensor.ProgressSources.find(
                    level.getBlockEntity(destination.position()), destination.side());
            return progress == null ? 0 : maximum ? progress.maximum() : progress.value();
        } finally {
            visited.remove(this);
            if (visited.isEmpty()) CONTROLS.remove();
        }
    }

    @Override
    public com.gregtech.gregtech.api.sensor.ProgressSources.Access progressSource(Direction side) {
        if (!universal() || (((SourceExtenderBlock) getBlockState().getBlock()).spec().bridge && side == null)) return null;
        // Advertise the read-only face, not a cached endpoint or live machine-control feature.
        // Each observation retains progressOperation's cycle, removal and unloaded guards.
        return new com.gregtech.gregtech.api.sensor.ProgressSources.Access() {
            @Override public long value() { return progressOperation(side, false); }
            @Override public long maximum() { return progressOperation(side, true); }
        };
    }

    @Override
    public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side) {
        if (!universal() || (((SourceExtenderBlock) getBlockState().getBlock()).spec().bridge && side == null)) return null;
        return new com.gregtech.gregtech.api.machine.MachineControl() {

            // Resolve feature queries just like commands: targets can disappear, change or form a cycle.
            public boolean available() {
                return controlOperation(side, false, control -> control.available());
            }

            public boolean supportsSwitch() {
                return controlOperation(side, false, control -> control.supportsSwitch());
            }

            public boolean supportsProgress() {
                return controlOperation(side, false, control -> control.supportsProgress());
            }

            public boolean supportsMode() {
                return controlOperation(side, false, control -> control.supportsMode());
            }

            public int mode() {
                return controlOperation(side, 0, control -> control.mode());
            }

            public int setMode(int value) {
                return controlOperation(side, 0, control -> control.setMode(value));
            }

            public boolean enabled() {
                return controlOperation(side, false, control -> control.enabled());
            }

            public boolean setEnabled(boolean value) {
                return controlOperation(side, false, control -> control.setEnabled(value));
            }

            public boolean running() {
                return controlOperation(side, false, control -> control.running());
            }

            public boolean active() {
                return controlOperation(side, false, control -> control.active());
            }

            public long progress() {
                return progressOperation(side, false);
            }

            public long progressMax() {
                return progressOperation(side, true);
            }
        };
    }

    private int incoming(Direction direction, boolean comparator) {
        var pos = worldPosition.relative(direction);
        if (!level.hasChunkAt(pos)) return 0;
        var state = level.getBlockState(pos);
        if (comparator) {
            if (level.getBlockEntity(pos) instanceof ExtenderBlockEntity relay && relay.universal()) return relay.comparator(direction.getOpposite());
            if (level.getBlockEntity(pos) instanceof DungeonPortalBlockEntity portal) return portal.comparator(direction.getOpposite());
            if (state.hasAnalogOutputSignal()) return state.getAnalogOutputSignal(level, pos);
        }
        return level.getSignal(pos, direction);
    }

    private int guardedSignal(java.util.function.IntSupplier read) {
        if (!universal() || level == null || isRemoved()) return 0;
        var visited = SIGNALS.get();
        if (!visited.add(this)) return 0;
        try {
            return net.minecraft.util.Mth.clamp(read.getAsInt(), 0, 15);
        }
        finally {
            visited.remove(this);
            if (visited.isEmpty()) SIGNALS.remove();
        }
    }

    /** Vanilla passes the opposite of the actual output face, just as GT6 did. */
    public int signal(Direction query) {
        if (com.gregtech.gregtech.content.cover.PanelCover.of(getCover(query.getOpposite())) != null) return panels.signal(query.getOpposite());
        return guardedSignal(() -> {
            var source = (SourceExtenderBlock) getBlockState().getBlock();
            if (source.spec().bridge) return incoming(query.getOpposite(), false);
            var front = getBlockState().getValue(SourceExtenderBlock.FACING);
            if (query.getOpposite() != front) return incoming(front, false);
            int maximum = 0;
            for (var side : Direction.values())
                if (side != front) maximum = Math.max(maximum, incoming(side, false));
            return maximum;
        });
    }

    /** Sided bridge comparator reads are available to other relays. Vanilla's block API is unsided. */
    public int comparator(Direction output) {
        return guardedSignal(() -> {
            var source = (SourceExtenderBlock) getBlockState().getBlock();
            if (!source.spec().bridge) return incoming(getBlockState().getValue(SourceExtenderBlock.FACING), true);
            if (output != null) return incoming(output.getOpposite(), true);
            int maximum = 0;
            for (var side : Direction.values())
                maximum = Math.max(maximum, incoming(side, true));
            return maximum;
        });
    }

    public void tickSignals() {
        panels.beforeTick();
        panels.afterTick();
        boolean changed = false;
        for (var side : Direction.values()) {
            int value = signal(side);
            if (previousSignals[side.ordinal()] != value) {
                previousSignals[side.ordinal()] = value;
                changed = true;
            }
        }
        int comparator = comparator(null);
        if (previousComparator != comparator) {
            previousComparator = comparator;
            changed = true;
        }
        if (changed) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
}
