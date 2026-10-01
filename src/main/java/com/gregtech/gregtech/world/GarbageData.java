package com.gregtech.gregtech.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6 {@code GarbageGT}: the server-wide "Garbage Dimension". Ender Garbage Bins
 * teleport their contents here; the (admin-only) Ender Garbage Dump retrieves them.
 * Unlike GT6's static lists this survives restarts as world SavedData.
 */
public class GarbageData extends SavedData {

    private static final String NAME = "gregtech_garbage";

    /**
     * Hard cap on the number of distinct garbage piles kept in the dump.
     *
     * <p>A pile merges by {@link ItemStack#isSameItemSameTags}, so a stack carrying per-instance NBT
     * (a used tool, a charged battery, a bee's random genome) never merges with anything: every trashed
     * stack became a <em>new</em> permanent entry. Nothing else removes an entry - the only removal
     * path is {@link #extractItem} draining that exact pile to zero - so the list grew with every
     * NBT-carrying stack the Ender Garbage Bin ever voided, in the heap and in the world save alike.
     * Past this cap the oldest pile is dropped, which bounds the dump while leaving the behaviour for
     * every dump a player can actually reach through the container (it shows 54 piles) unchanged.</p>
     */
    public static final int MAX_ENTRIES = 256;

    /** One merged garbage pile: an item template (count 1) plus an unbounded count. */
    private static final class Entry {
        final ItemStack template;
        long count;
        Entry(ItemStack template, long count) { this.template = template; this.count = count; }
    }

    private final List<Entry> items = new ArrayList<>();
    private final List<FluidStack> fluids = new ArrayList<>();

    public static GarbageData get(Level level) {
        MinecraftServer server = level.getServer();
        if (server == null) throw new IllegalStateException("GarbageData requested on the client");
        return server.overworld().getDataStorage()
                .computeIfAbsent(GarbageData::load, GarbageData::new, NAME);
    }

    // ── trashing (Ender Garbage Bin) ─────────────────────────────────────

    public void trash(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        for (Entry e : items) {
            if (ItemStack.isSameItemSameTags(e.template, stack)) {
                e.count += stack.getCount();
                setDirty();
                return;
            }
        }
        trim(items);
        items.add(new Entry(stack.copyWithCount(1), stack.getCount()));
        setDirty();
    }

    public void trash(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return;
        for (FluidStack f : fluids) {
            if (f.isFluidEqual(fluid)) {
                long sum = (long) f.getAmount() + fluid.getAmount();
                f.setAmount((int) Math.min(Integer.MAX_VALUE, sum));
                setDirty();
                return;
            }
        }
        trim(fluids);
        fluids.add(fluid.copy());
        setDirty();
    }

    /**
     * Drops the oldest entries until the list has room for one more, so a dump fed with
     * NBT-carrying stacks cannot grow without bound; see {@link #MAX_ENTRIES}.
     */
    private static void trim(List<?> entries) {
        while (entries.size() >= MAX_ENTRIES) {
            entries.remove(0);
        }
    }

    // ── retrieval (Ender Garbage Dump) ───────────────────────────────────

    public int entryCount() { return items.size(); }

    /** Pile {@code slot} clamped to a displayable stack for container slots. */
    public ItemStack viewItem(int slot) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        Entry e = items.get(slot);
        return e.template.copyWithCount((int) Math.min(e.count, e.template.getMaxStackSize()));
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= items.size() || amount <= 0) return ItemStack.EMPTY;
        Entry e = items.get(slot);
        int take = (int) Math.min(Math.min(e.count, amount), e.template.getMaxStackSize());
        if (take <= 0) return ItemStack.EMPTY;
        ItemStack out = e.template.copyWithCount(take);
        if (!simulate) {
            e.count -= take;
            if (e.count <= 0) items.remove(slot);
            setDirty();
        }
        return out;
    }

    /** Drains from the first non-empty garbage fluid, up to {@code maxDrain}. */
    public FluidStack drain(int maxDrain, boolean simulate) {
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack f = fluids.get(i);
            if (f.isEmpty()) continue;
            int take = Math.min(maxDrain, f.getAmount());
            FluidStack out = new FluidStack(f, take);
            if (!simulate) {
                f.shrink(take);
                if (f.isEmpty()) fluids.remove(i);
                setDirty();
            }
            return out;
        }
        return FluidStack.EMPTY;
    }

    // ── persistence ──────────────────────────────────────────────────────

    public static GarbageData load(CompoundTag tag) {
        GarbageData data = new GarbageData();
        for (Tag t : tag.getList("Items", Tag.TAG_COMPOUND)) {
            // A save written before the cap existed can hold far more piles than MAX_ENTRIES; keep the
            // oldest ones, which are exactly the ones the container shows and drains.
            if (data.items.size() >= MAX_ENTRIES) break;
            CompoundTag c = (CompoundTag) t;
            ItemStack template = ItemStack.of(c.getCompound("Item"));
            long count = c.getLong("Count");
            if (!template.isEmpty() && count > 0) data.items.add(new Entry(template, count));
        }
        for (Tag t : tag.getList("Fluids", Tag.TAG_COMPOUND)) {
            if (data.fluids.size() >= MAX_ENTRIES) break;
            FluidStack f = FluidStack.loadFluidStackFromNBT((CompoundTag) t);
            if (!f.isEmpty()) data.fluids.add(f);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag itemList = new ListTag();
        for (Entry e : items) {
            CompoundTag c = new CompoundTag();
            // ItemStack.save clamps Count to a byte, so the real count is kept beside it
            c.put("Item", e.template.save(new CompoundTag()));
            c.putLong("Count", e.count);
            itemList.add(c);
        }
        tag.put("Items", itemList);
        ListTag fluidList = new ListTag();
        for (FluidStack f : fluids) fluidList.add(f.writeToNBT(new CompoundTag()));
        tag.put("Fluids", fluidList);
        return tag;
    }
}
