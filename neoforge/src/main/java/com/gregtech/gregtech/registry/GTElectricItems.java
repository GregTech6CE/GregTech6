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
 public static final DeferredItem<ElectricToolItem> ELECTRIC_DRILL=tool("electric_drill","Drill",100000,100),ELECTRIC_CHAINSAW=tool("electric_chainsaw","Chainsaw",100000,100),ELECTRIC_WRENCH=tool("electric_wrench","Wrench",50000,50),ELECTRIC_SCREWDRIVER=tool("electric_screwdriver","Screwdriver",50000,50);
 private static DeferredItem<ElectricToolItem> tool(String id,String name,long capacity,long use){var spec=com.gregtech.gregtech.content.tool.ElectricToolCatalog.get(id);return ITEMS.register(id,()->new ElectricToolItem(spec.name(),Tiers.IRON,spec.capacity(),1,spec.energyPerUse(),new Item.Properties().stacksTo(1)));}
 public static java.util.List<DeferredItem<ElectricToolItem>> tools(){return java.util.List.of(ELECTRIC_DRILL,ELECTRIC_CHAINSAW,ELECTRIC_WRENCH,ELECTRIC_SCREWDRIVER);}
 public static void register(IEventBus bus){ITEMS.register(bus);}
}
