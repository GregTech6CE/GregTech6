package gregtech.content.block.machine;

import gregtech.content.blockentity.machine.LightningRodControllerBlockEntity;
import gregtech.content.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
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

import javax.annotation.Nullable;
import java.util.List;

import gregtech.content.block.GTBlock;
/** GT6 numeric ID 17998: the controller occupies the bottom-center tungsten wall. */
public final class OriginalLightningRodControllerBlock extends GTBlock implements gregtech.engine.multiblock.StructurePreviewSource {
    public OriginalLightningRodControllerBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LightningRodControllerBlockEntity(pos, state);
    }

    @Nullable @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.LIGHTNING_ROD.get()) return null;
        return (world, pos, current, be) -> LightningRodControllerBlockEntity.serverTick(
                world, pos, current, (LightningRodControllerBlockEntity) be);
    }

    @Override protected void appendTooltip(ItemStack stack, @Nullable BlockGetter level,
                                          List<Component> tooltip, TooltipFlag flag) {
        gregtech.client.UtilityControllerTooltips.lightning(tooltip);
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        ItemStack stack = new ItemStack(this);
        if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof LightningRodControllerBlockEntity rod) {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(rod,
                    gregtech.engine.stack.NbtCompat.saveForBlockItem(rod, context.getLevel().registryAccess()));
            data.remove("gt.energy");
            gregtech.engine.stack.BlockEntityData.write(stack, data);
        }
        return List.of(stack);
    }

//? if >=1.21.1 {
    /*@Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,net.minecraft.world.level.LevelReader level,BlockPos pos,net.minecraft.world.entity.player.Player player) {
*///?} else {
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
//?}
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof LightningRodControllerBlockEntity rod) {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(rod,
                    gregtech.engine.stack.NbtCompat.saveForBlockItem(rod, rod.getLevel().registryAccess()));
            data.remove("gt.energy");
            gregtech.engine.stack.BlockEntityData.write(stack, data);
        }
        return stack;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      @Nullable LivingEntity placer, ItemStack stack) {
        var saved = gregtech.engine.stack.BlockEntityData.read(stack);
        if (saved != null && level.getBlockEntity(pos) instanceof LightningRodControllerBlockEntity rod)
            rod.restorePersistentState(saved, level.registryAccess());
    }
}
