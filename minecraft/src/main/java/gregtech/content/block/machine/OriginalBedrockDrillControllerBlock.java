package gregtech.content.block.machine;

import gregtech.content.blockentity.machine.BedrockDrillControllerBlockEntity;
import gregtech.content.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
//?}

import javax.annotation.Nullable;
import java.util.List;

import gregtech.content.block.GTBlock;
/** GT6 numeric ID 17999: real top-center drilling controller, output on its upper side. */
public final class OriginalBedrockDrillControllerBlock extends GTBlock implements gregtech.engine.multiblock.StructurePreviewSource {
    @Override
    protected InteractionResult useBlock(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill))
        return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
//? if >=1.21.1 {
        /*var tank = drill.fluidCapability(hit.getDirection());
*///?} else {
        var tank = drill.getCapability(ForgeCapabilities.FLUID_HANDLER, hit.getDirection()).resolve().orElse(null);
//?}
        if (tank != null && FluidUtil.interactWithFluidHandler(player, hand, tank))
        return InteractionResult.CONSUME;
        if (player.getItemInHand(hand).isEmpty()) {
//? if >=1.21.1 {
            /*var inventory = drill.itemCapability(Direction.UP);
*///?} else {
            var inventory = drill.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
//?}
            if (inventory != null) {
                ItemStack taken = inventory.extractItem(0, 64, false);
                if (!taken.isEmpty() && !player.getInventory().add(taken)) player.drop(taken, false);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public OriginalBedrockDrillControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BedrockDrillControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.BEDROCK_DRILL.get()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> BedrockDrillControllerBlockEntity.serverTick(
                tickLevel, tickPos, tickState, (BedrockDrillControllerBlockEntity) blockEntity);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill)
        drill.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    protected void appendTooltip(ItemStack stack, @Nullable BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        gregtech.client.OriginalControllerTooltips.standalone(
                gregtech.content.multiblock.OriginalControllerTooltipData.Family.BEDROCK_DRILL, tooltip);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        ItemStack stack = new ItemStack(this);
        if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BedrockDrillControllerBlockEntity drill) {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(drill,
                    gregtech.engine.stack.NbtCompat.saveForBlockItem(drill, context.getLevel().registryAccess()));
            data.remove("gt.output");
            data.remove("gt.drill_energy");
            data.remove("gt.stone_type");
            data.remove("gt.lubricant");
            // The output slot is dropped separately by onRemove.
            gregtech.engine.stack.BlockEntityData.write(stack, data);
        }
        return List.of(stack);
    }

//? if >=1.21.1 {
    /*@Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit, net.minecraft.world.level.LevelReader level,
            BlockPos pos, net.minecraft.world.entity.player.Player player) {
*///?} else {
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
//?}
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill) {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(drill,
                    gregtech.engine.stack.NbtCompat.saveForBlockItem(drill, drill.getLevel().registryAccess()));
            data.remove("gt.output");
            data.remove("gt.drill_energy");
            data.remove("gt.stone_type");
            data.remove("gt.lubricant");
            gregtech.engine.stack.BlockEntityData.write(stack, data);
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        var saved = gregtech.engine.stack.BlockEntityData.read(stack);
        if (saved != null && level.getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity drill)
            drill.restorePersistentState(saved, level.registryAccess());
    }
}
