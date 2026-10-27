package gregtech.client;

import gregtech.engine.fluid.GTWaterParity;
import gregtech.engine.mod.GregTechIdentity;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.function.BiConsumer;

/** Exposes vanilla water tint to maps and other consumers of block models. */
public final class WorldWaterBlockColors {
    private WorldWaterBlockColors() {}

    public static void register(BiConsumer<BlockColor, Block[]> registrar) {
        BlockColor color = (state, level, pos, tintIndex) -> level != null && pos != null
                ? BiomeColors.getAverageWaterColor(level, pos)
                : GTWaterParity.VANILLA_WATER_TINT & 0xFFFFFF;
        Block[] blocks = BuiltInRegistries.BLOCK.stream().filter(block -> {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            return GregTechIdentity.MOD_ID.equals(id.getNamespace())
                    && GTWaterParity.WORLD_WATER_PATHS.contains(id.getPath());
        }).toArray(Block[]::new);
        registrar.accept(color, blocks);
    }
}
