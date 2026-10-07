package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.GTMachineBlock;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import com.gregtech.gregtech.registry.GTSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Smelting crucible shell (GT6 {@code MultiTileEntitySmeltery}). */
public class SmeltingCrucibleBlock extends Block implements EntityBlock, GTMachineBlock {
    /** Hollow interior for rendering — inner 2/16–14/16, open top. */
    private static final VoxelShape INTERIOR = Shapes.box(
            2 / 16.0D, 2 / 16.0D, 2 / 16.0D,
            14 / 16.0D, 1.0D, 14 / 16.0D);
    private static final VoxelShape SHELL = Shapes.join(Shapes.block(), INTERIOR, BooleanOp.ONLY_FIRST);

    /** GT6 {@code addCollisionBoxesToList2} — walls inset 1/16 (0.0625), open top. */
    private static final double P = 1 / 16.0D;
    private static final VoxelShape COLLISION = Shapes.or(
            Shapes.box(14 * P, 1 * P, 1 * P, 15 * P, 15 * P, 15 * P),
            Shapes.box(1 * P, 1 * P, 14 * P, 15 * P, 15 * P, 15 * P),
            Shapes.box(1 * P, 1 * P, 1 * P, 2 * P, 15 * P, 15 * P),
            Shapes.box(1 * P, 1 * P, 1 * P, 15 * P, 15 * P, 2 * P),
            Shapes.box(1 * P, 1 * P, 1 * P, 15 * P, 2 * P, 15 * P));

    private final CrucibleSpec spec;

    public SmeltingCrucibleBlock(CrucibleSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public CrucibleSpec spec() {
        return spec;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHELL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SmeltingCrucibleBlockEntity crucible) {
            GTEntityHelper.applyTemperatureDamage(entity, crucible.getTemperature(), 1.0F, 10.0F);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SmeltingCrucibleBlockEntity crucible) {
                GTEntityHelper.applyTemperatureDamage(entity, crucible.getTemperature(), 1.0F, 10.0F);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHELL;
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmeltingCrucibleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == GTBlockEntities.SMELTING_CRUCIBLE.get()
                ? (lvl, pos, st, be) -> SmeltingCrucibleBlockEntity.serverTick(lvl, pos, st, (SmeltingCrucibleBlockEntity) be)
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SmeltingCrucibleBlockEntity crucible
                && hit.getDirection() == Direction.UP
                && crucible.canHandleUse(player, hand)) {
            // Client prediction only — authoritative logic runs in CrucibleInteractionHandler on server.
            return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SmeltingCrucibleBlockEntity crucible) {
                crucible.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
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

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide && GTToolHelper.isMachineWrench(tool)) {
            level.playSound(null, pos, GTSounds.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (GTToolHelper.canCollectMachineDrop(tool, state)) {
                ItemStack machine = getCloneItemStack(level, pos, state);
                com.gregtech.gregtech.content.cover.CoverDrops.capture(state,(net.minecraft.server.level.ServerLevel)level,blockEntity,java.util.List.of(machine));
                if (!machine.isEmpty()) {
                    if (!player.getInventory().add(machine)) {
                        popResource(level, pos, machine);
                    } else if (player instanceof ServerPlayer) {
                        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                                0.2F, (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F);
                    }
                }
                return;
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    public static Properties defaultProperties(CrucibleSpec spec) {
        return Properties.of()
                .strength(spec.hardness(), spec.blastResistance())
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }
}
