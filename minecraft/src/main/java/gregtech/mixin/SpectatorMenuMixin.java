package gregtech.mixin;

import gregtech.engine.inventory.SpectatorMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retain interaction-event cancellation and vanilla spectator inventory restrictions. */
@Mixin(ServerPlayerGameMode.class)
public abstract class SpectatorMenuMixin {
    @Inject(
            method = "useItemOn",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getMenuProvider(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/MenuProvider;"),
            cancellable = true,
            require = 1)
    private void gregtech$previewMenu(
            ServerPlayer player,
            Level level,
            ItemStack stack,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> result) {
        if (player.isSpectator() && SpectatorMenus.open(player, level, hit)) {
            result.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
