package gregtech.mixin;

import gregtech.content.damage.MaterialProjectileDamageSource;
import gregtech.engine.tool.MaterialEnchantmentStacks;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** GT6 weakness prevents Enderman teleportation, and Disjunction ammunition can apply it on first hit. */
@Mixin(EnderMan.class)
public abstract class EndermanEnchantmentMixin extends Monster {
    protected EndermanEnchantmentMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "teleport(DDD)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$weaknessPreventsTeleport(
            double x, double y, double z, CallbackInfoReturnable<Boolean> result) {
        if (hasEffect(MobEffects.WEAKNESS)) result.setReturnValue(false);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$disjunctionProjectile(
            DamageSource source, float damage, CallbackInfoReturnable<Boolean> result) {
        if (!source.is(DamageTypeTags.IS_PROJECTILE)) return;
        boolean disjunction = source instanceof MaterialProjectileDamageSource projectile
                && MaterialEnchantmentStacks.level(projectile.ammunition(), "disjunction", level()) > 0;
        if (disjunction || hasEffect(MobEffects.WEAKNESS)) result.setReturnValue(super.hurt(source, damage));
    }
}
