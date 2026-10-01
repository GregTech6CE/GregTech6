package com.gregtech.gregtech.content.food;

import com.gregtech.gregtech.damage.GTDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * GT6 {@code gregapi.player.EntityFoodTracker}: the five food statistics that build up from eating and
 * drinking, and the effects they cause.
 *
 * <p>Ported as stored state on the player instead of a 1.7.10 {@code IExtendedEntityProperties}: the six
 * values are 7-bit ({@code UT.Code.bind7}) and live in the player's Forge persistent data under GT6's own
 * compound name {@code gt.props.food} with GT6's own byte keys ({@code a}, {@code c}, {@code d},
 * {@code s}, {@code f} - radiation is the sixth key {@code r} and belongs to
 * {@link com.gregtech.gregtech.content.nuclear.PlayerRadiation}, the port of GT6's radiation branch).
 *
 * <p>Numbers are GT6's, including the two config defaults it ships with
 * ({@code DeathByOverdosingCertainFoods = true}, {@code NutritionSystem = true}, {@code GT_API.java:511-512}):
 * <ul>
 *   <li>every 50 ticks each statistic at 25/50/75/100 applies its potion effects (durations 1200 or 300
 *       ticks, amplifiers 0..3, exactly as {@code EntityFoodTracker.tick():84-185} lists them);</li>
 *   <li>at 100 the three "overdose" statistics (alcohol, caffeine, and - under the nutrition system -
 *       fat, sugar, dehydration) also deal {@code 2} damage of their own GT damage type;</li>
 *   <li>every 100 ticks each statistic decays by one, except radiation, which never decays
 *       ({@code EntityFoodTracker:193} - only a Radaway or death clears it).</li>
 * </ul>
 */
public final class PlayerFoodStats {
    /** GT6 {@code NBT_FOOD} = {@code "gt.props.food"}. */
    private static final String KEY = "gt.props.food";
    /** GT6's byte keys, in the same order as {@link GTFoodStats}' statistics. */
    private static final String[] KEYS = {"a", "c", "d", "s", "f"};
    /** GT6 config default {@code DeathByOverdosingCertainFoods}. */
    public static final boolean OVERDOSE_DEATH = true;
    /** GT6 config default {@code NutritionSystem}: fat, sugar and dehydration have effects. */
    public static final boolean NUTRITION_SYSTEM = true;

    private PlayerFoodStats() {}

    private static CompoundTag store(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY)) {
            data.put(KEY, new CompoundTag());
        }
        return data.getCompound(KEY);
    }

    /** GT6 {@code mAlcohol}/{@code mCaffeine}/… : one 7-bit statistic by {@link GTFoodStats} index. */
    public static int get(Player player, int stat) {
        if (stat < 0 || stat >= GTFoodStats.TRACKED || player.getPersistentData().getCompound(KEY) == null) {
            return 0;
        }
        return Math.max(0, Math.min(127, player.getPersistentData().getCompound(KEY).getByte(KEYS[stat])));
    }

    /** GT6 {@code changeAlcohol}/{@code changeSugar}/… : add, clamped into GT6's 7-bit range. */
    public static void change(Player player, long amount, int stat) {
        if (stat < 0 || stat >= GTFoodStats.TRACKED) {
            return;
        }
        int value = NutritionRules.change(get(player,stat),amount);
        if (value == 0) {
            store(player).remove(KEYS[stat]);
        } else {
            store(player).putByte(KEYS[stat], (byte) value);
        }
    }

    /** GT6 {@code EntityFoodTracker.add}: a fresh tracker is empty. */
    public static void clear(Player player) {
        player.getPersistentData().remove(KEY);
    }

    /**
     * GT6 {@code EntityFoodTracker.tick()}: the effect (and overdose damage) pass every 50 ticks, the decay
     * pass every 100. GT6 runs both off {@code SERVER_TIME}, so the port gates on the level's game time.
     */
    public static void tick(Player player) {
        long time = player.level().getGameTime();
        if (time % 50 == 0) {
            applyEffects(player);
        }
        if (time % 100 == 0) {
            decay(player);
        }
    }

    /** The threshold table, in GT6's order and with GT6's exact numbers. */
    public static void applyEffects(Player player) {
        if (!player.isAlive()) {
            return;
        }
        for(int stat=0;stat<GTFoodStats.TRACKED;stat++) {
            if(!NUTRITION_SYSTEM&&stat>=2)continue;int value=get(player,stat);
            if(value>=100)overdose(player,switch(stat){case 0->GTDamageTypes.ALCOHOL;case 1->GTDamageTypes.CAFFEINE;case 2->GTDamageTypes.DEHYDRATION;case 3->GTDamageTypes.SUGAR;default->GTDamageTypes.FAT;});
            for(var row:NutritionRules.effects(stat,value)) effect(player,switch(row.id()){case "nausea"->MobEffects.CONFUSION;case "strength"->MobEffects.DAMAGE_BOOST;case "weakness"->MobEffects.WEAKNESS;case "haste"->MobEffects.DIG_SPEED;case "hunger"->MobEffects.HUNGER;case "mining_fatigue"->MobEffects.DIG_SLOWDOWN;case "speed"->MobEffects.MOVEMENT_SPEED;case "jump_boost"->MobEffects.JUMP;case "slowness"->MobEffects.MOVEMENT_SLOWDOWN;default->MobEffects.DAMAGE_RESISTANCE;},row.duration(),row.amplifier());
        }
    }

    /**
     * GT6 {@code EntityFoodTracker.tick()}'s decay pass: one point off every statistic that can decay.
     * Radiation is deliberately absent - GT6 comments that it is the only one that never decreases
     * ({@code EntityFoodTracker:193}), so only a Radaway or death removes it.
     */
    public static void decay(Player player) {
        for (int stat = 0; stat < GTFoodStats.TRACKED; stat++) {
            if (get(player, stat) > 0) {
                change(player, -1, stat);
            }
        }
    }

    /**
     * GT6's overdose line: {@code if (FOOD_OVERDOSE_DEATH || health >= 2) attackEntityFrom(source, FOOD_OVERDOSE_DEATH ? 2 : 1)}.
     * The port ships GT6's default ({@code FOOD_OVERDOSE_DEATH = true}), so the health guard is skipped and
     * the damage is always 2; the branch for the other config value is kept so the difference is visible.
     */
    private static void overdose(Player player, net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type) {
        if (OVERDOSE_DEATH) {
            player.hurt(GTDamageTypes.source(player.level(), type), 2.0F);
        } else if (player.getHealth() >= 2.0F) {
            player.hurt(GTDamageTypes.source(player.level(), type), 1.0F);
        }
    }

    /** GT6 {@code UT.Entities.applyPotion(entity, potion, duration, level, particles)}. */
    private static void effect(Player player, MobEffect effect, int duration, int amplifier) {
        // GT6 re-applies the effect unconditionally; vanilla keeps the longer of the two, which matches
        // "applyPotion" for a fresh 1200/300 tick effect.
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
    }
}
