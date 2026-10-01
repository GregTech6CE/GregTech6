package com.gregtech.gregtech.util;

import com.gregtech.gregtech.api.energy.GTVoltageTiers;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.damage.GTDamageTypes;
import com.gregtech.gregtech.damage.GTHazmat;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code UT.Entities} - the hazard half: every "this hurts" helper in GT6 funnels through these,
 * and they are the only place that decides whether an entity is protected (hazard suit, creative
 * mode, being made of fire, being a skeleton) before dealing a GT damage type.
 *
 * <p>Ported one-for-one, including GT6's odd corners:
 * <ul>
 *   <li>{@code applyChemDamage} poisons as well as hurting, and skips <em>plain</em> skeletons only
 *       (GT6 compares the exact class, so a wither skeleton was never immune);</li>
 *   <li>{@code applyHeatDamage} skips blazes and anything with fire resistance;</li>
 *   <li>{@code applyTemperatureDamage} turns a temperature in Kelvin into heat above 320&nbsp;K and
 *       frost below 260&nbsp;K, with two formulas (capped and uncapped);</li>
 *   <li>{@code applyElectricityDamage} deals {@code tierMax(voltage) * amperage * 4} - the tier
 *       <em>index</em>, not the voltage, so ULV (8&nbsp;V) deals nothing at all
 *       ({@link GTVoltageTiers}).</li>
 * </ul>
 *
 * <p>The hazard-suit checks live in {@link GTHazmat}. GT6 multiplies every hazard by its
 * {@code TFC_DAMAGE_MULTIPLIER} config value, which only exists for the TerraFirmaCraft integration;
 * the port has no such config and uses 1.
 */
public final class GTEntityHelper {
    /** GT6 {@code CS.DEF_ENV_TEMP}: 300&nbsp;K, the temperature below which heat damage stops. */
    public static final long ENV_TEMP = 300L;
    /** GT6's heat threshold: above {@code 320 K} (about 47&nbsp;&deg;C) a surface burns. */
    public static final long HEAT_THRESHOLD = 320L;
    /** GT6's frost threshold: below {@code 260 K} a surface freezes. */
    public static final long FROST_THRESHOLD = 260L;

    private GTEntityHelper() {}

    /** GT6 {@code UT.Entities.isCreative}: creative mode is immune to every hazard. */
    public static boolean isCreative(Entity entity) {
        return entity instanceof LivingEntity living && GTHazmat.isCreative(living);
    }

    /** GT6 {@code UT.Entities.isInvincible} (creative, the only invincibility GT6 models). */
    public static boolean isInvincible(Entity entity) {
        return entity instanceof LivingEntity living && GTHazmat.isInvincible(living);
    }

    /** GT6 {@code UT.Entities.isImmuneToBreathingGases}: wears the full gas suit. */
    public static boolean isImmuneToBreathingGases(Entity entity) {
        return entity instanceof LivingEntity living && GTHazmat.isImmuneToBreathingGases(living);
    }

