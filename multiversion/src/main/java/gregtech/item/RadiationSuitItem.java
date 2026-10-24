package gregtech.item;
//? if >=1.21.1 {
//?} else {

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
//?}
import net.minecraft.world.item.*;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.item.crafting.Ingredient;

//?}
/** The six GT6 hazard suits: four pieces each, 128 durability and one armor point per piece. */
public final class RadiationSuitItem extends ArmorItem {
    private final String family;

    public RadiationSuitItem(Type type) {
        this(type, "radiation");
    }

    public RadiationSuitItem(Type type, String family) {
//? if >=1.21.1 {
        /*super(gregtech.registry.GTRadiationProtection.MATERIAL, type, new Properties().durability(128));
*///?} else {
        super(SuitMaterial.INSTANCE, type, new Properties());
//?}
        gregtech.content.hazard.HazmatSuitCatalog.texture(family);
        this.family = family;
    }
//? if >=1.21.1 {
    /*@Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", texturePath(slot));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> lines,
            TooltipFlag flags) {
        lines.add(net.minecraft.network.chat.Component.translatable(gregtech.content.hazard.HazmatSuitCatalog.tooltip(family)));
    }
*///?} else {
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String layer) {
        return "gt6ce:" + texturePath(slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> lines, TooltipFlag flags) {
        lines.add(net.minecraft.network.chat.Component.translatable(gregtech.content.hazard.HazmatSuitCatalog.tooltip(family)));
    }
    private enum SuitMaterial implements ArmorMaterial {
        INSTANCE;

        public int getDurabilityForType(Type type) {
            return 128;
        }

        public int getDefenseForType(Type type) {
            return 1;
        }

        public int getEnchantmentValue() {
            return 0;
        }

        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        public String getName() {
            return "gt6ce:radiation_suit";
        }

        public float getToughness() {
            return 0;
        }

        public float getKnockbackResistance() {
            return 0;
        }
    }
//?}

    private String texturePath(net.minecraft.world.entity.EquipmentSlot slot) {
        int index = switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            default -> 3;
        };
        return "textures/armor/" + gregtech.content.hazard.HazmatSuitCatalog.texture(family) + "/" + index + ".png";
    }
}
