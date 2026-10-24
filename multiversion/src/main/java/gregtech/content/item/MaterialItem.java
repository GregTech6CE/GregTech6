package gregtech.content.item;
//? if >=1.21.1 {

/*import gregtech.internal.material.GTMaterial;
*///?} else {
//?}
import gregtech.internal.material.MaterialPresentation;
//? if >=1.21.1 {
//?} else {

import gregtech.GregTech;
import gregtech.internal.material.GTMaterial;
//?}
import gregtech.internal.material.GTMaterialRegistry;
//? if >=1.21.1 {
//?} else {
import gregtech.client.MaterialTooltips;
//?}
import gregtech.content.data.MaterialPrefix;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
//?}

/** Neo platform item: material and prefix belong to its actual registry identity, never stack NBT. */
//? if >=1.21.1 {
//?} else {
import java.util.List;

/**
 * One registered item per (prefix, material) pair.
 * <p>
 * Material data lives on the {@link Item} instance (registry identity), not on stack NBT.
 * This matches the GregTech Modern approach on 1.20.1: no MetaItem / stack-tag material payload.
 */
//?}
public class MaterialItem extends Item implements gregtech.internal.material.MaterialFormItem {
    private final MaterialPrefix prefix;
    private final GTMaterial material;

    public MaterialItem(Properties properties, MaterialPrefix prefix, GTMaterial material) {
        super(properties);
        this.prefix = prefix;
        this.material = material;
    }

//? if >=1.21.1 {
    /*public MaterialPrefix getPrefix() { return prefix; }
    public GTMaterial getMaterial() { return material; }

    public static GTMaterial getMaterial(ItemStack stack) {
        return stack.getItem() instanceof gregtech.internal.material.MaterialFormItem item ? item.getMaterial() : GTMaterialRegistry.get("NULL");
*///?} else {
    public MaterialPrefix getPrefix() {
        return prefix;
//?}
    }

//? if >=1.21.1 {
//?} else {
    public GTMaterial getMaterial() {
        return material;
    }

    /** Resolves material from the registered item type, never from stack NBT. */
    public static GTMaterial getMaterial(ItemStack stack) {
        return stack.getItem() instanceof gregtech.internal.material.MaterialFormItem materialItem
                ? materialItem.getMaterial()
                : GTMaterialRegistry.get("NULL");
    }

    /** Resolves prefix from the registered item type, never from stack NBT. */
//?}
    public static MaterialPrefix getPrefix(ItemStack stack) {
//? if >=1.21.1 {
        /*return stack.getItem() instanceof gregtech.internal.material.MaterialFormItem item ? item.getPrefix() : null;
*///?} else {
        return stack.getItem() instanceof gregtech.internal.material.MaterialFormItem materialItem ? materialItem.getPrefix() : null;
//?}
    }

    public static boolean isMaterialItem(ItemStack stack, MaterialPrefix prefix, GTMaterial material) {
//? if >=1.21.1 {
        /*return stack.getItem() instanceof gregtech.internal.material.MaterialFormItem item
                && item.getPrefix() == prefix && item.getMaterial().resolve() == material.resolve();
*///?} else {
        if (!(stack.getItem() instanceof gregtech.internal.material.MaterialFormItem materialItem)) return false;
        return materialItem.getPrefix() == prefix && materialItem.getMaterial().resolve() == material.resolve();
//?}
    }

    /** PrefixItemProjectile:115-125 consumes ammunition used in a living-entity melee attack. */
    public static boolean consumeAmmoOnAttack(ItemStack stack, net.minecraft.world.entity.player.Player player,
                                              net.minecraft.world.entity.Entity target) {
        if (!player.level().isClientSide && !player.getAbilities().instabuild
                && target instanceof net.minecraft.world.entity.LivingEntity
                && stack.getItem() instanceof gregtech.internal.material.MaterialFormItem form
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
            return MaterialPresentation.formName(prefix.getName(), material,
                    Component.translatable(getDescriptionId() + "_empty"));
        Component fallback = prefix.isAddon()
                ? Component.translatableWithFallback(getDescriptionId(),
                prefix.displayPrefix() + "%s" + prefix.displaySuffix(), MaterialPresentation.name(material))
                : Component.translatable(getDescriptionId(), MaterialPresentation.name(material));
        return MaterialPresentation.formName(prefix.getName(), material, fallback);
    }

    @Override
    public String getDescriptionId() {
        if (prefix.isAddon()) return "item." + prefix.itemNamespace() + "." + prefix.getRegistryName();
//? if >=1.21.1 {
        /*return "item.gt6ce." + prefix.getRegistryName();
*///?} else {
        return "item." + GregTech.NAMESPACE + "." + prefix.getRegistryName();
//?}
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        gregtech.client.MaterialTooltips.append(stack, material, prefix, tooltip, flag);
        super.appendHoverText(stack, context, tooltip, flag);
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        MaterialTooltips.append(stack, material, prefix, tooltip, flag);
        super.appendHoverText(stack, level, tooltip, flag);
//?}
    }

    public int getTintColor() {
        return 0xFF000000 | (material.getColor() & 0xFFFFFF);
    }
}
