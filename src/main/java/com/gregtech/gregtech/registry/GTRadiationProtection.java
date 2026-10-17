package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.item.RadiationSuitItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GTRadiationProtection {
    public static final Map<ArmorItem.Type, RegistryObject<RadiationSuitItem>> SUIT = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<RadiationSuitItem>> ALL = new LinkedHashMap<>();
    static {
        for (String family : com.gregtech.gregtech.content.hazard.HazmatSuitCatalog.FAMILIES)
            for (var type : new ArmorItem.Type[]{ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS}) {
                String id=family+"_suit_"+type.getName();
                var item=GTItems.ITEMS.register(id, () -> new RadiationSuitItem(type,family));
                ALL.put(id,item);
                if(family.equals("radiation"))SUIT.put(type,item);
            }
    }
    private GTRadiationProtection() {}
    public static void registerAll() {}
}
