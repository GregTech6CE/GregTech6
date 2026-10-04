package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.item.ChemicalBatteryItem;
import com.gregtech.gregtech.item.ElectricToolItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Electric tools; old Java battery handles resolve to real chemical items without registration. */
public final class GTElectricItems {
    private static final List<RegistryObject<? extends Item>> ALL = new ArrayList<>();

    // Compatibility handles only: no port-only item appears in the registry or creative list.
    @Deprecated public static final Supplier<ChemicalBatteryItem> BATTERY_LV = battery("battery_lv");
    @Deprecated public static final Supplier<ChemicalBatteryItem> BATTERY_MV = battery("battery_mv");
    @Deprecated public static final Supplier<ChemicalBatteryItem> BATTERY_HV = battery("battery_hv");
    @Deprecated public static final Supplier<ChemicalBatteryItem> BATTERY_EV = battery("battery_ev");
    @Deprecated public static final Supplier<ChemicalBatteryItem> BATTERY_IV = battery("battery_iv");

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

    private static Supplier<ChemicalBatteryItem> battery(String oldId) {
        var spec = com.gregtech.gregtech.content.energy.BatteryItemMigration.target(oldId);
        return () -> GTChemicalBatteries.item(spec.chemistry(), spec.tier());
    }

    private static ElectricToolItem electric(String id){var spec=com.gregtech.gregtech.content.tool.ElectricToolCatalog.get(id);return new ElectricToolItem(spec.name(),Tiers.IRON,spec.capacity(),1,spec.energyPerUse(),new Item.Properties().stacksTo(1));}
    public static void registerAll() {
        // Electric tools
        ELECTRIC_DRILL = reg("electric_drill", () -> electric("electric_drill"));
        ELECTRIC_CHAINSAW = reg("electric_chainsaw", () -> electric("electric_chainsaw"));
        ELECTRIC_WRENCH = reg("electric_wrench", () -> electric("electric_wrench"));
        ELECTRIC_SCREWDRIVER = reg("electric_screwdriver", () -> electric("electric_screwdriver"));
    }
}
