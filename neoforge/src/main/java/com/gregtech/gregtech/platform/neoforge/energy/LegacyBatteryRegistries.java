package com.gregtech.gregtech.platform.neoforge.energy;
import com.gregtech.gregtech.content.energy.LegacyBatteryDefinitions;
import com.gregtech.gregtech.item.BatteryItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import java.util.*;
/** Retained original rechargeable item IDs; powered tools and placed chemical batteries are separate ports. */
public final class LegacyBatteryRegistries {
 private LegacyBatteryRegistries(){}
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gregtech");
 private static final List<DeferredHolder<Item,BatteryItem>> ALL=new ArrayList<>();
 static{for(var spec:LegacyBatteryDefinitions.ALL)ALL.add(ITEMS.register(spec.id(),()->new BatteryItem(spec.name(),spec.capacity(),spec.tier(),new Item.Properties().stacksTo(1))));}
 public static List<DeferredHolder<Item,BatteryItem>> all(){return Collections.unmodifiableList(ALL);}
 public static void register(IEventBus bus){ITEMS.register(bus);}
}
