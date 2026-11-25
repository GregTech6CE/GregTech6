package gregtech.engine.stack;

import java.util.Map;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.ItemEnchantments;
*///?} else {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;
//?}

/** Cross-version enchantment lookup and stack enchantment helpers. */
public final class EnchantmentCompat {
    private EnchantmentCompat() {}

    /** Item enchantment level by registry ID; unknown IDs return {@code 0}. */
    public static int level(ItemStack stack, ResourceLocation id, @Nullable HolderLookup.Provider lookup) {
//? if >=1.21.1 {
/*        var registry = Objects.requireNonNull(lookup, "enchantment lookup needs the registry lookup")
                .lookupOrThrow(Registries.ENCHANTMENT);
        return registry.get(ResourceKey.create(Registries.ENCHANTMENT, id))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
*///?} else {
        var enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
        return enchantment == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
//?}
    }

    /** Item enchantment level by registry ID using {@code level}'s registry access. */
    public static int level(ItemStack stack, ResourceLocation id, LevelAccessor level) {
        return level(stack, id, level.registryAccess());
    }

    /** Item enchantment level by short or namespaced ID ({@code "fortune"} defaults to {@code minecraft:}). */
    public static int level(ItemStack stack, String name, LevelAccessor level) {
        return level(stack, ResourceLocation.parse(name.contains(":") ? name : "minecraft:" + name), level);
    }

    /** Fortune level on {@code stack}. */
    public static int fortune(ItemStack stack, LevelAccessor level) {
//? if >=1.21.1 {
/*        return EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), stack);
*///?} else {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, stack);
//?}
    }

    /** Silk Touch level on {@code stack}. */
    public static int silkTouch(ItemStack stack, LevelAccessor level) {
//? if >=1.21.1 {
/*        return EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), stack);
*///?} else {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack);
//?}
    }

    /** Looting level active on {@code attacker}. */
    public static int looting(LivingEntity attacker) {
//? if >=1.21.1 {
/*        var enchantment = attacker.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LOOTING);
        return EnchantmentHelper.getEnchantmentLevel(enchantment, attacker);
*///?} else {
        return EnchantmentHelper.getMobLooting(attacker);
//?}
    }

    /** Enchants {@code stack} with Fortune at {@code level}. */
    public static void enchantFortune(ItemStack stack, int level, LevelAccessor world) {
//? if >=1.21.1 {
/*        stack.enchant(
                world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), level);
*///?} else {
        stack.enchant(Enchantments.BLOCK_FORTUNE, level);
//?}
    }

    /** Removes all non-stored item enchantments from {@code stack}. */
    public static void clear(ItemStack stack) {
//? if >=1.21.1 {
/*        stack.remove(DataComponents.ENCHANTMENTS);
*///?} else {
        stack.removeTagKey("Enchantments");
//?}
    }

    /** Whether {@code stack} carries any curse enchantment (non-stored). */
    public static boolean hasCurse(ItemStack stack) {
//? if >=1.21.1 {
/*        return EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet().stream()
                .anyMatch(holder -> holder.is(EnchantmentTags.CURSE));
*///?} else {
        return EnchantmentHelper.getEnchantments(stack).keySet().stream().anyMatch(Enchantment::isCurse);
//?}
    }

    /** Sum of vanilla {@code getMinCost(level)} across all non-stored enchantments on {@code stack}. */
    public static int minCostSum(ItemStack stack) {
//? if >=1.21.1 {
/*        return EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet().stream()
                .mapToInt(entry -> entry.getKey().value().getMinCost(entry.getIntValue()))
                .sum();
*///?} else {
        return EnchantmentHelper.getEnchantments(stack).entrySet().stream()
                .mapToInt(entry -> entry.getKey().getMinCost(entry.getValue()))
                .sum();
//?}
    }

    /** Upgrades/merges every curse enchantment from {@code from} onto {@code to}. */
    public static void copyCurses(ItemStack from, ItemStack to) {
//? if >=1.21.1 {
/*        var source = from.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (source.isEmpty()) return;
        var target = new ItemEnchantments.Mutable(to.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        boolean changed = false;
        for (var entry : source.entrySet()) {
            if (entry.getKey().is(EnchantmentTags.CURSE)) {
                target.upgrade(entry.getKey(), entry.getIntValue());
                changed = true;
            }
        }
        if (changed) to.set(DataComponents.ENCHANTMENTS, target.toImmutable());
*///?} else {
        var source = EnchantmentHelper.getEnchantments(from);
        if (source.isEmpty()) return;
        var target = EnchantmentHelper.getEnchantments(to);
        boolean changed = false;
        for (var entry : source.entrySet()) {
            if (entry.getKey().isCurse()) {
                target.merge(entry.getKey(), entry.getValue(), Math::max);
                changed = true;
            }
        }
        if (changed) EnchantmentHelper.setEnchantments(target, to);
//?}
    }

    /**
     * Upgrades/merges every enchantment in {@code levels} (map of registry ID to level) onto
     * {@code stack}. Unknown IDs are ignored.
     */
    public static void upgradeAll(
            ItemStack stack, Map<ResourceLocation, Integer> levels, @Nullable HolderLookup.Provider lookup) {
        if (levels.isEmpty()) return;
//? if >=1.21.1 {
/*        var registry = Objects.requireNonNull(lookup, "enchantment lookup needs the registry lookup")
                .lookupOrThrow(Registries.ENCHANTMENT);
        var enchantments = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        for (var entry : levels.entrySet()) {
            registry.get(ResourceKey.create(Registries.ENCHANTMENT, entry.getKey()))
                    .ifPresent(holder -> enchantments.upgrade(holder, entry.getValue()));
        }
        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
*///?} else {
        var enchantments = EnchantmentHelper.getEnchantments(stack);
        for (var entry : levels.entrySet()) {
            var enchantment = BuiltInRegistries.ENCHANTMENT.get(entry.getKey());
            if (enchantment != null) enchantments.merge(enchantment, entry.getValue(), Math::max);
        }
        EnchantmentHelper.setEnchantments(enchantments, stack);
//?}
    }
}
