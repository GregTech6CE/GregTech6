package gregtech.mixin;

import gregtech.engine.inventory.SpectatorMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** GT menus opened through custom packets are also inspectable spectator targets. */
@Mixin(Gui.class)
public abstract class SpectatorCrosshairMixin {
    @Inject(method = "canRenderCrosshairForSpectator", at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$containerCrosshair(HitResult hit, CallbackInfoReturnable<Boolean> result) {
        var level = Minecraft.getInstance().level;
        if (!result.getReturnValueZ()
                && level != null
                && hit instanceof BlockHitResult blockHit
                && hit.getType() == HitResult.Type.BLOCK
                && SpectatorMenus.canOpen(level, blockHit)) {
            result.setReturnValue(true);
        }
    }
}
