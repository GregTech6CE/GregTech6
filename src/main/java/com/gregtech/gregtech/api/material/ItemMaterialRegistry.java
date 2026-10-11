package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Immutable per-item compositions; stack damage is evaluated without caching ItemStacks. */
public final class ItemMaterialRegistry {
    private static final Map<Item, ItemComposition> BY_ITEM = new IdentityHashMap<>();
    private ItemMaterialRegistry() {}
    public static void register(Item item, @Nullable MaterialPrefix prefix, GTMaterial material) {
        if (item != null && prefix != null && prefix.hasEmptyAmmunitionForm()) {
            var parts = MaterialChemistry.prefixMaterialWeights(material, prefix).stream()
                    .map(c -> MaterialComponent.of(c.material(), c.amount())).toList();
            if (!parts.isEmpty()) register(item, new ItemComposition(prefix, parts, "GT6 OP ammunition components", true));
            return;
        }
        register(item, prefix, material, prefix == null ? GTValues.U : prefix.getMaterialWeight());
    }
    public static void register(Item item, @Nullable MaterialPrefix prefix, GTMaterial material, long amount) {
        if (item != null && material != null && material.isValid() && amount > 0)
            register(item, new ItemComposition(prefix, material, amount));
    }
    public static void register(Item item, ItemComposition data) {
        if (item != null && data != null) BY_ITEM.put(item, data);
    }
    /** Data registered for this item. Tag-derived compositions are not included. */
    public static Optional<ItemComposition> explicit(Item item) {
        var data = BY_ITEM.get(item);
        return data == null ? Optional.empty() : Optional.of(data);
    }
    public static Optional<ItemComposition> base(Item item) {
        var data = explicit(item);
        return data.isPresent() ? data : com.gregtech.gregtech.content.recipe.ExternalOreProcessing.composition(item);
    }
    public static Map<Item, ItemComposition> entries() { return Collections.unmodifiableMap(BY_ITEM); }
    public static Optional<ItemComposition> get(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        var data = base(stack.getItem()).orElse(null);
        if (data == null) return Optional.empty();
        if (!stack.isDamaged()) return Optional.of(data);
        long maximum = stack.getMaxDamage();
        long remaining = Math.max(0, maximum - stack.getDamageValue());
        var scaled = data.components().stream().map(c -> MaterialComponent.of(c.material(), c.amount() * remaining / maximum))
                .filter(c -> c.amount() > 0).toList();
        return Optional.of(new ItemComposition(data.prefix(), scaled, data.source(), data.recoverable()));
    }
    /** Do not consume stored inventories/entities or fluid containers as empty shells. */
    public static boolean canRecover(ItemStack stack) {
        var data = get(stack);
        if (data.isEmpty() || !data.get().recoverable() || data.get().components().isEmpty()) return false;
        if (hasStoredContents(stack)) return false;
        return !(stack.getItem() instanceof net.minecraft.world.item.BucketItem) || stack.is(net.minecraft.world.item.Items.BUCKET);
    }
    public static boolean hasStoredContents(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && (tag.contains("BlockEntityTag") || tag.contains("Items") || tag.contains("EntityTag") || tag.contains("ChargedProjectiles"));
    }
}
