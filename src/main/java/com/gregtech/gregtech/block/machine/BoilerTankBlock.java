package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.BoilerSpec;
import com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 single-block Steam Boiler Tank. Symmetric full cube (top/bottom/side
 * material-tinted textures); HU in from any face, steam out the top.
 * The front face shows a pressure gauge; rotate with a wrench.
 */
public class BoilerTankBlock extends Block implements EntityBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {

    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state,ItemStack tool) {
        return com.gregtech.gregtech.api.tool.GTToolHelper.isMachineWrench(tool)
                ? com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING,MachineRotationType.HORIZONTAL) : null;
    }
    /** Source boiler collision bounds are inset two pixels so touching entities enter its heat surface. */
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return Block.box(2,2,2,14,14,14);
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BoilerTankBlockEntity boiler)
            com.gregtech.gregtech.util.GTEntityHelper.applyHeatDamage(entity,boiler.contactDamage());
    }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.Entity entity) {
        entityInside(state,level,pos,entity);
        super.stepOn(level,pos,state,entity);
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final BoilerSpec spec;

    public BoilerTankBlock(BoilerSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public BoilerSpec spec() { return spec; }

    // GT6 MultiTileEntityBoilerTank.removedByPlayer: pressure > 4/31, survival only.
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()
                && level.getBlockEntity(pos) instanceof BoilerTankBlockEntity boiler
                && boiler.barometerValue() > 4) boiler.explode();
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        var entity = context.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (entity instanceof BoilerTankBlockEntity boiler && boiler.barometerValue() > 4) return List.of();
        return List.of(new ItemStack(this));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoilerTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.STEAM_BOILER.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<BoilerTankBlockEntity>) BoilerTankBlockEntity::serverTick;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BoilerTankBlockEntity boiler) {
                return boiler.onBlockActivated(state, player, hand, hit);
            }
        }
        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("1 L Water + 80 HU → 160 L Steam")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.gregtech.machine.energy_in")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(spec.heatInputRecommended() + " HU/t (any face)")
                        .withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.translatable("tooltip.gregtech.machine.energy_out")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal(spec.steamOutput() + " L/t Steam (top)")
                        .withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Needs water — runs dry or over-pressure → explodes")
                .withStyle(ChatFormatting.DARK_RED));
    }
}
