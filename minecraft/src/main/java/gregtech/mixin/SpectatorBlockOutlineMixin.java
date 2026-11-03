package gregtech.mixin;

import gregtech.engine.inventory.SpectatorMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the vanilla HUD/camera restrictions while recognizing GT spectator menus. */
@Mixin(GameRenderer.class)
public abstract class SpectatorBlockOutlineMixin {
    @Shadow
    private boolean renderBlockOutline;

    @Inject(method = "shouldRenderBlockOutline", at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$containerOutline(CallbackInfoReturnable<Boolean> result) {
        var mc = Minecraft.getInstance();
        if (!result.getReturnValueZ()
                && renderBlockOutline
                && !mc.options.hideGui
                && mc.level != null
                && mc.gameMode != null
                && mc.gameMode.getPlayerMode() == GameType.SPECTATOR
                && mc.getCameraEntity() instanceof Player
                && mc.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && SpectatorMenus.canOpen(mc.level, hit)) {
            result.setReturnValue(true);
        }
    }
}
