package gregtech.content.block.misc;

import gregtech.content.blockentity.CapabilityRelayBlockEntity;
import gregtech.internal.fluid.FluidDisplayBinding;
import gregtech.content.logistics.LogisticsItemFilter;
import gregtech.content.logistics.LogisticsSemiFilteredItem;
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

/** Ghost templates select cargo; front-side capability operations bypass template selection. */
public class FilterBlockEntity extends CapabilityRelayBlockEntity implements MenuProvider, LogisticsSemiFilteredItem {
    private boolean blacklist;
    private final SimpleContainer templates = new SimpleContainer(gregtech.content.logistics.FilterPolicy.TEMPLATES) {

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
        if (slot >= 0 && slot < gregtech.content.logistics.FilterPolicy.TEMPLATES) templates.setItem(slot,
                prefixMode() && slot != 0 ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    public boolean prefixMode() {
        return kind().equals("oredict");
    }

    public void clearFilter() {
        blacklist = false;
        templates.clearContent();
        setChanged();
    }

    public String selectedPrefix() {
        return gregtech.content.logistics.FilterRules.prefix(templates.getItem(0));
    }

    @Override
    public LogisticsItemFilter logisticsItemFilter() {
        if (blacklist) return null;
        if (prefixMode()) {
            String prefix = selectedPrefix();
            return prefix.isEmpty() ? null : LogisticsItemFilter.prefix(prefix);
        }
        var selected = new java.util.ArrayList<ItemStack>(gregtech.content.logistics.FilterPolicy.TEMPLATES);
        for (int templateSlot = 0; templateSlot < gregtech.content.logistics.FilterPolicy.TEMPLATES; templateSlot++) selected.add(
                templates.getItem(templateSlot));
        return LogisticsItemFilter.items(selected);
    }

    private String kind() {
        return ((FilterBlock) getBlockState().getBlock()).filterType();
    }

    @Override
    protected boolean supportsItems() {
        return !kind().equals("fluids");
    }

    @Override
    protected boolean supportsFluids() {
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
            return !selected.isEmpty() && (blacklist != selected.equals(gregtech.content.logistics.FilterRules.prefix(stack)));
        }
        boolean found = false;
        for (int templateSlot = 0; templateSlot < gregtech.content.logistics.FilterPolicy.TEMPLATES && !found; templateSlot++)
            found = gregtech.content.logistics.FilterRules.itemMatches(templates.getItem(templateSlot), stack);
        return gregtech.content.logistics.FilterPolicy.allows(blacklist, found);
    }

    @Override
    public boolean permitsFluid(FluidStack stack) {
        if (stack.isEmpty()) return false;
        if (prefixMode()) return true;
        boolean found = false;
        for (int templateSlot = 0; templateSlot < gregtech.content.logistics.FilterPolicy.TEMPLATES && !found; templateSlot++) {
//? if >=1.21.1 {
            /*var template = templates.getItem(templateSlot);
            var fluid = level == null ? FluidStack.EMPTY : FluidDisplayBinding.resolve(level.registryAccess(), template);
*///?} else {
            var template = templates.getItem(templateSlot);
            var fluid = FluidDisplayBinding.resolve(template);
//?}
            if (fluid.isEmpty()) fluid = FluidUtil.getFluidContained(template).orElse(FluidStack.EMPTY);
            found = !fluid.isEmpty() && fluid.getFluid() == stack.getFluid();
        }
        return gregtech.content.logistics.FilterPolicy.allows(blacklist, found);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(prefixMode() ? "gt6ce.filter.prefix_title" : "gt6ce.filter.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new gregtech.client.gui.FilterMenu(id, inventory, this);
    }
//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putBoolean("gt.blacklist", blacklist);
        var list = new ListTag();
        for (int templateSlot = 0; templateSlot < gregtech.content.logistics.FilterPolicy.TEMPLATES; templateSlot++)
            if (!templates.getItem(templateSlot).isEmpty()) {
                var entry = (CompoundTag) templates.getItem(templateSlot).save(lookup);
                entry.putInt("Slot", templateSlot);
                list.add(entry);
            }
*///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("gt.blacklist", blacklist);
        var list = new ListTag();
        for (int templateSlot = 0; templateSlot < gregtech.content.logistics.FilterPolicy.TEMPLATES; templateSlot++)
            if (!templates.getItem(templateSlot).isEmpty()) {
                var entry = templates.getItem(templateSlot).save(new CompoundTag());
                entry.putInt("Slot", templateSlot);
                list.add(entry);
            }
//?}
        tag.put("gt.filter_templates", list);
    }
//? if >=1.21.1 {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        blacklist = tag.getBoolean("gt.blacklist");
        templates.clearContent();
*///?} else {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        blacklist = tag.getBoolean("gt.blacklist");
        templates.clearContent();
//?}
        for (var entry : tag.getList("gt.filter_templates", 10)) {
//? if >=1.21.1 {
            /*var entryData = (CompoundTag) entry;
            var templateStack = ItemStack.parseOptional(lookup, entryData);
*///?} else {
            var entryData = (CompoundTag) entry;
            var templateStack = ItemStack.of(entryData);
//?}
            if (prefixMode()) {
                if (selectedPrefix().isEmpty() && !gregtech.content.logistics.FilterRules.prefix(templateStack).isEmpty()) setTemplate(0,
                        templateStack);
            } else setTemplate(entryData.getInt("Slot"), templateStack);
        }
    }
}
