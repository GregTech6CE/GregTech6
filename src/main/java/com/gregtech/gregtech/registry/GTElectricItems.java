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

    public static void registerAll() {
        // Batteries (name, capacity EU, tier, properties)
        BATTERY_LV = reg("battery_lv", () -> new BatteryItem("LV Battery", 100_000, 1, new Item.Properties().stacksTo(1)));
        BATTERY_MV = reg("battery_mv", () -> new BatteryItem("MV Battery", 400_000, 2, new Item.Properties().stacksTo(1)));
        BATTERY_HV = reg("battery_hv", () -> new BatteryItem("HV Battery", 1_600_000, 3, new Item.Properties().stacksTo(1)));
        BATTERY_EV = reg("battery_ev", () -> new BatteryItem("EV Battery", 6_400_000, 4, new Item.Properties().stacksTo(1)));
        BATTERY_IV = reg("battery_iv", () -> new BatteryItem("IV Battery", 25_600_000, 5, new Item.Properties().stacksTo(1)));

        // Electric tools
        ELECTRIC_DRILL = reg("electric_drill", () -> new ElectricToolItem("Drill", Tiers.IRON, 100_000, 1, 100, new Item.Properties().stacksTo(1)));
        ELECTRIC_CHAINSAW = reg("electric_chainsaw", () -> new ElectricToolItem("Chainsaw", Tiers.IRON, 100_000, 1, 100, new Item.Properties().stacksTo(1)));
        ELECTRIC_WRENCH = reg("electric_wrench", () -> new ElectricToolItem("Wrench", Tiers.IRON, 50_000, 1, 50, new Item.Properties().stacksTo(1)));
        ELECTRIC_SCREWDRIVER = reg("electric_screwdriver", () -> new ElectricToolItem("Screwdriver", Tiers.IRON, 50_000, 1, 50, new Item.Properties().stacksTo(1)));
    }
}
