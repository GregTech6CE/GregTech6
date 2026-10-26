/* Gregorius Techneticies / GregTech-6 Team; LGPL-3.0-or-later. */
package gregtech.content.block.machine;

import gregtech.content.blockentity.machine.HeatExchangerBlockEntity;
import gregtech.content.energy.HeatExchangerCatalog;
import gregtech.engine.tool.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class HeatExchangerBlock extends DirectionalBlock implements EntityBlock, ToolInteractionTarget {
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    private static final VoxelShape COLLISION = Shapes.box(0, 0, 0, 1, 0.875, 1);
    private final HeatExchangerCatalog.Spec spec;
    public HeatExchangerBlock(HeatExchangerCatalog.Spec spec, Properties properties) {
        super(properties); this.spec = spec;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }
    public HeatExchangerCatalog.Spec spec() { return spec; }
//? if >=1.21.1 {
    /*@Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() { return com.mojang.serialization.MapCodec.unit(this); }
*///?} else {
//?}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getNearestLookingDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing == Direction.UP ? Direction.DOWN : facing);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool) ? ToolInteractionSpec.facing(FACING, MachineRotationType.BOTTOM_HORIZONTAL) : null;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return COLLISION; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HeatExchangerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != gregtech.content.registry.GTBlockEntities.HEAT_EXCHANGER.get() ? null
                : (world, pos, current, entity) -> ((HeatExchangerBlockEntity)entity).tick();
    }
//? if >=1.21.1 {
    /*@Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand, hit) == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit); }
    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
*///?} else {
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
//?}
        if (ToolInteractions.use(state, level, pos, player, hand, hit)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity entity) return entity.interact(player, hand);
        return InteractionResult.PASS;
    }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && state.getValue(ACTIVE)) gregtech.util.GTEntityHelper.applyHeatDamage(entity, Math.min(10F, spec.rate() / 10F));
    }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entityInside(state, level, pos, entity); super.stepOn(level, pos, state, entity);
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return java.util.List.of(new ItemStack(this));
    }
}
