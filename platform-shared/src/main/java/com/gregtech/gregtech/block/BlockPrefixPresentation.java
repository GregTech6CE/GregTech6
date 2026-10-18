package com.gregtech.gregtech.block;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
/** Minecraft presentation boundary for the shared original prefix fields. */
public final class BlockPrefixPresentation {
    private BlockPrefixPresentation() {}
    public static MapColor mapColor(BlockMaterialPrefix prefix) {
        return switch (prefix.mapColor()) { case METAL -> MapColor.METAL; case STONE -> MapColor.STONE;
            case WOOD -> MapColor.WOOD; case SAND -> MapColor.SAND; };
    }
    public static SoundType soundType(BlockMaterialPrefix prefix) {
        return switch (prefix.soundType()) { case METAL -> SoundType.METAL; case STONE -> SoundType.STONE;
            case WOOD -> SoundType.WOOD; case SAND -> SoundType.SAND; case GRAVEL -> SoundType.GRAVEL; };
    }
}
