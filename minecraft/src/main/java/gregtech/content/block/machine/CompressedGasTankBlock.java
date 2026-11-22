package gregtech.content.block.machine;

import gregtech.api.addon.machine.CompressedGasTankDeclaration;
import gregtech.content.block.GTBlock;
import gregtech.content.blockentity.machine.CompressedGasTankBlockEntity;
import gregtech.engine.addon.AddonMachines;
import gregtech.engine.fluid.FluidContainerInteraction;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Addon compressed gas drum. GregTech owns the block; the addon supplies the declaration. */
public class CompressedGasTankBlock extends GTBlock implements EntityBlock {
    private final AddonMachines.AcceptedCompressedGas accepted;
    private Supplier<BlockEntityType<?>> beType = () -> null;

    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return FluidContainerInteraction.clientResult(player, hand);
        if (!(level.getBlockEntity(pos) instanceof CompressedGasTankBlockEntity tank)) return InteractionResult.PASS;
        return FluidContainerInteraction.use(player, hand, tank) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public CompressedGasTankBlock(AddonMachines.AcceptedCompressedGas accepted, Properties properties) {
        super(properties);
        this.accepted = accepted;
    }

    public AddonMachines.AcceptedCompressedGas accepted() {
        return accepted;
    }

    public CompressedGasTankDeclaration declaration() {
        return accepted.declaration();
    }

    public int meltingPoint() {
        return accepted.meltingPoint();
    }

    public void setBeType(Supplier<BlockEntityType<?>> supplier) {
        this.beType = supplier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressedGasTankBlockEntity(beType.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != beType.get()) return null;
        return (lvl, pos, blockState, be) ->
                CompressedGasTankBlockEntity.serverTick(lvl, pos, blockState, (CompressedGasTankBlockEntity) be);
    }

    @Override
    protected void appendTooltip(
            ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt6ce.tank.compressed.store"));
        tooltip.add(Component.translatable("gt6ce.tank.compressed.compressor"));
    }
}
