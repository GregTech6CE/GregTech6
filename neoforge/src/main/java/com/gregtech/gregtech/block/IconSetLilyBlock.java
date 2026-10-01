package com.gregtech.gregtech.block;

import net.minecraft.world.level.block.WaterlilyBlock;

/** Lily-pad style iconset block — flat, placeable on water only (GT6 hexalily). */
public class IconSetLilyBlock extends WaterlilyBlock {
    @Override public com.mojang.serialization.MapCodec<WaterlilyBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    private final String iconName;

    public IconSetLilyBlock(Properties properties, String iconName) {
        super(properties);
        this.iconName = iconName;
    }

    public String iconName() { return iconName; }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return java.util.List.of(new net.minecraft.world.item.ItemStack(this));}
}
