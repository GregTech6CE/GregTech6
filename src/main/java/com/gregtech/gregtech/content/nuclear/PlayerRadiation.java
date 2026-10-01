package com.gregtech.gregtech.content.nuclear;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** GT6 EntityFoodTracker radiation branch: persistent 7-bit dose, no natural decay. */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class PlayerRadiation {
    private static final String KEY = "gregtech.radiation";
    /** Dose from which the symptoms turn lethal in GT6 (wither instead of poison). */
    public static final int SEVERE_DOSE = 100;
    private PlayerRadiation() {}

    public static int dose(Player player) {
        return Math.max(0, Math.min(127, player.getPersistentData().getInt(KEY)));
    }

    public static void change(Player player, long amount) {
        // Clamp the delta before addition so even third-party callers cannot overflow it.
        int value = (int) Math.max(0, Math.min(127, dose(player) + Math.max(-127, Math.min(127, amount))));
        if (value == 0) player.getPersistentData().remove(KEY);
        else player.getPersistentData().putInt(KEY, value);
    }

    public static void applySymptoms(Player player) {
        int value = dose(player);
        if (value < 25 || !player.isAlive()) return;
        player.addEffect(new MobEffectInstance(value >= SEVERE_DOSE ? MobEffects.WITHER : MobEffects.POISON, 100, 0));
        if (value >= SEVERE_DOSE) {
            // GT6 leaves the actual damage to IC2 (DamageSources.getRadioactiveDamage); the port has no
            // IC2, so the severe stage of its own dose tracker deals it - which is also what gives
            // GTDamageTypes.RADIATION ("was irradiated") its call site. One point per symptom tick
            // (every 50 ticks), i.e. 2 HP per 2.5 s on top of the vanilla effects below.
            player.hurt(com.gregtech.gregtech.damage.GTDamageTypes.radiation(player.level()), 1.0F);
        }
        if (value < 50) return;
        int amplifier = value >= SEVERE_DOSE ? 2 : value >= 75 ? 1 : 0;
        for (var effect : new net.minecraft.world.effect.MobEffect[] {MobEffects.CONFUSION,
                MobEffects.HUNGER, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN, MobEffects.WEAKNESS}) {
            player.addEffect(new MobEffectInstance(effect, 100, amplifier));
        }
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide
                && event.player.level().getGameTime() % 50 == 0) applySymptoms(event.player);
    }

    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().remove(KEY);
        if (!event.isWasDeath()) change(event.getEntity(), dose(event.getOriginal()));
    }
}
