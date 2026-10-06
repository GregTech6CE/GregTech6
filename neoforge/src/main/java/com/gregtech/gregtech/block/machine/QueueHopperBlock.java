package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.machine.HopperSpec;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.blockentity.machine.QueueHopperBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/** GT6 queuehopper block ({@code MultiTileEntityQueueHopper}). FIFO queue inventory with 6-way facing output. */
public class QueueHopperBlock extends DirectionalBlock implements EntityBlock, ToolInteractionTarget, com.gregtech.gregtech.api.tool.ScrewdriverUseTarget {
    // Bowl [0,10,0]-[16,16,16] + Funnel [4,4,4]-[12,10,12] + per-facing nozzle (4px high, none for UP)
    private static final VoxelShape BOWL   = Shapes.box(0, 10 / 16.0D, 0, 1.0D, 1.0D, 1.0D);
    private static final VoxelShape FUNNEL = Shapes.box(4 / 16.0D, 4 / 16.0D, 4 / 16.0D, 12 / 16.0D, 10 / 16.0D, 12 / 16.0D);
    private static final VoxelShape NOZZLE_NORTH = Shapes.box(6 / 16.0D, 4 / 16.0D, 0, 10 / 16.0D, 8 / 16.0D, 4 / 16.0D);
    private static final VoxelShape NOZZLE_SOUTH = Shapes.box(6 / 16.0D, 4 / 16.0D, 12 / 16.0D, 10 / 16.0D, 8 / 16.0D, 1.0D);
    private static final VoxelShape NOZZLE_WEST  = Shapes.box(0, 4 / 16.0D, 6 / 16.0D, 4 / 16.0D, 8 / 16.0D, 10 / 16.0D);
    private static final VoxelShape NOZZLE_EAST  = Shapes.box(12 / 16.0D, 4 / 16.0D, 6 / 16.0D, 1.0D, 8 / 16.0D, 10 / 16.0D);
    private static final VoxelShape NOZZLE_DOWN  = Shapes.box(6 / 16.0D, 0, 6 / 16.0D, 10 / 16.0D, 4 / 16.0D, 10 / 16.0D);
    private static final VoxelShape SHAPE_UP     = Shapes.or(BOWL, FUNNEL);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    static {
        SHAPES.put(Direction.NORTH, Shapes.or(BOWL, FUNNEL, NOZZLE_NORTH));
        SHAPES.put(Direction.SOUTH, Shapes.or(BOWL, FUNNEL, NOZZLE_SOUTH));
        SHAPES.put(Direction.WEST,  Shapes.or(BOWL, FUNNEL, NOZZLE_WEST));
        SHAPES.put(Direction.EAST,  Shapes.or(BOWL, FUNNEL, NOZZLE_EAST));
        SHAPES.put(Direction.DOWN,  Shapes.or(BOWL, FUNNEL, NOZZLE_DOWN));
        SHAPES.put(Direction.UP,    SHAPE_UP);
    }

    @Override public com.mojang.serialization.MapCodec<QueueHopperBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    private final HopperSpec spec;

    public QueueHopperBlock(HopperSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
    }

    public HopperSpec spec() { return spec; }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool) && !GTToolHelper.isMonkeyWrench(tool)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.ALL) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // ClientShapeHooks is only classloaded behind the isClientSide check (dist safety).
        if (level instanceof Level world && world.isClientSide()
                && com.gregtech.gregtech.client.ClientShapeHooks.holdingMachineWrench()) {
            return Shapes.block();
        }
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_UP);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_UP);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QueueHopperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        var expected = GTBlockEntities.QUEUE_HOPPER;
        if (expected != null && type == expected.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<QueueHopperBlockEntity>) QueueHopperBlockEntity::serverTick;
        }
        return null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof QueueHopperBlockEntity queue) {
                queue.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof QueueHopperBlockEntity qhopper) {
                qhopper.onNeighborChanged();
            }
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    private InteractionResult originalUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        // Monkey wrench: open GUI (must check before wrench rotation)
        if (GTToolHelper.isMonkeyWrench(held)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof QueueHopperBlockEntity qhopper) {
                if (!level.isClientSide && player instanceof ServerPlayer sp) {
                    sp.openMenu(qhopper);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // Wrench: click a face to rotate the queuehopper front to that face
        if (MachineRotationType.handleWrench(state, level, pos, player, hand, hit,
                FACING, MachineRotationType.ALL)) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof QueueHopperBlockEntity qhopper) {
                    qhopper.onNeighborChanged();
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof QueueHopperBlockEntity queue) {
            // Screwdriver: cycle mode
            if (GTToolHelper.isScrewdriver(held)) {
                if (!level.isClientSide) {
                    queue.cycleMode(player.isCrouching());
                    GTToolHelper.damageForUse(held, 1, player);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(queue.getModeDescription()), false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            // Soft hammer: reset mode
            if (GTToolHelper.isSoftHammer(held)) {
                if (!level.isClientSide) {
                    queue.resetMode();
                    GTToolHelper.damageForUse(held, 1, player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            // Magnifying glass: show mode info
            if (GTToolHelper.isMagnifyingGlass(held)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(queue.getModeDescription()), false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            // Empty hand / non-tool: open GUI
            if (held.isEmpty() || !GTToolHelper.isTool(held)) {
                if (!level.isClientSide && player instanceof ServerPlayer sp) {
                    sp.openMenu(queue);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    /** GT6: snow golem walking over collects a snowball into the last slot. */
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(originalUse(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return originalUse(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    @Override public InteractionResult useScrewdriver(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return originalUse(state,level,pos,player,hand,hit);}
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof SnowGolem) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof QueueHopperBlockEntity qhopper) {
                qhopper.collectSnowball();
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide && GTToolHelper.isMachineWrench(tool)) {
            ItemStack machine = new ItemStack(this);
                com.gregtech.gregtech.content.cover.CoverDrops.capture(state,(net.minecraft.server.level.ServerLevel)level,blockEntity,java.util.List.of(machine));
            if (!machine.isEmpty()) {
                if (!player.getInventory().add(machine)) {
                    popResource(level, pos, machine);
                } else if (player instanceof ServerPlayer) {
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                            0.2F, (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F);
                }
            }
            level.playSound(null, pos, com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
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
        if (tool.getItem() instanceof PickaxeItem) {
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) return 0;
            return tool.getDestroySpeed(state) / hardness / 30.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
