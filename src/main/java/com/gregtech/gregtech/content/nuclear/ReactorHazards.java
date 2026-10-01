package com.gregtech.gregtech.content.nuclear;

import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * GT6's reactor contact hazard: {@code gregtech/tileentity/energy/reactors/MultiTileEntityReactorCore.java:303}
 *
 * <pre>@Override public void onEntityCollidedWithBlock(Entity aEntity) {
 *     if (mRunning) {UT.Entities.applyHeatDamage(aEntity, 5); UT.Entities.applyRadioactivity(aEntity, 3, 1);}
 * }</pre>
 *
 * <p>So leaning on a <em>running</em> core (either size - that class is the shared base of the 1x1 and
 * the 2x2) burns for a flat 5 and adds a level-3 radiation dose of 1. The radial irradiation of
 * everything standing near the reactor was already ported ({@code ReactorCoreBlockEntity.irradiate},
 * GT6 {@code MultiTileEntityReactorCore1x1:87-95}); this is the part that needs the entity to actually
 * touch the core block, which is why it lives in the blocks' {@code entityInside}.
 *
 * <p>Both protections GT6 applies come for free through {@link GTEntityHelper} and
 * {@link ReactorRadiation}: a full radiation suit (or creative mode) ignores the dose, and a heat suit,
 * a blaze or fire resistance ignores the burn.
 */
public final class ReactorHazards {
    private ReactorHazards() {}

    /** True while this core position holds a reactor core that is neither stopped nor failed. */
    public static boolean isRunning(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ReactorCoreBlockEntity core && !core.stopped && !core.failed;
    }

    /** GT6 {@code MultiTileEntityReactorCore:303}: heat 5 plus a level-3 dose of 1, once per entity. */
    public static void contact(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !living.isAlive()) return;
        if (!isRunning(level, pos)) return;
        GTEntityHelper.applyHeatDamage(living, 5.0F);
        ReactorRadiation.apply(living, 3, 1);
    }
}
