package gregtech.engine.effect;

import gregtech.platform.Mc;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/**
 * Shared mob-effect facade across 1.20.1 Forge ({@link MobEffect}) and 1.21.1 NeoForge
 * ({@code Holder<MobEffect>} and {@code EffectCures}).
 */
public final class MobEffectCompat {
    private MobEffectCompat() {}

    /** Resolves a vanilla effect ID under the {@code minecraft} namespace. */
    public static ResourceLocation vanillaId(String path) {
        return Mc.id("minecraft", path);
    }

    /** Removes all status effects cured by milk from {@code entity}. */
    public static void cureWithMilk(LivingEntity entity) {
//? if >=1.21.1 {
        /*entity.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
*///?} else {
        entity.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
//?}
    }

    /** Returns the status effects carried by a potion item stack. */
    public static Iterable<MobEffectInstance> potionEffects(ItemStack potion) {
//? if >=1.21.1 {
        /*return potion.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                net.minecraft.world.item.alchemy.PotionContents.EMPTY).getAllEffects();
*///?} else {
        return net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(potion);
//?}
    }

    /** Applies a potion effect instance, triggering instantaneous effects directly. */
    public static void applyInstance(LivingEntity entity, MobEffectInstance instance) {
        MobEffect raw = unwrap(instance.getEffect());
        if (raw.isInstantenous()) {
            raw.applyInstantenousEffect(entity, entity, entity, instance.getAmplifier(), 1.0);
        } else {
            entity.addEffect(new MobEffectInstance(instance));
        }
    }

    /** Returns 0 when absent, or {@code amplifier + 1} for an active effect identified by {@code id}. */
    public static int effectLevel(LivingEntity entity, @Nullable ResourceLocation id) {
        var effect = resolve(id);
        if (effect == null) return 0;
        var active = entity.getEffect(effect);
        return active == null ? 0 : active.getAmplifier() + 1;
    }

    /** Returns remaining duration in ticks (clamped at 0) for an active effect identified by {@code id}. */
    public static int remainingDuration(LivingEntity entity, @Nullable ResourceLocation id) {
        var effect = resolve(id);
        if (effect == null) return 0;
        var active = entity.getEffect(effect);
        return active == null ? 0 : Math.max(0, active.getDuration());
    }

    /** Removes the effect identified by {@code id} if registered and active. */
    public static boolean remove(LivingEntity entity, @Nullable ResourceLocation id) {
        var effect = resolve(id);
        return effect != null && entity.removeEffect(effect);
    }

    /** Applies a non-instantaneous effect identified by {@code id}, ignoring unregistered IDs. */
    public static boolean apply(LivingEntity entity, @Nullable ResourceLocation id,
            int duration, int amplifier, boolean ambient, boolean visible) {
        var effect = resolve(id);
        return effect != null && entity.addEffect(new MobEffectInstance(effect, duration, amplifier, ambient, visible));
    }

    /** Applies an effect identified by {@code id}, triggering instantaneous effects directly. */
    public static boolean applyOrTrigger(LivingEntity entity, @Nullable ResourceLocation id,
            int duration, int amplifier, boolean ambient, boolean visible) {
        var effect = resolve(id);
        if (effect == null) return false;
        MobEffect raw = unwrap(effect);
        if (raw.isInstantenous()) {
            raw.applyInstantenousEffect(entity, entity, entity, amplifier, 1.0);
            return true;
        }
        return entity.addEffect(new MobEffectInstance(effect, duration, amplifier, ambient, visible));
    }

    /** Creates a {@link MobEffectInstance} for {@code id}, or {@code null} if unregistered. */
    @Nullable
    public static MobEffectInstance createInstance(@Nullable ResourceLocation id, int duration, int amplifier) {
        var effect = resolve(id);
        return effect == null ? null : new MobEffectInstance(effect, duration, amplifier);
    }

//? if >=1.21.1 {
    /*@Nullable
    private static net.minecraft.core.Holder<MobEffect> resolve(@Nullable ResourceLocation id) {
        return id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
    }

    private static MobEffect unwrap(net.minecraft.core.Holder<MobEffect> effect) {
        return effect.value();
    }
*///?} else {
    @Nullable
    private static MobEffect resolve(@Nullable ResourceLocation id) {
        return id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
    }

    private static MobEffect unwrap(MobEffect effect) {
        return effect;
    }
//?}
}
