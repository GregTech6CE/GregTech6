package gregtech.content.blockentity.tool;

import gregtech.api.material.*;

import gregtech.engine.machine.crucible.CrucibleItemInput;
import gregtech.engine.machine.crucible.CrucibleMaterialStack;
import gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity;
import gregtech.content.data.MaterialPrefix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 Dust Funnel: combines dust fractions into a selected size, pulling from
 * above and pushing its output below. A crucible is also a valid receiver.
 */
public class DustFunnelBlockEntity extends BlockEntity {

    private gregtech.api.material.GTMaterial material;
    private long amount;
    private boolean inventoryChanged = true;
    /** NBT mode from a save written before the block state carried it; applied on the next server tick. */
    private int pendingModeSync = -1;
    private final ItemStackHandler buffer = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) { inventoryChanged = true; setChanged(); }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && accepts(stack); }
        @Override public ItemStack extractItem(int slot, int count, boolean simulate) {
            return slot == 1 ? super.extractItem(slot, count, simulate) : ItemStack.EMPTY;
        }
    };

    public DustFunnelBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.DUST_FUNNEL.get(), pos, state);
    }

    public static boolean isDust(ItemStack stack) {
        var form = gregtech.engine.material.MaterialEquivalence.form(stack);
        if (form != null && gregtech.content.tool.UtilityToolRules.dust(form.prefix().getName())) return true;
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem item)) return false;
        if (item.getBlock() instanceof gregtech.content.block.MaterialBlockLike block)
            return block.prefix() == gregtech.engine.prefix.BlockMaterialPrefix.blockDust;
        // GT6 black sands are their material's blockDust without MaterialBlockLike: they keep their
        // own name and texture, so only the GTBlocks binding identifies them as that form.
        var data = gregtech.engine.material.ItemMaterialRegistry.explicit(item).orElse(null);
        if (data == null || data.components().size() != 1) return false;
        var bound = gregtech.content.registry.GTBlocks.getStack(
                gregtech.engine.prefix.BlockMaterialPrefix.blockDust, data.material());
        return !bound.isEmpty() && bound.getItem() == item;
    }

    /** @return how many items were taken from the held stack */
    public int insert(ItemStack held) {
        if (!accepts(held)) return 0;
        ItemStack leftover = buffer.insertItem(0, held.copy(), false);
        return held.getCount() - leftover.getCount();
    }

    public ItemStack retrieve() {
        for (int slot : new int[]{1, 0}) if (!buffer.getStackInSlot(slot).isEmpty()) {
            var out = buffer.getStackInSlot(slot); buffer.setStackInSlot(slot, ItemStack.EMPTY); return out;
        }
        return retrieveFraction();
    }

    /** GT6 pincers empty the material reservoir, leaving both ordinary slots alone. */
    public ItemStack retrieveFraction() {
        var out = material == null ? ItemStack.EMPTY : gregtech.util.OM.dust(material, amount);
        if (!out.isEmpty()) {
            var parts = CrucibleItemInput.parse(out.copyWithCount(1));
            if (!parts.isEmpty()) amount -= parts.get(0).amount * out.getCount();
            if (amount <= 0) { amount = 0; material = null; }
            inventoryChanged = true;
            setChanged();
        }
        return out;
    }

    public int mode() {
        BlockState state = getBlockState();
        return state.hasProperty(gregtech.content.block.tool.DustFunnelBlock.MODE)
                ? state.getValue(gregtech.content.block.tool.DustFunnelBlock.MODE) : 0;
    }
    public long retainedAmount() { return amount; }
    public boolean needsMoreDust() { return amount > 0 && amount < outputAmount(); }
    @Nullable
    public gregtech.api.material.GTMaterial retainedMaterial() { return material; }

    public void cycleMode(boolean reverse) {
        BlockState state = getBlockState();
        if (level == null || !state.hasProperty(gregtech.content.block.tool.DustFunnelBlock.MODE)) return;
        int next = Math.floorMod(mode() + (reverse ? -1 : 1), 5);
        level.setBlock(worldPosition, state.setValue(gregtech.content.block.tool.DustFunnelBlock.MODE, next), Block.UPDATE_ALL);
        inventoryChanged = true;
        setChanged();
    }
    private long outputAmount() { return mode() == 0 ? 9 * gregtech.api.material.GTValues.U
            : outputPrefix().getMaterialWeight(); }
    private MaterialPrefix outputPrefix() { return new MaterialPrefix[]{MaterialPrefix.dust, MaterialPrefix.dust,
            MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.dustDiv72}[mode()]; }
    private ItemStack output(gregtech.api.material.GTMaterial mat) {
        return mode() == 0 ? gregtech.content.registry.GTBlocks.getStack(gregtech.engine.prefix.BlockMaterialPrefix.blockDust, mat)
                : gregtech.content.registry.GTItems.getStack(outputPrefix(), mat, 1);
    }
    public ItemStack modeIcon() { return output(gregtech.content.material.Materials.Iron); }
    public ItemStack inputForInspection() { return buffer.getStackInSlot(0).copy(); }
    public ItemStack outputForInspection() { return buffer.getStackInSlot(1).copy(); }
    public ItemStack preparedForInspection() { return material == null ? ItemStack.EMPTY : output(material); }

    private boolean accepts(ItemStack stack) {
        if (!isDust(stack)) return false;
        var parts = CrucibleItemInput.parse(stack.copyWithCount(1));
        return parts.size() == 1 && parts.get(0).amount > 0 && !output(parts.get(0).material).isEmpty()
                && (material == null || amount == 0 || material.resolve() == parts.get(0).material.resolve());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DustFunnelBlockEntity funnel) {
        if (funnel.pendingModeSync >= 0) {
            int storedMode = funnel.pendingModeSync;
            funnel.pendingModeSync = -1;
            if (state.hasProperty(gregtech.content.block.tool.DustFunnelBlock.MODE)
                    && state.getValue(gregtech.content.block.tool.DustFunnelBlock.MODE) != storedMode) {
                level.setBlock(pos, state.setValue(gregtech.content.block.tool.DustFunnelBlock.MODE, storedMode), Block.UPDATE_ALL);
            }
        }
        boolean transfer = funnel.inventoryChanged || level.getGameTime() % 100 == 0;
        funnel.inventoryChanged = false;
        if (transfer && funnel.buffer.getStackInSlot(0).isEmpty()) {
            var above = gregtech.engine.inventory.NativeItemInventories.at(level, pos.above(), Direction.DOWN);
            if (above != null) for (int slot = 0; slot < above.getSlots(); slot++) {
                var offered = above.extractItem(slot, 64, true);
                if (!funnel.accepts(offered)) continue;
                var taken = above.extractItem(slot, offered.getCount(), false);
                if (!taken.isEmpty()) funnel.buffer.setStackInSlot(0, taken);
                break;
            }
        }
        ItemStack stack = funnel.buffer.getStackInSlot(0);
        if (!stack.isEmpty() && funnel.amount < funnel.outputAmount()) {
            var parts = CrucibleItemInput.parse(stack.copyWithCount(1));
            if (!parts.isEmpty()) {
                var part = parts.get(0);
                if (funnel.material == null || funnel.amount == 0 || funnel.material.resolve() == part.material.resolve()) {
                    long count = Math.min(stack.getCount(), (funnel.outputAmount() - funnel.amount + part.amount - 1) / part.amount);
                    funnel.material = part.material.resolve(); funnel.amount += count * part.amount;
                    stack.shrink((int) count); funnel.buffer.setStackInSlot(0, stack); transfer = true;
                }
            }
        }
        if (funnel.material != null && funnel.amount >= funnel.outputAmount() && funnel.buffer.getStackInSlot(1).isEmpty()) {
            var output = funnel.output(funnel.material);
            if (!output.isEmpty()) {
                funnel.buffer.setStackInSlot(1, output); funnel.amount -= funnel.outputAmount();
                if (funnel.amount == 0) funnel.material = null;
                transfer = true;
            }
        }
        if (!transfer || funnel.buffer.getStackInSlot(1).isEmpty()) return;
        var below = gregtech.engine.inventory.NativeItemInventories.at(level, pos.below(), Direction.UP);
        var output = funnel.buffer.getStackInSlot(1);
        if (below != null) {
            ItemStack remaining = output.copy();
            for (int slot = 0; slot < below.getSlots() && !remaining.isEmpty(); slot++) remaining = below.insertItem(slot, remaining, false);
            funnel.buffer.setStackInSlot(1, remaining); return;
        }
if (!(level.getBlockEntity(pos.below()) instanceof SmeltingCrucibleBlockEntity crucible)) return;
        List<CrucibleMaterialStack> incoming = CrucibleItemInput.parse(output.copyWithCount(1));
        if (incoming.isEmpty()) return;
        if (crucible.addMaterialStacks(incoming, crucible.getTemperature())) {
            output.shrink(1);
            funnel.buffer.setStackInSlot(1, output);
        }
    }

    public void dropContents() {
        if (level == null) return;
        ItemStack stack;
        while (!(stack = retrieve()).isEmpty()) {
            gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
        }
    }

