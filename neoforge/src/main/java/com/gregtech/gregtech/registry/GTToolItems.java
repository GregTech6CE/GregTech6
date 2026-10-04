package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryToolItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.bus.api.IEventBus;
import java.util.*;
/** Original 37 manual-tool item IDs with one register and preserved smeltery aliases. */
public final class GTToolItems {
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("gregtech");
 public static final DeferredItem<SmelteryToolItem> CHISEL=ITEMS.register("tool_chisel",()->new SmelteryToolItem(SmelteryToolItem.Kind.CHISEL));
 public static final DeferredItem<SmelteryToolItem> PINCERS=ITEMS.register("tool_pincers",()->new SmelteryToolItem(SmelteryToolItem.Kind.PINCERS));
 private static final EnumMap<GTToolType,DeferredItem<? extends GTToolItem>> BY_TYPE=new EnumMap<>(GTToolType.class);
 static{for(var type:GTToolType.values())BY_TYPE.put(type,type==GTToolType.CHISEL?CHISEL:type==GTToolType.PINCERS?PINCERS:ITEMS.register("tool_"+type.id(),()->type.definition().isGun()?new com.gregtech.gregtech.item.GunToolItem(new Item.Properties().stacksTo(1),type):type.definition().isPocket()?new com.gregtech.gregtech.item.PocketToolItem(new Item.Properties().stacksTo(1),type):new GTToolItem(new Item.Properties().stacksTo(1),type)));}
 private GTToolItems(){}
 public static void register(IEventBus bus){if(BY_TYPE.size()!=GTToolType.values().length)throw new IllegalStateException("Incomplete manual tool register");ITEMS.register(bus);}
 public static Map<GTToolType,DeferredItem<? extends GTToolItem>> all(){return Collections.unmodifiableMap(BY_TYPE);}
 public static GTToolItem get(GTToolType type){var entry=BY_TYPE.get(type);return entry==null?null:entry.get();}
 public static ItemStack empty(GTToolType type){var item=get(type);return item==null?ItemStack.EMPTY:new ItemStack(item);}
}
