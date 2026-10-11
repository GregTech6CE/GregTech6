package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import java.util.List;

/** GT6 manual tool stations: mortar, grind stone, sifting table. */
public class ManualToolBlock extends net.minecraft.world.level.block.HorizontalDirectionalBlock implements EntityBlock, ToolInteractionTarget {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty STONE = net.minecraft.world.level.block.state.properties.BooleanProperty.create("stone");

    private final ManualToolBlockEntity.Kind kind;
    private final VoxelShape shape;

    private final String pestleMaterial;
    public ManualToolBlock(ManualToolBlockEntity.Kind kind, Properties properties) { this(kind, "Steel", properties); }
    public ManualToolBlock(ManualToolBlockEntity.Kind kind, String pestleMaterial, Properties properties) {
        super(properties);
        this.kind = kind;
        this.pestleMaterial = pestleMaterial;
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(STONE, false));
        this.shape = switch (kind) {
            case MORTAR -> Block.box(2, 0, 2, 14, 6, 14);
            case GRINDSTONE -> Block.box(2, 0, 2, 14, 15, 14);
            case SIFTING -> Block.box(0, 0, 0, 16, 12, 16);
        };
    }

    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STONE);
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return kind == ManualToolBlockEntity.Kind.GRINDSTONE && GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, com.gregtech.gregtech.block.machine.MachineRotationType.HORIZONTAL) : null;
    }

    public int tint(int index) {
        if (kind == ManualToolBlockEntity.Kind.MORTAR) {
            if (index == 0) return com.gregtech.gregtech.content.material.Materials.Ceramic.getColor();
            if (index == 1) return com.gregtech.gregtech.api.material.GTMaterialRegistry.get(pestleMaterial).resolve().getColor();
        }
        return index == 0 ? com.gregtech.gregtech.registry.GTToolBlocks.tintOf(kind) : 0xFFFFFF;
    }
    public ManualToolBlockEntity.Kind kind() { return kind; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManualToolBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return !level.isClientSide && kind == ManualToolBlockEntity.Kind.SIFTING
                && type == com.gregtech.gregtech.registry.GTBlockEntities.MANUAL_TOOL.get()
                ? (world, pos, blockState, be) -> ManualToolBlockEntity.serverTick(world, pos, blockState, (ManualToolBlockEntity) be) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        // (insert+extract in one click, double withdrawals, armor swap-backs)
        if (hand == net.minecraft.world.InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof ManualToolBlockEntity tool)) return InteractionResult.PASS;
        if (ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (kind == ManualToolBlockEntity.Kind.GRINDSTONE && hit.getDirection() != net.minecraft.core.Direction.UP
                && hit.getDirection() != state.getValue(FACING)) return InteractionResult.PASS;
        if (!level.isClientSide) tool.interact(player, hand, hit.getDirection());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ManualToolBlockEntity tool) {
            tool.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        com.gregtech.gregtech.client.FunctionalBlockTooltips.appendManual(kind.name(), getExplosionResistance(), tooltip);
    }
}
