package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.item.RadiationSuitItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GTRadiationProtection {
    public static final Map<ArmorItem.Type, RegistryObject<RadiationSuitItem>> SUIT = new LinkedHashMap<>();
    static {
        for (var type : ArmorItem.Type.values()) SUIT.put(type, GTItems.ITEMS.register(
                "radiation_suit_" + type.getName(), () -> new RadiationSuitItem(type)));
    }
    private GTRadiationProtection() {}
    public static void registerAll() {}
}