//? if >=1.21.1 {
    /*private final IItemHandler bottom = new net.neoforged.neoforge.items.wrapper.RangedWrapper(buffer, 1, 2);
    public IItemHandler itemHandler(){return itemHandler(null);}
    public IItemHandler itemHandler(@Nullable Direction side){return isRemoved() ? null : side == Direction.DOWN ? bottom : buffer;}
*///?} else {
    private LazyOptional<IItemHandler> cap = LazyOptional.of(() -> buffer);
    private LazyOptional<IItemHandler> bottomCap = LazyOptional.of(() -> new net.minecraftforge.items.wrapper.RangedWrapper(buffer, 1, 2));
//?}

//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        saveData(tag, lookup);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        loadData(tag, lookup);
    }
*///?} else {
    @Override
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return side == Direction.DOWN ? bottomCap.cast() : cap.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        cap.invalidate();
        bottomCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        cap = LazyOptional.of(() -> buffer);
        bottomCap = LazyOptional.of(() -> new net.minecraftforge.items.wrapper.RangedWrapper(buffer, 1, 2));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveData(tag, null);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadData(tag, null);
    }
//?}

    private void saveData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        tag.put("Buffer", gregtech.engine.stack.HandlerNbt.save(buffer, lookup));
        saveProcess(tag);
    }

    private void loadData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        if (tag.contains("Buffer")) gregtech.engine.stack.HandlerNbt.load(buffer, tag.getCompound("Buffer"), lookup);
        loadProcess(tag);
    }
    private void saveProcess(CompoundTag tag) {
        tag.putInt("gt.mode", mode()); tag.putLong("gt.amount", amount);
        if (material != null) tag.putString("gt.material", material.getName());
    }
    private void loadProcess(CompoundTag tag) {
        if (buffer.getSlots() != 2) { var input = buffer.getStackInSlot(0).copy(); buffer.setSize(2); buffer.setStackInSlot(0, input); }
        pendingModeSync = Math.floorMod(tag.getInt("gt.mode"), 5);
        amount = Math.max(0, tag.getLong("gt.amount"));
        material = amount > 0 ? gregtech.engine.material.GTMaterialRegistry.get(tag.getString("gt.material")) : null;
        inventoryChanged = true;
    }
}
