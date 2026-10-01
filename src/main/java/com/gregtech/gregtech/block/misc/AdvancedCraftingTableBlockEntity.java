package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.api.inventory.BlockContents;
import com.gregtech.gregtech.client.gui.HopperContainerMenu;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

/** GT6 Advanced Crafting Table: 71 addressed slots, ordinary grid and two independent stores.
 * Slot layout and consumption order follow MultiTileEntityAdvancedCraftingTable (LGPL-3.0-or-later).
 */
public class AdvancedCraftingTableBlockEntity extends BlockEntity implements BlockContents {
    public static final int SIZE=com.gregtech.gregtech.content.storage.AdvancedCraftingRules.SIZE;
    public static final int[] INPUTS=com.gregtech.gregtech.content.storage.AdvancedCraftingRules.inputs();
    public static final int[] STORAGE=com.gregtech.gregtech.content.storage.AdvancedCraftingRules.storage();
    private boolean blocked16,blocked36,filter16,filter36,flush,loading,capsValid=true;
    private LazyOptional<IItemHandler> itemCap=LazyOptional.empty();
    private long revision;
    private long cachedRevision=-1;
    private RecipeManager cachedManager;
    private Plan cachedPlan;
    private ItemStack cachedPreview=ItemStack.EMPTY;
    private final ItemStackHandler inventory=new ItemStackHandler(SIZE) {
        @Override protected void onContentsChanged(int slot) { if(!loading) inventoryChanged(); }
        @Override public int getSlotLimit(int slot) { return slot==30?1:64; }
        @Override public boolean isItemValid(int slot,ItemStack stack) { return slot!=31 && slot!=32 && (slot!=30 || isBlueprint(stack)); }
    };
    public AdvancedCraftingTableBlockEntity(BlockPos pos,BlockState state) {
        this(com.gregtech.gregtech.registry.GTBlockEntities.ADVANCED_CRAFTING_TABLE.get(),pos,state);
    }
    protected AdvancedCraftingTableBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {
        super(type,pos,state);
    }
    public ItemStackHandler items() { return inventory; }
    public void inventoryChanged() {
        revision++;setChanged();
        if(flush && IntStream.range(21,30).allMatch(i->inventory.getStackInSlot(i).isEmpty())) {flush=false;resetCapabilities();}
        if(level!=null&&!level.isClientSide) level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
    }
    public boolean charging(){return false;}
    public int modes() { return (blocked16?1:0)|(blocked36?2:0)|(filter16?4:0)|(filter36?8:0)|(flush?16:0)|(charging()?32:0); }
    public void toggleMode(boolean upper,boolean filter) {
        if(filter) { if(upper)filter16=!filter16;else filter36=!filter36; }
        else {if(upper)blocked16=!blocked16;else blocked36=!blocked36;}
        resetCapabilities();inventoryChanged();
    }
    public void flushGrid() { flush=true; resetCapabilities(); inventoryChanged(); }
    public void storeGrid() {
        for(int i=21;i<30;i++) {
            ItemStack rest=inventory.getStackInSlot(i).copy();
            for(int slot:STORAGE) if(ItemStack.isSameItemSameTags(rest,inventory.getStackInSlot(slot))) rest=inventory.insertItem(slot,rest,false);
            for(int slot:STORAGE) if(inventory.getStackInSlot(slot).isEmpty()) rest=inventory.insertItem(slot,rest,false);
            inventory.setStackInSlot(i,rest);
        }
        inventoryChanged();
    }
    public static boolean isBlueprint(ItemStack stack) {
        var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id!=null && id.getNamespace().equals("gregtech") && (id.getPath().equals("empty_blueprint")||id.getPath().equals("blueprint"));
    }
    public boolean writeBlueprint() {
        ItemStack blank=inventory.getStackInSlot(30);
        var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(blank.getItem());
        if(id==null || !id.toString().equals("gregtech:empty_blueprint"))return false;
        ItemStack written=new ItemStack(java.util.Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(com.gregtech.gregtech.GregTech.id("blueprint"))));
        net.minecraft.nbt.ListTag pattern=new net.minecraft.nbt.ListTag();
        for(int i=21;i<30;i++) pattern.add(inventory.getStackInSlot(i).copyWithCount(1).save(new CompoundTag()));
        written.getOrCreateTag().put("gt.CraftingPattern",pattern);
        ItemStack result=preview();if(!result.isEmpty())written.setHoverName(result.getHoverName());
        inventory.setStackInSlot(30,written);return true;
    }
    private ItemStack[] pattern() {
        ItemStack[] result=new ItemStack[9];
        var blueprint=inventory.getStackInSlot(30);
        var list=blueprint.hasTag()?blueprint.getTag().getList("gt.CraftingPattern",10):new net.minecraft.nbt.ListTag();
        for(int i=0;i<9;i++) result[i]=!inventory.getStackInSlot(i+21).isEmpty()?inventory.getStackInSlot(i+21).copyWithCount(1)
                :i<list.size()?ItemStack.of(list.getCompound(i)):ItemStack.EMPTY;
        return result;
    }
    private static CraftingContainer grid(ItemStack[] stacks) {
        var menu=new AbstractContainerMenu(null,0) {
            public boolean stillValid(Player player){return false;}
            public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
        };
        var grid=new TransientCraftingContainer(menu,3,3);
        for(int i=0;i<9;i++)grid.setItem(i,stacks[i].copy());return grid;
    }
    /** Same identity as GT6 equalTools: ordinary stacks include NBT; tools retain their own stats. */
    private static boolean same(ItemStack a,ItemStack b) {
        if(a.isEmpty()||b.isEmpty()||a.getItem()!=b.getItem())return false;
        if(a.getItem() instanceof com.gregtech.gregtech.item.GTToolItem) return com.gregtech.gregtech.api.tool.GTToolHelper.isUsable(a)&&com.gregtech.gregtech.api.tool.GTToolHelper.isUsable(b)
                &&com.gregtech.gregtech.api.tool.GTToolHelper.getHead(a)==com.gregtech.gregtech.api.tool.GTToolHelper.getHead(b);
        return ItemStack.isSameItemSameTags(a,b);
    }
    private static int source(ItemStack[] state,ItemStack wanted,int gridSlot) {
        return com.gregtech.gregtech.content.storage.AdvancedCraftingRules.source(i->same(state[i],wanted),i->state[i].getCount(),gridSlot);
    }
    private record Plan(ItemStack[] after,ItemStack output,CraftingRecipe recipe,CraftingContainer grid) {}
    private Plan plan() {
        if(level==null||level.isClientSide)return null;
        RecipeManager manager=level.getRecipeManager();
        if(cachedRevision==revision&&cachedManager==manager)return cachedPlan;
        cachedRevision=revision;cachedManager=manager;cachedPlan=null;cachedPreview=ItemStack.EMPTY;
        ItemStack[] pattern=pattern(),after=new ItemStack[SIZE],used=new ItemStack[9];int[] sources=new int[9];Arrays.fill(sources,-1);
        CraftingContainer template=grid(pattern);var displayed=manager.getRecipeFor(RecipeType.CRAFTING,template,level).orElse(null);
        if(displayed==null)return null;cachedPreview=displayed.assemble(template,level.registryAccess()).copy();
        for(int i=0;i<SIZE;i++)after[i]=inventory.getStackInSlot(i).copy();
        for(int i=0;i<9;i++) {
            used[i]=ItemStack.EMPTY;if(pattern[i].isEmpty())continue;
            int slot=source(after,pattern[i],21+i);if(slot<0)return null;
            sources[i]=slot;used[i]=after[slot].copyWithCount(1);after[slot].shrink(1);
        }
        CraftingContainer grid=grid(used);var recipe=manager.getRecipeFor(RecipeType.CRAFTING,grid,level).orElse(null);
        if(recipe==null)return null;
        ItemStack result=recipe.assemble(grid,level.registryAccess());if(result.isEmpty())return null;
        var remains=recipe.getRemainingItems(grid);
        for(int i=0;i<9;i++)if(!remains.get(i).isEmpty()) {
            ItemStack rest=remains.get(i).copy();int original=sources[i];
            if(original>=0)rest=put(after,original,rest);
            for(int slot:INPUTS)rest=put(after,slot,rest);
            // Never destroy a bucket/tool when all destinations are full. No source mutation on failure.
            if(!rest.isEmpty())return null;
        }
        return cachedPlan=new Plan(after,result,recipe,grid);
    }
    private static ItemStack put(ItemStack[] state,int slot,ItemStack stack) {
        if(stack.isEmpty())return ItemStack.EMPTY;
        ItemStack there=state[slot];if(!there.isEmpty()&&!ItemStack.isSameItemSameTags(there,stack))return stack;
        int n=Math.min(stack.getCount(),Math.min(64,stack.getMaxStackSize())-there.getCount());if(n<=0)return stack;
        state[slot]=stack.copyWithCount(there.getCount()+n);return stack.copyWithCount(stack.getCount()-n);
    }
    public ItemStack preview() { plan();return cachedPreview.copy(); }
    public ItemStack craftOutput() { Plan plan=plan();return plan==null?ItemStack.EMPTY:plan.output.copy(); }
    public ItemStack craftOne(Player player) {
        Plan plan=plan();if(plan==null)return ItemStack.EMPTY;
        loading=true;try {for(int i=0;i<SIZE;i++)inventory.setStackInSlot(i,plan.after[i].copy());}finally{loading=false;}
        inventoryChanged();ItemStack result=plan.output.copy();result.onCraftedBy(level,player,result.getCount());
        net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player,result.copy(),plan.grid);
        if(player instanceof net.minecraft.server.level.ServerPlayer sp)sp.awardRecipes(java.util.List.of(plan.recipe));
        return result;
    }
    public boolean usableBy(Player player) {return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    public HopperContainerMenu storageMenu(int id,Inventory player) {
        return new HopperContainerMenu(GTMenuTypes.forSlotCount(36),id,player,view(IntStream.range(35,71).toArray(),false)) {
            @Override public boolean stillValid(Player p){return usableBy(p);}
            @Override public void clicked(int slot,int button,ClickType type,Player p){super.clicked(slot,button,type,p);inventoryChanged();}
            @Override public ItemStack quickMoveStack(Player p,int slot){var result=super.quickMoveStack(p,slot);inventoryChanged();return result;}
            @Override public void broadcastChanges(){super.broadcastChanges();setChanged();}
        };
    }
    private int[] automationSlots() {
        return com.gregtech.gregtech.content.storage.AdvancedCraftingRules.automationSlots(flush,blocked16,blocked36);
    }
    private boolean accepts(int slot,ItemStack stack) {
        if(slot>=0&&slot<16) {if(filter16)for(int i=0;i<16;i++)if(same(stack,inventory.getStackInSlot(i)))return i==slot;return true;}
        if(slot>=35&&slot<71){if(filter36)for(int i=35;i<71;i++)if(same(stack,inventory.getStackInSlot(i)))return i==slot;return true;}
        return false;
    }
    private IItemHandlerModifiable view(int[] slots,boolean automation) {
        return new IItemHandlerModifiable() {
            private int actual(int slot){if(slot<0||slot>=slots.length)throw new IndexOutOfBoundsException(slot);return slots[slot];}
            public int getSlots(){return slots.length;}
            public ItemStack getStackInSlot(int slot){return inventory.getStackInSlot(actual(slot));}
            public int getSlotLimit(int slot){return inventory.getSlotLimit(actual(slot));}
            public boolean isItemValid(int slot,ItemStack stack){return !automation||accepts(actual(slot),stack);}
            public void setStackInSlot(int slot,ItemStack stack){if(automation)throw new UnsupportedOperationException();inventory.setStackInSlot(actual(slot),stack);}
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?inventory.insertItem(actual(slot),stack,simulate):stack;}
            public ItemStack extractItem(int slot,int count,boolean simulate){int i=actual(slot);return !automation||i==33||(flush&&i>=21&&i<30)?inventory.extractItem(i,count,simulate):ItemStack.EMPTY;}
        };
    }
    private IItemHandler automationHandler(){
        IItemHandler delegate=view(automationSlots(),true);
        return new IItemHandler(){
            public int getSlots(){return delegate.getSlots();}
            public ItemStack getStackInSlot(int slot){return delegate.getStackInSlot(slot);}
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return delegate.insertItem(slot,stack,simulate);}
            public ItemStack extractItem(int slot,int amount,boolean simulate){return delegate.extractItem(slot,amount,simulate);}
            public int getSlotLimit(int slot){return delegate.getSlotLimit(slot);}
            public boolean isItemValid(int slot,ItemStack stack){return delegate.isItemValid(slot,stack);}
        };
    }
    private void resetCapabilities(){itemCap.invalidate();itemCap=LazyOptional.empty();}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side) {
        if(cap==ForgeCapabilities.ITEM_HANDLER){if(!capsValid||isRemoved())return LazyOptional.empty();if(!itemCap.isPresent())itemCap=LazyOptional.of(this::automationHandler);return itemCap.cast();}
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();capsValid=false;resetCapabilities();}
    @Override public void reviveCaps(){super.reviveCaps();capsValid=true;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Inventory",inventory.serializeNBT());tag.putInt("Modes",modes());}
    @Override public void load(CompoundTag tag){super.load(tag);loading=true;try{var data=tag.getCompound("Inventory").copy();data.putInt("Size",SIZE);inventory.deserializeNBT(data);inventory.setStackInSlot(31,ItemStack.EMPTY);inventory.setStackInSlot(32,ItemStack.EMPTY);}finally{loading=false;}
        int mode=tag.getInt("Modes");blocked16=(mode&1)!=0;blocked36=(mode&2)!=0;filter16=(mode&4)!=0;filter36=(mode&8)!=0;flush=(mode&16)!=0;resetCapabilities();revision++;}
    @Override public void dropContents(){if(level==null||level.isClientSide)return;loading=true;try{for(int i=0;i<SIZE;i++){if(i!=31&&i!=32)BlockContents.drop(this,inventory.getStackInSlot(i).copy());inventory.setStackInSlot(i,ItemStack.EMPTY);}}finally{loading=false;}inventoryChanged();}
}
