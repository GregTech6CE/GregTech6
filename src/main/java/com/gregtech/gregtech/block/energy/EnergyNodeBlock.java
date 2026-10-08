package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.world.InteractionResult;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import javax.annotation.Nullable;
import java.util.List;

/** GT6 energy-net node block (motor/dynamo/transformer/turbine/solar/storage). */
public class EnergyNodeBlock extends DirectionalBlock implements EntityBlock, SimpleWaterloggedBlock, ToolInteractionTarget {

    private final EnergyNodeSpec spec;

    public EnergyNodeBlock(EnergyNodeSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(defaultBlockState().setValue(FACING,
                spec.kind() == EnergyNodeSpec.Kind.SOLAR ? Direction.DOWN : Direction.NORTH)
                .setValue(GTWaterloggable.WATERLOGGED, false));
    }

    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        boolean fixed = spec.id().startsWith("battery_eu");
        if ((com.gregtech.gregtech.content.energy.OriginalThermalConverter.handles(spec) || com.gregtech.gregtech.content.energy.MagnetMachineDefinitions.handles(spec)) && GTToolHelper.isMonkeyWrench(tool)) return null;
        // GT6's rotational transformer reserves the monkey wrench for mode reversal.
        // A regular wrench still rotates the block itself.
        if ((spec.id().startsWith("rotation_transformer_") || spec.id().startsWith("transformer_")) && GTToolHelper.isMonkeyWrench(tool)) return null;
        return !fixed && GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.facing(FACING, spec.kind() == EnergyNodeSpec.Kind.SOLAR
                    ? com.gregtech.gregtech.block.machine.MachineRotationType.BOTTOM_HORIZONTAL
                    : com.gregtech.gregtech.block.machine.MachineRotationType.ALL) : null;
    }

    public EnergyNodeSpec spec() { return spec; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // GT6 solar panels use inverse clicked-side placement; their top is not an output.
        Direction facing = spec.kind() == EnergyNodeSpec.Kind.SOLAR
                ? ctx.getClickedFace().getOpposite()
                : spec.id().startsWith("battery_eu") ? Direction.DOWN : ctx.getNearestLookingDirection();
        if (spec.kind() == EnergyNodeSpec.Kind.SOLAR && facing == Direction.UP) facing = Direction.DOWN;
        return GTWaterloggable.getStateForPlacement(defaultBlockState().setValue(FACING, facing), ctx);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return spec.id().equals("rotation_transformer_wood") ? 150 : super.getFlammability(state, level, pos, face);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return spec.id().equals("rotation_transformer_wood") ? 150 : super.getFireSpreadSpeed(state, level, pos, face);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return state;
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
        if (level.isClientSide || type != GTBlockEntities.ENERGY_NODE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<EnergyNodeBlockEntity>) EnergyNodeBlockEntity::serverTick;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        // (insert+extract in one click, double withdrawals, armor swap-backs)
        if (hand == net.minecraft.world.InteractionHand.OFF_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!(level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node)) {
            return InteractionResult.PASS;
        }
        if(node.hasControlPanels()&&(!node.getCover(hit.getDirection()).isEmpty()
                ||com.gregtech.gregtech.content.cover.PanelCover.of(held)!=null)){
            var result=com.gregtech.gregtech.content.cover.PanelCoverInteraction.use(node,player,hand,hit,true);
            if(result.consumesAction())return result;
        }
        if (ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (node.isOriginalMotor() && GTToolHelper.isMonkeyWrench(held)) {
            if (!level.isClientSide) {
                node.reverseMotor();
                GTToolHelper.damageForUse(held, 1, player);
                player.displayClientMessage(Component.literal(node.motorCounterClockwise() ? "Counterclockwise" : "Clockwise"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (node.isOriginalMotor() && GTToolHelper.isMagnifyingGlass(held)) {
            if (!level.isClientSide) {
                GTToolHelper.damageForUse(held, 1, player);
                player.displayClientMessage(Component.literal(node.motorCounterClockwise() ? "Counterclockwise" : "Clockwise"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // GT6 rotational transformers reverse with a monkey wrench; the other
        // invertible energy nodes keep their existing soft-hammer interaction.
        boolean modeTool = node.isRotationTransformer() || node.isElectricTransformer()
                ? GTToolHelper.isMonkeyWrench(held) : GTToolHelper.isSoftHammer(held);
        if (node.isInvertible() && modeTool) {
            if (!level.isClientSide) {
                boolean up = node.toggleInverted();
                if (node.isRotationTransformer() || node.isElectricTransformer()) GTToolHelper.damageForUse(held, 1, player);
                player.displayClientMessage(Component.translatable(
                        up ? "message.gregtech.transformer.step_up" : "message.gregtech.transformer.step_down"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (node.isTurbine() && GTToolHelper.matchesTool(held, com.gregtech.gregtech.api.tool.GTToolType.PLUNGER)) {
            if (!level.isClientSide) {
                long removed = node.purgeTurbineSteam();
                if (removed > 0) GTToolHelper.damageForToolClickReturn(held, removed, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Return obsolete installed rotors from worlds made by earlier port versions.
        if (node.isTurbine()) {
            if (node.hasRotor() && GTToolHelper.matchesTool(held,
                    com.gregtech.gregtech.api.tool.GTToolType.CROWBAR)) {
                if (!level.isClientSide) {
                    ItemStack removed = node.removeRotor();
                    if (!removed.isEmpty() && !player.addItem(removed)) player.drop(removed, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        // Battery boxes: install complete energy items, crowbar removes, GUI otherwise
        if (node.isBatteryBox()) {
            if (EnergyNodeBlockEntity.batteryCapacityOf(held) > 0) {
                if (!level.isClientSide && node.installBattery(held)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (GTToolHelper.matchesTool(held,
                    com.gregtech.gregtech.api.tool.GTToolType.CROWBAR)) {
                if (!level.isClientSide) {
                    ItemStack removed = node.removeBattery();
                    if (!removed.isEmpty() && !player.addItem(removed)) player.drop(removed, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!GTToolHelper.isTool(held)) {
                if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.openMenu(node.batteryMenu());
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.handles(spec)) {
            var data = stack.getTagElement("BlockEntityTag");
            com.gregtech.gregtech.client.EnergyDeviceTooltips.append(spec,
                    data != null && data.getBoolean("gt.inverted"), getExplosionResistance(), tooltip);
            return;
        }
        String outUnit = spec.outType().getShortName();
        switch (spec.kind()) {
            case MAGNET -> {
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyInLine(
                        spec.inputRate(), spec.inputRate(), spec.inputRate() * 2,
                        spec.inType().getShortName(), Component.translatable("gt.lang.face.sides").getString()));
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyOutLine(
                        spec.outputRate(), outUnit)
                        .append(Component.literal(" (+ ").append(Component.translatable("gt.lang.face.front"))
                                .append(", - ").append(Component.translatable("gt.lang.face.back")).append(")")
                                .withStyle(net.minecraft.ChatFormatting.WHITE)));
                tooltip.add(Component.translatable("tooltip.gregtech.magnet.controls")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            case SOLAR -> {
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyOutLine(spec.outputRate(), outUnit)
                        .append(Component.translatable("tooltip.gregtech.solar.output_range", spec.outputRate() / 8, spec.outputRate())
                                .withStyle(net.minecraft.ChatFormatting.WHITE)));
            }
            case TURBINE -> {
                tooltip.add(Component.translatable("gt.lang.energy.input").append(": ")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(Component.literal(spec.inputRate() + " L/t ").append(Component.translatable("fluid_type.gregtech.steam"))
                                .withStyle(net.minecraft.ChatFormatting.WHITE)));
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyOutLine(spec.outputRate(), outUnit));
            }
            default -> {
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyInLine(
                        spec.inputRate(), spec.inputRate(), spec.inputRate(),
                        spec.inType().getShortName(),
                        Component.translatable((spec.id().startsWith("rotation_transformer_") || spec.id().startsWith("transformer_"))
                                ? "gt.lang.face.front" : "gt.lang.face.sides").getString()));
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyOutLine(spec.outputRate(), outUnit)
                        .append(Component.literal(" (").append(Component.translatable(spec.id().startsWith("rotation_transformer_")
                                ? "gt.lang.face.back" : spec.id().startsWith("transformer_") ? "gt.lang.face.sides" : "gt.lang.face.front"))
                                .append(")").withStyle(net.minecraft.ChatFormatting.WHITE)));
                if (spec.kind() == EnergyNodeSpec.Kind.STORAGE) {
                    if (spec.batterySlots() > 0) {
                        tooltip.add(Component.translatable("tooltip.gregtech.node.battery_slots",spec.batterySlots())
                                .withStyle(net.minecraft.ChatFormatting.GRAY));
                    } else {
                        tooltip.add(Component.translatable("gt.lang.energy.capacity").append(": "
                                + com.gregtech.gregtech.client.TooltipHelper.formatLong(spec.capacity()) + " " + outUnit)
                                .withStyle(net.minecraft.ChatFormatting.YELLOW));
                    }
                } else if (spec.inType() == spec.outType()) {
                    tooltip.add(Component.translatable(spec.id().startsWith("transformer_") ? "tooltip.gregtech.transformer.controls" : "tooltip.gregtech.node.invert")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
        }
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
