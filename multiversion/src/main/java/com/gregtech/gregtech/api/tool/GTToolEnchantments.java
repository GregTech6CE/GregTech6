package com.gregtech.gregtech.api.tool;

import com.gregtech.gregtech.content.tool.MaterialToolEnchantments;
//? if >=1.21.1 {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?} else {
import net.minecraft.core.registries.BuiltInRegistries;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
*///?} else {
import net.minecraft.world.item.enchantment.EnchantmentHelper;
//?}

/** Applies the original material table to real enchantment NBT, retaining player enchantments. */

/** 1.21 enchantments use the active data-pack registry, never detached holders. */
public final class GTToolEnchantments {
    private GTToolEnchantments() {}
    private static final String APPLIED = "gt.material_enchantments.v1";
//? if >=1.21.1 {
    /*public static void applyCurrent(ItemStack stack) {
        var lookup = com.gregtech.gregtech.api.fluid.FluidDisplayBinding.currentLookup();
        if (lookup != null) apply(stack, lookup);
    }
    public static void apply(ItemStack stack, HolderLookup.Provider lookup) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (stack.isEmpty() || !data.contains(GTToolHelper.ROOT) || data.getBoolean(APPLIED)) return;
        var registry = lookup.lookupOrThrow(Registries.ENCHANTMENT);
        var enchantments = new ItemEnchantments.Mutable(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
*///?} else {
    public static void apply(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(GTToolHelper.ROOT)
                || stack.getTag().getBoolean(APPLIED)) return;
        var enchantments = EnchantmentHelper.getEnchantments(stack);
//?}
        var material=GTToolHelper.getHead(stack);
        java.util.Map<String,Integer> applicable;
        if(stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric) {
            String original=electric.definition().original();
            var flags=com.gregtech.gregtech.content.tool.OriginalToolFlags.of(original);
            applicable=MaterialToolEnchantments.of(flags.mining(),flags.weapon(),flags.ranged(),material);
        } else applicable=MaterialToolEnchantments.of(GTToolHelper.getType(stack).definition(),material);
        for (var entry : applicable.entrySet()) {
//? if >=1.21.1 {
            /*var key = ResourceKey.<Enchantment>create(Registries.ENCHANTMENT, ResourceLocation.parse(MaterialToolEnchantments.id(entry.getKey())));
            registry.get(key).ifPresent(holder -> enchantments.upgrade(holder, entry.getValue()));
*///?} else {
            var enchantment = BuiltInRegistries.ENCHANTMENT.get(ResourceLocation.parse(MaterialToolEnchantments.id(entry.getKey())));
            if (enchantment != null) enchantments.merge(enchantment, entry.getValue(), Math::max);
//?}
        }
//? if >=1.21.1 {
        /*stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(APPLIED, true));
*///?} else {
        EnchantmentHelper.setEnchantments(enchantments, stack);
        stack.getOrCreateTag().putBoolean(APPLIED, true);
//?}
    }
}
