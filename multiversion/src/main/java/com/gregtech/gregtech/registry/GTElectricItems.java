package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.content.tool.ElectricToolCatalog;
import com.gregtech.gregtech.item.ElectricToolItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import java.util.*;
/** Electric tool creation and registration shared by both platforms. */
public final class GTElectricItems {
    private GTElectricItems() {}
    private static final Map<String, RegisteredItem<ElectricToolItem>> BY_ID = new LinkedHashMap<>();
    public static RegisteredItem<ElectricToolItem> ELECTRIC_DRILL, ELECTRIC_CHAINSAW, ELECTRIC_WRENCH, ELECTRIC_SCREWDRIVER;
    public static List<RegisteredItem<ElectricToolItem>> all() { return List.copyOf(BY_ID.values()); }
    public static List<RegisteredItem<ElectricToolItem>> tools() { return all(); }
    public static ElectricToolItem get(String id) { return BY_ID.get(id).get(); }
    public static void registerAll() {
        if (!BY_ID.isEmpty()) throw new IllegalStateException("Electric tools registered twice");
        for (var spec : ElectricToolCatalog.ALL)
            BY_ID.put(spec.id(), RegisteredItem.register(spec.id(), () -> new ElectricToolItem(spec.name(), Tiers.IRON,
                    spec.capacity(), spec.tier(), spec.energyPerUse(), new Item.Properties().stacksTo(1))));
        ELECTRIC_DRILL=BY_ID.get("electric_drill"); ELECTRIC_CHAINSAW=BY_ID.get("electric_chainsaw");
        ELECTRIC_WRENCH=BY_ID.get("electric_wrench"); ELECTRIC_SCREWDRIVER=BY_ID.get("electric_screwdriver");
    }
    //? if >=1.21.1 {
    /*public static void register(net.neoforged.bus.api.IEventBus bus) { registerAll(); }
    *///?}
}
