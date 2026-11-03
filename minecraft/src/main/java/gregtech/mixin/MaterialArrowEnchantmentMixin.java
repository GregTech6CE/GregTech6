package gregtech.mixin;

import gregtech.content.entity.MaterialArrowEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractArrow.class)
public abstract class MaterialArrowEnchantmentMixin {
    @Redirect(
            method = "onHitEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1)
    private boolean gregtech$ammunitionEffects(Entity target, DamageSource source, float damage) {
        return (Object) this instanceof MaterialArrowEntity arrow
                ? arrow.hurtTarget(target, source, damage)
                : target.hurt(source, damage);
    }
}
