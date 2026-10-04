package com.gregtech.gregtech.api.tool;

import com.gregtech.gregtech.content.tool.MaterialToolEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** 1.21 enchantments use the active data-pack registry, never detached holders. */
public final class GTToolEnchantments {
    private GTToolEnchantments() {}
    private static final String APPLIED = "gt.material_enchantments.v1";
    public static void applyCurrent(ItemStack stack) {
        var lookup = com.gregtech.gregtech.api.fluid.FluidDisplayBinding.currentLookup();
        if (lookup != null) apply(stack, lookup);
    }
    public static void apply(ItemStack stack, HolderLookup.Provider lookup) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (stack.isEmpty() || !data.contains(GTToolHelper.ROOT) || data.getBoolean(APPLIED)) return;
        var registry = lookup.lookupOrThrow(Registries.ENCHANTMENT);
        var enchantments = new ItemEnchantments.Mutable(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        var material=GTToolHelper.getHead(stack);
        java.util.Map<String,Integer> applicable;
        if(stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric) {
            String original=switch(electric.toolName()){case "Drill"->"DRILL_LV";case "Chainsaw"->"CHAINSAW_LV";case "Wrench"->"WRENCH_LV";case "Screwdriver"->"SCREWDRIVER_LV";default->throw new IllegalStateException("Unknown electric tool classification");};
            var flags=com.gregtech.gregtech.content.tool.OriginalToolFlags.of(original);
            applicable=MaterialToolEnchantments.of(flags.mining(),flags.weapon(),flags.ranged(),material);
        } else applicable=MaterialToolEnchantments.of(GTToolHelper.getType(stack).definition(),material);
        for (var entry : applicable.entrySet()) {
            var key = ResourceKey.<Enchantment>create(Registries.ENCHANTMENT, ResourceLocation.parse(MaterialToolEnchantments.id(entry.getKey())));
            registry.get(key).ifPresent(holder -> enchantments.upgrade(holder, entry.getValue()));
        }
        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(APPLIED, true));
    }
}
