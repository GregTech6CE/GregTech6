package com.gregtech.gregtech.content.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * GT6's <em>bathing</em> hazard: a body that overlaps a fluid block with bathing rows gets those
 * effects, as long as it is not wearing a full chemical hazmat suit.
 *
 * <p>The GT6 entry point is {@code BlockBaseFluid.onEntityCollidedWithBlock}
 * ({@code gregapi/block/fluid/BlockBaseFluid.java:404-409}), i.e. Forge 1.7.10's per-block collision
 * callback fired once per tick for every block the entity's bounding box overlaps:
 *
 * <pre>
 *   if (mActLikeWeb) aEntity.setInWeb();
 *   if (!aWorld.isRemote &amp;&amp; !mEffectsBathing.isEmpty() &amp;&amp; aEntity instanceof EntityLivingBase
 *       &amp;&amp; !UT.Entities.isWearingFullChemHazmat((EntityLivingBase)aEntity)) {
 *       for (int[] tEffects : mEffectsBathing) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
 *   }
 * </pre>
 *
 * <p>1.20.1 has no such callback for fluids, so the same question is asked from a
 * {@link LivingEvent.LivingTickEvent} at {@link EventPriority#LOWEST} - the priority GT6 uses for its
 * own living-update hook ({@code GT_API_Proxy:520}) and the same one
 * {@link BreathingGasEvents} already subscribes with. The block iteration mirrors vanilla's own
 * {@code Entity#checkInsideBlocks} box (the bounding box shrunk by 1.0E-7 so a box that ends exactly
 * on a block border does not pick the next block up).
 *
 * <p>The effects are re-applied every tick, which is what GT6 effectively does: the collision callback
 * fires every tick while the entity overlaps the fluid, and a 100-tick regeneration row therefore
 * never expires while you stand in a geothermal spring. Only the drown damage beside it is throttled
 * to every 20 ticks ({@code :414}); that half lives in {@link BreathingGasEvents}.
 *
 * <p>The web flag ({@code BlockBaseFluid:405} {@code setInWeb()}) is not here: it is a property of the
 * fluid <em>block</em>, so {@code GTWorldFluidBlock#entityInside} applies it.
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
public final class BathingEffectEvents {

    private BathingEffectEvents() {}

    /** GT6 {@code GT_API_Proxy:520}, same {@code LOWEST} priority. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTick(EntityTickEvent.Pre event) {
        if(!(event.getEntity() instanceof LivingEntity entity))return;
        Level level = entity.level();
        if (level.isClientSide || entity.isDeadOrDying()) return;
        for (Fluid fluid : fluidsTouching(level, entity)) {
            WorldFluidEffects.applyBathing(entity, fluid);
        }
    }

    /**
     * The distinct fluids of the blocks the entity's bounding box overlaps, in a stable order. Only
     * fluids with a bathing row are returned, so the caller can loop without re-checking the table.
     */
    public static Set<Fluid> fluidsTouching(Level level, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        BlockPos min = BlockPos.containing(box.minX + 1.0E-7, box.minY + 1.0E-7, box.minZ + 1.0E-7);
        BlockPos max = BlockPos.containing(box.maxX - 1.0E-7, box.maxY - 1.0E-7, box.maxZ - 1.0E-7);
        Set<Fluid> found = new LinkedHashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            net.minecraft.world.level.material.FluidState state = level.getFluidState(pos);
            if (state.isEmpty()) continue;
            Fluid fluid = state.getType();
            if (WorldFluidEffects.hasBathingEffects(fluid)) found.add(fluid);
        }
        return found;
    }
}
