package gregtech.content.block.energy;

import gregtech.content.block.GTBlock;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import gregtech.content.nuclear.ReactorPorts;
import java.util.List;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 single-rod reactor core with shared rod/container interaction. */
public class ReactorCoreBlock extends GTBlock implements EntityBlock, gregtech.engine.tool.ToolInteractionTarget {

    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ReactorCoreBlockEntity core)
            return gregtech.content.nuclear.ReactorInteraction.use(core, player, hand, hit);
        return InteractionResult.PASS;
    }

    public ReactorCoreBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition
                .any()
                .setValue(ReactorPorts.HOT, net.minecraft.core.Direction.DOWN)
                .setValue(ReactorPorts.COLD, net.minecraft.core.Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ReactorPorts.HOT, ReactorPorts.COLD);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorCoreBlockEntity(pos, state);
    }

    /** GT6 {@code MultiTileEntityReactorCore}: touching a running core burns and irradiates. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        gregtech.content.nuclear.ReactorHazards.contact(level, pos, entity);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != gregtech.content.registry.GTBlockEntities.REACTOR_CORE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<ReactorCoreBlockEntity>) ReactorCoreBlockEntity::serverTick;
    }

    @Override
    protected void appendTooltip(
            ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.reactor.updated"));
    }

    @Override
    public void onRemove(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState next,
            boolean moving) {
        if (!state.is(next.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof gregtech.engine.inventory.BlockContents contents) {
            contents.dropContents();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public java.util.List<ItemStack> getDrops(
            BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack = new ItemStack(this);
        // GT6 keeps coolant, heat and rods out of the item; rods drop separately via BlockContents.
        ReactorPorts.saveItemState(stack, state);
        return java.util.List.of(stack);
    }

    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return ReactorPorts.tool(tool);
    }
}
