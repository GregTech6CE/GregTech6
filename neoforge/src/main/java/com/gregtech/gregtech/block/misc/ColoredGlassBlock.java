package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** GT6's {@code BlockGlassClear}/{@code BlockGlassGlow} coloured metadata family. */
public final class ColoredGlassBlock extends TransparentBlock {
    @Override protected com.mojang.serialization.MapCodec<? extends TransparentBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    private final boolean glow;

    public ColoredGlassBlock(boolean glow, Properties properties) {
        super(properties);
        this.glow = glow;
        registerDefaultState(stateDefinition.any().setValue(COLOR, DyeColor.LIGHT_GRAY));
    }

    public boolean glow() { return glow; }

    @Override public boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction side) {
        if (adjacent.is(this)) return state.getValue(COLOR) == adjacent.getValue(COLOR);
        return super.skipRendering(state, adjacent, side);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    public static ItemStack coloredItem(Block block, DyeColor color) {
        ItemStack stack = new ItemStack(block);
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,new net.minecraft.world.item.component.BlockItemStateProperties(java.util.Map.of("color",color.getName())));
        return stack;
    }

    public static DyeColor itemColor(ItemStack stack) {
        var data=stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        return data==null?DyeColor.LIGHT_GRAY:DyeColor.byName(data.properties().getOrDefault("color","light_gray"),DyeColor.LIGHT_GRAY);
    }

    @Override public ItemStack getCloneItemStack(BlockState state,net.minecraft.world.phys.HitResult hit,net.minecraft.world.level.LevelReader level,BlockPos pos,net.minecraft.world.entity.player.Player player) {
        return coloredItem(this, state.getValue(COLOR));
    }

    /** GT6: breaking either full glass block returns 80 glass scraps, not the pane itself. */
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack full = GTItems.getStack(MaterialPrefix.scrapGt, Materials.Glass, 64);
        ItemStack rest = GTItems.getStack(MaterialPrefix.scrapGt, Materials.Glass, 16);
        if (full.isEmpty() || rest.isEmpty()) return List.of();
        return List.of(full, rest);
    }

    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 0; }
}
