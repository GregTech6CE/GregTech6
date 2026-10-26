package gregtech.content.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * GT6 {@code GarbageGT}: the server-wide "Garbage Dimension". Ender Garbage Bins
 * teleport their contents here; the (admin-only) Ender Garbage Dump retrieves them.
 * GT6 also saves its static pools through GarbageGT server save/load hooks.
 * This port stores the server-wide pools in overworld SavedData.
 */
public class GarbageData extends SavedData {

    private static final String NAME = "gregtech_garbage";

    /**
     * Hard cap on the number of distinct garbage piles kept in the dump.
     *
     * <p>Equal item/tag or component combinations merge; distinct combinations use separate entries.
     * Adding a new entry at this cap discards the oldest pile, including one visible in the first
     * 54 GUI slots. This is the port's bounded retention policy, rather than an original GT6 limit.
     * The original pool also persists, but does not impose this entry cap.</p>
     */
    public static final int MAX_ENTRIES = gregtech.content.storage.GarbageStorageRules.MAX_ENTRIES;

    /** One merged garbage pile: an item template (count 1) plus a long count. */
    private static final class Entry {
        final ItemStack template;
        long count;

        Entry(ItemStack template, long count) {
            this.template = template;
            this.count = count;
        }
    }

    private final List<Entry> items = new ArrayList<>();
    private final List<FluidStack> fluids = new ArrayList<>();

    public static GarbageData get(Level level) {
        MinecraftServer server = level.getServer();
        if (server == null) throw new IllegalStateException("GarbageData requested on the client");
        return server.overworld().getDataStorage()
//? if >=1.21.1 {
        /*.computeIfAbsent(new SavedData.Factory<>(GarbageData::new, GarbageData::load, net.minecraft.util.datafix.DataFixTypes.LEVEL), NAME);
*///?} else {
                .computeIfAbsent(GarbageData::load, GarbageData::new, NAME);
//?}
    }

    // ── trashing (Ender Garbage Bin) ─────────────────────────────────────

    public void trash(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        for (Entry entry : items) {
//? if >=1.21.1 {
            /*if (ItemStack.isSameItemSameComponents(entry.template, stack)) {
*///?} else {
            if (ItemStack.isSameItemSameTags(entry.template, stack)) {
//?}
                entry.count += stack.getCount();
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
        for (FluidStack storedFluid : fluids) {
//? if >=1.21.1 {
            /*if (FluidStack.isSameFluidSameComponents(storedFluid, fluid)) {
*///?} else {
            if (storedFluid.isFluidEqual(fluid)) {
//?}
                storedFluid.setAmount(gregtech.content.storage.GarbageStorageRules.fluidMerge(storedFluid.getAmount(),
                        fluid.getAmount()));
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

    public int entryCount() {
        return items.size();
    }

    /** Pile {@code slot} clamped to a displayable stack for container slots. */
    public ItemStack viewItem(int slot) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        Entry entry = items.get(slot);
        return entry.template.copyWithCount(gregtech.content.storage.GarbageStorageRules.take(entry.count, Integer.MAX_VALUE,
                entry.template.getMaxStackSize()));
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= items.size() || amount <= 0) return ItemStack.EMPTY;
        Entry entry = items.get(slot);
        int take = gregtech.content.storage.GarbageStorageRules.take(entry.count, amount, entry.template.getMaxStackSize());
        if (take <= 0) return ItemStack.EMPTY;
        ItemStack out = entry.template.copyWithCount(take);
        if (!simulate) {
            entry.count -= take;
            if (entry.count <= 0) items.remove(slot);
            setDirty();
        }
        return out;
    }

    /** Drains from the first non-empty garbage fluid, up to {@code maxDrain}. */
    public FluidStack drain(int maxDrain, boolean simulate) {
        for (int fluidIndex = 0; fluidIndex < fluids.size(); fluidIndex++) {
            FluidStack storedFluid = fluids.get(fluidIndex);
            if (storedFluid.isEmpty()) continue;
            int take = Math.min(maxDrain, storedFluid.getAmount());
//? if >=1.21.1 {
            /*FluidStack out = storedFluid.copyWithAmount(take);
*///?} else {
            FluidStack out = new FluidStack(storedFluid, take);
//?}
            if (!simulate) {
                storedFluid.shrink(take);
                if (storedFluid.isEmpty()) fluids.remove(fluidIndex);
                setDirty();
            }
            return out;
        }
        return FluidStack.EMPTY;
    }

    // ── persistence ──────────────────────────────────────────────────────

//? if >=1.21.1 {
    /*public static GarbageData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
*///?} else {
    public static GarbageData load(CompoundTag tag) {
//?}
        GarbageData data = new GarbageData();
        for (Tag savedTag : tag.getList("Items", Tag.TAG_COMPOUND)) {
            // A save written before the cap existed can hold far more piles than MAX_ENTRIES; keep the
            // oldest ones, which are exactly the ones the container shows and drains.
            if (data.items.size() >= MAX_ENTRIES) break;
            CompoundTag compound = (CompoundTag) savedTag;
//? if >=1.21.1 {
            /*ItemStack template = ItemStack.parseOptional(lookup, compound.getCompound("Item"));
*///?} else {
            ItemStack template = ItemStack.of(compound.getCompound("Item"));
//?}
            long count = compound.getLong("Count");
            if (!template.isEmpty() && count > 0) data.items.add(new Entry(template, count));
        }
        for (Tag savedTag : tag.getList("Fluids", Tag.TAG_COMPOUND)) {
            if (data.fluids.size() >= MAX_ENTRIES) break;
//? if >=1.21.1 {
            /*FluidStack storedFluid = FluidStack.parseOptional(lookup, (CompoundTag) savedTag);
*///?} else {
            FluidStack storedFluid = FluidStack.loadFluidStackFromNBT((CompoundTag) savedTag);
//?}
            if (!storedFluid.isEmpty()) data.fluids.add(storedFluid);
        }
        return data;
    }

    @Override
//? if >=1.21.1 {
    /*public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
*///?} else {
    public CompoundTag save(CompoundTag tag) {
//?}
        ListTag itemList = new ListTag();
        for (Entry entry : items) {
            CompoundTag compound = new CompoundTag();
            // Keep the pool count separately; the saved item template represents one item
//? if >=1.21.1 {
            /*compound.put("Item", entry.template.save(lookup));
*///?} else {
            compound.put("Item", entry.template.save(new CompoundTag()));
//?}
            compound.putLong("Count", entry.count);
            itemList.add(compound);
        }
        tag.put("Items", itemList);
        ListTag fluidList = new ListTag();
//? if >=1.21.1 {
        /*for (FluidStack storedFluid : fluids) fluidList.add(storedFluid.save(lookup));
*///?} else {
        for (FluidStack storedFluid : fluids) fluidList.add(storedFluid.writeToNBT(new CompoundTag()));
//?}
        tag.put("Fluids", fluidList);
        return tag;
    }
}
