package gregtech.content.item;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.MaterialPresentation;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.client.MaterialTooltips;
import gregtech.content.data.MaterialPrefix;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * One registered item per (prefix, material) pair.
 * <p>
 * Material data lives on the {@link Item} instance (registry identity), not on stack NBT.
 */
public class MaterialItem extends GTItem implements gregtech.engine.material.MaterialFormItem {
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
        return stack.getItem() instanceof gregtech.engine.material.MaterialFormItem item ? item.getMaterial() : GTMaterialRegistry.get("NULL");
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
        return stack.getItem() instanceof gregtech.engine.material.MaterialFormItem materialItem
                ? materialItem.getMaterial()
                : GTMaterialRegistry.get("NULL");
    }

    /** Resolves prefix from the registered item type, never from stack NBT. */
//?}
    public static MaterialPrefix getPrefix(ItemStack stack) {
        return stack.getItem() instanceof gregtech.engine.material.MaterialFormItem item ? item.getPrefix() : null;
    }

    public static boolean isMaterialItem(ItemStack stack, MaterialPrefix prefix, GTMaterial material) {
        return stack.getItem() instanceof gregtech.engine.material.MaterialFormItem item
                && item.getPrefix() == prefix && item.getMaterial().resolve() == material.resolve();
    }

    /** PrefixItemProjectile consumes ammunition used in a living-entity melee attack. */
    public static boolean consumeAmmoOnAttack(ItemStack stack, net.minecraft.world.entity.player.Player player,
                                              net.minecraft.world.entity.Entity target) {
        if (!player.level().isClientSide) gregtech.engine.tool.MaterialEnchantmentStacks.applyAmmo(stack, player.level());
        if (!player.level().isClientSide && !player.getAbilities().instabuild
                && target instanceof net.minecraft.world.entity.LivingEntity
                && stack.getItem() instanceof gregtech.engine.material.MaterialFormItem form
                && form.getPrefix().hasEmptyAmmunitionForm()) stack.shrink(1);
        return false;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, net.minecraft.world.entity.player.Player player,
                                     net.minecraft.world.entity.Entity target) {
        return consumeAmmoOnAttack(stack, player, target);
    }

    @Override public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && prefix.hasEmptyAmmunitionForm()) gregtech.engine.tool.MaterialEnchantmentStacks.applyAmmo(stack, level);
    }

    @Override public boolean isFoil(ItemStack stack) { return false; }
    @Override public boolean isBookEnchantable(ItemStack stack, ItemStack book) { return false; }

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
        return "item." + GregTechIdentity.REGISTRY_NAMESPACE + "." + prefix.getRegistryName();
    }

    @Override
    protected void appendTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        MaterialTooltips.append(stack, material, prefix, tooltip, flag);
        super.appendTooltip(stack, level, tooltip, flag);
    }

    public int getTintColor() {
        return 0xFF000000 | (material.getColor() & 0xFFFFFF);
    }
}
