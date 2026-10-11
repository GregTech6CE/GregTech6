package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.machine.PumpSpec;
import com.gregtech.gregtech.blockentity.energy.PumpBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 rotational pump — drains fluid blocks using RU power. */
public class PumpBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final PumpSpec spec;

    public PumpBlock(PumpSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected com.mojang.serialization.MapCodec<? extends Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public PumpSpec spec() { return spec; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PumpBlockEntity pump) pump.onRemoved();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<PumpBlockEntity>) PumpBlockEntity::serverTick;
    }

    @Override
    public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.pump.1").withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.translatable("gt.lang.energy.input").withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(": " + spec.inputSpeed() + " ")
                .append(Component.translatable("gt.td.short.energy.kinetic_rotation"))
                .append("/t (").append(Component.translatable("gt.lang.face.back")).append(")"));
        tooltip.add(Component.translatable("gt.tooltip.pump.2").withStyle(net.minecraft.ChatFormatting.GOLD));
        tooltip.add(Component.translatable("gt.tooltip.pump.3").withStyle(net.minecraft.ChatFormatting.GOLD));
    }
}
