package gregtech.content.block.energy;

import gregtech.content.block.GTDirectionalBlock;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.cover.PanelCover;
import gregtech.content.cover.PanelCoverInteraction;
import gregtech.content.energy.EnergyNodeSpecProvider;
import gregtech.content.registry.GTEnergyNodes;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyNodeSpec.DeviceType;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.ToolInteractionSpec;
import gregtech.engine.tool.ToolInteractionTarget;
import gregtech.engine.tool.ToolInteractions;
import java.util.List;
import javax.annotation.Nullable;
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
import net.minecraft.world.phys.BlockHitResult;

/** GT6 energy-net node block (motor/dynamo/transformer/turbine/solar/storage). */
public class EnergyNodeBlock extends GTDirectionalBlock implements EntityBlock, EnergyNodeSpecProvider, ToolInteractionTarget,
        gregtech.engine.machine.PipeGeometry.FluidConnectorGeometry {

    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        // (insert+extract in one click, double withdrawals, armor swap-backs)
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!(level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node)) {
            return InteractionResult.PASS;
        }
        if (node.hasControlPanels()
                && (!node.getCover(hit.getDirection()).isEmpty() || PanelCover.of(held) != null)) {
            var result = PanelCoverInteraction.use(node, player, hand, hit, true);
            if (result.consumesAction()) return result;
        }
        if (ToolInteractions.use(state, level, pos, player, hand, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return node.useDevice(player, held);
    }

    private final EnergyNodeSpec spec;

    public EnergyNodeBlock(EnergyNodeSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        BlockState initial = defaultBlockState().setValue(FACING,
                spec.deviceType() == DeviceType.SOLAR ? Direction.DOWN : Direction.NORTH);
        registerDefaultState(initial);
    }

    @Override
    public double fluidConnectorHalf(BlockState state, Direction face) {
        if (!isTurbine()) return gregtech.engine.machine.PipeGeometry.NONE;
        Direction front = state.getValue(FACING);
        if (face == front) return gregtech.engine.machine.PipeGeometry.NONE;
        return gregtech.engine.machine.PipeGeometry.virtual(face.getAxis() == front.getAxis() ? 4 : 2);
    }

    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        boolean fixed = spec.id().startsWith("battery_eu");
        boolean reservesMonkeyWrench = isTransformer() || isOriginalThermalConverter()
                || spec.deviceType() == DeviceType.MAGNET;
        if (reservesMonkeyWrench && GTToolHelper.isMonkeyWrench(tool)) return null;
        return !fixed && GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, spec.deviceType() == DeviceType.SOLAR
                    ? gregtech.engine.tool.MachineRotationType.BOTTOM_HORIZONTAL
                    : gregtech.engine.tool.MachineRotationType.ALL) : null;
    }

    @Override
    public EnergyNodeSpec spec() { return spec; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Ordinary device fronts face the placer; solar panels and fixed batteries keep their special sides.
        // GT6 solar panels use inverse clicked-side placement; their top is not an output.
        Direction facing;
        if (spec.deviceType() == DeviceType.SOLAR) {
            facing = ctx.getClickedFace().getOpposite();
            if (facing == Direction.UP) facing = Direction.DOWN;
        } else {
            facing = spec.id().startsWith("battery_eu") ? Direction.DOWN : ctx.getNearestLookingDirection().getOpposite();
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return spec.id().equals("rotation_transformer_wood") ? 150 : super.getFlammability(state, level, pos, face);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return spec.id().equals("rotation_transformer_wood") ? 150 : super.getFireSpreadSpeed(state, level, pos, face);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTEnergyNodes.ENERGY_NODE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<EnergyNodeBlockEntity>) EnergyNodeBlockEntity::serverTick;
    }

    @Override
    protected void appendTooltip(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (gregtech.content.energy.OriginalEnergyDeviceTooltipData.handles(spec)) {
            var data = gregtech.engine.stack.BlockEntityData.readOrEmpty(stack);
            gregtech.client.EnergyDeviceTooltips.append(spec,
                    data.getBoolean("gt.inverted"), getExplosionResistance(), tooltip);
            return;
        }
        String outUnit = spec.outType().getShortName();
        switch (spec.kind()) {
            case MAGNET -> {
                tooltip.add(gregtech.client.TooltipHelper.energyInLine(
                        spec.inputRate(), spec.inputRate(), spec.inputRate() * 2,
                        spec.inType().getShortName(), Component.translatable("gt.lang.face.sides").getString()));
                tooltip.add(gregtech.client.TooltipHelper.energyOutLine(
                        spec.outputRate(), outUnit)
                        .append(Component.literal(" (+ ").append(Component.translatable("gt.lang.face.front"))
                                .append(", - ").append(Component.translatable("gt.lang.face.back")).append(")")
                                .withStyle(net.minecraft.ChatFormatting.WHITE)));
                tooltip.add(Component.translatable("tooltip.gt6ce.magnet.controls")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            case SOLAR -> {
                tooltip.add(gregtech.client.TooltipHelper.energyOutLine(spec.outputRate(), outUnit)
                        .append(Component.translatable("tooltip.gt6ce.solar.output_range", spec.outputRate() / 8, spec.outputRate())
                                .withStyle(net.minecraft.ChatFormatting.WHITE)));
            }
            case TURBINE -> {
                tooltip.add(Component.translatable("gt.lang.energy.input").append(": ")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(Component.literal(spec.inputRate() + " " + gregtech.engine.fluid.FluidUnits.unit() + "/t ").append(Component.translatable("fluid_type.gt6ce.steam"))
                                .withStyle(net.minecraft.ChatFormatting.WHITE)));
                tooltip.add(gregtech.client.TooltipHelper.energyOutLine(spec.outputRate(), outUnit));
            }
            default -> {
                boolean electricTransformer = isElectricTransformer();
                boolean rotationTransformer = isRotationTransformer();
                String outputFace = rotationTransformer ? "gt.lang.face.back"
                        : electricTransformer ? "gt.lang.face.sides" : "gt.lang.face.front";
                tooltip.add(gregtech.client.TooltipHelper.energyInLine(
                        spec.inputRate(), spec.inputRate(), spec.inputRate(),
                        spec.inType().getShortName(),
                        Component.translatable((rotationTransformer || electricTransformer)
                                ? "gt.lang.face.front" : "gt.lang.face.sides").getString()));
                tooltip.add(gregtech.client.TooltipHelper.energyOutLine(spec.outputRate(), outUnit)
                        .append(Component.literal(" (").append(Component.translatable(outputFace))
                                .append(")").withStyle(net.minecraft.ChatFormatting.WHITE)));
                if (spec.kind() == EnergyNodeSpec.Kind.STORAGE) {
                    if (spec.batterySlots() > 0) {
                        tooltip.add(Component.translatable("tooltip.gt6ce.node.battery_slots",spec.batterySlots())
                                .withStyle(net.minecraft.ChatFormatting.GRAY));
                    } else {
                        tooltip.add(Component.translatable("gt.lang.energy.capacity").append(": "
                                + gregtech.client.TooltipHelper.formatLong(spec.capacity()) + " " + outUnit)
                                .withStyle(net.minecraft.ChatFormatting.YELLOW));
                    }
                } else if (spec.inType() == spec.outType()) {
                    tooltip.add(Component.translatable(electricTransformer ? "tooltip.gt6ce.transformer.controls" : "tooltip.gt6ce.node.invert")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
        }
    }
//? if >=1.21.1 {
    /*@Override public void toolStateChanged(Level level,BlockPos pos,BlockState state) {
        level.invalidateCapabilities(pos);
    }
*///?} else {
//?}
    @Override public void onRemove(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof gregtech.engine.inventory.BlockContents contents) {
            contents.dropContents();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
