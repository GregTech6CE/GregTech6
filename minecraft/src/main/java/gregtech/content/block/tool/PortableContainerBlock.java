package gregtech.content.block.tool;

import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import gregtech.engine.GTWaterloggable;
import gregtech.engine.fluid.PortableFluidContainerSpec;
import gregtech.content.blockentity.tool.PortableContainerBlockEntity;
import gregtech.engine.tool.ToolInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

/** Placeable vessel; the same item fluid data survives placement and removal. */
public final class PortableContainerBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    private final PortableFluidContainerSpec spec;
    private final VoxelShape shape;
//? if >=1.21.1 {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return ToolInteractions.toItemResult(use(state, level, pos, player, hand, hit));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
    @Override
//?}
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof PortableContainerBlockEntity vessel) {
            if(player.getItemInHand(hand).isEmpty()&&hit.getDirection().getAxis().isHorizontal()
                    &&gregtech.content.tool.PortableContainerLimits.adjustable(spec)){
                if(!level.isClientSide){
                    int limit=vessel.adjustLimit(hit.getLocation().y-pos.getY(),player.isShiftKeyDown());
                    player.displayClientMessage(gregtech.engine.fluid.FluidUnits.text("message.gt6ce.portable_fluid.limit",limit),true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (vessel.interact(player, hand, hit.getDirection())) return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
    public PortableContainerBlock(PortableFluidContainerSpec spec, Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(defaultBlockState().setValue(GTWaterloggable.WATERLOGGED, false));
        this.spec = spec;
        VoxelShape result = Shapes.empty();
        for (int[] b : gregtech.content.tool.PortableContainerShapes.bounds(spec.shapeId()))
            result = Shapes.or(result, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        shape = spec.shapeId().equals("cell")?Block.box(5,0,5,11,12,11):result.optimize();
    }
    public PortableFluidContainerSpec spec() { return spec; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PortableContainerBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof PortableContainerBlockEntity vessel) vessel.setContents(stack);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(be instanceof PortableContainerBlockEntity vessel ? vessel.contents() : new ItemStack(this));
    }
//? if >=1.21.1 {
    /*@Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult target,LevelReader level,BlockPos pos,Player player) {
*///?} else {
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
//?}
        return level.getBlockEntity(pos) instanceof PortableContainerBlockEntity vessel ? vessel.contents() : new ItemStack(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return GTWaterloggable.getStateForPlacement(super.getStateForPlacement(context), context);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }
}
