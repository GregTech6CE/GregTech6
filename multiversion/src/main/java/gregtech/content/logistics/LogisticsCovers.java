package gregtech.content.logistics;

import gregtech.internal.inventory.BlockContents;
//? if >=1.21.1 {
/*import gregtech.internal.transport.PipeConnections;
import net.minecraft.core.HolderLookup;
*///?} else {
import gregtech.block.energy.ElectricWireBlock;
//?}
import gregtech.block.machine.LogisticsWireBlock;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared face storage; installed stack data holds filters, priority and target quantity. */
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
//? if >=1.21.1 {

    /*public HolderLookup.Provider registries() {
        return owner.getLevel().registryAccess();
    }
*///?} else {
//?}

    public ItemStack get(Direction face) {
        return faces[face.ordinal()];
    }

    public int displaySignal(Direction face) {
        return displaySignals[face.ordinal()] & 15;
    }

    public int displayVisual(Direction face) {
        return displayVisuals[face.ordinal()] & 255;
    }

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
        if (owner instanceof gregtech.content.cover.PanelCoverHost panels && !panels.getCover(face).isEmpty()) return false;
        faces[face.ordinal()] = held.copyWithCount(1);
        displaySignals[face.ordinal()] = 0;
        displayVisuals[face.ordinal()] = 0;
        // GT6 AbstractCoverAttachmentLogistics.onCoverPlaced disconnects the covered connector face.
        var state = owner.getBlockState();
        if (owner.getLevel() != null && state.getBlock() instanceof LogisticsWireBlock
//? if >=1.21.1 {
                /*&& state.getValue(PipeConnections.propFor(face))) {
*///?} else {
                && state.getValue(ElectricWireBlock.propFor(face))) {
//?}
            var neighborPos = owner.getBlockPos().relative(face);
            if (owner.getLevel().hasChunkAt(neighborPos)) {
                var neighbor = owner.getLevel().getBlockState(neighborPos);
                if (neighbor.getBlock() instanceof LogisticsWireBlock
//? if >=1.21.1 {
                        /*&& neighbor.getValue(PipeConnections.propFor(face.getOpposite())))
*///?} else {
                        && neighbor.getValue(ElectricWireBlock.propFor(face.getOpposite())))
//?}
                owner.getLevel().setBlockAndUpdate(neighborPos,
//? if >=1.21.1 {
                        /*neighbor.setValue(PipeConnections.propFor(face.getOpposite()), false));
*///?} else {
                        neighbor.setValue(ElectricWireBlock.propFor(face.getOpposite()), false));
//?}
            }
            owner.getLevel().setBlockAndUpdate(owner.getBlockPos(),
//? if >=1.21.1 {
                    /*state.setValue(PipeConnections.propFor(face), false));
*///?} else {
                    state.setValue(ElectricWireBlock.propFor(face), false));
//?}
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
        gregtech.content.cover.CoverWorldInteraction.removed(owner);
        return new ItemStack(result.getItem());
    }

    public void changed() {
        owner.setChanged();
        if (gregtech.content.cover.ComponentCoverFallback.uses(owner)) {
            ((gregtech.content.cover.FallbackCoverHost) owner).gregtechComponentStorage(true);
            gregtech.content.cover.ComponentCoverFallback.track(owner);
        }
        if (owner.getLevel() != null && !owner.getLevel().isClientSide)
            owner.getLevel().sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 2);
    }

//? if >=1.21.1 {
    /*public void save(CompoundTag tag, HolderLookup.Provider lookup) {
*///?} else {
    public void save(CompoundTag tag) {
//?}
        for (int faceIndex = 0; faceIndex < faces.length; faceIndex++)
            if (!faces[faceIndex].isEmpty()) {
//? if >=1.21.1 {
                /*tag.put(NBT_PREFIX + faceIndex, faces[faceIndex].save(lookup));
*///?} else {
                tag.put(NBT_PREFIX + faceIndex, faces[faceIndex].save(new CompoundTag()));
//?}
                if (LogisticsCoverType.of(faces[faceIndex]) != null
                        && LogisticsCoverType.of(faces[faceIndex]).role() == LogisticsCoverType.Role.DISPLAY) {
                    tag.putByte(NBT_PREFIX + faceIndex + ".signal", displaySignals[faceIndex]);
                    tag.putByte(NBT_PREFIX + faceIndex + ".visual", displayVisuals[faceIndex]);
                }
            }
    }

//? if >=1.21.1 {
    /*public void load(CompoundTag tag, HolderLookup.Provider lookup) {
*///?} else {
    public void load(CompoundTag tag) {
//?}
        for (int faceIndex = 0; faceIndex < faces.length; faceIndex++)
            faces[faceIndex] = tag.contains(NBT_PREFIX + faceIndex)
//? if >=1.21.1 {
        /*// Version-specific branch.
                    ? ItemStack.parseOptional(lookup, tag.getCompound(NBT_PREFIX + faceIndex)) : ItemStack.EMPTY;
*///?} else {
        // Version-specific branch.
                    ? ItemStack.of(tag.getCompound(NBT_PREFIX + faceIndex)) : ItemStack.EMPTY;
//?}
        for (int faceIndex = 0; faceIndex < faces.length; faceIndex++) {
            var type = LogisticsCoverType.of(faces[faceIndex]);
            boolean display = type != null && type.role() == LogisticsCoverType.Role.DISPLAY;
            displaySignals[faceIndex] = (byte) (display ? Math.max(0, Math.min(15,
                    tag.getByte(NBT_PREFIX + faceIndex + ".signal"))) : 0);
            displayVisuals[faceIndex] = (byte) (display ? Math.max(0, Math.min(10,
                    tag.getByte(NBT_PREFIX + faceIndex + ".visual"))) : 0);
        }
    }

    public void dropAll() {
        for (int faceIndex = 0; faceIndex < faces.length; faceIndex++) {
            if (!faces[faceIndex].isEmpty() && !gregtech.content.cover.CoverDrops.retained(owner))
                BlockContents.drop(owner, faces[faceIndex]);
            faces[faceIndex] = ItemStack.EMPTY;
            displaySignals[faceIndex] = 0;
            displayVisuals[faceIndex] = 0;
        }
        changed();
    }
}
