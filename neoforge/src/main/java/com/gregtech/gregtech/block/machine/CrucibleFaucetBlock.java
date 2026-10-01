package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.GTMachineBlock;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.blockentity.machine.CrucibleFaucetBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;

/** Crucible faucet (GT6 {@code MultiTileEntityFaucet}) — directional pour spout from crucible to mold. */
public class CrucibleFaucetBlock extends HorizontalDirectionalBlock implements EntityBlock, GTMachineBlock, SimpleWaterloggedBlock, ToolInteractionTarget {
    @Override public com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    private static final VoxelShape NORTH = Shapes.or(
            Shapes.box(6 / 16.0D, 1 / 16.0D, 0.0D, 10 / 16.0D, 2 / 16.0D, 4 / 16.0D),
            Shapes.box(5 / 16.0D, 2 / 16.0D, 0.0D, 6 / 16.0D, 6 / 16.0D, 4 / 16.0D),
            Shapes.box(10 / 16.0D, 2 / 16.0D, 0.0D, 11 / 16.0D, 6 / 16.0D, 4 / 16.0D));
    private static final VoxelShape SOUTH = Shapes.or(
            Shapes.box(6 / 16.0D, 1 / 16.0D, 12 / 16.0D, 10 / 16.0D, 2 / 16.0D, 1.0D),
            Shapes.box(5 / 16.0D, 2 / 16.0D, 12 / 16.0D, 6 / 16.0D, 6 / 16.0D, 1.0D),
            Shapes.box(10 / 16.0D, 2 / 16.0D, 12 / 16.0D, 11 / 16.0D, 6 / 16.0D, 1.0D));
    private static final VoxelShape WEST = Shapes.or(
            Shapes.box(0.0D, 1 / 16.0D, 6 / 16.0D, 4 / 16.0D, 2 / 16.0D, 10 / 16.0D),
            Shapes.box(0.0D, 2 / 16.0D, 5 / 16.0D, 4 / 16.0D, 6 / 16.0D, 6 / 16.0D),
            Shapes.box(0.0D, 2 / 16.0D, 10 / 16.0D, 4 / 16.0D, 6 / 16.0D, 11 / 16.0D));
    private static final VoxelShape EAST = Shapes.or(
            Shapes.box(12 / 16.0D, 1 / 16.0D, 6 / 16.0D, 1.0D, 2 / 16.0D, 10 / 16.0D),
            Shapes.box(12 / 16.0D, 2 / 16.0D, 5 / 16.0D, 1.0D, 6 / 16.0D, 6 / 16.0D),
            Shapes.box(12 / 16.0D, 2 / 16.0D, 10 / 16.0D, 1.0D, 6 / 16.0D, 11 / 16.0D));

    private final CrucibleSpec spec;

    public CrucibleFaucetBlock(CrucibleSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public CrucibleSpec spec() { return spec; }

    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.HORIZONTAL) : null;
    }
    @Override public void toolStateChanged(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CrucibleFaucetBlockEntity faucet) faucet.setFacing(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        BlockState base;
        if (clicked.getAxis().isHorizontal()) {
            base = defaultBlockState().setValue(FACING, clicked.getOpposite());
        } else {
            base = defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
        return GTWaterloggable.getStateForPlacement(base, context);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shapeFor(state.getValue(FACING)); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shapeFor(state.getValue(FACING)); }
    @Override public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) { return false; }
    @Override public boolean useShapeForLightOcclusion(BlockState state) { return true; }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return state;
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        if (player.getMainHandItem().getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric)
            return com.gregtech.gregtech.content.tool.ElectricWrenchHarvest.canHarvest(electric, player.getMainHandItem(), state);
        ItemStack tool = player.getMainHandItem();
        if (GTToolHelper.isMachineWrench(tool)) return true;
        if (tool.getItem() instanceof PickaxeItem) return true;
        if (GTToolHelper.matchesTool(tool, GTToolType.PICKAXE)) return true;
        return super.canHarvestBlock(state, level, pos, player);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (player.getMainHandItem().getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem)
            return super.getDestroyProgress(state, player, level, pos);
        ItemStack tool = player.getMainHandItem();
        if (GTToolHelper.isMachineWrench(tool)) {
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) return 0;
            return 1.0F / hardness / 10.0F;
        }
        if (tool.getItem() instanceof PickaxeItem || GTToolHelper.matchesTool(tool, GTToolType.PICKAXE)) {
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) return 0;
            float speed = GTToolHelper.matchesTool(tool, GTToolType.PICKAXE) && GTToolHelper.isUsable(tool)
                    ? GTToolHelper.getMiningSpeed(tool, state)
                    : tool.getItem() instanceof PickaxeItem ? tool.getDestroySpeed(state) : 1.0F;
            return speed / hardness / 30.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleFaucetBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == GTBlockEntities.CRUCIBLE_FAUCET.get()
                ? (lvl, pos, st, be) -> CrucibleFaucetBlockEntity.serverTick(lvl, pos, st, (CrucibleFaucetBlockEntity) be)
                : null;
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrucibleFaucetBlockEntity faucet)) {
            return InteractionResult.PASS;
        }
        if (ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        return InteractionResult.PASS;
    }

    public static Properties defaultProperties(CrucibleSpec spec) {
        return Properties.of()
                .strength(spec.hardness(), spec.blastResistance())
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion();
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return interact(state,level,pos,player,hand,hit)==InteractionResult.PASS?net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION:net.minecraft.world.ItemInteractionResult.SUCCESS;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return java.util.List.of(new ItemStack(this));}
}
