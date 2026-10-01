package com.gregtech.gregtech.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6-style nuclear fuel rod with NBT.
 *
 * <p>Each fuel rod stores: fuel type (enum), remaining health (0 = depleted),
 * and enrichment level. These are inserted into reactor cores. Unlike the old
 * 1x1 core which used bare {@link MaterialPrefix#stick} items, fuel rods carry
 * the state needed for neutron flux simulation and breeding.</p>
 */
public class FuelRodItem extends net.minecraft.world.item.BlockItem {

    public static final String TAG_FUEL_TYPE = "gt_fuel_type";
    public static final String TAG_HEALTH = "gt_health";
    public static final String TAG_MAX_HEALTH = "gt_max_health";
    public static final String TAG_DEPLETED = "gt_depleted";

    private final com.gregtech.gregtech.content.nuclear.ReactorRodCatalog.Rod definition;
    private final FuelType fuelType;
    private final int tier;
    private final int maxHealth;

    public FuelRodItem(net.minecraft.world.level.block.Block block, FuelType fuelType, int tier, Properties properties) {
        super(block, properties);
        this.definition = com.gregtech.gregtech.content.nuclear.ReactorRodCatalog.byOriginal(switch(fuelType) {
            case U_235 -> 9221; case U_238 -> 9220; case Pu_239 -> 9233; case Th_232 -> 9210;
            case Co_60 -> 9250; case Am_241 -> 9241; default -> 0;
        });
        this.fuelType = fuelType;
        this.tier = tier;
        this.maxHealth = fuelType.baseHealth * (tier + 1);
    }

    public FuelRodItem(net.minecraft.world.level.block.Block block, com.gregtech.gregtech.content.nuclear.ReactorRodCatalog.Rod definition, Properties properties) {
        super(block,properties);this.definition=definition;this.fuelType=FuelType.NONE;this.tier=0;this.maxHealth=0;
    }
    @Override public String getDescriptionId() { return "item.gregtech." + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath(); }
    public com.gregtech.gregtech.content.nuclear.ReactorRodCatalog.Rod definition(){return definition;}
    public FuelType fuelType() { return fuelType; }
    public int tier() { return tier; }
    public int maxHealth() { return maxHealth; }
    public int tintRgb() {
        if(definition!=null)return com.gregtech.gregtech.api.material.GTMaterialRegistry.get(definition.material()).getColor();
        var material = switch (fuelType) {
            case U_235 -> com.gregtech.gregtech.content.material.Materials.Uranium235;
            case U_238 -> com.gregtech.gregtech.content.material.Materials.Uranium;
            case Pu_239, MOX -> com.gregtech.gregtech.content.material.Materials.Plutonium239;
            case Th_232 -> com.gregtech.gregtech.content.material.Materials.Thorium;
            case Co_60 -> com.gregtech.gregtech.content.material.Materials.Cobalt60;
            case Am_241 -> com.gregtech.gregtech.content.material.Materials.Americium241;
            case Am_243 -> com.gregtech.gregtech.content.material.Materials.Americium;
            case Naquadah -> com.gregtech.gregtech.content.material.Materials.Naquadah;
            default -> com.gregtech.gregtech.content.material.Materials.Steel;
        };
        return material.getColor();
    }

    /** Create a fresh fuel rod ItemStack. */
    public ItemStack freshStack() {
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_FUEL_TYPE, fuelType.name());
        tag.putInt(TAG_HEALTH, maxHealth);
        tag.putInt(TAG_MAX_HEALTH, maxHealth);
        tag.putBoolean(TAG_DEPLETED, false);
        if(definition!=null)tag.putLong("gt.reactor_life",definition.life());
        stack.setTag(tag);
        return stack;
    }

    /** Read fuel type from NBT. */
    public static FuelType getFuelType(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelRodItem)) return FuelType.NONE;
        CompoundTag tag = stack.getTag();
        if (tag == null) return FuelType.NONE;
        try { return FuelType.valueOf(tag.getString(TAG_FUEL_TYPE)); }
        catch (IllegalArgumentException e) { return FuelType.NONE; }
    }

    /** Read remaining health from NBT. */
    public static int getHealth(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelRodItem)) return 0;
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_HEALTH) : 0;
    }

    /** Read max health from NBT. */
    public static int getMaxHealth(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelRodItem)) return 0;
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_MAX_HEALTH) : 0;
    }

    /** Whether this rod is depleted. */
    public static boolean isDepleted(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelRodItem)) return true;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_DEPLETED);
    }

    /** Reduce health, mark depleted at 0. Returns true if rod was consumed. */
    public static boolean consumeHealth(ItemStack stack, int amount) {
        if (!(stack.getItem() instanceof FuelRodItem)) return false;
        CompoundTag tag = stack.getOrCreateTag();
        int health = tag.getInt(TAG_HEALTH);
        health = Math.max(0, health - amount);
        tag.putInt(TAG_HEALTH, health);
        if (health <= 0) tag.putBoolean(TAG_DEPLETED, true);
        return health <= 0;
    }

    /** Check if any ItemStack is a fuel rod (regardless of depletion). */
    public static boolean isFuelRod(ItemStack stack) {
        return stack.getItem() instanceof FuelRodItem;
    }

    /** Check if stack is a non-depleted fuel rod. */
    public static boolean isActiveFuelRod(ItemStack stack) {
        return stack.getItem() instanceof FuelRodItem && !isDepleted(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if(definition!=null) {
            tooltip.add(Component.literal(definition.kind().name()).withStyle(ChatFormatting.GRAY));
            if(definition.life()>0) tooltip.add(Component.literal("Remaining: "+com.gregtech.gregtech.content.nuclear.RodPhysics.life(stack)).withStyle(ChatFormatting.GREEN));
            if(definition.kind()==com.gregtech.gregtech.content.nuclear.ReactorRodCatalog.Kind.NUCLEAR)
                tooltip.add(Component.literal("Self: "+definition.self()+" | Emission: "+definition.emission()+" | Max: "+definition.maximum()+" | Factor: 1/"+definition.divisor()));
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {tooltip.add(Component.translatable("gt.tooltip.reactor.legacy_rod"));return;}
        int health = tag.getInt(TAG_HEALTH);
        int max = tag.getInt(TAG_MAX_HEALTH);
        boolean depleted = tag.getBoolean(TAG_DEPLETED);

        tooltip.add(Component.literal("Fuel: " + fuelType.displayName)
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Tier: " + (tier + 1))
                .withStyle(ChatFormatting.GRAY));

        if (depleted || health <= 0) {
            tooltip.add(Component.literal("Depleted")
                    .withStyle(ChatFormatting.RED));
        } else if (max > 0) {
            int pct = health * 100 / max;
            ChatFormatting color = pct > 50 ? ChatFormatting.GREEN
                    : pct > 25 ? ChatFormatting.YELLOW : ChatFormatting.RED;
            tooltip.add(Component.literal("Durability: " + health + "/" + max + " (" + pct + "%)")
                    .withStyle(color));
        }
    }

    // =========================================================================
    // Fuel types
    // =========================================================================

    public enum FuelType {
        NONE("None", 0, 72000, 32),
        U_235("Uranium-235", 0, 72000, 32),
        U_238("Uranium-238", 0, 36000, 16),
        Pu_239("Plutonium-239", 0, 54000, 48),
        Th_232("Thorium-232", 0, 96000, 8),
        MOX("MOX", 1, 48000, 64),
        Naquadah("Naquadah", 2, 144000, 128),
        Co_60("Cobalt-60", 0, 18000, 24),
        Am_241("Americium-241", 0, 36000, 32),
        Am_243("Americium-243", 0, 27000, 48),
        ;

        public final String displayName;
        public final int minimumTier;
        public final int baseHealth;
        public final long baseHuPerTick;

        FuelType(String displayName, int minimumTier, int baseHealth, long baseHuPerTick) {
            this.displayName = displayName;
            this.minimumTier = minimumTier;
            this.baseHealth = baseHealth;
            this.baseHuPerTick = baseHuPerTick;
        }
    }
}
