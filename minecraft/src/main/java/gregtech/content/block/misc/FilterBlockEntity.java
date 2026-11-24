package gregtech.content.block.misc;

import gregtech.content.blockentity.CapabilityRelayBlockEntity;
import gregtech.content.logistics.FilterPolicy;
import gregtech.content.logistics.FilterRules;
import gregtech.content.logistics.LogisticsItemFilter;
import gregtech.content.logistics.LogisticsSemiFilteredItem;
import gregtech.engine.fluid.FluidDisplayBinding;
import gregtech.engine.stack.NbtCompat;
import java.util.BitSet;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.*;
*///?} else {
import net.minecraftforge.fluids.*;
//?}

import javax.annotation.Nullable;

/** Ghost templates select cargo; front-side capability operations bypass template selection. */
public class FilterBlockEntity extends CapabilityRelayBlockEntity implements MenuProvider, LogisticsSemiFilteredItem {
    private boolean blacklist;
    private final BitSet ignoreItemData = new BitSet(FilterPolicy.TEMPLATES);
    private final SimpleContainer templates = new SimpleContainer(FilterPolicy.TEMPLATES) {

        @Override
        public void setChanged() {
            super.setChanged();
            FilterBlockEntity.this.setChanged();
        }
    };

    public FilterBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.FILTER.get(), pos, state);
    }

    public SimpleContainer templates() {
        return templates;
    }

    public boolean blacklist() {
        return blacklist;
    }

    public void toggleMode() {
        blacklist = !blacklist;
        setChanged();
    }

    public void setTemplate(int slot, ItemStack stack) {
        setTemplate(slot, stack, false);
    }

    private void setTemplate(int slot, ItemStack stack, boolean ignoreData) {
        if (!FilterPolicy.validSlot(slot)) return;
        ignoreItemData.set(slot, ignoreData && !stack.isEmpty() && supportsItemMatchModes());
        templates.setItem(slot, prefixMode() && slot != 0 ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    public boolean supportsItemMatchModes() {
        return supportsItems() && !prefixMode();
    }

    public boolean ignoresItemData(int slot) {
        return FilterPolicy.validSlot(slot) && ignoreItemData.get(slot);
    }

    public void toggleItemMatchMode(int slot) {
        if (!FilterPolicy.validSlot(slot) || templates.getItem(slot).isEmpty() || !supportsItemMatchModes()) return;
        ignoreItemData.flip(slot);
        setChanged();
    }

    public boolean prefixMode() {
        return kind().equals("oredict");
    }

    public void clearFilter() {
        blacklist = false;
        ignoreItemData.clear();
        templates.clearContent();
        setChanged();
    }

    public String selectedPrefix() {
        return FilterRules.prefix(templates.getItem(0));
    }

    @Override
    public LogisticsItemFilter logisticsItemFilter() {
        if (blacklist || !supportsItems()) return null;
        if (prefixMode()) {
            String prefix = selectedPrefix();
            return prefix.isEmpty() ? null : LogisticsItemFilter.prefix(prefix);
        }
        return LogisticsItemFilter.items(templates, ignoreItemData);
    }

    private String kind() {
        return ((FilterBlock) getBlockState().getBlock()).filterType();
    }

    @Override
    protected boolean supportsItems() {
        return !kind().equals("fluids");
    }

    @Override
    public boolean supportsFluids() {
        return kind().equals("fluids") || kind().equals("items_fluids") || prefixMode();
    }

    @Override
    protected Target target(Direction side) {
        var front = getBlockState().getValue(FilterBlock.FACING);
        var exit = side == front ? FilterBlock.secondary(getBlockState()) : front;
        return new Target(worldPosition.relative(exit), exit.getOpposite());
    }

    @Override
    protected boolean permitsItem(Direction side, ItemStack stack) {
        return side == getBlockState().getValue(FilterBlock.FACING) || permitsItem(stack);
    }

    @Override
    protected boolean permitsFluid(Direction side, FluidStack stack) {
        return side == getBlockState().getValue(FilterBlock.FACING) || permitsFluid(stack);
    }

    @Override
    public boolean permitsItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (prefixMode()) {
            var selected = selectedPrefix();
            return !selected.isEmpty() && (blacklist != selected.equals(FilterRules.prefix(stack)));
        }
        boolean found = false;
        for (int slot = 0; slot < FilterPolicy.TEMPLATES && !found; slot++)
            found = FilterRules.itemMatches(templates.getItem(slot), stack, ignoresItemData(slot));
        return FilterPolicy.allows(blacklist, found);
    }

    @Override
    public boolean permitsFluid(FluidStack stack) {
        if (stack.isEmpty()) return false;
        if (prefixMode()) return true;
        boolean found = false;
        for (int templateSlot = 0; templateSlot < FilterPolicy.TEMPLATES && !found; templateSlot++) {
            var template = templates.getItem(templateSlot);
            var fluid = level == null ? FluidStack.EMPTY : FluidDisplayBinding.resolve(level.registryAccess(), template);
            if (fluid.isEmpty()) fluid = FluidUtil.getFluidContained(template).orElse(FluidStack.EMPTY);
            found = !fluid.isEmpty() && fluid.getFluid() == stack.getFluid();
        }
        return FilterPolicy.allows(blacklist, found);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new gregtech.client.gui.FilterMenu(id, inventory, this);
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putBoolean("gt.blacklist", blacklist);
        var list = new ListTag();
        for (int templateSlot = 0; templateSlot < FilterPolicy.TEMPLATES; templateSlot++)
            if (!templates.getItem(templateSlot).isEmpty()) {
                var entry = (CompoundTag) NbtCompat.saveItem(templates.getItem(templateSlot), lookup);
                entry.putInt("Slot", templateSlot);
                if (ignoresItemData(templateSlot)) entry.putBoolean("IgnoreItemData", true);
                list.add(entry);
            }
        tag.put("gt.filter_templates", list);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        blacklist = tag.getBoolean("gt.blacklist");
        ignoreItemData.clear();
        templates.clearContent();
        for (var entry : tag.getList("gt.filter_templates", 10)) {
            var entryData = (CompoundTag) entry;
            var templateStack = NbtCompat.readItem(entryData, lookup);
            if (prefixMode()) {
                if (selectedPrefix().isEmpty() && !FilterRules.prefix(templateStack).isEmpty()) setTemplate(0,
                        templateStack);
            } else setTemplate(entryData.getInt("Slot"), templateStack, entryData.getBoolean("IgnoreItemData"));
        }
    }
}
