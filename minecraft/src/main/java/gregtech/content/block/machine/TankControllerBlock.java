package gregtech.content.block.machine;

import gregtech.content.block.GTDirectionalBlock;
import gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity;
import gregtech.content.multiblock.TankValveParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Multiblock tank controller (3x3x3 or 5x5x5). Size is determined by the block variant. */
public class TankControllerBlock extends GTDirectionalBlock implements gregtech.engine.multiblock.StructurePreviewSource, gregtech.engine.tool.ToolInteractionTarget {
    private final int size;
    private final TankValveParameters valveSpec;

    public TankControllerBlock(TankValveParameters spec, Properties properties) {
        super(properties);
        this.size = spec.size();
        this.valveSpec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return gregtech.engine.tool.GTToolHelper.isMachineWrench(tool)
                ? gregtech.engine.tool.ToolInteractionSpec.facing(FACING, MachineRotationType.ALL) : null;
    }

    @Override
    public void toolStateChanged(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof MultiblockTankControllerBlockEntity tank)
            gregtech.engine.multiblock.tracking.StructureTracking.invalidate(tank);
    }
    @Override
    protected net.minecraft.world.InteractionResult useBlock(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (gregtech.engine.tool.ToolInteractions.use(state, level, pos, player, hand, hit))
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        if (level.getBlockEntity(pos) instanceof MultiblockTankControllerBlockEntity tank && tank.isStructureOk()) {
            if (level.isClientSide)
                return gregtech.engine.fluid.FluidContainerInteraction.clientResult(player, hand);
            if (gregtech.engine.fluid.FluidContainerInteraction.use(player, hand, tank.fluidHandler()))
                return net.minecraft.world.InteractionResult.CONSUME;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    public int size() {
        return size;
    }
    public TankValveParameters valveSpec() {
        return valveSpec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext placement) {
        return defaultBlockState().setValue(FACING, placement.getNearestLookingDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MultiblockTankControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof MultiblockTankControllerBlockEntity controller)
                MultiblockTankControllerBlockEntity.serverTick(tickLevel, tickPos, tickState, controller);
        };
    }

    @Override
    protected void appendTooltip(@NotNull ItemStack stack, @Nullable BlockGetter level,
            @Nullable HolderLookup.Provider registries, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        gregtech.client.MultiblockTankTooltips.append(size, valveSpec, stack, registries, tooltip);
    }

    @Override
    public java.util.List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        ItemStack result = new ItemStack(this);
        if (context.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)
                instanceof MultiblockTankControllerBlockEntity tank) {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(tank,
                    gregtech.engine.stack.NbtCompat.saveForBlockItem(tank, tank.getLevel().registryAccess()));
            data.remove("gt.tank");
            data.remove("gt.size");
            gregtech.engine.stack.BlockEntityData.write(result, data);
        }
        return java.util.List.of(result);
    }

//? if >=1.21.1 {
    /*@Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, net.minecraft.world.level.LevelReader level,
            BlockPos pos, net.minecraft.world.entity.player.Player player) {
*///?} else {
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
//?}
        ItemStack result = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof MultiblockTankControllerBlockEntity tank) {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(tank,
                    gregtech.engine.stack.NbtCompat.saveForBlockItem(tank, tank.getLevel().registryAccess()));
            data.remove("gt.tank");
            data.remove("gt.size");
            gregtech.engine.stack.BlockEntityData.write(result, data);
        }
        return result;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        net.minecraft.nbt.CompoundTag saved = gregtech.engine.stack.BlockEntityData.read(stack);
        if (level.getBlockEntity(pos) instanceof MultiblockTankControllerBlockEntity tank && saved != null)
            gregtech.engine.stack.NbtCompat.loadBlockEntity(tank, saved, level.registryAccess());
    }
}
