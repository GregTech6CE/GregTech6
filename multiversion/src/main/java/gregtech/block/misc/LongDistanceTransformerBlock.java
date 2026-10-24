/**
 * Copyright (c) 2021 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech.block.misc;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;

public class LongDistanceTransformerBlock extends DirectionalBlock implements EntityBlock, gregtech.internal.tool.ToolInteractionTarget,
        gregtech.internal.machine.GTMachineBlock {
    private final long voltage;

    public long voltage() {
        return voltage;
    }

    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ACTIVITY =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("activity", 0, 3);

    public LongDistanceTransformerBlock(long voltage, Properties properties) {
        super(properties);
        this.voltage = voltage;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(ACTIVITY, 0));
    }

    public gregtech.internal.material.GTMaterial material() {
        return gregtech.internal.material.GTMaterialRegistry.get(
                gregtech.content.logistics.LongDistanceCatalog.transformerMaterial(voltage));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVITY);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LongDistanceTransformerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == gregtech.registry.GTBlockEntities.LONG_DIST_TRANSFORMER.get() ? (world, pos, current,
                entity) -> ((LongDistanceTransformerBlockEntity) entity).tickActivity() : null;
    }

    @Override
    public gregtech.internal.tool.ToolInteractionSpec toolInteraction(BlockState state, net.minecraft.world.item.ItemStack tool) {
        return gregtech.internal.tool.GTToolHelper.matchesTool(tool, gregtech.internal.tool.GTToolType.WRENCH)
                ? gregtech.internal.tool.ToolInteractionSpec.facing(FACING, gregtech.block.machine.MachineRotationType.ALL) : null;
    }

    @Override
    public void toolStateChanged(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        LongDistanceTopology.changed(level);
        if (level.getBlockEntity(pos) instanceof LongDistEndpointBlockEntity endpoint) endpoint.invalidateRoute();
        if (level.getBlockEntity(pos) instanceof LongDistanceTransformerBlockEntity endpoint) endpoint.invalidateRoute();
    }

    public net.minecraft.world.InteractionResult interact(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        var tool = player.getItemInHand(hand);
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return net.minecraft.world.InteractionResult.PASS;
        if (gregtech.internal.tool.GTToolHelper.isSoftHammer(tool) || gregtech.internal.tool.GTToolHelper.isMagnifyingGlass(tool)) {
            if (!level.isClientSide) {
                boolean reset = gregtech.internal.tool.GTToolHelper.isSoftHammer(tool);
                if (level.getBlockEntity(pos) instanceof LongDistEndpointBlockEntity endpoint) {
                    if (reset) endpoint.rescan();
                    else endpoint.describe(player);
                }
                if (level.getBlockEntity(pos) instanceof LongDistanceTransformerBlockEntity endpoint) {
                    if (reset) endpoint.rescan();
                    else endpoint.describe(player);
                }
                gregtech.internal.tool.GTToolHelper.damageForToolClickReturn(tool, reset ? 10000 : 1, player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        var spec = toolInteraction(state, tool);
        boolean same = spec != null && state.getValue(FACING) == gregtech.internal.tool.ToolInteractions.selectedFace(hit);
        boolean allowed = spec != null && gregtech.internal.tool.ToolInteractions.canApply(spec, state, level, pos, player, tool,
                gregtech.internal.tool.ToolInteractions.selectedFace(hit));
        if (gregtech.internal.tool.ToolInteractions.use(state, level, pos, player, hand, hit)) {
            if (allowed && !level.isClientSide && !(tool.getItem() instanceof gregtech.item.ElectricToolItem))
                gregtech.internal.tool.GTToolHelper.damageForToolClickReturn(tool, same ? 10000 : 9900, player);
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var player = context.getPlayer();
        int side = gregtech.content.logistics.LongDistanceNetwork.placement(player == null ? 0 : player.getXRot(),
                context.getHorizontalDirection().getOpposite().ordinal());
        return defaultBlockState().setValue(FACING, Direction.values()[side]);
    }

//? if >=1.21.1 {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state,
            net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return interact(state, level, pos, player, hand,
                hit) == net.minecraft.world.InteractionResult.PASS ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        return interact(state, level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
*///?} else {
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return interact(state, level, pos, player, hand, hit);
    }
//?}

    @Override
    public void onPlace(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState previous, boolean moving) {
        if (!state.is(previous.getBlock()) || state.getValue(FACING) != previous.getValue(FACING)) LongDistanceTopology.changed(level);
        super.onPlace(state, level, pos, previous, moving);
    }

    @Override
    public void onRemove(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) || state.getValue(FACING) != next.getValue(FACING)) LongDistanceTopology.changed(level);
        super.onRemove(state, level, pos, next, moving);
    }
}
