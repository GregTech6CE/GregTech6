package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.item.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
public final class GTElectricItems {
 private GTElectricItems(){}
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("gregtech");
 // Java compatibility handles only; these do not register or expose port-only items.
 private static java.util.function.Supplier<ChemicalBatteryItem> battery(String id){var spec=com.gregtech.gregtech.content.energy.BatteryItemMigration.target(id);return ()->GTChemicalBatteries.item(spec.chemistry(),spec.tier());}
 @Deprecated public static final java.util.function.Supplier<ChemicalBatteryItem> BATTERY_LV=battery("battery_lv"),BATTERY_MV=battery("battery_mv"),BATTERY_HV=battery("battery_hv"),BATTERY_EV=battery("battery_ev"),BATTERY_IV=battery("battery_iv");
 private static final java.util.Map<String,DeferredItem<ElectricToolItem>> BY_ID=new java.util.LinkedHashMap<>();
 static {for(var spec:com.gregtech.gregtech.content.tool.ElectricToolCatalog.ALL)BY_ID.put(spec.id(),ITEMS.register(spec.id(),()->new ElectricToolItem(spec.name(),Tiers.IRON,spec.capacity(),spec.tier(),spec.energyPerUse(),new Item.Properties().stacksTo(1))));}
 public static ElectricToolItem get(String id){return BY_ID.get(id).get();}
 public static java.util.List<DeferredItem<ElectricToolItem>> tools(){return java.util.List.copyOf(BY_ID.values());}
 public static final DeferredItem<ElectricToolItem> ELECTRIC_DRILL=BY_ID.get("electric_drill"),ELECTRIC_CHAINSAW=BY_ID.get("electric_chainsaw"),ELECTRIC_WRENCH=BY_ID.get("electric_wrench"),ELECTRIC_SCREWDRIVER=BY_ID.get("electric_screwdriver");
 public static void register(IEventBus bus){ITEMS.register(bus);}
}
