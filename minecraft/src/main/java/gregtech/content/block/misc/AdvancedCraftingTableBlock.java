package gregtech.content.block.misc;

import gregtech.api.material.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

/** GT6 Advanced Crafting Table — crafting with inventory cache and recipe memory. */
public class AdvancedCraftingTableBlock extends HorizontalDirectionalBlock implements EntityBlock,
        gregtech.engine.tool.ToolInteractionTarget {
//? if >=1.21.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
*///?} else {
//?}
    private final gregtech.api.material.GTMaterial material;

    public gregtech.api.material.GTMaterial material() {
        return material;
    }

    public AdvancedCraftingTableBlock(gregtech.api.material.GTMaterial material, Properties properties) {
        super(properties);
        this.material = material;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedCraftingTableBlockEntity(pos, state);
    }

    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, net.minecraft.world.item.ItemStack tool) {
        return gregtech.engine.tool.GTToolHelper.isMachineWrench(tool) ? gregtech.engine.tool.ToolInteractionSpec.facing(FACING,
                gregtech.content.block.machine.MachineRotationType.HORIZONTAL) : null;
    }
//? if >=1.21.1 {
    /*public net.minecraft.world.InteractionResult interact(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
*///?} else {
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
//?}
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND
                || !(level.getBlockEntity(pos) instanceof AdvancedCraftingTableBlockEntity table)) return net.minecraft.world.InteractionResult.PASS;
        var tool = player.getItemInHand(hand);
        if (gregtech.engine.tool.GTToolHelper.isMonkeyWrench(tool) || gregtech.engine.tool.GTToolHelper.isScrewdriver(tool)) {
            if (!level.isClientSide && player.mayBuild() && level.mayInteract(player, pos)) {
                boolean upper = hit.getDirection() == Direction.UP;
                boolean filter = gregtech.engine.tool.GTToolHelper.isScrewdriver(tool);
                table.toggleMode(upper, filter);
                gregtech.engine.tool.GTToolHelper.damageForToolClickReturn(tool, 10000, player);
                boolean enabled = (table.modes() & (filter ? (upper ? 4 : 8) : (upper ? 1 : 2))) != 0;
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(filter ? "message.gt6ce.crafting.filter"
                        : "message.gt6ce.crafting.access", upper ? "4×4" : "9×4",
                        net.minecraft.network.chat.Component.translatable((filter ? enabled : !enabled) ? "options.on" : "options.off")), true);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (gregtech.engine.tool.ToolInteractions.use(state, level, pos, player, hand,
                hit)) return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        boolean upper = hit.getDirection() == Direction.UP;
        if (!upper && hit.getDirection().getAxis() != state.getValue(FACING).getAxis()) return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide) player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> upper ? new gregtech.client.gui.AdvancedCraftingMenu(id, inv, table) : table.storageMenu(id, inv),
                net.minecraft.network.chat.Component.translatable(getDescriptionId())));
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
//? if >=1.21.1 {
    /*@Override
    public net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held, BlockState state,
            net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return switch (interact(state, level, pos, player, hand, hit)) {
            case SUCCESS -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME, CONSUME_PARTIAL -> net.minecraft.world.ItemInteractionResult.CONSUME;
            case FAIL -> net.minecraft.world.ItemInteractionResult.FAIL;
            default -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    @Override
    public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        return interact(state, level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
//?}
    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof AdvancedCraftingTableBlockEntity table) table.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
//? if >=1.21.1 {
    /*@Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
*///?} else {
    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
//?}
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        gregtech.client.StorageBlockTooltips.craftingTable(tooltip);
    }
}
