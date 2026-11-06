package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.item.RadiationSuitItem;
import net.minecraft.world.item.ArmorItem;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GTRadiationProtection {
    public static final Map<ArmorItem.Type, RegisteredItem<RadiationSuitItem>> SUIT = new LinkedHashMap<>();
    public static final Map<String, RegisteredItem<RadiationSuitItem>> ALL = new LinkedHashMap<>();

    private GTRadiationProtection() {
    }

    public static void register(ContentRegistrar registrar) {
        if (!ALL.isEmpty()) return;
        for (String family : gregtech.content.hazard.HazmatSuitCatalog.FAMILIES)
            for (var armorType : new ArmorItem.Type[]{
                    ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS
            }) {
                String itemId = family + "_suit_" + armorType.getName();
                var item = registrar.item(REGISTRY_NAMESPACE, itemId, () -> new RadiationSuitItem(armorType, family));
                ALL.put(itemId, item);
                if (family.equals("radiation")) SUIT.put(armorType, item);
            }
    }
//? if >=1.21.1 {
    /*public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.item.ArmorMaterial> MATERIALS = net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.ARMOR_MATERIAL, REGISTRY_NAMESPACE);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.ArmorMaterial,
            net.minecraft.world.item.ArmorMaterial> MATERIAL = MATERIALS.register("radiation_suit",
            () -> new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(
                    ArmorItem.Type.HELMET, 1,
                    ArmorItem.Type.CHESTPLATE, 1,
                    ArmorItem.Type.LEGGINGS, 1,
                    ArmorItem.Type.BOOTS, 1), 0, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
            java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(gregtech.engine.mod.GregTechIdentity.MOD_ID,
            "radiation_suit"))), 0, 0));
*///?} else {
//?}
}
