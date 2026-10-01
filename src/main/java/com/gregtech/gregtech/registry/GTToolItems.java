package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class GTToolItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GregTech.NAMESPACE);

    private static final Map<GTToolType, RegistryObject<GTToolItem>> BY_TYPE = new EnumMap<>(GTToolType.class);

    static {
        for (GTToolType type : GTToolType.values()) {
            String id = "tool_" + type.id();
            RegistryObject<GTToolItem> holder = ITEMS.register(id, () -> new GTToolItem(new Item.Properties().stacksTo(1), type));
            BY_TYPE.put(type, holder);
        }
    }

    public static Map<GTToolType, RegistryObject<GTToolItem>> all() {
        return Collections.unmodifiableMap(BY_TYPE);
    }

    public static GTToolItem get(GTToolType type) {
        RegistryObject<GTToolItem> holder = BY_TYPE.get(type);
        return holder == null ? null : holder.get();
    }

    public static ItemStack empty(GTToolType type) {
        GTToolItem item = get(type);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private GTToolItems() {}
}
