package com.gregtech.gregtech.block.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MultiblockPortBlock extends Block implements EntityBlock {
    @Override
    public void stepOn(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide && !entity.isSpectator() && entity instanceof net.minecraft.world.entity.LivingEntity
                && level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity port) {
            var crucible = port.crucibleController();
            if (crucible != null)
                com.gregtech.gregtech.util.GTEntityHelper.applyTemperatureDamage(entity, crucible.getCrucibleTemperature());
        }
        super.stepOn(level, pos, state, entity);
    }
    @Override public com.mojang.serialization.MapCodec<MultiblockPortBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.item.Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flag) {
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this);
        if (com.gregtech.gregtech.content.multiblock.OriginalMultiblockPartData.byPath(id.getPath()).isEmpty()) return;
        for (var key : com.gregtech.gregtech.content.multiblock.OriginalMultiblockPartData.TOOLTIP_KEYS)
            if (!com.gregtech.gregtech.client.CommonBlockTooltips.containsKey(lines, key))
                lines.add(net.minecraft.network.chat.Component.translatable(key).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CRUCIBLE_FORMED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("crucible_formed");
    @Override public int getFlammability(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.core.Direction side) {
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this);
        return com.gregtech.gregtech.content.multiblock.OriginalMultiblockPartData.byPath(id.getPath())
                .map(com.gregtech.gregtech.content.multiblock.OriginalMultiblockPartData.Part::flammability).orElse(0);
    }
    @Override public int getFireSpreadSpeed(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.core.Direction side) { return getFlammability(state,level,pos,side); }
    public MultiblockPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRUCIBLE_FORMED, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRUCIBLE_FORMED);
    }
    @Override public RenderShape getRenderShape(BlockState state) {
        return state.getValue(CRUCIBLE_FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(CRUCIBLE_FORMED) ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }
    @Override public int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(CRUCIBLE_FORMED) ? 0 : super.getLightBlock(state, level, pos);
    }
    private net.minecraft.world.InteractionResult interact(BlockState state, net.minecraft.world.level.Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity part)
            return com.gregtech.gregtech.content.logistics.LogisticsCoverInteraction.use(
                    part, level, player, hand, hit.getDirection());
        return net.minecraft.world.InteractionResult.PASS;
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return interact(state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
    @Override public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity part)
            part.logisticsCovers().dropAll();
        super.onRemove(state, level, pos, next, moving);
    }
    /** Structural bindings are transient; breaking a part returns only the part itself. */
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        return java.util.List.of(new net.minecraft.world.item.ItemStack(this));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity(pos,state);
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level,
                                   BlockPos pos, net.minecraft.core.Direction side) {
        return com.gregtech.gregtech.content.logistics.LogisticsCoverSignals.at(level, pos, side.getOpposite());
    }
    @Override public int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level,
                                         BlockPos pos, net.minecraft.core.Direction side) {
        return getSignal(state, level, pos, side);
    }
}
