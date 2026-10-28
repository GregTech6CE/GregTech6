package gregtech.mixin;

import gregtech.content.cover.*;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exposes emitting cover outputs through BlockState; basic machines and signal wires keep their own paths. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class CoverSignalMixin {
    private static PanelCoverHost emittingHost(BlockGetter level, BlockPos pos, Direction query) {
        var owner = level.getBlockEntity(pos);
        if (owner == null
                || !ComponentCoverFallback.ownClass(owner)
                || owner instanceof gregtech.content.blockentity.machine.BasicMachineBlockEntity
                || owner instanceof gregtech.content.blockentity.energy.SignalWireBlockEntity
                || !(owner instanceof PanelCoverHost host)) return null;
        var cover = host.getCover(query.getOpposite());
        var panel = PanelCover.of(cover);
        var detector = MachineCoverSpec.of(cover);
        return panel != null && panel.output() || detector != null && detector.detector() ? host : null;
    }

    @Inject(method = "isSignalSource", at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$signalSource(CallbackInfoReturnable<Boolean> callback) {
        var block = ((net.minecraft.world.level.block.state.BlockState) (Object) this).getBlock();
        if (block instanceof net.minecraft.world.level.block.EntityBlock
                && block.getClass().getName().startsWith("gregtech.")) callback.setReturnValue(true);
    }

    @Inject(method = "getSignal", at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$weakSignal(
            BlockGetter level, BlockPos pos, Direction query, CallbackInfoReturnable<Integer> callback) {
        var host = emittingHost(level, pos, query);
        if (host != null) callback.setReturnValue(host.panels().signal(query.getOpposite()));
    }

    @Inject(method = "getDirectSignal", at = @At("RETURN"), cancellable = true, require = 1)
    private void gregtech$strongSignal(
            BlockGetter level, BlockPos pos, Direction query, CallbackInfoReturnable<Integer> callback) {
        var host = emittingHost(level, pos, query);
        if (host != null) callback.setReturnValue(host.panels().strongSignal(query.getOpposite()));
    }
}
