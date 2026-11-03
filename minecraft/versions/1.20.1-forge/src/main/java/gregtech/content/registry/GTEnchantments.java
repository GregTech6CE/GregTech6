package gregtech.content.registry;

import gregtech.GregTech;
import gregtech.content.tool.GTEnchantmentEffects;
import gregtech.content.tool.GTEnchantmentKind;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class GTEnchantments {
    private static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, GregTech.NAMESPACE);

    static {
        for (var kind : GTEnchantmentKind.values())
            ENCHANTMENTS.register(kind.path(), () -> new MaterialEnchantment(kind));
        // Decode old stacks without losing their item. Inventory migration replaces these with vanilla enchantments.
        for (String name : new String[] {"butchery", "gt_haste", "sharpness_multi", "smite_multi"})
            ENCHANTMENTS.register(name, LegacyEnchantment::new);
    }

    private GTEnchantments() {}

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);
    }

    private static final class MaterialEnchantment extends Enchantment {
        private final GTEnchantmentKind kind;

        private MaterialEnchantment(GTEnchantmentKind kind) {
            super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
            this.kind = kind;
        }

        @Override
        public int getMaxLevel() {
            return 5;
        }

        @Override
        public int getMinCost(int level) {
            return kind.obtainable() ? 5 + (level - 1) * 8 : Integer.MAX_VALUE;
        }

        @Override
        public int getMaxCost(int level) {
            return kind.obtainable() ? getMinCost(level) + 20 : 0;
        }

        @Override
        public boolean canEnchant(ItemStack stack) {
            return kind.obtainable() && (stack.getItem() instanceof AxeItem || super.canEnchant(stack));
        }

        @Override
        public boolean isAllowedOnBooks() {
            return kind.obtainable();
        }

        @Override
        public boolean canApplyAtEnchantingTable(ItemStack stack) {
            return kind.obtainable() && super.canApplyAtEnchantingTable(stack);
        }

        @Override
        public boolean isTradeable() {
            return kind.obtainable();
        }

        @Override
        public boolean isDiscoverable() {
            return kind.obtainable();
        }

        @Override
        protected boolean checkCompatibility(Enchantment other) {
            return !(other instanceof DamageEnchantment)
                    && !(other instanceof MaterialEnchantment)
                    && super.checkCompatibility(other);
        }

        @Override
        public void doPostAttack(LivingEntity attacker, Entity target, int level) {
            GTEnchantmentEffects.apply(kind, level, target);
        }
    }

    private static final class LegacyEnchantment extends Enchantment {
        private LegacyEnchantment() {
            super(Rarity.VERY_RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
        }

        @Override
        public boolean canEnchant(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canApplyAtEnchantingTable(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isAllowedOnBooks() {
            return false;
        }

        @Override
        public boolean isTradeable() {
            return false;
        }

        @Override
        public boolean isDiscoverable() {
            return false;
        }

        @Override
        public boolean isTreasureOnly() {
            return true;
        }
    }
}
