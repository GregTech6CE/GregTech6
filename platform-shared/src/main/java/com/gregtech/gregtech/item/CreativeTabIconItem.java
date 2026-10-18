package com.gregtech.gregtech.item;

import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;

/** Placeholder item whose client model is mapped to {@code material_icons/none/} shared icons. */
public final class CreativeTabIconItem extends Item {
    @Nullable
    private final MaterialPrefix materialPrefix;
    @Nullable
    private final BlockMaterialPrefix blockPrefix;

    public CreativeTabIconItem(Properties properties, @Nullable MaterialPrefix materialPrefix,
                               @Nullable BlockMaterialPrefix blockPrefix) {
        super(properties);
        this.materialPrefix = materialPrefix;
        this.blockPrefix = blockPrefix;
    }

    @Nullable
    public MaterialPrefix materialPrefix() {
        return materialPrefix;
    }

    @Nullable
    public BlockMaterialPrefix blockPrefix() {
        return blockPrefix;
    }
}
