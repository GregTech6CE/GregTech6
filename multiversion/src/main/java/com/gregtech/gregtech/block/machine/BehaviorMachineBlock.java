package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration;
import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
//? if >=1.21.1 {
/**///?} else {
import com.gregtech.gregtech.api.tool.GTToolHelper;
//?}
import com.gregtech.gregtech.blockentity.machine.BehaviorMachineBlockEntity;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.platform.neoforge.NeoToolBindings;
*///?} else {
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.21.1 {
/*import net.minecraft.world.ItemInteractionResult;
*///?} else {
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.network.NetworkHooks;
//?}

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Single-block shell for an addon behavior machine. The addon does not subclass it. */
public class BehaviorMachineBlock extends GTFacingMachineBlock implements EntityBlock {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private final AddonMachines.AcceptedBehavior accepted;
    private Supplier<BlockEntityType<?>> beType = () -> null;

    public BehaviorMachineBlock(AddonMachines.AcceptedBehavior accepted, Properties properties) {
        super(spec(accepted), properties);
        this.accepted = accepted;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
                .setValue(LIT, false).setValue(RUNNING, false));
    }

    private static MachineSpec spec(AddonMachines.AcceptedBehavior accepted) {
        BehaviorMachineDeclaration declaration = accepted.declaration();
        var material = GTMaterialRegistry.get(declaration.material());
        return new MachineSpec(declaration.id(), material.getLocalName(), material.getColor(),
                0, 0, declaration.machineType(), 5.0F, 5.0F);
    }

    public AddonMachines.AcceptedBehavior accepted() { return accepted; }
    public BehaviorMachineDeclaration declaration() { return accepted.declaration(); }

    public ResourceLocation modelId(String suffix) {
        return ResourceLocation.fromNamespaceAndPath(accepted.addonId(),
                "block/machine/basic/" + accepted.declaration().machineType() + suffix);
    }

    public void setBeType(Supplier<BlockEntityType<?>> supplier) { this.beType = supplier; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, RUNNING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BehaviorMachineBlockEntity(beType.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != beType.get()) return null;
        return (lvl, pos, blockState, be) -> BehaviorMachineBlockEntity.serverTick(lvl, pos, blockState, (BehaviorMachineBlockEntity) be);
    }

    @Override
//? if >=1.21.1 {
/*    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = interact(state, level, pos, player, hand, hit);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                       InteractionHand hand, BlockHitResult hit) {
        InteractionResult wrench = handleWrench(state, level, pos, player, hand, hit);
*///?} else {
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult wrench = super.use(state, level, pos, player, hand, hit);
//?}
        if (wrench != InteractionResult.PASS) return wrench;
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof BehaviorMachineBlockEntity machine)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
//? if >=1.21.1 {
/*        IFluidHandlerItem fluidItem = held.getCapability(Capabilities.FluidHandler.ITEM);
*///?} else {
        IFluidHandlerItem fluidItem = held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
//?}
        if (fluidItem != null && machine.tanks() > 0) {
            if (!level.isClientSide) pour(player, hand, machine, fluidItem);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && machine.onUse(player, hand)) return InteractionResult.sidedSuccess(false);
//? if >=1.21.1 {
/*        if (!level.isClientSide && player instanceof ServerPlayer server && !NeoToolBindings.isTool(held))
            server.openMenu(machine, buf -> buf.writeBlockPos(pos));
*///?} else {
        if (!level.isClientSide && player instanceof ServerPlayer server && !GTToolHelper.isTool(held)) {
            NetworkHooks.openScreen(server, machine, buf -> buf.writeBlockPos(pos));
        }
//?}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void pour(Player player, InteractionHand hand, BehaviorMachineBlockEntity machine, IFluidHandlerItem fluidItem) {
        FluidStack available = fluidItem.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) return;
        int accepted = machine.fluids().fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        FluidStack drained = fluidItem.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return;
        machine.fluids().fill(drained, IFluidHandler.FluidAction.EXECUTE);
        player.setItemInHand(hand, fluidItem.getContainer());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof BehaviorMachineBlockEntity machine)
            machine.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
}
