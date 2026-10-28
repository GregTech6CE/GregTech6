package gregtech.platform.neoforge.energy;

import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.blockentity.energy.ElectricWireBlockEntity;
import gregtech.platform.neoforge.NeoToolBindings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Original cutter connection path; general tool actions and powered tools remain separate. */
public final class WireToolInteractions {

    private WireToolInteractions() {}

    public static boolean use(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var heldTool = player.getItemInHand(hand);
        if (!NeoToolBindings.isWireCutter(heldTool)) return false;
        var connectionSide = gregtech.platform.neoforge.transport.FluidPipeToolInteractions.selectedFace(hit);
        var neighborPos = pos.relative(connectionSide);
        if (!player.mayBuild()
                || !level.mayInteract(player, pos)
                || !level.hasChunkAt(neighborPos)
                || !level.mayInteract(player, neighborPos)) return true;
        boolean connect = !state.getValue(ElectricWireBlock.propFor(connectionSide));
        if (connect && !gregtech.content.cover.CoverConnections.canConnect(level, pos, connectionSide)) return true;
        if (level.isClientSide) return true;
        if (!level.setBlockAndUpdate(pos, state.setValue(ElectricWireBlock.propFor(connectionSide), connect)))
            return true;
        var neighborState = level.getBlockState(neighborPos);
        if (state.getBlock() instanceof ElectricWireBlock && neighborState.getBlock() instanceof ElectricWireBlock
                || state.getBlock() instanceof gregtech.content.block.energy.SignalWireBlock
                        && neighborState.getBlock() instanceof gregtech.content.block.energy.SignalWireBlock) {
            level.setBlockAndUpdate(
                    neighborPos,
                    neighborState.setValue(ElectricWireBlock.propFor(connectionSide.getOpposite()), connect));
            changed(level, neighborPos);
        }
        changed(level, pos);
        level.playSound(
                null,
                pos,
                gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                1F,
                1F);
        NeoToolBindings.damageForUse(heldTool, 1, player);
        return true;
    }

    private static void changed(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ElectricWireBlockEntity electricWireEntity)
            electricWireEntity.onConnectionChanged();
    }
}
