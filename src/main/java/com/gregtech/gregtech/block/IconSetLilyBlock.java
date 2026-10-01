package com.gregtech.gregtech.block;

import net.minecraft.world.level.block.WaterlilyBlock;

/** Lily-pad style iconset block — flat, placeable on water only (GT6 hexalily). */
public class IconSetLilyBlock extends WaterlilyBlock {
    private final String iconName;

    public IconSetLilyBlock(Properties properties, String iconName) {
        super(properties);
        this.iconName = iconName;
    }

    public String iconName() { return iconName; }
}
