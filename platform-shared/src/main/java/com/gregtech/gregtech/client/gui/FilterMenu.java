package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.block.misc.FilterBlockEntity;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Ghost slots cannot be extracted, dragged, shift-clicked or swapped into player inventory. */
public class FilterMenu extends AbstractContainerMenu {
    private final FilterBlockEntity filter;
    private final boolean prefixMode;
    private final int templateSlots;
    private final Container templates;
    private final ContainerData data;
    public FilterMenu(int id,Inventory inventory) { this(id,inventory,null); }
    public FilterMenu(int id,Inventory inventory,boolean prefixMode) {this(id,inventory,null,prefixMode);}
    public FilterMenu(int id,Inventory inventory,FilterBlockEntity filter) {this(id,inventory,filter,filter!=null&&filter.prefixMode());}
    private FilterMenu(int id,Inventory inventory,FilterBlockEntity filter,boolean prefixMode) {

        super(GTMenuTypes.FILTER.get(),id);this.filter=filter;this.prefixMode=prefixMode;this.templateSlots=prefixMode?1:com.gregtech.gregtech.content.logistics.FilterPolicy.TEMPLATES;templates=filter==null?new SimpleContainer(com.gregtech.gregtech.content.logistics.FilterPolicy.TEMPLATES):filter.templates();
        data=filter==null?new SimpleContainerData(1):new ContainerData() {
            public int get(int key) { return filter.blacklist()?1:0; }
            public void set(int key,int value) {}
            public int getCount() { return 1; }
        };addDataSlots(data);
        for(int slot=0;slot<templateSlots;slot++) addSlot(new Slot(templates,slot,prefixMode?80:8+slot%9*18,18+slot/9*18) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(inventory,col+row*9+9,8+col*18,(prefixMode?50:140)+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,prefixMode?108:198));
    }
    public boolean prefixMode(){return prefixMode;}
    public boolean blacklist() { return data.get(0)!=0; }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(slot>=0 && slot<templateSlots) {
            if(type==ClickType.PICKUP) setGhost(slot,button==1?ItemStack.EMPTY:getCarried());
            broadcastChanges();return;
        }
        super.clicked(slot,button,type,player);
    }
    private void setGhost(int slot,ItemStack stack){
        if(prefixMode&&!stack.isEmpty()&&com.gregtech.gregtech.content.logistics.FilterRules.prefix(stack).isEmpty())return;
        if(filter!=null)filter.setTemplate(slot,stack);else templates.setItem(slot,stack.copyWithCount(1));
    }
    @Override public boolean clickMenuButton(Player player,int id) { if(id!=0||filter==null||!stillValid(player)) return false;filter.toggleMode();broadcastChanges();return true; }
    @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return filter==null || !filter.isRemoved() && filter.getLevel()==player.level() && player.distanceToSqr(filter.getBlockPos().getCenter())<=64; }
}
