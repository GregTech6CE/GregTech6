package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.blockentity.machine.MultiblockTankControllerBlockEntity;
import com.gregtech.gregtech.content.multiblock.TankValveSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Multiblock tank controller (3x3x3 or 5x5x5). Size is determined by the block variant. */
public class TankControllerBlock extends DirectionalBlock implements EntityBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {
    private final int size;
    @Nullable private final TankValveSpec valveSpec;

    public TankControllerBlock(int size) {
        this(size, Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }

    public TankControllerBlock(int size, Properties properties) {
        this(size, properties, null);
    }

    public TankControllerBlock(TankValveSpec spec, Properties properties) {
        this(spec.size(), properties, spec);
    }

    private TankControllerBlock(int size, Properties properties, @Nullable TankValveSpec spec) {
        super(properties);
        this.size = size;
        this.valveSpec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return com.gregtech.gregtech.api.tool.GTToolHelper.isMachineWrench(tool)
                ? com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING,MachineRotationType.ALL) : null;
    }
    @Override public void toolStateChanged(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof MultiblockTankControllerBlockEntity tank) tank.isStructureOk();
    }
    private net.minecraft.world.InteractionResult interact(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (com.gregtech.gregtech.api.tool.ToolInteractions.use(state,level,pos,player,hand,hit))
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        if (level.getBlockEntity(pos) instanceof MultiblockTankControllerBlockEntity tank && tank.isStructureOk()) {
            if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
            if (net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, tank.fluidHandler()))
                return net.minecraft.world.InteractionResult.CONSUME;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    public int size() { return size; }
    @Nullable public TankValveSpec valveSpec() { return valveSpec; }

    @Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
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
        return (lvl, pos, st, be) -> {
            if (be instanceof MultiblockTankControllerBlockEntity t)
                MultiblockTankControllerBlockEntity.serverTick(lvl, pos, st, t);
        };
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        int dim = size; // e.g. 3
        tooltip.add(Component.literal("Tank " + dim + "x" + dim + "x" + dim + " Multiblock"));
        long capacity = valveSpec == null ? (size == 5 ? 1_024_000 : 320_000) : valveSpec.capacity();
        tooltip.add(Component.literal("Capacity: " + String.format(java.util.Locale.ROOT, "%,d", capacity) + " mB"));
        if (valveSpec != null) {
            tooltip.add(Component.literal("Required wall: " + valveSpec.wall().getName().getString()));
            if (valveSpec.simpleOnly()) tooltip.add(Component.literal("Simple fluids only"));
        }
    }

    @Override public java.util.List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        ItemStack result = new ItemStack(this);
        if (context.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)
                instanceof MultiblockTankControllerBlockEntity tank)
            result.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(tank,tank.saveWithId(tank.getLevel().registryAccess()))));
        return java.util.List.of(result);
    }

    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult target,net.minecraft.world.level.LevelReader level,BlockPos pos,net.minecraft.world.entity.player.Player player) {
        ItemStack result = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof MultiblockTankControllerBlockEntity tank)
            result.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(tank,tank.saveWithId(tank.getLevel().registryAccess()))));
        return result;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        var component=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        net.minecraft.nbt.CompoundTag saved=component==null?null:component.copyTag();
        if (level.getBlockEntity(pos) instanceof MultiblockTankControllerBlockEntity tank && saved != null)
            tank.loadAdditional(saved,level.registryAccess());
    }
}
