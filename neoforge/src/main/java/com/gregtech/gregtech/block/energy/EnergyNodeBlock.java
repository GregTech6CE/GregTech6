package com.gregtech.gregtech.block.energy;





import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import net.minecraft.world.InteractionResult;
import com.gregtech.gregtech.registry.GTEnergyNodes;
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
public class EnergyNodeBlock extends DirectionalBlock implements EntityBlock, SimpleWaterloggedBlock {

    private final EnergyNodeSpec spec;

    public EnergyNodeBlock(EnergyNodeSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(defaultBlockState().setValue(FACING,
                spec.kind() == EnergyNodeSpec.Kind.SOLAR ? Direction.DOWN : Direction.NORTH)
                .setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public EnergyNodeSpec spec() { return spec; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // solar panels always output downward; other nodes face away from the player
        Direction facing = spec.kind() == EnergyNodeSpec.Kind.SOLAR || spec.id().startsWith("battery_eu")
                ? Direction.DOWN
                : ctx.getNearestLookingDirection();
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
        if (level.isClientSide || type != GTEnergyNodes.ENERGY_NODE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<EnergyNodeBlockEntity>) EnergyNodeBlockEntity::serverTick;
    }

    private static final net.minecraft.world.phys.shapes.VoxelShape SOLAR_SHAPE =
            net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 4, 16);

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
        // GT6 solar panels are thin plates; battery boxes are inset appliances
        if (spec.kind() == EnergyNodeSpec.Kind.SOLAR) return SOLAR_SHAPE;

        return super.getShape(state, level, pos, ctx);
    }

    protected InteractionResult interact(BlockState state, Level level, BlockPos pos,
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
        if (useOrientation(state,level,pos,player,hand,hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        // GT6 rotational transformers reverse with a monkey wrench; the other
        // invertible energy nodes keep their existing soft-hammer interaction.
        boolean modeTool = node.isRotationTransformer() || node.isElectricTransformer()
                ? com.gregtech.gregtech.platform.neoforge.NeoToolBindings.isMonkeyWrench(held) : com.gregtech.gregtech.platform.neoforge.NeoToolBindings.isSoftHammer(held);
        if (node.isInvertible() && modeTool) {
            if (!level.isClientSide) {
                boolean up = node.toggleInverted();
                if (node.isRotationTransformer() || node.isElectricTransformer()) com.gregtech.gregtech.platform.neoforge.NeoToolBindings.damageForUse(held, 1, player);
                player.displayClientMessage(Component.translatable(
                        up ? "message.gregtech.transformer.step_up" : "message.gregtech.transformer.step_down"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Turbines: install a rotor part, crowbar removes it
        if (node.isTurbine()) {
            if (EnergyNodeBlockEntity.isRotorItem(held)) {
                if (!level.isClientSide && node.installRotor(held)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (node.hasRotor() && com.gregtech.gregtech.platform.neoforge.NeoToolBindings.matches(held,"crowbar")) {
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
            if (com.gregtech.gregtech.platform.neoforge.NeoToolBindings.matches(held,"crowbar")) {
                if (!level.isClientSide) {
                    ItemStack removed = node.removeBattery();
                    if (!removed.isEmpty() && !player.addItem(removed)) player.drop(removed, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!com.gregtech.gregtech.platform.neoforge.NeoToolBindings.isTool(held)) {
                if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.openMenu(node.batteryMenu());
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String outUnit = spec.outType().getShortName();
        switch (spec.kind()) {
            case MAGNET -> {
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyInLine(
                        spec.inputRate(), spec.inputRate(), spec.inputRate() * 2,
                        spec.inType().getShortName(), "four sides"));
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyOutLine(
                        spec.outputRate(), outUnit)
                        .append(Component.literal(" (+ front, - back)")
                                .withStyle(net.minecraft.ChatFormatting.WHITE)));
                tooltip.add(Component.translatable("tooltip.gregtech.magnet.controls")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            case SOLAR -> {
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyOutLine(spec.outputRate(), outUnit));
                tooltip.add(Component.translatable("tooltip.gregtech.node.solar")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            case TURBINE -> {
                tooltip.add(Component.translatable("tooltip.gregtech.machine.energy_in")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(Component.literal(spec.inputRate() + " L/t Steam")
                                .withStyle(net.minecraft.ChatFormatting.WHITE)));
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyOutLine(spec.outputRate(), outUnit));
                tooltip.add(Component.translatable("tooltip.gregtech.node.rotor")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            default -> {
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyInLine(
                        spec.inputRate(), spec.inputRate(), spec.inputRate(),
                        spec.inType().getShortName(),
                        (spec.id().startsWith("rotation_transformer_") || spec.id().startsWith("transformer_")) ? "front" : "sides"));
                tooltip.add(com.gregtech.gregtech.client.TooltipHelper.energyOutLine(spec.outputRate(), outUnit)
                        .append(Component.literal(spec.id().startsWith("rotation_transformer_")
                                ? " (back)" : spec.id().startsWith("transformer_") ? " (sides)" : " (front)").withStyle(net.minecraft.ChatFormatting.WHITE)));
                if (spec.kind() == EnergyNodeSpec.Kind.STORAGE) {
                    if (spec.batterySlots() > 0) {
                        tooltip.add(Component.translatable("tooltip.gregtech.node.battery_slots",spec.batterySlots())
                                .withStyle(net.minecraft.ChatFormatting.GRAY));
                    } else {
                        tooltip.add(Component.literal("Capacity: "
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
    @Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,hand,hit).consumesAction()?net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide):net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
    private boolean useOrientation(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        var tool=player.getItemInHand(hand);boolean monkey=com.gregtech.gregtech.platform.neoforge.NeoToolBindings.isMonkeyWrench(tool);
        if(spec.kind()==EnergyNodeSpec.Kind.SOLAR||spec.id().startsWith("battery_eu")||monkey&&(spec.id().startsWith("transformer_")||spec.id().startsWith("rotation_transformer_")||spec.kind()==EnergyNodeSpec.Kind.MAGNET)||!com.gregtech.gregtech.platform.neoforge.NeoToolBindings.isMachineWrench(tool))return false;
        if(!player.mayBuild()||!level.mayInteract(player,pos)||level.isClientSide)return true;
        var side=com.gregtech.gregtech.platform.neoforge.transport.FluidPipeToolInteractions.selectedFace(hit);if(state.getValue(FACING)==side)return true;
        if(!level.setBlockAndUpdate(pos,state.setValue(FACING,side)))return true;
        level.invalidateCapabilities(pos);level.playSound(null,pos,com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(),net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);com.gregtech.gregtech.platform.neoforge.NeoToolBindings.damageForUse(tool,1,player);return true;
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
