package gregtech.engine.tool;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.tool.MaterialToolEnchantments;
import gregtech.engine.material.MaterialFormItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
*///?} else {
import net.minecraft.core.registries.BuiltInRegistries;
//?}

/** Material ammunition uses real enchantments, including the original caliber looting multiplier. */
public final class MaterialEnchantmentStacks {
    private static final String AMMO_APPLIED = "gt.ammo_enchantments.v2";
    private MaterialEnchantmentStacks() {}

    public static void applyAmmo(ItemStack stack, Level level) {
        if (!(stack.getItem() instanceof MaterialFormItem form) || !form.getPrefix().hasEmptyAmmunitionForm()
                || !form.getMaterial().isValid() || "Empty".equals(form.getMaterial().getName())) return;
//? if >=1.21.1 {
        /*if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(AMMO_APPLIED)) return;
        var enchants = new ItemEnchantments.Mutable(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
*///?} else {
        if (stack.hasTag() && stack.getTag().getBoolean(AMMO_APPLIED)) return;
        var enchants = EnchantmentHelper.getEnchantments(stack);
//?}
        int multiplier = form.getPrefix() == MaterialPrefix.bulletGtLarge ? 3 : form.getPrefix() == MaterialPrefix.bulletGtMedium ? 2 : 1;
        for (var entry : MaterialToolEnchantments.ammunition(form.getMaterial()).entrySet()) {
            int value = entry.getValue() * (entry.getKey().equals("looting") ? multiplier : 1);
            var id = ResourceLocation.parse(MaterialToolEnchantments.id(entry.getKey()));
//? if >=1.21.1 {
            /*registry.get(ResourceKey.create(Registries.ENCHANTMENT, id)).ifPresent(holder -> enchants.upgrade(holder, value));
*///?} else {
            var enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
            if (enchantment != null) enchants.merge(enchantment, value, Math::max);
//?}
        }
//? if >=1.21.1 {
        /*stack.set(DataComponents.ENCHANTMENTS, enchants.toImmutable());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(AMMO_APPLIED, true));
*///?} else {
        EnchantmentHelper.setEnchantments(enchants, stack);
        stack.getOrCreateTag().putBoolean(AMMO_APPLIED, true);
//?}
    }

    public static int level(ItemStack stack, String name, Level level) {
        var id = ResourceLocation.parse(MaterialToolEnchantments.id(name));
//? if >=1.21.1 {
        /*return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, id))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
*///?} else {
        var enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
        return enchantment == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
//?}
    }

    public static float damageBonus(ItemStack ammo, Entity target, DamageSource source) {
//? if >=1.21.1 {
        /*return target.level() instanceof ServerLevel server ? EnchantmentHelper.modifyDamage(server, ammo, target, source, 0) : 0;
*///?} else {
        return target instanceof LivingEntity living ? EnchantmentHelper.getDamageBonus(ammo, living.getMobType()) : 0;
//?}
    }

    /** Only run the ammunition's attacker effects; the native hit already processes the target's armor. */
    public static void postAttack(ItemStack ammo, Entity target, DamageSource source) {
//? if >=1.21.1 {
        /*if (!(target.level() instanceof ServerLevel server)) return;
        var owner = source.getEntity() instanceof LivingEntity living ? living : null;
        var inUse = new net.minecraft.world.item.enchantment.EnchantedItemInUse(ammo, null, owner, ignored -> {});
        for (var entry : ammo.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet())
            entry.getKey().value().doPostAttack(server, entry.getIntValue(), inUse,
                    net.minecraft.world.item.enchantment.EnchantmentTarget.ATTACKER, target, source);
*///?} else {
        if (target.level().isClientSide) return;
        var fallback = target instanceof net.minecraft.world.entity.boss.EnderDragonPart part ? part.parentMob : target;
        var owner = source.getEntity() instanceof LivingEntity living ? living : fallback instanceof LivingEntity living ? living : null;
        EnchantmentHelper.getEnchantments(ammo).forEach((enchantment, value) -> {
            if (owner != null) enchantment.doPostAttack(owner, target, value);
        });
//?}
    }

    /** Hidden legacy registrations exist only so an old stack can be decoded and upgraded. */
    public static void migrateLegacy(ItemStack stack, Level level) {
//? if >=1.21.1 {
        /*var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var component : java.util.List.of(DataComponents.ENCHANTMENTS, DataComponents.STORED_ENCHANTMENTS)) {
            var old = stack.getOrDefault(component, ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable changed = null;
            for (var entry : old.entrySet()) {
                var key = entry.getKey().unwrapKey();
                if (key.isEmpty()) continue;
                var id = key.get().location().toString();
                var replacement = MaterialToolEnchantments.id(id);
                if (replacement.equals(id)) continue;
                var holder = registry.get(ResourceKey.<Enchantment>create(Registries.ENCHANTMENT, ResourceLocation.parse(replacement)));
                if (holder.isEmpty()) continue;
                if (changed == null) changed = new ItemEnchantments.Mutable(old);
                changed.removeIf(value -> value.equals(entry.getKey()));
                changed.upgrade(holder.get(), entry.getIntValue());
            }
            if (changed != null) stack.set(component, changed.toImmutable());
        }
*///?} else {
        if (!stack.hasTag()) return;
        for (String key : new String[] {"Enchantments", "StoredEnchantments"}) {
            if (!stack.getTag().contains(key, 9)) continue;
            var list = stack.getTag().getList(key, 10);
            boolean changed = false;
            var enchants = new java.util.LinkedHashMap<String, Integer>();
            for (int i = 0; i < list.size(); i++) {
                var entry = list.getCompound(i);
                String id = entry.getString("id"), replacement = MaterialToolEnchantments.id(id);
                changed |= !id.equals(replacement);
                enchants.merge(replacement, (int) entry.getShort("lvl"), Math::max);
            }
            if (changed) {
                var updated = new net.minecraft.nbt.ListTag();
                enchants.forEach((id, value) -> {
                    var entry = new net.minecraft.nbt.CompoundTag();
                    entry.putString("id", id); entry.putShort("lvl", value.shortValue()); updated.add(entry);
                });
                stack.getTag().put(key, updated);
            }
        }
//?}
    }
}
