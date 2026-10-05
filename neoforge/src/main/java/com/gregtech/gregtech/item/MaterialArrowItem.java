package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialFormItem;
import com.gregtech.gregtech.api.material.MaterialPresentation;
import com.gregtech.gregtech.client.MaterialTooltips;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.entity.MaterialArrowEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Native bow/crossbow ammunition retaining GT6's registered material identity. */
public final class MaterialArrowItem extends ArrowItem implements MaterialFormItem {
    private final MaterialPrefix prefix;
    private final GTMaterial material;

    public MaterialArrowItem(Properties properties, MaterialPrefix prefix, GTMaterial material) {
        super(properties);
        this.prefix = prefix;
        this.material = material;
    }

    public MaterialPrefix getPrefix() { return prefix; }
    public GTMaterial getMaterial() { return material; }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new MaterialArrowEntity(level, shooter, ammo, weapon);
    }

    @Override
    public net.minecraft.world.entity.projectile.Projectile asProjectile(Level level, net.minecraft.core.Position pos,
                                                                         ItemStack ammo, net.minecraft.core.Direction direction) {
        var arrow = new MaterialArrowEntity(level, pos.x(), pos.y(), pos.z(), ammo);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, net.minecraft.world.entity.player.Player player,
                                     net.minecraft.world.entity.Entity target) {
        return MaterialItem.consumeAmmoOnAttack(stack, player, target);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback("oredict."+prefix.getName()+material.getName(), "%s",
                Component.translatable(getDescriptionId(), MaterialPresentation.name(material)));
    }

    @Override
    public String getDescriptionId() { return "item.gregtech." + prefix.getRegistryName(); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MaterialTooltips.append(stack, material, prefix, tooltip, flag);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
