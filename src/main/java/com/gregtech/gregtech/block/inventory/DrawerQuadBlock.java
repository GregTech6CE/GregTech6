package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 Quad Drawer block — front-face quadrants address the four compartments. */
public class DrawerQuadBlock extends DirectionalBlock implements EntityBlock, SimpleWaterloggedBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {

    private final com.gregtech.gregtech.api.material.GTMaterial material;
    public com.gregtech.gregtech.api.material.GTMaterial material() { return material; }
    public DrawerQuadBlock(com.gregtech.gregtech.api.material.GTMaterial material, Properties properties) {
        super(properties);
        this.material = material;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GTWaterloggable.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        float pitch = ctx.getPlayer() == null ? 0 : ctx.getPlayer().getXRot();
        Direction facing = pitch >= 65 ? Direction.UP : pitch <= -65 ? Direction.DOWN : ctx.getHorizontalDirection().getOpposite();
        BlockState base = defaultBlockState().setValue(FACING, facing);
        return GTWaterloggable.getStateForPlacement(base, ctx);
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrawerQuadBlockEntity(pos, state);
    }

    /** GT6 UT.Code.getFacingCoordsClicked, including the exact middle-line tie break. */
    public static int quadrant(Direction facing, BlockPos pos, Vec3 hit) {
        return com.gregtech.gregtech.content.storage.ContainerStorageRules.quadrant(facing.ordinal(),hit.x-pos.getX(),hit.y-pos.getY(),hit.z-pos.getZ());
    }
    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return com.gregtech.gregtech.api.tool.GTToolHelper.isMachineWrench(tool)
                ? com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING, com.gregtech.gregtech.block.machine.MachineRotationType.ALL) : null;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof DrawerQuadBlockEntity drawer)) return InteractionResult.PASS;
        ItemStack tool = player.getItemInHand(hand);
        if (com.gregtech.gregtech.api.tool.GTToolHelper.isMonkeyWrench(tool)) {
            if (!level.isClientSide) {
                drawer.toggleSidedAccess();
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForToolClickReturn(tool,10000,player);
                player.displayClientMessage(Component.translatable(drawer.sidedAccess() ? "message.gregtech.drawer.sided" : "message.gregtech.drawer.anywhere"),true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (com.gregtech.gregtech.api.tool.ToolInteractions.use(state,level,pos,player,hand,hit)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (hit.getDirection() != state.getValue(FACING)) return InteractionResult.PASS;
        int page = quadrant(state.getValue(FACING), pos, hit.getLocation());
        if (!level.isClientSide) player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> drawer.createMenu(page,id,inv), Component.translatable("gui.gregtech.drawer.page", Component.translatable(getDescriptionId()), page+1)));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.drawer.1"));
        tooltip.add(Component.translatable("gt.tooltip.drawer.2"));

    }
    @Override public void onRemove(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof com.gregtech.gregtech.api.inventory.BlockContents contents) {
            contents.dropContents();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
