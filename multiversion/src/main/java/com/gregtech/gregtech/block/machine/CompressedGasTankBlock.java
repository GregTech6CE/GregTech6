package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.CompressedGasTankDeclaration;
import com.gregtech.gregtech.blockentity.machine.CompressedGasTankBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.fluids.FluidUtil;
//?}

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Addon compressed gas drum. GregTech owns the block; the addon supplies the declaration. */
public class CompressedGasTankBlock extends Block implements EntityBlock {
    private final AddonMachines.AcceptedCompressedGas accepted;
    private Supplier<BlockEntityType<?>> beType = () -> null;

    public CompressedGasTankBlock(AddonMachines.AcceptedCompressedGas accepted, Properties properties) {
        super(properties);
        this.accepted = accepted;
    }

    public AddonMachines.AcceptedCompressedGas accepted() { return accepted; }
    public CompressedGasTankDeclaration declaration() { return accepted.declaration(); }
    public int meltingPoint() { return accepted.meltingPoint(); }
    public void setBeType(Supplier<BlockEntityType<?>> supplier) { this.beType = supplier; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressedGasTankBlockEntity(beType.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != beType.get()) return null;
        return (lvl, pos, blockState, be) ->
                CompressedGasTankBlockEntity.serverTick(lvl, pos, blockState, (CompressedGasTankBlockEntity) be);
    }

//? if >=1.21.1 {
    /*private InteractionResult useOriginal(BlockState state, Level level, BlockPos pos, Player player,
*///?} else {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//?}
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);
        if (!(level.getBlockEntity(pos) instanceof CompressedGasTankBlockEntity tank)) return InteractionResult.PASS;
        return FluidUtil.interactWithFluidHandler(player, hand, tank)
                ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

//? if >=1.21.1 {
    /*@Override public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
*///?} else {
    @Override public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
//?}
                                           List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt6ce.tank.compressed.store"));
        tooltip.add(Component.translatable("gt6ce.tank.compressed.compressor"));
    }

//? if >=1.21.1 {
    /*@Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var result = useOriginal(state, level, pos, player, hand, hit);
        return result == InteractionResult.PASS ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : result == InteractionResult.FAIL ? net.minecraft.world.ItemInteractionResult.FAIL
                : net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return useOriginal(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
//?}
}
