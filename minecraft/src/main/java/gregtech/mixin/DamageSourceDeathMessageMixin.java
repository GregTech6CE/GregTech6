package gregtech.mixin;

import gregtech.engine.tool.GTToolHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** GT6 per-tool death messages; the tool table carries the translation key. */
@Mixin(DamageSource.class)
public abstract class DamageSourceDeathMessageMixin {
    @Inject(method = "getLocalizedDeathMessage", at = @At("HEAD"), cancellable = true)
    private void gt6ce$toolDeathMessage(LivingEntity victim, CallbackInfoReturnable<Component> cir) {
        DamageSource source = (DamageSource) (Object) this;
        Entity killer = source.getEntity();
        if (!(killer instanceof Player player) || source.getDirectEntity() != killer) {
            return;
        }
        Component message = GTToolHelper.toolDeathMessage(player.getMainHandItem(), victim, player);
        if (message != null) {
            cir.setReturnValue(message);
        }
    }
}
