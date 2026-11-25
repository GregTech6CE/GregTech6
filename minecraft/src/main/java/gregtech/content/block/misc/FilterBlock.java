package gregtech.content.block.misc;

import gregtech.content.block.GTDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** GT6 Filter block — pipe-insertable item/fluid filter. */
public class FilterBlock extends GTDirectionalBlock
        implements net.minecraft.world.level.block.EntityBlock, gregtech.engine.tool.ToolInteractionTarget {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty SECONDARY =
            net.minecraft.world.level.block.state.properties.DirectionProperty.create("secondary");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SECONDARY_SET =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("secondary_set");
    private final String filterType;

    public static Direction secondary(BlockState state) {
        return state.getValue(SECONDARY_SET)
                ? state.getValue(SECONDARY)
                : state.getValue(FACING).getOpposite();
    }

    public FilterBlock(String filterType, Properties properties) {
        super(properties);
        this.filterType = filterType;
        registerDefaultState(stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SECONDARY, Direction.SOUTH)
                .setValue(SECONDARY_SET, false));
    }

    public String filterType() {
        return filterType;
    }

    @Override
    protected net.minecraft.world.InteractionResult useBlock(
            BlockState state,
            net.minecraft.world.level.Level level,
            BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (gregtech.engine.tool.ToolInteractions.use(state, level, pos, player, hand, hit))
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        var tool = player.getItemInHand(hand);
        if (gregtech.engine.tool.GTToolHelper.isScrewdriver(tool)
                || gregtech.engine.tool.GTToolHelper.isSoftHammer(tool)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof FilterBlockEntity filter) {
                if (gregtech.engine.tool.GTToolHelper.isSoftHammer(tool)) filter.clearFilter();
                else filter.toggleMode();
                gregtech.engine.tool.GTToolHelper.damageForUse(tool, 1, player);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                filter.blacklist() ? "gt6ce.filter.blacklist" : "gt6ce.filter.whitelist"),
                        true);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide
                && player instanceof net.minecraft.server.level.ServerPlayer server
                && level.getBlockEntity(pos) instanceof FilterBlockEntity filter)
            gregtech.platform.Mc.openMenu(server, filter, buffer -> buffer.writeBoolean(filter.prefixMode()));
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void appendTooltip(
            net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.level.BlockGetter level,
            java.util.List<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.gt6ce.filter.routing"));
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.gt6ce.filter.tools"));
        if (filterType.equals("oredict"))
            lines.add(net.minecraft.network.chat.Component.translatable("tooltip.gt6ce.filter.prefix"));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SECONDARY, SECONDARY_SET);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var front = context.getNearestLookingDirection();
        return defaultBlockState()
                .setValue(FACING, front)
                .setValue(SECONDARY, front.getOpposite())
                .setValue(SECONDARY_SET, true);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilterBlockEntity(pos, state);
    }

    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(
            BlockState state, net.minecraft.world.item.ItemStack tool) {
        var property = gregtech.engine.tool.GTToolHelper.isMonkeyWrench(tool)
                ? SECONDARY
                : gregtech.engine.tool.GTToolHelper.isMachineWrench(tool) ? FACING : null;
        return property == null
                ? null
                : gregtech.engine.tool.ToolInteractionSpec.facing(
                        property, gregtech.engine.tool.MachineRotationType.ALL);
    }

    @Override
    public void toolStateChanged(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            BlockState state,
            gregtech.engine.tool.ToolInteractionSpec operation) {
        if (operation.facing() == SECONDARY && !state.getValue(SECONDARY_SET))
            level.setBlockAndUpdate(pos, state.setValue(SECONDARY_SET, true));
    }

    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(
            BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack = new net.minecraft.world.item.ItemStack(this);
        var entity = builder.getOptionalParameter(
                net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (entity instanceof FilterBlockEntity) {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(
                    entity,
                    gregtech.engine.stack.NbtCompat.saveForBlockItem(
                            entity, builder.getLevel().registryAccess()));
            gregtech.engine.stack.BlockEntityData.write(stack, data);
        }
        return java.util.List.of(stack);
    }
}
