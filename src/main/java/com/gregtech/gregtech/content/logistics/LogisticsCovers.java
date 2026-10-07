package com.gregtech.gregtech.content.logistics;

import com.gregtech.gregtech.api.inventory.BlockContents;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.LogisticsWireBlock;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared face storage. Cover configuration lives in the installed ItemStack NBT. */
public final class LogisticsCovers {
    private static final String NBT_PREFIX = "gt.logistics.cover.";
    private final BlockEntity owner;
    private final LogisticsHost host;
    private final ItemStack[] faces = {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };
    private final byte[] displaySignals = new byte[6];
    private final byte[] displayVisuals = new byte[6];

    public LogisticsCovers(BlockEntity owner, LogisticsHost host) {
        this.owner = owner;
        this.host = host;
    }

    public ItemStack get(Direction face) { return faces[face.ordinal()]; }

    public int displaySignal(Direction face) { return displaySignals[face.ordinal()] & 15; }
    public int displayVisual(Direction face) { return displayVisuals[face.ordinal()] & 255; }

    /** Update a CPU display only when the powered core scans this host, as in GT6. */
    public void setDisplay(Direction face, int used, int total) {
        var type = LogisticsCoverType.of(get(face));
        if (type == null || type.role() != LogisticsCoverType.Role.DISPLAY) return;
        int index = face.ordinal();
        byte signal = (byte) LogisticsCpuDisplay.signal(used, total);
        byte visual = (byte) LogisticsCpuDisplay.visual(used, total);
        if (displaySignals[index] == signal && displayVisuals[index] == visual) return;
        boolean signalChanged = displaySignals[index] != signal;
        displaySignals[index] = signal;
        displayVisuals[index] = visual;
        changed();
        if (signalChanged && owner.getLevel() != null && !owner.getLevel().isClientSide)
            owner.getLevel().updateNeighborsAt(owner.getBlockPos(), owner.getBlockState().getBlock());
    }

    public boolean attach(Direction face, ItemStack held) {
        if (LogisticsCoverType.of(held) == null || !host.canLogistics(null) || !get(face).isEmpty()) return false;
        if (owner instanceof com.gregtech.gregtech.content.cover.PanelCoverHost panels && !panels.getCover(face).isEmpty()) return false;
        faces[face.ordinal()] = held.copyWithCount(1);
        displaySignals[face.ordinal()] = 0;
        displayVisuals[face.ordinal()] = 0;
        // GT6 AbstractCoverAttachmentLogistics.onCoverPlaced disconnects the covered connector face.
        var state = owner.getBlockState();
        if (owner.getLevel() != null && state.getBlock() instanceof LogisticsWireBlock
                && state.getValue(ElectricWireBlock.propFor(face))) {
            var neighborPos = owner.getBlockPos().relative(face);
            if (owner.getLevel().hasChunkAt(neighborPos)) {
                var neighbor = owner.getLevel().getBlockState(neighborPos);
                if (neighbor.getBlock() instanceof LogisticsWireBlock
                        && neighbor.getValue(ElectricWireBlock.propFor(face.getOpposite())))
                    owner.getLevel().setBlockAndUpdate(neighborPos,
                            neighbor.setValue(ElectricWireBlock.propFor(face.getOpposite()), false));
            }
            owner.getLevel().setBlockAndUpdate(owner.getBlockPos(),
                    state.setValue(ElectricWireBlock.propFor(face), false));
        }
        changed();
        return true;
    }

    public ItemStack remove(Direction face) {
        ItemStack result = get(face);
        if (result.isEmpty()) return ItemStack.EMPTY;
        faces[face.ordinal()] = ItemStack.EMPTY;
        displaySignals[face.ordinal()] = 0;
        displayVisuals[face.ordinal()] = 0;
        changed();
        if (owner.getLevel() != null && !owner.getLevel().isClientSide)
            owner.getLevel().updateNeighborsAt(owner.getBlockPos(), owner.getBlockState().getBlock());
        return new ItemStack(result.getItem());
    }

    public void changed() {
        owner.setChanged();
        if(com.gregtech.gregtech.content.cover.ComponentCoverFallback.uses(owner)){
            ((com.gregtech.gregtech.content.cover.FallbackCoverHost)owner).gregtechComponentStorage(true);
            com.gregtech.gregtech.content.cover.ComponentCoverFallback.track(owner);
        }
        if (owner.getLevel() != null && !owner.getLevel().isClientSide)
            owner.getLevel().sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 2);
    }

    public void save(CompoundTag tag) {
        for (int i = 0; i < faces.length; i++) if (!faces[i].isEmpty()) {
            tag.put(NBT_PREFIX + i, faces[i].save(new CompoundTag()));
            if (LogisticsCoverType.of(faces[i]) != null
                    && LogisticsCoverType.of(faces[i]).role() == LogisticsCoverType.Role.DISPLAY) {
                tag.putByte(NBT_PREFIX + i + ".signal", displaySignals[i]);
                tag.putByte(NBT_PREFIX + i + ".visual", displayVisuals[i]);
            }
        }
    }

    public void load(CompoundTag tag) {
        for (int i = 0; i < faces.length; i++)
            faces[i] = tag.contains(NBT_PREFIX + i)
                    ? ItemStack.of(tag.getCompound(NBT_PREFIX + i)) : ItemStack.EMPTY;
        for (int i = 0; i < faces.length; i++) {
            var type = LogisticsCoverType.of(faces[i]);
            boolean display = type != null && type.role() == LogisticsCoverType.Role.DISPLAY;
            displaySignals[i] = (byte) (display ? Math.max(0, Math.min(15,
                    tag.getByte(NBT_PREFIX + i + ".signal"))) : 0);
            displayVisuals[i] = (byte) (display ? Math.max(0, Math.min(10,
                    tag.getByte(NBT_PREFIX + i + ".visual"))) : 0);
        }
    }

    public void dropAll() {
        for (int i = 0; i < faces.length; i++) {
            if (!faces[i].isEmpty()&&!com.gregtech.gregtech.content.cover.CoverDrops.retained(owner)) BlockContents.drop(owner, faces[i]);
            faces[i] = ItemStack.EMPTY;
            displaySignals[i] = 0;
            displayVisuals[i] = 0;
        }
        changed();
    }
}
