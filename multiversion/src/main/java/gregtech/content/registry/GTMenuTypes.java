package gregtech.content.registry;
/** Source-compatible name pointing at the native module's sole menu registration. */


//? if >=1.21.1 {
/**///?} else {
import gregtech.GregTech;
import gregtech.client.gui.BasicMachineContainerMenu;
import gregtech.client.gui.HopperContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;


//?}
public final class GTMenuTypes {

//? if >=1.21.1 {
/* private GTMenuTypes(){}
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.BasicMachineContainerMenu>> BASIC_MACHINE=gregtech.platform.neoforge.machine.BasicMachineRegistries.MENU;
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.BehaviorMachineMenu>> BEHAVIOR_MACHINE=gregtech.platform.neoforge.machine.BasicMachineRegistries.BEHAVIOR_MENU;
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.PartMenu>> PART=gregtech.platform.neoforge.machine.BasicMachineRegistries.PART_MENU;
 public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENU_TYPES=net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.MENU,"gt6ce");
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.BumbliaryContainerMenu>> BUMBLIARY=MENU_TYPES.register("bumbliary",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buffer)->new gregtech.client.gui.BumbliaryContainerMenu(id,inventory,false)));
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.BumbliaryContainerMenu>> ADVANCED_BUMBLIARY=MENU_TYPES.register("advanced_bumbliary",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buffer)->new gregtech.client.gui.BumbliaryContainerMenu(id,inventory,true)));
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.AdvancedCraftingMenu>> ADVANCED_CRAFTING=MENU_TYPES.register("advanced_crafting",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buffer)->new gregtech.client.gui.AdvancedCraftingMenu(id,inventory)));
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.HopperContainerMenu>> GARBAGE_DUMP=MENU_TYPES.register("garbage_dump",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buffer)->new gregtech.client.gui.GarbageDumpContainerMenu(id,inventory)));
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.DataSwitchMenu>> USB_SWITCH=MENU_TYPES.register("usb_switch",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buffer)->new gregtech.client.gui.DataSwitchMenu(id,inventory,gregtech.content.block.inventory.UsbSwitchBlock.Kind.USB)));
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.DataSwitchMenu>> HDD_SWITCH=MENU_TYPES.register("hdd_switch",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buffer)->new gregtech.client.gui.DataSwitchMenu(id,inventory,gregtech.content.block.inventory.UsbSwitchBlock.Kind.HDD)));
 public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.FilterMenu>> FILTER=MENU_TYPES.register("filter",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buffer)->new gregtech.client.gui.FilterMenu(id,inventory,buffer.readBoolean())));
 private static final int[] HOPPER_SIZES=gregtech.internal.inventory.InventorySlotLayout.supportedSizes();
 private static final java.util.Map<Integer,net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.HopperContainerMenu>>> BY_SIZE=new java.util.HashMap<>();
 static{for(int n:HOPPER_SIZES)BY_SIZE.put(n,MENU_TYPES.register("hopper_"+n,()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inventory,buffer)->new gregtech.client.gui.HopperContainerMenu(id,inventory,n))));}
 public static net.minecraft.world.inventory.MenuType<gregtech.client.gui.HopperContainerMenu> forSlotCount(int n){var exact=BY_SIZE.get(n);if(exact!=null)return exact.get();for(int size:HOPPER_SIZES)if(size>=n)return BY_SIZE.get(size).get();return BY_SIZE.get(36).get();}
 public static Iterable<net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<gregtech.client.gui.HopperContainerMenu>>> allHopperTypes(){return BY_SIZE.values();}
 public static void register(net.neoforged.bus.api.IEventBus bus){MENU_TYPES.register(bus);}
*///?} else {
    private GTMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, GregTech.NAMESPACE);

    // All distinct hopper slot counts used by GT6 materials
    private static final int[] HOPPER_SIZES = gregtech.internal.inventory.InventorySlotLayout.supportedSizes();
    private static final Map<Integer, RegistryObject<MenuType<HopperContainerMenu>>> BY_SIZE = new HashMap<>();

    static {
        for (int n : HOPPER_SIZES) {
            BY_SIZE.put(n, MENU_TYPES.register("hopper_" + n,
                    () -> IForgeMenuType.create((w, i, b) -> new HopperContainerMenu(w, i, n))));
        }
    }

    /** Return the MenuType matching a given slot count, or the closest supported size. */
    public static MenuType<HopperContainerMenu> forSlotCount(int n) {
        RegistryObject<MenuType<HopperContainerMenu>> exact = BY_SIZE.get(n);
        if (exact != null) return exact.get();
        // Find nearest supported size >= n
        for (int size : HOPPER_SIZES) {
            if (size >= n) return BY_SIZE.get(size).get();
        }
        return BY_SIZE.get(36).get();
    }

    /** All registered hopper MenuTypes for screen registration. */
    public static Iterable<RegistryObject<MenuType<HopperContainerMenu>>> allHopperTypes() {
        return BY_SIZE.values();
    }

    public static final RegistryObject<MenuType<gregtech.client.gui.AdvancedCraftingMenu>> ADVANCED_CRAFTING =
            MENU_TYPES.register("advanced_crafting",()->IForgeMenuType.create((id,inventory,buffer)->new gregtech.client.gui.AdvancedCraftingMenu(id,inventory)));

    public static final RegistryObject<MenuType<BasicMachineContainerMenu>> BASIC_MACHINE =
            MENU_TYPES.register("basic_machine",
                    () -> IForgeMenuType.create((w, i, b) -> new BasicMachineContainerMenu(w, i, b)));

    public static final RegistryObject<MenuType<gregtech.client.gui.BehaviorMachineMenu>> BEHAVIOR_MACHINE =
            MENU_TYPES.register("behavior_machine",
                    () -> IForgeMenuType.create((w, i, b) -> new gregtech.client.gui.BehaviorMachineMenu(w, i, b)));

    public static final RegistryObject<MenuType<gregtech.client.gui.PartMenu>> PART =
            MENU_TYPES.register("part",
                    () -> IForgeMenuType.create((w, i, b) -> new gregtech.client.gui.PartMenu(w, i, b)));

    public static final RegistryObject<MenuType<HopperContainerMenu>> GARBAGE_DUMP =
            MENU_TYPES.register("garbage_dump",
                    () -> IForgeMenuType.create((w, i, b) -> (HopperContainerMenu)
                            new gregtech.client.gui.GarbageDumpContainerMenu(w, i)));
    public static final RegistryObject<MenuType<gregtech.client.gui.FilterMenu>> FILTER = MENU_TYPES.register("filter",()->IForgeMenuType.create((id,inventory,buffer)->new gregtech.client.gui.FilterMenu(id,inventory,buffer.readBoolean())));

    public static final RegistryObject<MenuType<gregtech.client.gui.DataSwitchMenu>> USB_SWITCH =
            MENU_TYPES.register("usb_switch", () -> IForgeMenuType.create((id, inventory, buffer) ->
                    new gregtech.client.gui.DataSwitchMenu(id, inventory,
                            gregtech.content.block.inventory.UsbSwitchBlock.Kind.USB)));
    public static final RegistryObject<MenuType<gregtech.client.gui.DataSwitchMenu>> HDD_SWITCH =
            MENU_TYPES.register("hdd_switch", () -> IForgeMenuType.create((id, inventory, buffer) ->
                    new gregtech.client.gui.DataSwitchMenu(id, inventory,
                            gregtech.content.block.inventory.UsbSwitchBlock.Kind.HDD)));

    /** GT6's bumbliary GUI (36 slots) and the advanced one (20 slots). */
    public static final RegistryObject<MenuType<gregtech.client.gui.BumbliaryContainerMenu>> BUMBLIARY =
            MENU_TYPES.register("bumbliary", () -> IForgeMenuType.create(
                    (id, inventory, buffer) -> new gregtech.client.gui.BumbliaryContainerMenu(id, inventory, false)));
    public static final RegistryObject<MenuType<gregtech.client.gui.BumbliaryContainerMenu>> ADVANCED_BUMBLIARY =
            MENU_TYPES.register("advanced_bumbliary", () -> IForgeMenuType.create(
                    (id, inventory, buffer) -> new gregtech.client.gui.BumbliaryContainerMenu(id, inventory, true)));
//?}

}
