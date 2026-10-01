package com.gregtech.gregtech.content.transport;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import java.util.function.Supplier;
/** Thin stack/capability boundary for the shared masson-derived transfer accounting. */
public final class ItemPipeTransferAdapter {
 private ItemPipeTransferAdapter(){}
 private static final java.util.Set<IItemHandler> REPORTED=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
 public static ItemPipeTransfer.Result transfer(ItemStack offered,Supplier<IItemHandler> lookup){
  IItemHandler[] current=new IItemHandler[1];
  var result=ItemPipeTransfer.transfer(offered,new ItemPipeTransfer.Port<ItemStack>(){
   public boolean begin(boolean simulate){current[0]=lookup.get();return current[0]!=null;}
   public int slots(){return current[0].getSlots();}
   public ItemStack stack(int slot){return current[0].getStackInSlot(slot);}
   public int count(ItemStack stack){return stack.getCount();}
   public ItemStack copyWithCount(ItemStack stack,int count){return stack.copyWithCount(count);}
   public boolean same(ItemStack first,ItemStack second){return ItemStack.isSameItemSameTags(first,second);}
   public ItemStack insert(int slot,ItemStack stack,boolean simulate){return current[0].insertItem(slot,stack,simulate);}
  });
  if(result.handlerFailed()){var handler=current[0];if(handler!=null&&REPORTED.add(handler))com.mojang.logging.LogUtils.getLogger().warn("Item handler {} returned an invalid remainder or threw; unconfirmed pipe source retained",handler.getClass().getName());}
  return result;
 }
}
