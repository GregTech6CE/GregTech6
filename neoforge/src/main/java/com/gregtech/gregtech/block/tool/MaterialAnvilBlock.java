package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;
import java.util.function.Supplier;

/** Material identity belongs to registration; contents and wear belong to the placed anvil. */
public final class MaterialAnvilBlock extends ShapedToolBlock implements EntityBlock {
    private final Supplier<GTMaterial> material;
    private final long durability;
    public MaterialAnvilBlock(Supplier<GTMaterial> material, long durability, Properties properties) {
        super("anvil", properties); this.material = material; this.durability = durability;
    }
    public GTMaterial material() { return material.get(); }
    public long durability() { return durability; }
    @Override public int tintRgb() { return material().getColor(); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MaterialAnvilBlockEntity(pos, state); }
    @Override public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        var rotation = super.interact(state, level, pos, player, hand, hit);
        if (rotation.consumesAction()) return rotation;
        if (hit.getDirection() == net.minecraft.core.Direction.DOWN || hit.getLocation().y - pos.getY() < .25) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MaterialAnvilBlockEntity anvil) anvil.interact(player, hand, hit);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private ItemStack packed(BlockEntity entity,net.minecraft.core.HolderLookup.Provider lookup) {
        ItemStack stack = new ItemStack(this);
        if (entity instanceof MaterialAnvilBlockEntity anvil) {
            var data = com.gregtech.gregtech.content.cover.ComponentCoverFallback.forItem(anvil,anvil.saveWithId(lookup));
            data.remove("gt.work0"); data.remove("gt.work1");
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(data));
        }
        return stack;
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(packed(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY),builder.getLevel().registryAccess()));
    }
    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,LevelReader level,BlockPos pos,Player player) { return packed(level.getBlockEntity(pos),level.registryAccess()); }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        long remaining = durability;
        var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(data.contains("gt.durability"))remaining=Math.max(0,data.getLong("gt.durability"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.anvil.durability", (remaining + 9999) / 10000));
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MaterialAnvilBlockEntity anvil) anvil.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
}
