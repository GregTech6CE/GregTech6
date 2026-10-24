package gregtech.api.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
//?}

/** Six live neighbour views; state/identity changes and loader capability invalidation both expire them. */
public final class AdjacentCapabilityCache<T> {
    private final BlockEntity owner;
//? if >=1.21.1 {
    /*private final BlockCapability<T, Direction> capability;
    public AdjacentCapabilityCache(BlockEntity owner, BlockCapability<T, Direction> capability) {
*///?} else {
    private final Capability<T> capability;
    public AdjacentCapabilityCache(BlockEntity owner, Capability<T> capability) {
//?}
        this.owner = owner;
        this.capability = capability;
    }

    private final java.util.List<Entry> entries = new java.util.ArrayList<>(java.util.Collections.nCopies(6, null));

    public T get(BlockEntity neighbour, Direction receivingSide) {
        if (neighbour == null || neighbour.isRemoved() || owner.isRemoved() || owner.getLevel() == null) return null;
        int index = receivingSide.ordinal();
        Entry entry = entries.get(index);
        if (entry == null || entry.neighbour != neighbour || entry.state != neighbour.getBlockState()
                || entry.level != owner.getLevel()) {
            entry = new Entry(neighbour, receivingSide);
            entries.set(index, entry);
        }
//? if >=1.21.1 {
        /*return entry.cached == null ? null : entry.cached.getCapability();
*///?} else {
        // An empty/invalid LazyOptional must be queried again: capabilities may appear later.
        if (!entry.cached.isPresent()) entry.cached = neighbour.getCapability(capability, receivingSide);
        return entry.cached.orElse(null);
//?}
    }

    private final class Entry {
        final BlockEntity neighbour;
        final BlockState state;
        final net.minecraft.world.level.Level level;
//? if >=1.21.1 {
        /*final BlockCapabilityCache<T, Direction> cached;
*///?} else {
        LazyOptional<T> cached;
//?}
        Entry(BlockEntity neighbour, Direction side) {
            this.neighbour = neighbour;
            state = neighbour.getBlockState();
            level = owner.getLevel();
//? if >=1.21.1 {
            /*cached = level instanceof ServerLevel server ? BlockCapabilityCache.create(capability, server,
                    neighbour.getBlockPos(), side,
                    () -> !owner.isRemoved() && owner.getLevel() == level && entries.get(side.ordinal()) == this,
                    () -> {}) : null;
*///?} else {
            cached = neighbour.getCapability(capability, side);
//?}
        }
    }
}
