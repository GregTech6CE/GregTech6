package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.machine.GTMachineBlock;
import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;

/**
 * Horizontal machine shell (GT6 {@code TileEntityBase09FacingSingle} placement).
 */
public abstract class GTFacingMachineBlock extends Block implements GTMachineBlock, ToolInteractionTarget {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.block();

    private final MachineSpec spec;

    protected GTFacingMachineBlock(MachineSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    public MachineSpec spec() {
        return spec;
    }

    public Direction getFrontFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.HORIZONTAL) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Original rotation contract, also used by Native subclasses' existing interactions. */
    protected InteractionResult handleWrench(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        return MachineRotationType.handleWrench(state, level, pos, player, hand, hit,
                FACING, MachineRotationType.HORIZONTAL)
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state,
            Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleWrench(state, level, pos, player, hand, hit) == InteractionResult.PASS
                ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LIT) ? 13 : 0;
    }

    /** GT6 wrench auto-collect: machine item goes to inventory; spills only when full. */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide && GTToolHelper.canCollectMachineDrop(tool, state)) {
            ItemStack machine = createMachineDrop(state, blockEntity);
                com.gregtech.gregtech.content.cover.CoverDrops.capture(state,(net.minecraft.server.level.ServerLevel)level,blockEntity,java.util.List.of(machine));
            if (!machine.isEmpty()) {
                if (!player.getInventory().add(machine)) {
                    popResource(level, pos, machine);
                } else if (player instanceof ServerPlayer) {
                    level.playSound(
                            null,
                            pos,
                            SoundEvents.ITEM_PICKUP,
                            SoundSource.PLAYERS,
                            0.2F,
                            (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F
                    );
                }
            }
            level.playSound(null, pos, com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        if (!level.isClientSide && GTToolHelper.isMachineWrench(tool)) {
            level.playSound(null, pos, com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
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
    protected ItemStack createMachineDrop(BlockState state, @Nullable BlockEntity entity) { return createMachineDrop(state); }
}
