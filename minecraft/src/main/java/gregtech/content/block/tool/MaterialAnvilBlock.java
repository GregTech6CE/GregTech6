package gregtech.content.block.tool;

import gregtech.api.material.GTMaterial;
import gregtech.content.blockentity.tool.MaterialAnvilBlockEntity;
import gregtech.engine.GTWaterloggable;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Material identity belongs to registration; contents and wear belong to the placed anvil. */
public final class MaterialAnvilBlock extends ShapedToolBlock implements EntityBlock, SimpleWaterloggedBlock {
    private final Supplier<GTMaterial> material;
    private final long durability;

    public MaterialAnvilBlock(Supplier<GTMaterial> material, long durability, Properties properties) {
        super("anvil", properties);
        registerDefaultState(defaultBlockState().setValue(GTWaterloggable.WATERLOGGED, false));
        this.material = material;
        this.durability = durability;
    }

    public GTMaterial material() {
        return material.get();
    }

    public long durability() {
        return durability;
    }

    @Override
    public int tintRgb() {
        return material().getColor();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaterialAnvilBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        var rotation = super.useBlock(state, level, pos, player, hand, hit);
        if (rotation != null && rotation.consumesAction()) return rotation;
        if (hit.getDirection() == net.minecraft.core.Direction.DOWN || hit.getLocation().y - pos.getY() < .25)
            return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MaterialAnvilBlockEntity anvil)
            anvil.interact(player, hand, hit);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private ItemStack packed(BlockEntity entity) {
        ItemStack stack = new ItemStack(this);
        if (entity instanceof MaterialAnvilBlockEntity anvil) {
            var data = gregtech.content.cover.ComponentCoverFallback.forItem(
                    anvil,
                    gregtech.engine.stack.NbtCompat.saveForBlockItem(
                            anvil, anvil.getLevel() != null ? anvil.getLevel().registryAccess() : null));
            data.remove("gt.work0");
            data.remove("gt.work1");
            gregtech.engine.stack.BlockEntityData.write(stack, data);
        }
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(packed(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));
    }

    @Override
    protected ItemStack onGetCloneItemStack(
            BlockState state, @Nullable HitResult target, BlockGetter level, BlockPos pos, @Nullable Player player) {
        return packed(level.getBlockEntity(pos));
    }

    @Override
    protected void appendTooltip(
            ItemStack stack,
            BlockGetter level,
            List<net.minecraft.network.chat.Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        long remaining = durability;
        var data = gregtech.engine.stack.BlockEntityData.readOrEmpty(stack);
        if (data.contains("gt.durability")) remaining = Math.max(0, data.getLong("gt.durability"));
        tooltip.add(gregtech.client.TooltipHelper.stat(
                "gt.tooltip.anvil.durability", net.minecraft.ChatFormatting.AQUA, (remaining + 9999) / 10000));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MaterialAnvilBlockEntity anvil) anvil.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return GTWaterloggable.getStateForPlacement(super.getStateForPlacement(context), context);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }
}
