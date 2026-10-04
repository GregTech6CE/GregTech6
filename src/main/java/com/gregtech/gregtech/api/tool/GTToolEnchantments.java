package com.gregtech.gregtech.api.tool;

import com.gregtech.gregtech.content.tool.MaterialToolEnchantments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Applies the original material table to real enchantment NBT, retaining player enchantments. */
public final class GTToolEnchantments {
    private GTToolEnchantments() {}
    private static final String APPLIED = "gt.material_enchantments.v1";
    public static void apply(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(GTToolHelper.ROOT)
                || stack.getTag().getBoolean(APPLIED)) return;
        var enchantments = EnchantmentHelper.getEnchantments(stack);
        var material=GTToolHelper.getHead(stack);
        java.util.Map<String,Integer> applicable;
        if(stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric) {
            String original=switch(electric.toolName()){case "Drill"->"DRILL_LV";case "Chainsaw"->"CHAINSAW_LV";case "Wrench"->"WRENCH_LV";case "Screwdriver"->"SCREWDRIVER_LV";default->throw new IllegalStateException("Unknown electric tool classification");};
            var flags=com.gregtech.gregtech.content.tool.OriginalToolFlags.of(original);
            applicable=MaterialToolEnchantments.of(flags.mining(),flags.weapon(),flags.ranged(),material);
        } else applicable=MaterialToolEnchantments.of(GTToolHelper.getType(stack).definition(),material);
        for (var entry : applicable.entrySet()) {
            var enchantment = BuiltInRegistries.ENCHANTMENT.get(ResourceLocation.parse(MaterialToolEnchantments.id(entry.getKey())));
            if (enchantment != null) enchantments.merge(enchantment, entry.getValue(), Math::max);
        }
        EnchantmentHelper.setEnchantments(enchantments, stack);
        stack.getOrCreateTag().putBoolean(APPLIED, true);
    }
}