    /**
     * GT6 {@code UT.Entities.getHeatDamageFromItem}: how much a carried item burns its holder, summed
     * over the prefix ({@code OP.ingotHot.mHeatDamage = 3.0F}) and the material. GT6 adds the
     * material's own {@code mHeatDamage}, which nothing in GT6 or the port declares, so the prefix
     * decides.
     */
    public static float heatDamageFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0F;
        }
        // GT6 reads OM.anydata(stack).mPrefix, which covers both GT material items and unified vanilla
        // ones; the port keeps material items off ItemMaterialRegistry (their prefix lives on the
        // MaterialItem instance), so both paths are needed here.
        com.gregtech.gregtech.data.MaterialPrefix prefix =
                stack.getItem() instanceof com.gregtech.gregtech.item.MaterialItem materialItem
                        ? materialItem.getPrefix()
                        : ItemMaterialRegistry.get(stack)
                                .map(com.gregtech.gregtech.api.material.ItemComposition::prefix).orElse(null);
        return prefix == null ? 0.0F : prefix.heatDamage();
    }

    /** GT6 {@code UT.Entities.applyChemDamage}: chemical burn plus a poison effect. */
    public static boolean applyChemDamage(Entity entity, float damage) {
        if (damage <= 0.0F || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        // GT6 compares the exact class (EntitySkeleton), so only plain skeletons shrug chemicals off.
        if (living.getClass() == Skeleton.class || GTHazmat.isChemProtected(living)) {
            return false;
        }
        if (!living.hurt(GTDamageTypes.chem(living.level()), damage)) {
            return false;
        }
        // GT6: duration = max(20, damage * 100 + the effect's remaining duration), amplifier 1 (Poison II).
        MobEffectInstance previous = living.getEffect(MobEffects.POISON);
        int duration = Math.max(20, (int) (damage * 100.0F)
                + (previous == null ? 0 : Math.max(0, previous.getDuration())));
        living.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1));
        return true;
    }

    /** GT6 {@code UT.Entities.applyHeatDamage} - death message "was boiled alive". */
    public static boolean applyHeatDamage(Entity entity, float damage) {
        if (damage <= 0.0F || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (living instanceof Blaze || living.hasEffect(MobEffects.FIRE_RESISTANCE)
                || GTHazmat.isHeatProtected(living)) {
            return false;
        }
        return living.hurt(GTDamageTypes.heat(living.level()), damage);
    }

    /** GT6 {@code UT.Entities.applyFrostDamage} - death message "got frozen". */
    public static boolean applyFrostDamage(Entity entity, float damage) {
        if (damage <= 0.0F || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (GTHazmat.isFrostProtected(living)) {
            return false;
        }
        return living.hurt(GTDamageTypes.frost(living.level()), damage);
    }

    /**
     * GT6 {@code UT.Entities.applyElectricityDamage(Entity, long aVoltage, long aAmperage)}: the
     * damage is {@code tierMax(voltage) * amperage * 4}, i.e. it scales with the <em>tier index</em>
     * (LV = 1, MV = 2, HV = 3 …), so 8&nbsp;V (ULV, index 0) is harmless and 512&nbsp;V hits four
     * times as hard as 32&nbsp;V for the same amperage.
     */
    public static boolean applyElectricityDamage(Entity entity, long voltage, long amperage) {
        return applyElectricityDamageValue(entity, (long) GTVoltageTiers.tierMax(voltage) * amperage * 4L);
    }

    /** GT6 {@code UT.Entities.applyElectricityDamage(Entity, long aWattage)}: {@code tierMax(wattage) * 4}. */
    public static boolean applyElectricityDamage(Entity entity, long wattage) {
        return applyElectricityDamageValue(entity, (long) GTVoltageTiers.tierMax(wattage) * 4L);
    }

    private static boolean applyElectricityDamageValue(Entity entity, long damage) {
        if (damage <= 0L || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (GTHazmat.isElectroProtected(living)) {
            return false;
        }
        return living.hurt(GTDamageTypes.electric(living.level()), damage);
    }

    /** GT6 {@code UT.Entities.applyTemperatureDamage(Entity, long)} - multiplier 1. */
    public static boolean applyTemperatureDamage(Entity entity, long temperature) {
        return applyTemperatureDamage(entity, temperature, 1.0F);
    }

    /** GT6 {@code UT.Entities.applyTemperatureDamage(Entity, long, float)} - uncapped. */
    public static boolean applyTemperatureDamage(Entity entity, long temperature, float multiplier) {
        if (temperature > HEAT_THRESHOLD) {
            return applyHeatDamage(entity, (multiplier * (temperature - ENV_TEMP)) / 50.0F);
        }
        if (temperature < FROST_THRESHOLD) {
            return applyFrostDamage(entity, (multiplier * (270L - temperature)) / 25.0F);
        }
        return false;
    }

    /**
     * GT6 {@code UT.Entities.applyTemperatureDamage(Entity, long, float, float)} - the capped
     * variant, which is the one GT6's fluid pipes use ({@code multiplier 1, cap 5}).
     */
    public static boolean applyTemperatureDamage(Entity entity, long temperature, float multiplier, float cap) {
        if (temperature > HEAT_THRESHOLD) {
            return applyHeatDamage(entity,
                    Math.max(1.0F, Math.min(cap, (multiplier * (temperature - ENV_TEMP)) / 50.0F)));
        }
        if (temperature < FROST_THRESHOLD) {
            return applyFrostDamage(entity,
                    Math.max(1.0F, Math.min(cap, (multiplier * (270L - temperature)) / 25.0F)));
        }
        return false;
    }

    /** GT6 {@code UT.Entities.applyTemperatureDamage(Entity, long, float, float)} with GT6's own
     *  fluid-pipe arguments ({@code multiplier 1, cap 5}); used by pipes and hot machine surfaces. */
    public static boolean applyContactTemperatureDamage(Entity entity, long temperature) {
        return applyTemperatureDamage(entity, temperature, 1.0F, 5.0F);
    }
}
