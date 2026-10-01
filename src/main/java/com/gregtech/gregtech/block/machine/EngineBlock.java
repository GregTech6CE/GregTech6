package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.ElectricEngineSpec;
import com.gregtech.gregtech.api.machine.EngineType;
import com.gregtech.gregtech.api.machine.FluxEngineSpec;
import com.gregtech.gregtech.api.machine.GTMachineBlock;
import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.machine.RotationEngineSpec;
import com.gregtech.gregtech.api.machine.SteamEngineData;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.blockentity.machine.EngineBaseBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticDieselEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticElectricEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticFluxEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticRotationEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** KU generator engine block. 6-directional facing (DirectionalBlock), no GUI. */
public class EngineBlock extends DirectionalBlock implements EntityBlock, GTMachineBlock, SimpleWaterloggedBlock, ToolInteractionTarget {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    static {
        // Down-facing: front plate at y=0-2, back at y=14-16, middle bar along Y
        SHAPES.put(Direction.DOWN, Shapes.or(
                Shapes.box(0, 0, 0, 1, 0.125, 1),
                Shapes.box(0, 0.875, 0, 1, 1, 1),
                Shapes.box(0.0625, 0.125, 0.0625, 0.9375, 0.875, 0.9375)));
        // Up-facing: front plate at y=14-16, back at y=0-2
        SHAPES.put(Direction.UP, Shapes.or(
                Shapes.box(0, 0.875, 0, 1, 1, 1),
                Shapes.box(0, 0, 0, 1, 0.125, 1),
                Shapes.box(0.0625, 0.125, 0.0625, 0.9375, 0.875, 0.9375)));
        // North-facing: front at z=14-16, back at z=0-2, middle along Z
        SHAPES.put(Direction.NORTH, Shapes.or(
                Shapes.box(0, 0, 0.875, 1, 1, 1),
                Shapes.box(0, 0, 0, 1, 1, 0.125),
                Shapes.box(0.0625, 0.0625, 0.125, 0.9375, 0.9375, 0.875)));
        // South-facing: front at z=0-2, back at z=14-16
        SHAPES.put(Direction.SOUTH, Shapes.or(
                Shapes.box(0, 0, 0, 1, 1, 0.125),
                Shapes.box(0, 0, 0.875, 1, 1, 1),
                Shapes.box(0.0625, 0.0625, 0.125, 0.9375, 0.9375, 0.875)));
        // West-facing: front at x=0-2, back at x=14-16, middle along X
        SHAPES.put(Direction.WEST, Shapes.or(
                Shapes.box(0, 0, 0, 0.125, 1, 1),
                Shapes.box(0.875, 0, 0, 1, 1, 1),
                Shapes.box(0.125, 0.0625, 0.0625, 0.875, 0.9375, 0.9375)));
        // East-facing: front at x=14-16, back at x=0-2
        SHAPES.put(Direction.EAST, Shapes.or(
                Shapes.box(0.875, 0, 0, 1, 1, 1),
                Shapes.box(0, 0, 0, 0.125, 1, 1),
                Shapes.box(0.125, 0.0625, 0.0625, 0.875, 0.9375, 0.9375)));
    }

    private final EngineType engineType;
    private final MachineSpec machineSpec;
    private final Object engineSpec; // ElectricEngineSpec / FluxEngineSpec / SteamEngineSpec / RotationEngineSpec
    private Supplier<BlockEntityType<?>> beTypeSupplier;

    public EngineBlock(EngineType engineType, MachineSpec machineSpec, Object engineSpec, Properties properties) {
        super(properties);
        this.engineType = engineType;
        this.machineSpec = machineSpec;
        this.engineSpec = engineSpec;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN).setValue(LIT, false)
                .setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public EngineType engineType() { return engineType; }
    public MachineSpec machineSpec() { return machineSpec; }
    @SuppressWarnings("unchecked")
    public <T> T engineSpec(Class<T> type) { return (T) engineSpec; }

    public void setBeTypeSupplier(Supplier<BlockEntityType<?>> s) { this.beTypeSupplier = s; }
    public BlockEntityType<?> getBeType() { return beTypeSupplier != null ? beTypeSupplier.get() : null; }

    public int tintRgb() {
        return switch (engineType) {
            case ELECTRIC -> engineSpec(ElectricEngineSpec.class).tintRgb();
            case FLUX -> engineSpec(FluxEngineSpec.class).tintRgb();
            case STEAM -> {
                Object spec = engineSpec;
                if (spec instanceof SteamEngineData d) yield d.tintRgb();
                yield 0xFFFFFF;
            }
            case ROTATION -> engineSpec(RotationEngineSpec.class).tintRgb();
            case DIESEL -> engineSpec(com.gregtech.gregtech.api.machine.DieselEngineSpec.class).tintRgb();
        };
    }

