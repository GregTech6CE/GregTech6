package com.gregtech.gregtech.block.sensor;

import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

import javax.annotation.Nullable;
import java.util.List;

/** GT6 sensor panel block (fluid/item/energy/progress meters). */
public class SensorBlock extends DirectionalBlock implements EntityBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {

    private final SensorBlockEntity.Kind kind;

    public SensorBlock(SensorBlockEntity.Kind kind, Properties properties) {
        super(properties.noOcclusion());
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public SensorBlockEntity.Kind kind() { return kind; }

    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        int[] b = com.gregtech.gregtech.api.sensor.SensorPanelGeometry.bounds(state.getValue(FACING));
        return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }
    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(tool, com.gregtech.gregtech.api.tool.GTToolType.WRENCH)
                ? com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING, com.gregtech.gregtech.block.machine.MachineRotationType.ALL) : null;
    }
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        var tool = player.getItemInHand(hand);
        if (com.gregtech.gregtech.api.tool.GTToolHelper.isMonkeyWrench(tool)) {
            var side = com.gregtech.gregtech.api.tool.ToolInteractions.selectedFace(hit);
            if (!player.mayBuild() || !level.mayInteract(player, pos)
                    || side == state.getValue(FACING)) return net.minecraft.world.InteractionResult.PASS;
            if (tool.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric
                    && !electric.canInteract(tool)) return net.minecraft.world.InteractionResult.PASS;
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SensorBlockEntity sensor) {
                sensor.setInputSide(side);
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(tool, 10000, player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (com.gregtech.gregtech.api.tool.ToolInteractions.use(state, level, pos, player, hand, hit))
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        if (hit.getDirection() != state.getValue(FACING) || !player.mayBuild() || !level.mayInteract(player,pos)) return net.minecraft.world.InteractionResult.PASS;
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SensorBlockEntity sensor)) return net.minecraft.world.InteractionResult.PASS;
        var point = com.gregtech.gregtech.api.block.FaceCoordinates.pixels(hit.getDirection(),
                hit.getLocation().x-pos.getX(), hit.getLocation().y-pos.getY(), hit.getLocation().z-pos.getZ());
        var held = player.getItemInHand(hand);
        boolean screwdriver = com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held, com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER);
        boolean reset = com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held, com.gregtech.gregtech.api.tool.GTToolType.SOFT_HAMMER);
        if (!sensor.control(point.x(), point.y(), screwdriver, reset)) return net.minecraft.world.InteractionResult.PASS;
        if (screwdriver || reset) com.gregtech.gregtech.api.tool.GTToolHelper.damageForUse(held,1,player);
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public void toolStateChanged(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof SensorBlockEntity sensor) sensor.resetInputSide();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // display faces the clicked surface's outward normal (panel on the machine wall)
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SensorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.SENSOR.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<SensorBlockEntity>) SensorBlockEntity::serverTick;
    }

    // ── Redstone: analog signal proportional to the measured fill ratio ─────

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof SensorBlockEntity sensor
                && direction != state.getValue(FACING).getOpposite()) {
            return sensor.signal();
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        com.gregtech.gregtech.client.SensorTooltips.add(tooltip, kind.name());
    }
}
