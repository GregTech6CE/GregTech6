package gregtech.block.energy;

import gregtech.api.tool.*;
import gregtech.block.machine.MachineRotationType;
import gregtech.blockentity.energy.MagicFieldAbsorberBlockEntity;
import gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;

public final class MagicFieldAbsorberBlock extends Block implements EntityBlock, ToolInteractionTarget {
    // The trophy always sits on world UP, so UP cannot be an output face.
    public static final DirectionProperty FACING = DirectionProperty.create("facing", side -> side != Direction.UP);
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    public MagicFieldAbsorberBlock() {
        super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(4, 4).sound(SoundType.METAL));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN).setValue(ACTIVE, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState(); }
    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool) ? ToolInteractionSpec.facing(FACING, MachineRotationType.ALL) : null;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MagicFieldAbsorberBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == GTBlockEntities.MAGIC_FIELD_ABSORBER.get()
                ? (world, pos, block, entity) -> ((MagicFieldAbsorberBlockEntity) entity).serverTick() : null;
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos from, boolean moving) {
        if (level.getBlockEntity(pos) instanceof MagicFieldAbsorberBlockEntity absorber) absorber.recheck();
    }
    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!GTToolHelper.isSoftHammer(player.getItemInHand(hand))) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MagicFieldAbsorberBlockEntity absorber) {
            absorber.setEnabled(!absorber.enabled());
            GTToolHelper.damageForUse(player.getItemInHand(hand), 1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
//? if >=1.21.1 {
    /*@Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand).consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tip, TooltipFlag flag) {
*///?} else {
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return interact(state, level, pos, player, hand); }
    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tip, TooltipFlag flag) {
//?}
        tip.add(Component.translatable("gt.tooltip.magicenergyabsorber"));
        tip.add(Component.translatable("tooltip.gt6ce.magic_field_absorber.sources"));
        tip.add(Component.translatable("tooltip.gt6ce.magic_field_absorber.output"));
    }
}