    private boolean isWoodenRotation() {
        return engineType == EngineType.ROTATION
                && engineSpec instanceof RotationEngineSpec rotation
                && rotation.material() == WoodMaterials.WoodTreated;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return isWoodenRotation() ? 150 : super.getFlammability(state, level, pos, face);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return isWoodenRotation() ? 150 : super.getFireSpreadSpeed(state, level, pos, face);
    }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.ALL) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        return GTWaterloggable.getStateForPlacement(defaultBlockState().setValue(FACING, facing), context);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (engineType == EngineType.ROTATION) return Shapes.block();
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPES.get(Direction.DOWN));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (engineType == EngineType.ROTATION) return Shapes.block();
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPES.get(Direction.DOWN));
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (engineType == EngineType.STEAM) return 0;
        return state.getValue(LIT) ? 10 : 0;
    }

    // ── Waterlogging ──────────────────────────────────────────────────────

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return state;
    }

    // ── BE ────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (beTypeSupplier == null) return null;
        return beTypeSupplier.get().create(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (BlockEntityTicker<T>) switch (engineType) {
            case ELECTRIC -> (BlockEntityTicker<KineticElectricEngineBlockEntity>)
                    KineticElectricEngineBlockEntity::serverTick;
            case FLUX -> (BlockEntityTicker<KineticFluxEngineBlockEntity>)
                    KineticFluxEngineBlockEntity::serverTick;
            case STEAM -> (BlockEntityTicker<KineticSteamEngineBlockEntity>)
                    KineticSteamEngineBlockEntity::serverTick;
            case ROTATION -> (BlockEntityTicker<KineticRotationEngineBlockEntity>)
                    KineticRotationEngineBlockEntity::serverTick;
            case DIESEL -> (BlockEntityTicker<KineticDieselEngineBlockEntity>)
                    KineticDieselEngineBlockEntity::serverTick;
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EngineBaseBlockEntity engine) {
                engine.invalidateCaps();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ── Interaction ───────────────────────────────────────────────────────

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        // Wrench: 6-directional rotation
        if (MachineRotationType.handleWrench(state, level, pos, player, hand, hit,
                FACING, MachineRotationType.ALL)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EngineBaseBlockEntity engine) {
            // GT6's bipolar rotation engine has a fixed four-side RU input and
            // two-end KU output. Its only facing tool is the wrench; cycling a
            // generic face mask here would consume screwdriver wear but could
            // never change its actual shaft geometry.
            if (engineType != EngineType.ROTATION && GTToolHelper.isScrewdriver(held)) {
                if (!level.isClientSide) {
                    if (engine instanceof com.gregtech.gregtech.blockentity.machine.PistonEngineBlockEntity piston) {
                        piston.cyclePower();
                        player.displayClientMessage(piston.statusDescription(), true);
                    } else engine.setFaceConfig(cycleFaceConfig(engine.getFaceConfig()));
                    GTToolHelper.damageForUse(held, 1, player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // The rotation engine has no soft-hammer face reset in GT6 either.
            if (engineType != EngineType.ROTATION && GTToolHelper.isSoftHammer(held)) {
                if (!level.isClientSide) {
                    if (engine instanceof KineticSteamEngineBlockEntity steam) {
                        if (steam.isShutdown()) {
                            steam.resetShutdown();
                        } else {
                            steam.setEnabled(!steam.isEnabled());
                        }
                    } else if (engine instanceof com.gregtech.gregtech.blockentity.machine.PistonEngineBlockEntity piston) {
                        piston.toggleStopped();
                    } else {
                        engine.setFaceConfig(engine.defaultFaceConfig());
                    }
                    GTToolHelper.damageForUse(held, 1, player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Magnifying glass: show status for steam engines
            if (GTToolHelper.isMagnifyingGlass(held)) {
                if (!level.isClientSide) {
                    if (engine instanceof KineticSteamEngineBlockEntity steam) {
                        player.sendSystemMessage(
                                Component.literal(steam.statusDescription()));
                    } else if (engine instanceof com.gregtech.gregtech.blockentity.machine.PistonEngineBlockEntity piston) {
                        player.sendSystemMessage(piston.statusDescription());
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    private static FaceConfig cycleFaceConfig(FaceConfig current) {
        // Cycle energy output among front, back, left, right
        int[] candidates = {FaceConfig.FRONT, FaceConfig.BACK, FaceConfig.LEFT, FaceConfig.RIGHT};
        int currentOut = current.energyOutputs();
        for (int i = 0; i < candidates.length; i++) {
            if (currentOut == (1 << candidates[i])) {
                int next = candidates[(i + 1) % candidates.length];
                return FaceConfig.builder()
                        .energyIn(current.energyInputs() & ~(1 << next))
                        .energyOut(next)
                        .fluidIn(current.fluidInputs())
                        .build();
            }
        }
        // Default: set to FRONT output, others input
        return FaceConfig.builder()
                .energyIn(FaceConfig.BOTTOM, FaceConfig.TOP, FaceConfig.LEFT, FaceConfig.RIGHT, FaceConfig.BACK)
                .energyOut(FaceConfig.FRONT)
                .fluidIn(current.fluidInputs())
                .build();
    }

    // ── Wrench harvest ────────────────────────────────────────────────────

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide && GTToolHelper.canCollectMachineDrop(tool, state)) {
            ItemStack machine = createMachineDrop(state);
            if (!machine.isEmpty()) {
                if (!player.getInventory().add(machine)) {
                    popResource(level, pos, machine);
                } else if (player instanceof ServerPlayer) {
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                            0.2F, (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F);
                }
            }
            level.playSound(null, pos, com.gregtech.gregtech.registry.GTSounds.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        if (!level.isClientSide && GTToolHelper.isMachineWrench(tool)) {
            level.playSound(null, pos, com.gregtech.gregtech.registry.GTSounds.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
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
}
