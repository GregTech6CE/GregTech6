package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.engine.tool.GTToolType;
import gregtech.content.item.GTToolItem;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.smeltery.SmelteryToolItem;
*///?} else {
//?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class GTToolItems {
    private static final Map<GTToolType, RegisteredItem<? extends GTToolItem>> BY_TYPE = new EnumMap<>(GTToolType.class);
    public static RegisteredItem<? extends GTToolItem> CHISEL, PINCERS;

    private GTToolItems() {}

    public static void register(ContentRegistrar registrar) {
        if (!BY_TYPE.isEmpty()) throw new IllegalStateException("Incomplete manual tool register");
        for (GTToolType type : GTToolType.values()) {
            RegisteredItem<? extends GTToolItem> item = create(registrar, type);
            BY_TYPE.put(type, item);
            if (type == GTToolType.CHISEL) CHISEL = item;
            if (type == GTToolType.PINCERS) PINCERS = item;
        }
    }

    private static RegisteredItem<? extends GTToolItem> create(ContentRegistrar registrar, GTToolType type) {
//? if >=1.21.1 {
        /*if (type == GTToolType.CHISEL)
            return registrar.item(REGISTRY_NAMESPACE, "tool_chisel", () -> new SmelteryToolItem(SmelteryToolItem.Kind.CHISEL));
        if (type == GTToolType.PINCERS)
            return registrar.item(REGISTRY_NAMESPACE, "tool_pincers", () -> new SmelteryToolItem(SmelteryToolItem.Kind.PINCERS));
*///?} else {
//?}
        return registrar.item(REGISTRY_NAMESPACE, "tool_" + type.id(), () ->
                type.definition().isGun()
                        ? new gregtech.content.item.GunToolItem(new Item.Properties().stacksTo(1), type)
                        : type.definition().isPocket()
                                ? new gregtech.content.item.PocketToolItem(new Item.Properties().stacksTo(1), type)
                                : new GTToolItem(new Item.Properties().stacksTo(1), type));
    }

    public static Map<GTToolType, RegisteredItem<? extends GTToolItem>> all() {
        return Collections.unmodifiableMap(BY_TYPE);
    }

    public static GTToolItem get(GTToolType type) {
        var entry = BY_TYPE.get(type);
        return entry == null ? null : entry.get();
    }

    public static ItemStack empty(GTToolType type) {
        GTToolItem item = get(type);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
