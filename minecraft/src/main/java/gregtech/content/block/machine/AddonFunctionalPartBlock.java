package gregtech.content.block.machine;

import gregtech.api.addon.machine.LayoutPart;
import gregtech.content.blockentity.machine.AddonFunctionalPartBlockEntity;
import gregtech.content.blockentity.machine.FunctionalPartRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Addon part with slots, a tick, a click handler or named fields, and no energy role.
 * A part that also emits or accepts energy uses {@link AddonEnergyPartBlock}.
 */
public class AddonFunctionalPartBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private final String addonId;
    private final LayoutPart declaration;

    public AddonFunctionalPartBlock(Properties properties, String addonId, LayoutPart declaration) {
        super(properties);
        this.addonId = addonId;
        this.declaration = declaration;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public String addonId() { return addonId; }

    public LayoutPart declaration() { return declaration; }

//? if >=1.21.1 {
    /*@Override public com.mojang.serialization.MapCodec<AddonFunctionalPartBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
*///?} else {
//?}

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AddonFunctionalPartBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !declaration.ticks()
                || type != gregtech.content.registry.GTBlockEntities.ADDON_FUNCTIONAL_PART.get()) return null;
        return (lvl, pos, blockState, be) -> AddonFunctionalPartBlockEntity.serverTick(lvl, pos, blockState,
                (AddonFunctionalPartBlockEntity) be);
    }

//? if >=1.21.1 {
/*    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state,
            Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return switch (FunctionalPartInteraction.interact(level, pos, player, hand, hit)) {
            case SUCCESS -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME -> net.minecraft.world.ItemInteractionResult.CONSUME;
            default -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        return FunctionalPartInteraction.interact(level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        return FunctionalPartInteraction.interact(level, pos, player, hand, hit);
    }
//?}

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof FunctionalPartRuntime.Host host && host.function() != null)
            host.function().dropInto(level, pos);
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity part)
            part.logisticsCovers().dropAll();
        super.onRemove(state, level, pos, next, moving);
    }

    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        return java.util.List.of(new net.minecraft.world.item.ItemStack(asItem()));
    }
}
