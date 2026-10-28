package gregtech.content.cover;

import gregtech.engine.inventory.BlockContents;
import gregtech.engine.stack.NbtCompat;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Cover storage and lifecycle shared by the machine and both native pipes.
 *
 * <p>The store owns the face array, the lazy {@link PanelCoverRuntime} and the
 * {@code gt_cover_*} persistence; {@code ComponentCoverStorage} remains the mixin fallback for
 * hosts that do not keep their own slots. Behaviour dispatch stays in the host.</p>
 *
 * <p>A host without covers keeps zero cover allocation: the array is {@code null} until the first
 * attach and the panel runtime is created on first use, so a coverless pipe pays one boolean read
 * on its tick path.</p>
 */
public final class CoverStore {
    /** Host-specific rules and side effects; the generic checks and storage stay here. */
    public interface Hooks extends PanelCoverHost {
        /**
         * Host admission asked before the generic checks. The item pipe disconnects a face here
         * when an item filter is placed against another item pipe.
         */
        default boolean canAttach(Direction side, ItemStack stack) {
            return true;
        }

        /** Runs after the cover was stored and {@link PanelCoverRuntime#attached(Direction)} ran. */
        default void attached(Direction side, ItemStack stack) {}

        /** Runs between {@link PanelCoverRuntime#beforeTick()} and the removed item's return. */
        default void removed(Direction side, ItemStack stack) {}
    }

    private static final String NBT_PREFIX = "gt_cover_";
    private static final ItemStack[] NO_FACES = new ItemStack[0];

    private final Hooks host;
    private ItemStack[] covers;
    private boolean hasCovers;
    private PanelCoverRuntime panels;

    public CoverStore(Hooks host) {
        this.host = host;
    }

    public PanelCoverRuntime panels() {
        if (panels == null) panels = new PanelCoverRuntime(host);
        return panels;
    }

    public boolean hasCovers() {
        return hasCovers;
    }

    /** Whether a controller cover suspended the others, without creating the runtime. */
    public boolean stopped() {
        return panels != null && panels.stopped();
    }

    /** Whether this face's shutter is closed, without creating the runtime. */
    public boolean shuttered(Direction side) {
        return panels != null && panels.shuttered(side);
    }

    public ItemStack get(Direction side) {
        return covers == null ? ItemStack.EMPTY : covers[side.ordinal()];
    }

    /** Read-only face array for whole-host scans; empty while no cover was ever attached. */
    public ItemStack[] faces() {
        return covers == null ? NO_FACES : covers;
    }

    public boolean attach(Direction side, ItemStack stack) {
        if (stack.isEmpty()
                || !get(side).isEmpty()
                || !host.canAttach(side, stack)
                || !panels().canAttach(side, stack)
                || !CoverAdmission.allowCover(host.coverOwner(), side, stack)) return false;
        if (covers == null) {
            covers = new ItemStack[6];
            java.util.Arrays.fill(covers, ItemStack.EMPTY);
        }
        covers[side.ordinal()] = stack.copyWithCount(1);
        hasCovers = true;
        panels().attached(side);
        host.attached(side, get(side));
        return true;
    }

    public ItemStack remove(Direction side) {
        if (covers == null) return ItemStack.EMPTY;
        ItemStack cover = covers[side.ordinal()];
        if (cover.isEmpty()) return ItemStack.EMPTY;
        covers[side.ordinal()] = ItemStack.EMPTY;
        hasCovers = anyFace();
        panels().beforeTick();
        host.removed(side, cover);
        return panels().removed(side, cover);
    }

    /** Break-time drop of every face, honouring the harvested-cover retention of {@link CoverDrops}. */
    public void drop() {
        if (covers == null) return;
        boolean retained = CoverDrops.retained(host.coverOwner());
        for (int i = 0; i < covers.length; i++) {
            if (!retained) BlockContents.drop(host.coverOwner(), covers[i]);
            covers[i] = ItemStack.EMPTY;
        }
        hasCovers = false;
    }

    /** Writes each non-empty face under its flat key; {@code lookup} is the 1.21 registry provider. */
    public void save(CompoundTag tag, Object lookup) {
        if (covers == null) return;
        for (int i = 0; i < covers.length; i++) {
            if (covers[i].isEmpty()) continue;
            tag.put(NBT_PREFIX + i, NbtCompat.saveItem(covers[i], lookup));
        }
    }

    /**
     * Replaces every face from the tag's flat {@code gt_cover_*} keys; a tag without any of them
     * clears the faces, so a stale client copy cannot keep a removed cover. Legacy 1.20-style
     * cover NBT inside a 1.21 tag is re-read through {@link NbtCompat#readItem}.
     */
    public void load(CompoundTag tag, Object lookup) {
        boolean present = false;
        for (int i = 0; i < 6 && !present; i++) present = tag.contains(NBT_PREFIX + i);
        if (!present) {
            if (covers != null) java.util.Arrays.fill(covers, ItemStack.EMPTY);
            hasCovers = false;
            return;
        }
        if (covers == null) {
            covers = new ItemStack[6];
            java.util.Arrays.fill(covers, ItemStack.EMPTY);
        }
        boolean any = false;
        for (int i = 0; i < 6; i++) {
            covers[i] = tag.contains(NBT_PREFIX + i)
                    ? NbtCompat.readItem(tag.getCompound(NBT_PREFIX + i), lookup)
                    : ItemStack.EMPTY;
            any |= !covers[i].isEmpty();
        }
        hasCovers = any;
        // GT6 re-reads each cover's stored values on load; only worth doing when one is there.
        if (any) panels().loaded();
        for (var side : Direction.values()) ComponentCoverRuntime.attached(host, side);
    }

    private boolean anyFace() {
        for (ItemStack stack : covers) if (!stack.isEmpty()) return true;
        return false;
    }
}
