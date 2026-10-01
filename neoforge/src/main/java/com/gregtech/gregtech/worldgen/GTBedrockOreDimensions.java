package com.gregtech.gregtech.worldgen;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Forge dimension boundary for the shared, unchanged bedrock-ore tables. */
public final class GTBedrockOreDimensions {
    private GTBedrockOreDimensions() {}

    public static List<GTBedrockOres.BedrockOre> forDimension(ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER) return GTBedrockOres.NETHER;
        if (dimension == Level.OVERWORLD) return GTBedrockOres.OVERWORLD;
        return List.of();
    }
}
