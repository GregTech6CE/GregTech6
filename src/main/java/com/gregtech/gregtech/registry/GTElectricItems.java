package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.item.BatteryItem;
import com.gregtech.gregtech.item.ElectricToolItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Wave 47: Batteries and electric tools implementing IItemEnergy. */
public final class GTElectricItems {
    private static final List<RegistryObject<? extends Item>> ALL = new ArrayList<>();

    // Batteries
    public static RegistryObject<BatteryItem> BATTERY_LV;
    public static RegistryObject<BatteryItem> BATTERY_MV;
    public static RegistryObject<BatteryItem> BATTERY_HV;
    public static RegistryObject<BatteryItem> BATTERY_EV;
    public static RegistryObject<BatteryItem> BATTERY_IV;

    // Electric tools
    public static RegistryObject<ElectricToolItem> ELECTRIC_DRILL;
    public static RegistryObject<ElectricToolItem> ELECTRIC_CHAINSAW;
    public static RegistryObject<ElectricToolItem> ELECTRIC_WRENCH;
    public static RegistryObject<ElectricToolItem> ELECTRIC_SCREWDRIVER;

    private GTElectricItems() {}

    public static List<RegistryObject<? extends Item>> all() { return Collections.unmodifiableList(ALL); }

    private static <T extends Item> RegistryObject<T> reg(String id, Supplier<T> itemSupplier) {
        RegistryObject<T> ro = GTItems.ITEMS.register(id, itemSupplier);
        ALL.add(ro);
        return ro;
    }

    private static RegistryObject<BatteryItem> regBattery(String id) {
        var spec=com.gregtech.gregtech.content.energy.LegacyBatteryDefinitions.get(id);
        return reg(id,()->new BatteryItem(spec.name(),spec.capacity(),spec.tier(),new Item.Properties().stacksTo(1)));
    }

    private static ElectricToolItem electric(String id){var spec=com.gregtech.gregtech.content.tool.ElectricToolCatalog.get(id);return new ElectricToolItem(spec.name(),Tiers.IRON,spec.capacity(),1,spec.energyPerUse(),new Item.Properties().stacksTo(1));}
    public static void registerAll() {
        // Batteries (name, capacity EU, tier, properties)
        BATTERY_LV = regBattery("battery_lv");
        BATTERY_MV = regBattery("battery_mv");
        BATTERY_HV = regBattery("battery_hv");
        BATTERY_EV = regBattery("battery_ev");
        BATTERY_IV = regBattery("battery_iv");

        // Electric tools
        ELECTRIC_DRILL = reg("electric_drill", () -> electric("electric_drill"));
        ELECTRIC_CHAINSAW = reg("electric_chainsaw", () -> electric("electric_chainsaw"));
        ELECTRIC_WRENCH = reg("electric_wrench", () -> electric("electric_wrench"));
        ELECTRIC_SCREWDRIVER = reg("electric_screwdriver", () -> electric("electric_screwdriver"));
    }
}
