package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

/** GT6 BlockColored metadata palette; colour survives picking, drops and vanilla placement. */
public class ColoredConstructionBlock extends Block {
    public static final EnumProperty<DyeColor> COLOR = ConcreteBlock.COLOR;
    public ColoredConstructionBlock(Properties properties, DyeColor color) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COLOR, color));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(COLOR); }
    public static DyeColor itemColor(ItemStack stack) {
        DyeColor fallback = stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().hasProperty(COLOR)
                ? item.getBlock().defaultBlockState().getValue(COLOR) : DyeColor.WHITE;
        var data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        return data == null ? fallback : DyeColor.byName(data.properties().get("color"), fallback);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(ConcreteBlock.coloredItem(this, state.getValue(COLOR)));
    }
    @Override public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit, net.minecraft.world.level.LevelReader level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        return ConcreteBlock.coloredItem(this, state.getValue(COLOR));
    }
}
