/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

// Adapted from Override_Drops.java and the LOWEST LivingDrops listener in GT_Proxy.
package com.gregtech.gregtech.content.loot;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.tool.OriginalToolMaterials;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Original default-enabled mob drops, on the authoritative server drop collection. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class MobDropEvents {
    private MobDropEvents() {}
    public static MobDropRules.Kind kind(LivingEntity entity) {
        if (entity instanceof ZombifiedPiglin) return MobDropRules.Kind.PIG_ZOMBIE;
        if (entity instanceof ZombieVillager) return MobDropRules.Kind.ZOMBIE_VILLAGER;
        if (entity instanceof Zombie) return MobDropRules.Kind.ZOMBIE;
        if (entity instanceof Spider) return MobDropRules.Kind.SPIDER;
        if (entity instanceof AbstractSkeleton) return MobDropRules.Kind.SKELETON;
        if (entity instanceof Witch) return MobDropRules.Kind.WITCH;
        if (entity instanceof Hoglin) return MobDropRules.Kind.HOGLIN;
        if (entity instanceof Zoglin) return MobDropRules.Kind.ZOGLIN;
        if (entity instanceof Strider) return MobDropRules.Kind.STRIDER;
        if (entity instanceof Wolf) return MobDropRules.Kind.WOLF;
        if (entity instanceof ZombieHorse) return MobDropRules.Kind.ZOMBIE_HORSE;
        if (entity instanceof SkeletonHorse) return MobDropRules.Kind.SKELETON_HORSE;
        if (entity instanceof Donkey) return MobDropRules.Kind.DONKEY;
        if (entity instanceof Mule) return MobDropRules.Kind.MULE;
        if (entity instanceof AbstractHorse) return MobDropRules.Kind.HORSE;
        if (entity instanceof MushroomCow) return MobDropRules.Kind.MOOSHROOM;
        if (entity instanceof Cow) return MobDropRules.Kind.COW;
        if (entity instanceof Sheep) return MobDropRules.Kind.SHEEP;
        if (entity instanceof Pig) return MobDropRules.Kind.PIG;
        if (entity instanceof Chicken) return MobDropRules.Kind.CHICKEN;
        if (entity instanceof Rabbit) return MobDropRules.Kind.RABBIT;
        return MobDropRules.Kind.OTHER;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        var dead = event.getEntity();
        if (dead.level().isClientSide || dead instanceof Player) return;
        apply(dead, event.getDrops(), looting(event), event.isRecentlyHit());
    }
    private static int looting(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return 0;
        var enchantment = event.getEntity().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(enchantment, attacker);
    }
    public static void apply(LivingEntity dead, Collection<ItemEntity> drops, int looting, boolean playerKill) {
        String type = dead.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        if (type.equals("entitytflichminion") || type.equals("entityskeletonboss")) return;
        var random = dead.getRandom();
        var kind = kind(dead);
        double jump = dead instanceof AbstractHorse horse ? horse.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH) : 0;
        var context = new MobDropRules.Context(kind, looting, dead instanceof Animal && dead.isBaby(),
                dead.isOnFire(), playerKill, jump, dead.getMaxHealth());
        var batch = MobDropRules.additions(context, random::nextInt);
        for (var addition : batch.additions()) {
            var stack = resolved(addition);
            if (!stack.isEmpty()) drops.add(entity(dead, stack));
        }
        drops.removeIf(Objects::isNull);
        int rotation = batch.rotation(), scraps = 0;
        for (var item : drops) {
            var stack = item.getItem();
            if (stack.isEmpty()) continue;
            if (!playerKill) {
                if (woodTool(stack)) stack = new ItemStack(Items.STICK);
                else if (stoneTool(stack)) stack = new ItemStack(Items.STICK, 2);
            }
            if (stack.is(Items.ARROW) && MobDropRules.headlessArrow(looting, random::nextInt)) {
                var headless = GTLootTables.stackOf("i:arrowGtWood:Empty:1");
                if (!headless.isEmpty()) stack = preservingMetadata(stack, headless, stack.getCount());
            }
            if (batch.replaceIron() || type.startsWith("entitygaia")) {
                var lead = lead(stack);
                if (!lead.isEmpty()) stack = preservingMetadata(stack, lead, stack.getCount());
            }
            boolean meat = isMeat(stack);
            if (meat && random.nextInt(3) == 0) scraps++;
            if (stack.is(Items.COD) || stack.is(Items.SALMON)) {
                // GT6's fish branch stops the replacement loop after cooking this stack.
                if (dead.isOnFire()) item.setItem(preservingMetadata(stack, new ItemStack(stack.is(Items.COD) ? Items.COOKED_COD : Items.COOKED_SALMON), stack.getCount()));
                break;
            }
            var family = meatFamily(stack);
            boolean cooked = dead.isOnFire() || isCooked(stack);
            var replacement = MobDropRules.meatReplacement(family, stack.getCount(), cooked, rotation, random::nextInt);
            if (replacement != null) {
                var converted = resolved(replacement);
                if (!converted.isEmpty()) stack = preservingMetadata(stack, converted, converted.getCount());
            }
            item.setItem(stack);
            rotation++;
        }
        for (int i = 0; i < scraps; i++) drops.add(entity(dead, GTLootTables.stackOf("tech:scrap_meat:1")));
        if (dead instanceof Mob mob && mob.isPersistenceRequired() && mob.hasCustomName()) {
            var tag = new ItemStack(Items.NAME_TAG);
            tag.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, mob.getCustomName());
            drops.add(entity(dead, tag));
        }
    }
    // ST.set(..., checkNBT=false) keeps source stack metadata during item replacement.
    private static ItemStack preservingMetadata(ItemStack source, ItemStack target, int count) {
        var result = target.copyWithCount(count);
        result.applyComponents(source.getComponentsPatch());
        return result;
    }
    public static ItemStack resolved(MobDropRules.Drop drop) {
        var stack = GTLootTables.stackOf(drop.spec()).copy();
        if (!stack.isEmpty()) {
            stack.setCount(drop.count());
            if (!drop.name().isEmpty()) stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(drop.name()));
        }
        return stack;
    }
    private static ItemEntity entity(LivingEntity dead, ItemStack stack) {
        return new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), stack);
    }
    private static boolean woodTool(ItemStack s) {
        return s.is(Items.WOODEN_SWORD) || s.is(Items.WOODEN_PICKAXE) || s.is(Items.WOODEN_SHOVEL) || s.is(Items.WOODEN_AXE) || s.is(Items.WOODEN_HOE);
    }
    private static boolean stoneTool(ItemStack s) {
        return s.is(Items.STONE_SWORD) || s.is(Items.STONE_PICKAXE) || s.is(Items.STONE_SHOVEL) || s.is(Items.STONE_AXE) || s.is(Items.STONE_HOE);
    }
    public static ItemStack lead(ItemStack stack) {
        var form = MaterialEquivalence.form(stack);
        // Vanilla composition rows describe recovery amounts, without a form prefix.
        if (form == null && stack.is(Items.IRON_INGOT)) form = new MaterialEquivalence.Form(MaterialPrefix.ingot, GTMaterialRegistry.get("Iron"));
        if (form == null && stack.is(Items.IRON_NUGGET)) form = new MaterialEquivalence.Form(MaterialPrefix.nugget, GTMaterialRegistry.get("Iron"));
        if (form == null || !OriginalToolMaterials.inFamily(form.material(), "AnyIronOrSteel")) return ItemStack.EMPTY;
        var prefix = form.prefix();
        if (prefix != MaterialPrefix.plate && prefix != MaterialPrefix.ingot && prefix != MaterialPrefix.chunkGt && prefix != MaterialPrefix.nugget) return ItemStack.EMPTY;
        return com.gregtech.gregtech.registry.GTItems.getStack(prefix, GTMaterialRegistry.get("Lead"), 1);
    }
    private static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(); }
    private static boolean tag(ItemStack stack, String tag) { return stack.is(ItemTags.create(ResourceLocation.parse(tag))); }
    public static boolean isCooked(ItemStack stack) {
        String id = id(stack);
        return id.startsWith("cooked_") || id.startsWith("grilled_") || tag(stack, "c:foods/cooked_meat") || tag(stack, "forge:cooked_meats");
    }
    public static MobDropRules.Meat meatFamily(ItemStack stack) {
        if (stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP)) return MobDropRules.Meat.PORK;
        if (stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF) || tag(stack, "forge:raw_beef") || tag(stack, "forge:cooked_beef")) return MobDropRules.Meat.BEEF;
        String id = id(stack);
        if (id.contains("horse_meat") || tag(stack, "forge:raw_venison") || tag(stack, "forge:cooked_venison")) return MobDropRules.Meat.HORSE;
        return MobDropRules.Meat.NONE;
    }
    public static boolean isMeat(ItemStack stack) {
        if (tag(stack, "c:foods/meat_substitute") || tag(stack, "forge:meat_substitutes")) return false;
        if (meatFamily(stack) != MobDropRules.Meat.NONE) return true;
        if (stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON) || stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT)) return true;
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gregtech"))
            return tag(stack, "c:foods/raw_meat") || tag(stack, "c:foods/cooked_meat") || tag(stack, "forge:raw_meats") || tag(stack, "forge:cooked_meats");
        return Set.of("dogmeat", "grilled_dogmeat", "raw_ham", "cooked_ham", "raw_ham_slice", "cooked_ham_slice",
                "raw_bacon", "grilled_bacon", "raw_ribs", "grilled_ribs", "raw_rib_eye_steak", "grilled_rib_eye_steak", "mutton", "grilled_mutton",
                "donkey_meat", "grilled_donkey_meat", "mule_meat", "grilled_mule_meat").contains(id(stack));
    }
}
