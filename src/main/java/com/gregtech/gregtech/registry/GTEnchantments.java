package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Wave 48 N13: GT6 enchantments (Disjunction, Butchery, etc.). */
public final class GTEnchantments {
    private static final DeferredRegister<Enchantment> ENCHS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, GregTech.MODID);

    public static final RegistryObject<Enchantment> DISJUNCTION = ENCHS.register("disjunction",
            () -> new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND}) {});
    public static final RegistryObject<Enchantment> BUTCHERY = ENCHS.register("butchery",
            () -> new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND}) {});
    public static final RegistryObject<Enchantment> HASTE = ENCHS.register("gt_haste",
            () -> new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.DIGGER, new EquipmentSlot[]{EquipmentSlot.MAINHAND}) {});
    public static final RegistryObject<Enchantment> SHARPNESS_MULTI = ENCHS.register("sharpness_multi",
            () -> new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND}) {});
    public static final RegistryObject<Enchantment> SMITE_MULTI = ENCHS.register("smite_multi",
            () -> new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND}) {});

    private GTEnchantments() {}

    public static void register(IEventBus bus) {
        ENCHS.register(bus);
    }
}
