package gregtech.content.registry;
//? if >=1.21.1 {
//?} else {

import gregtech.GregTech;
//?}
import gregtech.internal.tool.GTToolType;
import gregtech.content.item.GTToolItem;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.smeltery.SmelteryToolItem;
*///?} else {
//?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.bus.api.IEventBus;
import java.util.*;
/^* Original 37 manual-tool item IDs with one register and preserved smeltery aliases. ^/
*///?} else {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

//?}
public final class GTToolItems {
//? if >=1.21.1 {
 /*public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("gt6ce");
 public static final DeferredItem<SmelteryToolItem> CHISEL=ITEMS.register("tool_chisel",()->new SmelteryToolItem(SmelteryToolItem.Kind.CHISEL));
 public static final DeferredItem<SmelteryToolItem> PINCERS=ITEMS.register("tool_pincers",()->new SmelteryToolItem(SmelteryToolItem.Kind.PINCERS));
 private static final EnumMap<GTToolType,DeferredItem<? extends GTToolItem>> BY_TYPE=new EnumMap<>(GTToolType.class);
 static{for(var type:GTToolType.values())BY_TYPE.put(type,type==GTToolType.CHISEL?CHISEL:type==GTToolType.PINCERS?PINCERS:ITEMS.register("tool_"+type.id(),()->type.definition().isGun()?new gregtech.content.item.GunToolItem(new Item.Properties().stacksTo(1),type):type.definition().isPocket()?new gregtech.content.item.PocketToolItem(new Item.Properties().stacksTo(1),type):new GTToolItem(new Item.Properties().stacksTo(1),type)));}
 private GTToolItems(){}
 public static void register(IEventBus bus){if(BY_TYPE.size()!=GTToolType.values().length)throw new IllegalStateException("Incomplete manual tool register");ITEMS.register(bus);}
 public static Map<GTToolType,DeferredItem<? extends GTToolItem>> all(){return Collections.unmodifiableMap(BY_TYPE);}
 public static GTToolItem get(GTToolType type){var entry=BY_TYPE.get(type);return entry==null?null:entry.get();}
 public static ItemStack empty(GTToolType type){var item=get(type);return item==null?ItemStack.EMPTY:new ItemStack(item);}
*///?} else {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);

    private static final Map<GTToolType, RegistryObject<GTToolItem>> BY_TYPE = new EnumMap<>(GTToolType.class);

    static {
        for (GTToolType type : GTToolType.values()) {
            String id = "tool_" + type.id();
            RegistryObject<GTToolItem> holder = ITEMS.register(id, () -> type.definition().isGun()?new gregtech.content.item.GunToolItem(new Item.Properties().stacksTo(1),type):type.definition().isPocket()?new gregtech.content.item.PocketToolItem(new Item.Properties().stacksTo(1),type):new GTToolItem(new Item.Properties().stacksTo(1), type));
            BY_TYPE.put(type, holder);
        }
    }

    public static Map<GTToolType, RegistryObject<GTToolItem>> all() {
        return Collections.unmodifiableMap(BY_TYPE);
    }

    public static GTToolItem get(GTToolType type) {
        RegistryObject<GTToolItem> holder = BY_TYPE.get(type);
        return holder == null ? null : holder.get();
    }

    public static ItemStack empty(GTToolType type) {
        GTToolItem item = get(type);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private GTToolItems() {}
//?}
}
