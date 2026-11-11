package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.client.gui.AdvancedCraftingMenu;
import gregtech.client.gui.BasicMachineContainerMenu;
import gregtech.client.gui.BehaviorMachineMenu;
import gregtech.client.gui.BumbliaryContainerMenu;
import gregtech.client.gui.DataSwitchMenu;
import gregtech.client.gui.FilterMenu;
import gregtech.client.gui.GarbageDumpContainerMenu;
import gregtech.client.gui.HopperContainerMenu;
import gregtech.client.gui.MetalChestMenu;
import gregtech.client.gui.PartMenu;
import gregtech.client.gui.StorageContainerMenu;
import gregtech.content.block.inventory.UsbSwitchBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** Shared menu catalog; only the platform menu-type factory differs. */
public final class GTMenuTypes {
    private GTMenuTypes() {}

//? if >=1.21.1 {
    /*public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, REGISTRY_NAMESPACE);
*///?} else {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, REGISTRY_NAMESPACE);
//?}

    public static final Supplier<MenuType<HopperContainerMenu>> METAL_CHEST = MENU_TYPES.register(
            "metal_chest", () -> create((id, inventory, buffer) -> new MetalChestMenu(id, inventory)));

    public static final Supplier<MenuType<HopperContainerMenu>> STORAGE_15 = MENU_TYPES.register(
            "storage_15", () -> create((id, inventory, buffer) -> new StorageContainerMenu(id, inventory, 15)));
    public static final Supplier<MenuType<HopperContainerMenu>> STORAGE_36 = MENU_TYPES.register(
            "storage_36", () -> create((id, inventory, buffer) -> new StorageContainerMenu(id, inventory, 36)));

    public static final Supplier<MenuType<AdvancedCraftingMenu>> ADVANCED_CRAFTING = MENU_TYPES.register(
            "advanced_crafting", () -> create((id, inventory, buffer) -> new AdvancedCraftingMenu(id, inventory)));

    public static final Supplier<MenuType<BasicMachineContainerMenu>> BASIC_MACHINE =
            MENU_TYPES.register("basic_machine", GTMenuTypes::createBasicMachine);

    public static final Supplier<MenuType<BehaviorMachineMenu>> BEHAVIOR_MACHINE = MENU_TYPES.register(
            "behavior_machine",
            () -> create((id, inventory, buffer) -> new BehaviorMachineMenu(id, inventory, buffer)));

    public static final Supplier<MenuType<PartMenu>> PART = MENU_TYPES.register(
            "part", () -> create((id, inventory, buffer) -> new PartMenu(id, inventory, buffer)));

    public static final Supplier<MenuType<HopperContainerMenu>> GARBAGE_DUMP = MENU_TYPES.register(
            "garbage_dump", () -> create((id, inventory, buffer) -> new GarbageDumpContainerMenu(id, inventory)));

    public static final Supplier<MenuType<FilterMenu>> FILTER = MENU_TYPES.register(
            "filter", () -> create((id, inventory, buffer) -> new FilterMenu(id, inventory, buffer.readBoolean())));

    public static final Supplier<MenuType<DataSwitchMenu>> USB_SWITCH = MENU_TYPES.register(
            "usb_switch",
            () -> create((id, inventory, buffer) ->
                    new DataSwitchMenu(id, inventory, UsbSwitchBlock.Kind.USB)));
    public static final Supplier<MenuType<DataSwitchMenu>> HDD_SWITCH = MENU_TYPES.register(
            "hdd_switch",
            () -> create((id, inventory, buffer) ->
                    new DataSwitchMenu(id, inventory, UsbSwitchBlock.Kind.HDD)));

    /** GT6's bumbliary GUI (36 slots) and the advanced one (20 slots). */
    public static final Supplier<MenuType<BumbliaryContainerMenu>> BUMBLIARY = MENU_TYPES.register(
            "bumbliary", () -> create((id, inventory, buffer) -> new BumbliaryContainerMenu(id, inventory, false)));
    public static final Supplier<MenuType<BumbliaryContainerMenu>> ADVANCED_BUMBLIARY = MENU_TYPES.register(
            "advanced_bumbliary",
            () -> create((id, inventory, buffer) -> new BumbliaryContainerMenu(id, inventory, true)));

    private static final int[] HOPPER_SIZES = gregtech.engine.inventory.InventorySlotLayout.supportedSizes();
    private static final Map<Integer, Supplier<MenuType<HopperContainerMenu>>> BY_SIZE = new HashMap<>();

    public static void registerAll() {
        for (int n : HOPPER_SIZES)
            BY_SIZE.computeIfAbsent(
                    n,
                    size -> MENU_TYPES.register(
                            "hopper_" + size,
                            () -> create((id, inventory, buffer) -> new HopperContainerMenu(id, inventory, size))));
    }

    /** Return the MenuType matching a given slot count, or the closest supported size. */
    public static MenuType<HopperContainerMenu> forSlotCount(int n) {
        var exact = BY_SIZE.get(n);
        if (exact != null) return exact.get();
        for (int size : HOPPER_SIZES) if (size >= n) return BY_SIZE.get(size).get();
        return BY_SIZE.get(36).get();
    }

    /** All registered hopper MenuTypes for screen registration. */
    public static Iterable<Supplier<MenuType<HopperContainerMenu>>> allHopperTypes() {
        return BY_SIZE.values();
    }

    public static void submit(IEventBus bus) {
        MENU_TYPES.register(bus);
    }

    public static MenuType<HopperContainerMenu> storageForSlotCount(int size) {
        return switch (size) {
            case 15 -> STORAGE_15.get();
            case 36 -> STORAGE_36.get();
            default -> throw new IllegalArgumentException("Unsupported storage size " + size);
        };
    }

    private static <T extends AbstractContainerMenu> MenuType<T> create(MenuFactory<T> factory) {
//? if >=1.21.1 {
        return IMenuTypeExtension.create(factory::create);
//?} else {
        return IForgeMenuType.create(factory::create);
//?}
    }

//? if >=1.21.1 {
    /*private static MenuType<BasicMachineContainerMenu> createBasicMachine() {
        return IMenuTypeExtension.create(BasicMachineContainerMenu::new);
    }
*///?} else {
    private static MenuType<BasicMachineContainerMenu> createBasicMachine() {
        return IForgeMenuType.create(
                (id, inventory, buffer) -> new BasicMachineContainerMenu(id, inventory, buffer));
    }
//?}

    @FunctionalInterface
    private interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int id, Inventory inventory, FriendlyByteBuf buffer);
    }
}
