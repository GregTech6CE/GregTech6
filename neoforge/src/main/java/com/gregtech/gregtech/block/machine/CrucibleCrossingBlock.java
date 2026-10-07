package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.GTMachineBlock;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.blockentity.machine.CrucibleCrossingBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Crucible crossing (GT6 {@code MultiTileEntityCrossing}) — 4-way molten material routing channel. */
public class CrucibleCrossingBlock extends Block implements EntityBlock, GTMachineBlock, SimpleWaterloggedBlock {
    @Override public com.mojang.serialization.MapCodec<? extends Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    private static final VoxelShape OUTLINE = Shapes.box(0.0D, 1 / 16.0D, 0.0D, 1.0D, 6 / 16.0D, 1.0D);
    private static final VoxelShape COLLISION = Shapes.box(0.0D, 1 / 16.0D, 0.0D, 1.0D, 7 / 16.0D, 1.0D);

    private final CrucibleSpec spec;

    public CrucibleCrossingBlock(CrucibleSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public CrucibleSpec spec() { return spec; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return GTWaterloggable.getStateForPlacement(defaultBlockState(), ctx);
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

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return OUTLINE; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return COLLISION; }
    @Override public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return OUTLINE; }
    @Override public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) { return false; }
    @Override public boolean useShapeForLightOcclusion(BlockState state) { return true; }
    @Override public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) { return false; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleCrossingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == GTBlockEntities.CRUCIBLE_CROSSING.get()
                ? (lvl, pos, st, be) -> CrucibleCrossingBlockEntity.serverTick(lvl, pos, st, (CrucibleCrossingBlockEntity) be)
                : null;
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
            level.playSound(null, pos, com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (GTToolHelper.canCollectMachineDrop(tool, state)) {
                ItemStack machine = getCloneItemStack(state,null,level,pos,player);
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
                .requiresCorrectToolForDrops()
                .noOcclusion();
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return java.util.List.of(new ItemStack(this));}
}
