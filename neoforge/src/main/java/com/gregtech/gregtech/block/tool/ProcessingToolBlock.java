package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Existing GT6 utility meshes with real manual recipe processing. */
public final class ProcessingToolBlock extends ShapedToolBlock implements EntityBlock {
    public ProcessingToolBlock(String id, Properties properties) { super(id, properties); }
    private boolean wooden() { return toolId().equals("bathing_pot_wood") || toolId().equals("bathing_pot_table_wood"); }
    @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return wooden() ? 100 : super.getFlammability(state, level, pos, face);
    }
    @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return wooden() ? 100 : super.getFireSpreadSpeed(state, level, pos, face);
    }
    @Override public int tintRgb() {
        return toolId().equals("bathing_pot") || toolId().equals("bathing_pot_table")
                ? com.gregtech.gregtech.content.material.Materials.StainlessSteel.getColor() : super.tintRgb();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ProcessingToolBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.PROCESSING_TOOL.get()
                || toolId().equals("juicer")) return null;
        return (world, pos, blockState, entity) -> {
            if (com.gregtech.gregtech.content.tool.OpenVesselRules.rainDue(world.getGameTime())) ((ProcessingToolBlockEntity) entity).collectRain();
        };
    }
    @Override public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        var rotation = super.interact(state, level, pos, player, hand, hit);
        if (rotation.consumesAction()) return rotation;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ProcessingToolBlockEntity tool) tool.interact(player, hand, hit.getDirection());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ProcessingToolBlockEntity tool) tool.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack=new net.minecraft.world.item.ItemStack(this);
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof ProcessingToolBlockEntity tool) {
            var data=com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(tool,tool.saveWithId(builder.getLevel().registryAccess())); data.remove("gt.items");
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
        }
        return java.util.List.of(stack);
    }
}
