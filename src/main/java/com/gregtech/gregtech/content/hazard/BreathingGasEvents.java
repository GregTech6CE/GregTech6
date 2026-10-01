package com.gregtech.gregtech.content.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * GT6's "breathing" hazard: standing with your head inside a fluid that declares head-inside effects.
 *
 * <p><b>The GT6 entry point.</b> {@code GT_API_Proxy.java:520-528} subscribes
 * {@code LivingUpdateEvent} at {@link EventPriority#LOWEST} and asks one question per living entity
 * per tick: what block is at the <em>eye</em>?
 * <pre>
 *   tX = roundDown(posX);
 *   tY = roundDown(posY + getEyeHeight());
 *   tZ = roundDown(posZ);
 *   if (block is IBlockOnHeadInside) onHeadInside(entity, world, tX, tY, tZ);
 * </pre>
 * That interface ({@code IBlockOnHeadInside.java:29}) has three fluid implementations in GT6:
 * {@code BlockBaseFluid.java:411-415} (every GT6 fluid block, including the natural-gas spring),
 * {@code BlockWaterlike.java:226-231} (the swamp) and {@code BlockSwamp.java:193} (which adds "slimes
 * are immune" before delegating). The rules themselves - which fluid has which effects, which immunity
 * is asked, and whether the fluid drowns - live in {@link WorldFluidEffects}, one table per GT6
 * registration line, so this class only carries the tick cadence:
 * <pre>
 *   if (!world.isRemote &amp;&amp; !effects.isEmpty() &amp;&amp; gate) {
 *       for (int[] tEffects : effects) UT.Entities.applyPotion(entity, ...);   // every tick
 *       if (getMaterial() != Material.water &amp;&amp; SERVER_TIME % 20 == 0)
 *           entity.attackEntityFrom(DamageSource.drown, 2.0F);                 // every 20 ticks
 *   }
 * </pre>
 * The effects are therefore re-applied <em>every</em> tick (a gas you leave after one tick still
 * leaves you poisoned for 300), while only the 2.0 drown damage is throttled - the split this class
 * keeps: {@link WorldFluidEffects#applyHeadInside} every tick, {@link #breatheIn(LivingEntity)} on
 * {@link #isDrownTick} ticks.
 *
 * <p>The rule is split into pure pieces ({@link #headPos}, {@link #headFluid}, {@link #isDrownTick},
 * {@link #breatheIn(LivingEntity, Fluid)}) so it can be driven without a world, following
 * {@code FoodStatEvents}, the other event class of this kind.
 */
@Mod.EventBusSubscriber(modid = "gregtech")
public final class BreathingGasEvents {
    /** GT6 {@code BlockBaseFluid:414} / {@code BlockWaterlike:229}: {@code SERVER_TIME % 20 == 0}. */
    public static final int DROWN_INTERVAL = WorldFluidEffects.DROWN_INTERVAL;
    /** GT6 {@code BlockBaseFluid:414}: {@code attackEntityFrom(DamageSource.drown, 2.0F)}. */
    public static final float DROWN_DAMAGE = WorldFluidEffects.DROWN_DAMAGE;

    /**
     * Fluid registry path of GT6's natural-gas spring ({@code Loader_Worldgen}, {@code FL.Gas_Natural}) -
     * the gas whose breathing row the port has had since §107. The oils' rows
     * ({@code Loader_Blocks.java:149-152}) now use the same path.
     */
    public static final String NATURAL_GAS = WorldFluidEffects.NATURAL_GAS;

    private BreathingGasEvents() {}

    /** GT6 {@code GT_API_Proxy:520} {@code onLivingUpdate}, at the same {@code LOWEST} priority. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide) return;
        Fluid fluid = headFluid(entity);
        // GT6 BlockBaseFluid:412-414 - the effects are re-applied on every tick the head is inside,
        // the damage only on the 20-tick beat.
        WorldFluidEffects.applyHeadInside(entity, fluid);
        if (isDrownTick(level.getGameTime())) breatheIn(entity, fluid);
    }

    /** GT6's {@code SERVER_TIME % 20 == 0} half of the drown rule, as a pure predicate. */
    public static boolean isDrownTick(long gameTime) {
        return WorldFluidEffects.isDrownTick(gameTime);
    }

    /**
     * GT6 {@code GT_API_Proxy:523-525}: the block the entity's <em>eyes</em> are in, which is
     * {@code roundDown(posY + getEyeHeight())} - not the block its feet are in. That is why a gas
     * one block above the head is harmless and a gas at head height is not.
     */
    public static BlockPos headPos(LivingEntity entity) {
        return BlockPos.containing(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
    }

    /** The fluid at {@link #headPos}; {@code Fluids.EMPTY} when the eyes are in air. */
    public static Fluid headFluid(LivingEntity entity) {
        return entity.level().getFluidState(headPos(entity)).getType();
    }

    /** {@link #breatheIn(LivingEntity, Fluid)} against the fluid actually at the entity's eyes. */
    public static boolean breatheIn(LivingEntity entity) {
        if (entity.level().isClientSide) return false;
        return breatheIn(entity, headFluid(entity));
    }

    /**
     * The whole GT6 rule for one fluid, minus the 20-tick gate - the form a test can drive directly:
     * apply the effects (if the fluid has a row and the immunity gate lets them through) and then deal
     * one 2.0 drown hit if the fluid's material is not water.
     *
     * @return true when the entity was actually hurt
     */
    public static boolean breatheIn(LivingEntity entity, Fluid fluid) {
        if (entity.level().isClientSide || entity.isDeadOrDying()) return false;
        // GT6 BlockBaseFluid:412 / BlockWaterlike:227: a non-empty effect list is the switch, and the
        // immunity is asked per path (gas suit for the base path, gas-or-chem suit for the swamp).
        if (!WorldFluidEffects.applyHeadInside(entity, fluid)) return false;
        // GT6 :414 - getMaterial() != Material.water: only the oils and the natural gas drown.
        if (!WorldFluidEffects.headInsideDrowns(fluid)) return false;
        return entity.hurt(entity.level().damageSources().drown(), DROWN_DAMAGE);
    }

    /**
     * GT6's effect half only, for callers that run their own cadence (and for tests that want to see
     * the effects without the damage).
     *
     * @return whether the fluid declared head-inside effects and the entity was allowed to get them
     */
    public static boolean applyBreathingEffects(LivingEntity entity, Fluid fluid) {
        return WorldFluidEffects.applyHeadInside(entity, fluid);
    }
}
