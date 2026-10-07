package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Neo platform item: material and prefix belong to its actual registry identity, never stack NBT. */
public class MaterialItem extends Item implements com.gregtech.gregtech.api.material.MaterialFormItem {
    private final MaterialPrefix prefix;
    private final GTMaterial material;

    public MaterialItem(Properties properties, MaterialPrefix prefix, GTMaterial material) {
        super(properties);
        this.prefix = prefix;
        this.material = material;
    }

    public MaterialPrefix getPrefix() { return prefix; }
    public GTMaterial getMaterial() { return material; }

    public static GTMaterial getMaterial(ItemStack stack) {
        return stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem item ? item.getMaterial() : GTMaterialRegistry.get("NULL");
    }

    public static MaterialPrefix getPrefix(ItemStack stack) {
        return stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem item ? item.getPrefix() : null;
    }

    public static boolean isMaterialItem(ItemStack stack, MaterialPrefix prefix, GTMaterial material) {
        return stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem item
                && item.getPrefix() == prefix && item.getMaterial().resolve() == material.resolve();
    }

    /** PrefixItemProjectile:115-125 consumes ammunition used in a living-entity melee attack. */
    public static boolean consumeAmmoOnAttack(ItemStack stack, net.minecraft.world.entity.player.Player player,
                                              net.minecraft.world.entity.Entity target) {
        if (!player.level().isClientSide && !player.getAbilities().instabuild
                && target instanceof net.minecraft.world.entity.LivingEntity
                && stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem form
                && form.getPrefix().hasEmptyAmmunitionForm()) stack.shrink(1);
        return false;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, net.minecraft.world.entity.player.Player player,
                                     net.minecraft.world.entity.Entity target) {
        return consumeAmmoOnAttack(stack, player, target);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return RockItemPlacement.place(context);
    }

    @Override
    public Component getName(ItemStack stack) {
        if ("Empty".equals(material.getName()) && prefix.hasEmptyAmmunitionForm())
            return Component.translatableWithFallback(com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceTranslationKey(prefix.getName(), material.getName()), "%s",
                    Component.translatable(getDescriptionId() + "_empty"));
        // Same domain translation data/argument contract as Forge MaterialPresentation.
        return Component.translatableWithFallback(com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceTranslationKey(prefix.getName(), material.getName()), "%s",
                Component.translatable(getDescriptionId(),
                        Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback())));
    }

    @Override
    public String getDescriptionId() {
        return "item.gregtech." + prefix.getRegistryName();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        com.gregtech.gregtech.client.MaterialTooltips.append(stack, material, prefix, tooltip, flag);
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public int getTintColor() {
        return 0xFF000000 | (material.getColor() & 0xFFFFFF);
    }
}
