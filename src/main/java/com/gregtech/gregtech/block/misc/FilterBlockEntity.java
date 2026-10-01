package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.blockentity.CapabilityRelayBlockEntity;
import com.gregtech.gregtech.api.fluid.FluidDisplayBinding;
import com.gregtech.gregtech.content.logistics.LogisticsItemFilter;
import com.gregtech.gregtech.content.logistics.LogisticsSemiFilteredItem;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.*;

/** Filter templates are ghosts, never stored cargo. Both insertion and extraction obey the same rule. */
public class FilterBlockEntity extends CapabilityRelayBlockEntity implements MenuProvider, LogisticsSemiFilteredItem {
    private boolean blacklist;
    private final SimpleContainer templates=new SimpleContainer(com.gregtech.gregtech.content.logistics.FilterPolicy.TEMPLATES) {
        @Override public void setChanged() { super.setChanged();FilterBlockEntity.this.setChanged(); }
    };
    public FilterBlockEntity(BlockPos pos,BlockState state) { super(com.gregtech.gregtech.registry.GTBlockEntities.FILTER.get(),pos,state); }
    public SimpleContainer templates() { return templates; }
    public boolean blacklist() { return blacklist; }
    public void toggleMode() { blacklist=!blacklist;setChanged(); }
    public void setTemplate(int slot,ItemStack stack) { if(slot>=0&&slot<com.gregtech.gregtech.content.logistics.FilterPolicy.TEMPLATES) templates.setItem(slot,prefixMode()&&slot!=0?ItemStack.EMPTY:stack.copyWithCount(1)); }
    public boolean prefixMode() { return kind().equals("oredict"); }
    public void clearFilter() { blacklist=false;templates.clearContent();setChanged(); }
    public String selectedPrefix() { return com.gregtech.gregtech.content.logistics.FilterRules.prefix(templates.getItem(0)); }
    @Override public LogisticsItemFilter logisticsItemFilter() {
        if (blacklist) return null;
        if (prefixMode()) {
            String prefix = selectedPrefix();
            return prefix.isEmpty() ? null : LogisticsItemFilter.prefix(prefix);
        }
        var selected = new java.util.ArrayList<ItemStack>(com.gregtech.gregtech.content.logistics.FilterPolicy.TEMPLATES);
        for (int i = 0; i < com.gregtech.gregtech.content.logistics.FilterPolicy.TEMPLATES; i++) selected.add(templates.getItem(i));
        return LogisticsItemFilter.items(selected);
    }
    private String kind() { return ((FilterBlock)getBlockState().getBlock()).filterType(); }
    @Override protected boolean supportsItems() { return !kind().equals("fluids"); }
    @Override protected boolean supportsFluids() { return kind().equals("fluids")||kind().equals("items_fluids")||prefixMode(); }
    @Override protected Target target(Direction side) {
        var front=getBlockState().getValue(FilterBlock.FACING);
        var exit=side==front?FilterBlock.secondary(getBlockState()):front;
        return new Target(worldPosition.relative(exit),exit.getOpposite());
    }
    @Override protected boolean permitsItem(Direction side,ItemStack stack) {
        return side==getBlockState().getValue(FilterBlock.FACING)||permitsItem(stack);
    }
    @Override protected boolean permitsFluid(Direction side,FluidStack stack) {
        return side==getBlockState().getValue(FilterBlock.FACING)||permitsFluid(stack);
    }
    @Override public boolean permitsItem(ItemStack stack) {
        if(stack.isEmpty())return false;
        if(prefixMode()) {
            var selected=selectedPrefix();
            return !selected.isEmpty()&&(blacklist!=selected.equals(com.gregtech.gregtech.content.logistics.FilterRules.prefix(stack)));
        }
        boolean found=false;
        for(int i=0;i<com.gregtech.gregtech.content.logistics.FilterPolicy.TEMPLATES&&!found;i++)found=com.gregtech.gregtech.content.logistics.FilterRules.itemMatches(templates.getItem(i),stack);
        return com.gregtech.gregtech.content.logistics.FilterPolicy.allows(blacklist,found);
    }
    @Override public boolean permitsFluid(FluidStack stack) {
        if(stack.isEmpty())return false;
        if(prefixMode())return true;
        boolean found=false;
        for(int i=0;i<com.gregtech.gregtech.content.logistics.FilterPolicy.TEMPLATES&&!found;i++) {
            var template=templates.getItem(i);var fluid=FluidDisplayBinding.resolve(template);
            if(fluid.isEmpty())fluid=FluidUtil.getFluidContained(template).orElse(FluidStack.EMPTY);
            found=!fluid.isEmpty()&&fluid.getFluid()==stack.getFluid();
        }
        return com.gregtech.gregtech.content.logistics.FilterPolicy.allows(blacklist,found);
    }
    @Override public Component getDisplayName() { return Component.translatable(prefixMode()?"gregtech.filter.prefix_title":"gregtech.filter.title"); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new com.gregtech.gregtech.client.gui.FilterMenu(id,inventory,this); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.putBoolean("gt.blacklist",blacklist);var list=new ListTag();
        for(int i=0;i<com.gregtech.gregtech.content.logistics.FilterPolicy.TEMPLATES;i++) if(!templates.getItem(i).isEmpty()) { var entry=templates.getItem(i).save(new CompoundTag());entry.putInt("Slot",i);list.add(entry); }
        tag.put("gt.filter_templates",list);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);blacklist=tag.getBoolean("gt.blacklist");templates.clearContent();
        for(var entry:tag.getList("gt.filter_templates",10)) {
            var data=(CompoundTag)entry;var stack=ItemStack.of(data);
            if(prefixMode()) {if(selectedPrefix().isEmpty()&&!com.gregtech.gregtech.content.logistics.FilterRules.prefix(stack).isEmpty())setTemplate(0,stack);}
            else setTemplate(data.getInt("Slot"),stack);
        }
    }
}
