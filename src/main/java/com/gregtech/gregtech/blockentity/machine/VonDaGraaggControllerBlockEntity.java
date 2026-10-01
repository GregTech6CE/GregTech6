package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.api.multiblock.MultiblockPortOwner;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.VonDaGraaggSpawnInhibitor;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/** GT6 17996: 41 galvanized base walls, 5 copper coils and the dense-steel top. */
public final class VonDaGraaggControllerBlockEntity extends GTEnergyBlockEntity implements MultiblockPortOwner {
    public static final long CAPACITY = 4096;
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
    private long energy;
    private int range;

    public VonDaGraaggControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.VON_DA_GRAAGG.get(), pos, state);
    }

    public long storedEnergy() { return energy; }
    public int currentRange() { return range; }

    @Override public boolean isStructureOk() {
        if (level == null || isRemoved() || worldPosition.getY() + 7 >= level.getMaxBuildHeight())
            return invalid();
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        // GT6 MultiTileEntityVonDaGraagg.checkStructure2: omit only the four 5x5 corners.
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.abs(x * z) >= 4) continue;
            for (int y = 0; y <= 1; y++) {
                if (x == 0 && y == 0 && z == 0) continue; // the controller
                if (!part(parts, x, y, z, 18028, MultiblockLayout.Role.ENERGY_INPUT))
                    return invalid();
            }
        }
        for (int y = 2; y <= 6; y++)
            if (!part(parts, 0, y, 0, 18040, MultiblockLayout.Role.CASING)) return invalid();
        if (!part(parts, 0, 7, 0, 18029, MultiblockLayout.Role.CASING)) return invalid();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            if (!part(parts, x, 6, z, 18029, MultiblockLayout.Role.CASING)) return invalid();
            if (x * z == 0) {
                if (!part(parts, x, 5, z, 18029, MultiblockLayout.Role.CASING)
                        || !part(parts, x, 7, z, 18029, MultiblockLayout.Role.CASING)) return invalid();
            }
        }
        return bindings.update(parts,
                pos -> level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port
                        && port.canBind(worldPosition),
                (pos, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::release);
    }

    private boolean part(LinkedHashMap<BlockPos, MultiblockLayout.Role> parts,
                         int x, int y, int z, int originalId, MultiblockLayout.Role role) {
        BlockPos pos = worldPosition.offset(x, y, z);
        if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(LargeMachineParts.block(originalId))
                || !(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity)) return false;
        parts.put(pos, role);
        return true;
    }

    private boolean invalid() { bindings.clear(this::release); return false; }
    private void release(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port)
            port.release(worldPosition);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  VonDaGraaggControllerBlockEntity be) {
        if (!(level instanceof ServerLevel server)) return;
        boolean formed = be.isStructureOk();
        int nextRange = formed ? (int) (Math.min(be.energy, CAPACITY) / 16) : 0;
        long nextEnergy = Math.max(0, be.energy - CAPACITY);
        boolean changed = nextRange != be.range || nextEnergy != be.energy;
        be.range = nextRange;
        be.energy = nextEnergy;
        if (be.range > 0) VonDaGraaggSpawnInhibitor.register(server, pos);
        else VonDaGraaggSpawnInhibitor.unregister(server, pos);
        if (changed) be.syncToClient();
    }

    @Override public void setRemoved() {
        bindings.clear(this::release);
        if (level instanceof ServerLevel server) VonDaGraaggSpawnInhibitor.unregister(server, worldPosition);
        super.setRemoved();
    }

    /** GT6 accepts electricity on the bottom of the controller and through every base wall. */
    @Override public boolean isEnergyType(GregTechTags.Tag type, @Nullable Direction side, boolean emitting) {
        return !emitting && type == GregTechTags.Energy.ELECTRICITY && (side == null || side == Direction.DOWN);
    }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return side == null || side == Direction.DOWN ? List.of(GregTechTags.Energy.ELECTRICITY) : List.of();
    }
    @Override public boolean hasEnergySurface(@Nullable Direction side) { return side == null || side == Direction.DOWN; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, @Nullable Direction side) { return 2048; }
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type, @Nullable Direction side) { return 256; }
    @Override public long getEnergySizeInputMax(GregTechTags.Tag type, @Nullable Direction side) { return 4096; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, @Nullable Direction side) { return 0; }
    @Override public long getEnergyOffered(GregTechTags.Tag type, @Nullable Direction side, long size) { return 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type, @Nullable Direction side, long size) {
        return type == GregTechTags.Energy.ELECTRICITY && size > 0 && size <= 4096
                ? Math.max(0, CAPACITY - energy) / size : 0;
    }
    @Override public long doInject(GregTechTags.Tag type, @Nullable Direction side,
                                   long size, long amount, boolean execute) {
        if (!isEnergyType(type, side, false) || size == Long.MIN_VALUE || amount <= 0) return 0;
        long packet = Math.abs(size);
        if (packet < 256 || packet > 4096) return 0;
        // GT6's doInject accumulates every same-tick packet and drains only 4096 EU per tick.
        // The reported capacitor size is 4096, but direct wall injection may temporarily exceed it.
        long accepted = Math.min(amount, (Long.MAX_VALUE - energy) / packet);
        if (execute && accepted > 0) { energy += packet * accepted; setChanged(); }
        return accepted;
    }
    @Override public long getEnergyStored(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.ELECTRICITY ? energy : 0;
    }
    @Override public long getEnergyCapacity(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.ELECTRICITY ? CAPACITY : 0;
    }

    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) { return null; }
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ENERGY_INPUT ? List.of(GregTechTags.Energy.ELECTRICITY) : List.of();
    }
    @Override public long portEnergyInputRecommended(MultiblockLayout.Role role, GregTechTags.Tag type) { return 2048; }
    @Override public long portEnergyInputMin(MultiblockLayout.Role role, GregTechTags.Tag type) { return 256; }
    @Override public long portEnergyInputMax(MultiblockLayout.Role role, GregTechTags.Tag type) { return 4096; }
    @Override public long portEnergyStored(MultiblockLayout.Role role, GregTechTags.Tag type) { return getEnergyStored(type, null); }
    @Override public long portEnergyCapacity(MultiblockLayout.Role role, GregTechTags.Tag type) { return getEnergyCapacity(type, null); }
    @Override public long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type,
                                           long size, long amount, boolean execute) {
        return role == MultiblockLayout.Role.ENERGY_INPUT && isStructureOk()
                ? doInject(type, null, size, amount, execute) : 0;
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("gt.energy", energy);
        tag.putInt("gt.range", range);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        energy = Math.max(0, tag.getLong("gt.energy"));
        range = Math.max(0, Math.min(256, tag.getInt("gt.range")));
    }
}
